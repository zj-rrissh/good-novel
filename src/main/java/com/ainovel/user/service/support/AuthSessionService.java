package com.ainovel.user.service.support;

import com.ainovel.cache.key.CacheKeyFactory;
import com.ainovel.common.api.StandardErrorCode;
import com.ainovel.infrastructure.exception.BusinessException;
import com.ainovel.infrastructure.log.AuditAction;
import com.ainovel.infrastructure.log.TraceIdHolder;
import com.ainovel.security.audit.SecurityAuditEvent;
import com.ainovel.security.audit.SecurityAuditService;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class AuthSessionService {

    private static final Duration LOGIN_VERSION_CACHE_TTL = Duration.ofDays(30);
    private static final Logger log = LoggerFactory.getLogger(AuthSessionService.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final SecurityProperties securityProperties;
    private final JwtTokenProvider jwtTokenProvider;
    private final CacheKeyFactory cacheKeyFactory;
    private final UserAccountMapper userAccountMapper;
    private final SecurityAuditService securityAuditService;

    public AuthSessionService(StringRedisTemplate redisTemplate,
                              ObjectMapper objectMapper,
                              SecurityProperties securityProperties,
                              JwtTokenProvider jwtTokenProvider,
                              CacheKeyFactory cacheKeyFactory,
                              UserAccountMapper userAccountMapper,
                              SecurityAuditService securityAuditService) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.securityProperties = securityProperties;
        this.jwtTokenProvider = jwtTokenProvider;
        this.cacheKeyFactory = cacheKeyFactory;
        this.userAccountMapper = userAccountMapper;
        this.securityAuditService = securityAuditService;
    }

    public com.ainovel.user.vo.AccessTokenVO issueTokens(UserAccount account, String deviceId) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(securityProperties.accessTokenTtl());
        String jti = UUID.randomUUID().toString();
        String refreshToken = UUID.randomUUID().toString().replace("-", "");
        String normalizedDeviceId = normalizeDeviceId(deviceId);
        AuthRefreshSession refreshSession =
                new AuthRefreshSession(account.id(), account.loginVersion(), jti, normalizedDeviceId);
        AuthAccessSession accessSession =
                new AuthAccessSession(account.id(), account.loginVersion(), normalizedDeviceId);

        String accessToken = jwtTokenProvider.issueAccessToken(
                account.id(),
                com.ainovel.persistence.support.DelimitedValueCodec.toRoleTypes(account.roles()),
                account.loginVersion(),
                jti,
                now,
                expiresAt);

        persistAuthSession(jti, accessSession, refreshToken, refreshSession);
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
        String raw;
        try {
            raw = redisTemplate.opsForValue().get(cacheKeyFactory.authRefreshToken(refreshToken));
        } catch (RuntimeException ex) {
            audit(AuditAction.AUTH_REDIS_FAILURE, null, null, "refresh_session_lookup_failed");
            throw new BusinessException(StandardErrorCode.DEPENDENCY_UNAVAILABLE, "redis auth session unavailable");
        }
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(raw, AuthRefreshSession.class));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("failed to parse refresh session", ex);
        }
    }

    public Optional<AuthAccessSession> getAccessSession(String jti) {
        if (jti == null || jti.isBlank()) {
            return Optional.empty();
        }
        String raw;
        try {
            raw = redisTemplate.opsForValue().get(cacheKeyFactory.authAccessToken(jti));
        } catch (RuntimeException ex) {
            log.debug("Redis GET access session failed, treat as absent. jti={}", jti, ex);
            return Optional.empty();
        }
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(raw, AuthAccessSession.class));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("failed to parse access session", ex);
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
        if (isAccessTokenBlacklisted(claims.jti())) {
            return false;
        }
        Long userId = Long.valueOf(claims.subject());
        AuthAccessSession accessSession = getAccessSession(claims.jti()).orElse(null);
        if (accessSession == null || !userId.equals(accessSession.userId())
                || accessSession.loginVersion() != claims.tokenVersion()) {
            return false;
        }
        long currentVersion = resolveLoginVersion(userId);
        return currentVersion > 0 && currentVersion == claims.tokenVersion();
    }

    public long resolveLoginVersion(Long userId) {
        if (userId == null) {
            return 0L;
        }
        String cacheKey = cacheKeyFactory.authLoginVersion(userId);
        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null && !cached.isBlank()) {
                return Long.parseLong(cached);
            }
        } catch (RuntimeException ex) {
            log.debug("Redis GET login version failed, fallback to DB. userId={}", userId, ex);
        }
        Long version = userAccountMapper.findLoginVersion(userId);
        if (version == null) {
            return 0L;
        }
        cacheLoginVersion(userId, version);
        return version;
    }

    public void cacheLoginVersion(Long userId, long loginVersion) {
        try {
            redisTemplate.opsForValue().set(
                    cacheKeyFactory.authLoginVersion(userId),
                    String.valueOf(loginVersion),
                    LOGIN_VERSION_CACHE_TTL);
        } catch (RuntimeException ex) {
            log.debug("Redis SET login version failed, skip cache write. userId={}", userId, ex);
        }
    }

    public void evictLoginVersion(Long userId) {
        try {
            redisTemplate.delete(cacheKeyFactory.authLoginVersion(userId));
        } catch (RuntimeException ex) {
            log.debug("Redis DEL login version failed, skip delete. userId={}", userId, ex);
        }
    }

    public void revokeRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        String sessionKey = cacheKeyFactory.authRefreshToken(refreshToken);
        String raw;
        try {
            raw = redisTemplate.opsForValue().get(sessionKey);
            redisTemplate.delete(sessionKey);
        } catch (RuntimeException ex) {
            audit(AuditAction.AUTH_REDIS_FAILURE, null, null, "revoke_refresh_token_failed");
            throw new BusinessException(StandardErrorCode.DEPENDENCY_UNAVAILABLE, "redis auth session unavailable");
        }
        if (raw == null || raw.isBlank()) {
            return;
        }
        try {
            AuthRefreshSession session = objectMapper.readValue(raw, AuthRefreshSession.class);
            if (session.jti() != null && !session.jti().isBlank()) {
                try {
                    redisTemplate.delete(cacheKeyFactory.authRefreshTokenJti(session.jti()));
                } catch (RuntimeException ex) {
                    audit(AuditAction.AUTH_REDIS_FAILURE, session.userId(), null, "delete_refresh_token_jti_failed");
                    throw new BusinessException(StandardErrorCode.DEPENDENCY_UNAVAILABLE, "redis auth session unavailable");
                }
            }
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("failed to parse refresh session", ex);
        }
    }

    public boolean matchesDevice(AuthRefreshSession session, String requestDeviceId) {
        if (session == null) {
            return false;
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
        } catch (RuntimeException ex) {
            throw new BusinessException(StandardErrorCode.DEPENDENCY_UNAVAILABLE, "redis auth session unavailable");
        }
    }

    private void saveAccessSession(String jti, AuthAccessSession session, Duration ttl) {
        try {
            String serialized = objectMapper.writeValueAsString(session);
            redisTemplate.opsForValue().set(cacheKeyFactory.authAccessToken(jti), serialized, ttl);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("failed to serialize access session", ex);
        } catch (RuntimeException ex) {
            throw new BusinessException(StandardErrorCode.DEPENDENCY_UNAVAILABLE, "redis auth session unavailable");
        }
    }

    private void persistAuthSession(String jti,
                                    AuthAccessSession accessSession,
                                    String refreshToken,
                                    AuthRefreshSession refreshSession) {
        try {
            saveAccessSession(jti, accessSession, securityProperties.accessTokenTtl());
            saveRefreshSession(refreshToken, refreshSession, securityProperties.refreshTokenTtl());
        } catch (RuntimeException ex) {
            cleanupAuthSessionWrite(jti, refreshToken, refreshSession.jti());
            throw ex;
        }
    }

    private void revokeAccessToken(String jti, Instant expiresAt) {
        if (jti == null || jti.isBlank() || expiresAt == null) {
            return;
        }
        try {
            redisTemplate.delete(cacheKeyFactory.authAccessToken(jti));
        } catch (RuntimeException ex) {
            audit(AuditAction.AUTH_REDIS_FAILURE, null, null, "delete_access_session_failed");
            throw new BusinessException(StandardErrorCode.DEPENDENCY_UNAVAILABLE, "redis auth session unavailable");
        }
        Duration ttl = Duration.between(Instant.now(), expiresAt);
        if (ttl.isNegative() || ttl.isZero()) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(cacheKeyFactory.authTokenBlacklist(jti), "1", ttl);
        } catch (RuntimeException ex) {
            audit(AuditAction.AUTH_REDIS_FAILURE, null, null, "write_token_blacklist_failed");
            throw new BusinessException(StandardErrorCode.DEPENDENCY_UNAVAILABLE, "redis auth session unavailable");
        }
    }

    private void revokeRefreshTokenByJti(String jti) {
        if (jti == null || jti.isBlank()) {
            return;
        }
        String reverseKey = cacheKeyFactory.authRefreshTokenJti(jti);
        String refreshToken;
        try {
            refreshToken = redisTemplate.opsForValue().get(reverseKey);
            redisTemplate.delete(reverseKey);
        } catch (RuntimeException ex) {
            audit(AuditAction.AUTH_REDIS_FAILURE, null, null, "revoke_refresh_token_by_jti_failed");
            throw new BusinessException(StandardErrorCode.DEPENDENCY_UNAVAILABLE, "redis auth session unavailable");
        }
        if (refreshToken != null && !refreshToken.isBlank()) {
            try {
                redisTemplate.delete(cacheKeyFactory.authRefreshToken(refreshToken));
            } catch (RuntimeException ex) {
                audit(AuditAction.AUTH_REDIS_FAILURE, null, null, "delete_refresh_token_failed");
                throw new BusinessException(StandardErrorCode.DEPENDENCY_UNAVAILABLE, "redis auth session unavailable");
            }
        }
    }

    private boolean isAccessTokenBlacklisted(String jti) {
        if (jti == null || jti.isBlank()) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(cacheKeyFactory.authTokenBlacklist(jti)));
        } catch (RuntimeException ex) {
            audit(AuditAction.AUTH_REDIS_FAILURE, null, null, "check_token_blacklist_failed");
            return true;
        }
    }

    private void cleanupAuthSessionWrite(String accessTokenJti, String refreshToken, String refreshTokenJti) {
        deleteQuietly(cacheKeyFactory.authAccessToken(accessTokenJti), "access session", accessTokenJti);
        deleteQuietly(cacheKeyFactory.authRefreshToken(refreshToken), "refresh session", refreshToken);
        deleteQuietly(cacheKeyFactory.authRefreshTokenJti(refreshTokenJti), "refresh session reverse index", refreshTokenJti);
    }

    private void deleteQuietly(String key, String label, String traceValue) {
        try {
            redisTemplate.delete(key);
        } catch (RuntimeException cleanupEx) {
            log.debug("Redis cleanup {} failed. trace={}", label, traceValue, cleanupEx);
        }
    }

    private String normalizeDeviceId(String deviceId) {
        return deviceId == null || deviceId.isBlank() ? "unknown" : deviceId.trim();
    }

    private void audit(AuditAction action, Long userId, String deviceId, String detail) {
        securityAuditService.record(new SecurityAuditEvent(
                action,
                userId,
                deviceId,
                "",
                "",
                TraceIdHolder.get().orElse(""),
                detail));
    }
}
