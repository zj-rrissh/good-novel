package com.ainovel.infrastructure.log;

public record AuditEventContext(
        AuditAction action,
        String bizType,
        String bizId,
        String fromStatus,
        String toStatus,
        String reason) {
}
