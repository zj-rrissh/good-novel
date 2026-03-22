package com.ainovel.community.domain;

import java.time.LocalDateTime;

public record Comment(
        Long id,
        TargetType targetType,
        Long targetId,
        Long userId,
        Long parentId,
        Long replyToUserId,
        String content,
        CommentStatus status,
        LocalDateTime createdAt,
        Long version) {
}
