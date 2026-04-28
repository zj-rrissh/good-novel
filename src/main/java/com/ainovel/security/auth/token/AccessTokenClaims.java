package com.ainovel.security.auth.token;

import com.ainovel.security.auth.rbac.RoleType;
import java.time.Instant;
import java.util.Set;

public record AccessTokenClaims(
        String subject,
        Set<RoleType> roles,
        String jti,
        long tokenVersion,
        Instant issuedAt,
        Instant expiresAt) {
}
