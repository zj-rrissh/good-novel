package com.ainovel.user.domain;

import java.time.LocalDateTime;

public record UserProfile(
        Long userId,
        String nickname,
        String avatarUrl,
        String bio,
        int level,
        String verifiedStatus,
        LocalDateTime updatedAt) {
}
