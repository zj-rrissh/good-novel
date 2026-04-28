package com.ainovel.admin.vo;

import java.time.LocalDateTime;

public record AdminOperationLogVO(
        Long logId,
        String action,
        String bizType,
        Long bizId,
        Long operatorId,
        String operatorRoles,
        String fromStatus,
        String toStatus,
        String reason,
        String traceId,
        String requestPath,
        LocalDateTime createdAt) {
}
