package com.ainovel.security.audit;

import com.ainovel.infrastructure.log.AuditAction;

public record SecurityAuditEvent(
        AuditAction action,
        Long userId,
        String deviceId,
        String path,
        String method,
        String traceId,
        String detail) {
}
