package com.ainovel.security.auth.context;

import com.ainovel.security.auth.rbac.RoleType;
import java.time.Instant;
import java.util.Set;

public record CurrentUser(
        Long userId,
        Set<RoleType> roles,
        String deviceId,
        boolean authenticated,
        String tokenId,
        long tokenVersion,
        Instant expiresAt) {

    public boolean hasRole(RoleType roleType) {
        return roles != null && roles.contains(roleType);
    }
}
