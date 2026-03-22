package com.ainovel.user.domain;

import java.time.LocalDateTime;
import java.util.Set;

public record UserAccount(
        Long id,
        String username,
        String passwordHash,
        UserStatus status,
        Set<UserRole> roles,
        long loginVersion,
        LocalDateTime createdAt) {
}
