package com.ainovel.user.domain;

import java.time.LocalDateTime;

public record UserMessage(
        Long id,
        Long toUserId,
        MessageType type,
        String title,
        String content,
        String bizType,
        Long bizId,
        String producer,
        String traceId,
        LocalDateTime readAt,
        LocalDateTime createdAt) {
}
