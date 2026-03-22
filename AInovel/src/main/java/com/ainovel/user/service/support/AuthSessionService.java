package com.ainovel.user.service.support;

import com.ainovel.cache.key.CacheKeyFactory;
import com.ainovel.security.auth.context.CurrentUser;
import com.ainovel.security.auth.token.AccessTokenClaims;
import com.ainovel.security.auth.token.JwtTokenProvider;
import com.ainovel.security.config.SecurityProperties;
import com.ainovel.user.domain.UserAccount;
import com.ainovel.user.mapper.UserAccountMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class AuthSessionService {

    private static final Duration LOGIN_VERSION_CACHE_TTL = Duration.ofDays(30);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final SecurityProperties securityProperties;
    private final JwtTokenProvider jwtTokenProvider;
    private final CacheKeyFactory cacheKeyFactory;
    private final UserAccountMapper userAccountMapper;

    public AuthSessionService(StringRedisTemplate redisTemplate,
                              ObjectMapper objectMapper,
                              SecurityProperties securityProperties,
                              JwtTokenProvider jwtTokenProvider,
                              CacheKeyFactory cacheKeyFactory,
                              UserAccountMapper userAccountMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.securityProperties = securityProperties;
        this.jwtTokenProvider = jwtTokenProvider;
        this.cacheKeyFactory = cacheKeyFactory;
        this.userAccountMapper = userAccountMapper;
    }

    public com.ainovel.user.vo.AccessTokenVO issueTokens(UserAccount account, String deviceId) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(securityProperties.accessTokenTtl());
        String jti = UUID.randomUUID().toString();
        String refreshToken = UUID.randomUUID().toString().replace("-", "");
        AuthRefreshSession refreshSession =
                new AuthRefreshSession(account.id(), account.loginVersion(), jti, normalizeDeviceId(deviceId));

        String accessToken = jwtTokenProvider.issueAccessToken(
                account.id(),
                com.ainovel.persistence.support.DelimitedValueCodec.toRoleTypes(account.roles()),
                account.loginVersion(),
                jti,
                now,
                expiresAt);

        saveRefreshSession(refreshToken, refreshSession, securityProperties.refreshTokenTtl());
        cacheLoginVersion(account.id(), account.loginVersion());
        return new com.ainovel.user.vo.AccessTokenVO(
                accessToken,
                refreshToken,
                LocalDateTime.ofInstant(expiresAt, ZoneId.systemDefault()),
                account.loginVersion());
    }

    public Optional<AuthRefreshSession> getRefreshSession(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return Optional.empty();
        }
        String raw = redisTemplate.opsForValue().get(cacheKeyFactory.authRefreshToken(refreshToken));
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(raw, AuthRefreshSession.class));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("failed to parse refresh session", ex);
        }
    }

    public com.ainovel.user.vo.AccessTokenVO rotateTokens(String refreshToken, UserAccount account, String deviceId) {
        getRefreshSession(refreshToken).ifPresent(session ->
                revokeAccessToken(session.jti(), Instant.now().plus(securityProperties.accessTokenTtl())));
        revokeRefreshToken(refreshToken);
        return issueTokens(account, deviceId);
    }

    public void revokeCurrentSession(CurrentUser currentUser) {
        if (currentUser == null) {
            return;
        }
        revokeAccessToken(currentUser.tokenId(), currentUser.expiresAt());
        revokeRefreshTokenByJti(currentUser.tokenId());
    }

    public boolean isAccessTokenActive(AccessTokenClaims claims) {
        if (claims == null || claims.subject() == null || claims.subject().isBlank()) {
            return false;
        }
        if (claims.jti() != null && Boolean.TRUE.equals(redisTemplate.hasKey(cacheKeyFactory.authTokenBlacklist(claims.jti())))) {
            return false;
        }
        Long userId = Long.valueOf(claims.subject());
        long currentVersion = resolveLoginVersion(userId);
        return currentVersion > 0 && currentVersion == claims.tokenVersion();
    }

    public long resolveLoginVersion(Long userId) {
        if (userId == null) {
            return 0L;
        }
        String cacheKey = cacheKeyFactory.authLoginVersion(userId);
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null && !cached.isBlank()) {
            return Long.parseLong(cached);
        }
        Long version = userAccountMapper.findLoginVersion(userId);
        if (version == null) {
            return 0L;
        }
        cacheLoginVersion(userId, version);
        return version;
    }

    public void cacheLoginVersion(Long userId, long loginVersion) {
        redisTemplate.opsForValue().set(
                cacheKeyFactory.authLoginVersion(userId),
                String.valueOf(loginVersion),
                LOGIN_VERSION_CACHE_TTL);
    }

    public void evictLoginVersion(Long userId) {
        redisTemplate.delete(cacheKeyFactory.authLoginVersion(userId));
    }

    public void revokeRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        String sessionKey = cacheKeyFactory.authRefreshToken(refreshToken);
        String raw = redisTemplate.opsForValue().get(sessionKey);
        redisTemplate.delete(sessionKey);
        if (raw == null || raw.isBlank()) {
            return;
        }
        try {
            AuthRefreshSession session = objectMapper.readValue(raw, AuthRefreshSession.class);
            if (session.jti() != null && !session.jti().isBlank()) {
                redisTemplate.delete(cacheKeyFactory.authRefreshTokenJti(session.jti()));
            }
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("failed to parse refresh session", ex);
        }
    }

    public boolean matchesDevice(AuthRefreshSession session, String requestDeviceId) {
        if (session == null) {
            return false;
        }
        // Keep backward compatibility for clients that do not send deviceId during refresh.
        if (requestDeviceId == null || requestDeviceId.isBlank()) {
            return true;
        }
        String sessionDevice = normalizeDeviceId(session.deviceId());
        String currentDevice = normalizeDeviceId(requestDeviceId);
        return sessionDevice.equals(currentDevice);
    }

    private void saveRefreshSession(String refreshToken, AuthRefreshSession session, Duration ttl) {
        try {
            String serialized = objectMapper.writeValueAsString(session);
            redisTemplate.opsForValue().set(cacheKeyFactory.authRefreshToken(refreshToken), serialized, ttl);
            redisTemplate.opsForValue().set(cacheKeyFactory.authRefreshTokenJti(session.jti()), refreshToken, ttl);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("failed to serialize refresh session", ex);
        }
    }

    private void revokeAccessToken(String jti, Instant expiresAt) {
        if (jti == null || jti.isBlank() || expiresAt == null) {
            return;
        }
        Duration ttl = Duration.between(Instant.now(), expiresAt);
        if (ttl.isNegative() || ttl.isZero()) {
            return;
        }
        redisTemplate.opsForValue().set(cacheKeyFactory.authTokenBlacklist(jti), "1", ttl);
    }

    private void revokeRefreshTokenByJti(String jti) {
        if (jti == null || jti.isBlank()) {
            return;
        }
        String reverseKey = cacheKeyFactory.authRefreshTokenJti(jti);
        String refreshToken = redisTemplate.opsForValue().get(reverseKey);
        redisTemplate.delete(reverseKey);
        if (refreshToken != null && !refreshToken.isBlank()) {
            redisTemplate.delete(cacheKeyFactory.authRefreshToken(refreshToken));
        }
    }

    private String normalizeDeviceId(String deviceId) {
        return deviceId == null || deviceId.isBlank() ? "unknown" : deviceId.trim();
    }
}
