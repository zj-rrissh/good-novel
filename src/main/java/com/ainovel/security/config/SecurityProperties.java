package com.ainovel.security.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ainovel.security")
public record SecurityProperties(
        List<String> whitelistPaths,
        Duration accessTokenTtl,
        Duration refreshTokenTtl,
        String jwtIssuer,
        String jwtSecret,
        boolean riskControlEnabled,
        boolean rateLimitEnabled,
        int rateLimitAuthThreshold,
        int rateLimitAuthWindowSeconds,
        int rateLimitCommentThreshold,
        int rateLimitCommentWindowSeconds,
        int rateLimitReadThreshold,
        int rateLimitReadWindowSeconds,
        int riskAuthThreshold,
        int riskAuthWindowSeconds,
        int riskCommentThreshold,
        int riskCommentWindowSeconds,
        int riskReadThreshold,
        int riskReadWindowSeconds) {

    public SecurityProperties {
        whitelistPaths = whitelistPaths == null || whitelistPaths.isEmpty()
                ? List.of("/api/v1/auth/login", "/api/v1/auth/register", "/actuator/health")
                : List.copyOf(whitelistPaths);
        accessTokenTtl = accessTokenTtl == null ? Duration.ofHours(2) : accessTokenTtl;
        refreshTokenTtl = refreshTokenTtl == null ? Duration.ofDays(7) : refreshTokenTtl;
        jwtIssuer = jwtIssuer == null || jwtIssuer.isBlank() ? "ainovel" : jwtIssuer;
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException("ainovel.security.jwt-secret must be configured");
        }
        jwtSecret = jwtSecret.trim();
        rateLimitAuthThreshold = positiveOrDefault(rateLimitAuthThreshold, 20);
        rateLimitAuthWindowSeconds = positiveOrDefault(rateLimitAuthWindowSeconds, 60);
        rateLimitCommentThreshold = positiveOrDefault(rateLimitCommentThreshold, 10);
        rateLimitCommentWindowSeconds = positiveOrDefault(rateLimitCommentWindowSeconds, 60);
        rateLimitReadThreshold = positiveOrDefault(rateLimitReadThreshold, 120);
        rateLimitReadWindowSeconds = positiveOrDefault(rateLimitReadWindowSeconds, 60);
        riskAuthThreshold = positiveOrDefault(riskAuthThreshold, 40);
        riskAuthWindowSeconds = positiveOrDefault(riskAuthWindowSeconds, 300);
        riskCommentThreshold = positiveOrDefault(riskCommentThreshold, 20);
        riskCommentWindowSeconds = positiveOrDefault(riskCommentWindowSeconds, 300);
        riskReadThreshold = positiveOrDefault(riskReadThreshold, 200);
        riskReadWindowSeconds = positiveOrDefault(riskReadWindowSeconds, 300);
    }

    private static int positiveOrDefault(int value, int defaultValue) {
        return value > 0 ? value : defaultValue;
    }
}
