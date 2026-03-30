package com.ainovel.user.service.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.ainovel.cache.key.CacheKeyFactory;
import com.ainovel.common.api.StandardErrorCode;
import com.ainovel.infrastructure.exception.BusinessException;
import com.ainovel.security.auth.context.CurrentUser;
import com.ainovel.security.auth.rbac.RoleType;
import com.ainovel.security.auth.token.JwtTokenProvider;
import com.ainovel.security.audit.SecurityAuditService;
import com.ainovel.security.config.SecurityProperties;
import com.ainovel.user.mapper.UserAccountMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class AuthSessionServiceFailClosedTests {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private AuthSessionService authSessionService;

    @BeforeEach
    void setUp() {
        redisTemplate = Mockito.mock(StringRedisTemplate.class);
        valueOperations = Mockito.mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        CacheKeyFactory cacheKeyFactory = Mockito.mock(CacheKeyFactory.class);
        when(cacheKeyFactory.authRefreshToken(anyString())).thenAnswer(invocation -> "auth:refresh:" + invocation.getArgument(0));
        when(cacheKeyFactory.authRefreshTokenJti(anyString())).thenAnswer(invocation -> "auth:refresh:jti:" + invocation.getArgument(0));
        when(cacheKeyFactory.authAccessToken(anyString())).thenAnswer(invocation -> "auth:access:" + invocation.getArgument(0));
        when(cacheKeyFactory.authTokenBlacklist(anyString())).thenAnswer(invocation -> "auth:blacklist:" + invocation.getArgument(0));
        when(cacheKeyFactory.authLoginVersion(any())).thenAnswer(invocation -> "auth:login-version:" + invocation.getArgument(0));

        authSessionService = new AuthSessionService(
                redisTemplate,
                new ObjectMapper(),
                new SecurityProperties(
                        List.of("/api/v1/auth/login"),
                        Duration.ofHours(2),
                        Duration.ofDays(7),
                        "ainovel",
                        "test-jwt-secret",
                        true,
                        true,
                        20,
                        60,
                        10,
                        60,
                        120,
                        60,
                        40,
                        300,
                        20,
                        300,
                        200,
                        300),
                Mockito.mock(JwtTokenProvider.class),
                cacheKeyFactory,
                Mockito.mock(UserAccountMapper.class),
                Mockito.mock(SecurityAuditService.class));
    }

    @Test
    void shouldFailClosedWhenRefreshSessionLookupRedisFails() {
        when(valueOperations.get(anyString())).thenThrow(new RuntimeException("redis down"));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authSessionService.getRefreshSession("refresh-token"));

        assertEquals(StandardErrorCode.DEPENDENCY_UNAVAILABLE, exception.getErrorCode());
    }

    @Test
    void shouldFailClosedWhenRevokingRefreshTokenRedisFails() {
        when(valueOperations.get(anyString())).thenThrow(new RuntimeException("redis down"));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authSessionService.revokeRefreshToken("refresh-token"));

        assertEquals(StandardErrorCode.DEPENDENCY_UNAVAILABLE, exception.getErrorCode());
    }

    @Test
    void shouldFailClosedWhenRevokingCurrentSessionRedisFails() {
        when(redisTemplate.delete(anyString())).thenThrow(new RuntimeException("redis down"));

        CurrentUser currentUser = new CurrentUser(
                1L,
                Set.of(RoleType.USER),
                "device-1",
                true,
                "jti-1",
                1L,
                Instant.now().plusSeconds(300));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authSessionService.revokeCurrentSession(currentUser));

        assertEquals(StandardErrorCode.DEPENDENCY_UNAVAILABLE, exception.getErrorCode());
    }
}
