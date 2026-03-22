package com.ainovel.community.domain;

import java.time.LocalDateTime;

public record UserFollow(
        Long id,
        Long userId,
        Long targetUserId,
        FollowStatus status,
        LocalDateTime updatedAt) {
}
