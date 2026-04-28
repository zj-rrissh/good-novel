package com.ainovel.audit.vo;

import com.ainovel.audit.domain.AuditStatus;
import com.ainovel.audit.domain.BizType;
import com.ainovel.audit.domain.RiskLevel;
import java.time.LocalDateTime;

public record AuditTaskVO(
        Long taskId,
        BizType bizType,
        Long bizId,
        AuditStatus auditStatus,
        RiskLevel riskLevel,
        String contentSnapshot,
        String contentHash,
        String reasonCode,
        String reasonText,
        Long reviewerId,
        Integer retryCount,
        String ruleVersion,
        LocalDateTime reviewedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
