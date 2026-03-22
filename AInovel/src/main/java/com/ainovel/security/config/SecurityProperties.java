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
        boolean riskControlEnabled) {

    public SecurityProperties {
        whitelistPaths = whitelistPaths == null || whitelistPaths.isEmpty()
                ? List.of("/api/v1/auth/login", "/api/v1/auth/register", "/actuator/health")
                : List.copyOf(whitelistPaths);
        accessTokenTtl = accessTokenTtl == null ? Duration.ofHours(2) : accessTokenTtl;
        refreshTokenTtl = refreshTokenTtl == null ? Duration.ofDays(7) : refreshTokenTtl;
        jwtIssuer = jwtIssuer == null || jwtIssuer.isBlank() ? "ainovel" : jwtIssuer;
        jwtSecret = jwtSecret == null ? "" : jwtSecret;
    }
}
