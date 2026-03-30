package com.ainovel.user.vo;

import com.ainovel.user.domain.MessageType;
import java.time.LocalDateTime;

public record UserMessageVO(
        Long messageId,
        MessageType type,
        String title,
        String content,
        String bizType,
        Long bizId,
        boolean read,
        LocalDateTime createdAt) {
}
