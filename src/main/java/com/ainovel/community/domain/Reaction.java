package com.ainovel.community.domain;

import java.time.LocalDateTime;

public record Reaction(
        Long id,
        ReactionType reactionType,
        TargetType targetType,
        Long targetId,
        Long userId,
        ReactionStatus status,
        LocalDateTime updatedAt) {
}
