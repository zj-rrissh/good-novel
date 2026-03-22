package com.ainovel.audit.domain;

import java.time.LocalDateTime;

public record AuditTask(
        Long taskId,
        BizType bizType,
        Long bizId,
        String contentSnapshot,
        String contentHash,
        AuditStatus auditStatus,
        RiskLevel riskLevel,
        String reasonCode,
        String reasonText,
        Long reviewerId,
        Integer retryCount,
        String ruleVersion,
        LocalDateTime reviewedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
