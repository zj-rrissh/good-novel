package com.ainovel.audit.engine;

import com.ainovel.audit.domain.AuditStatus;
import com.ainovel.audit.domain.RiskLevel;

public record AuditExecutionResult(AuditStatus auditStatus, RiskLevel riskLevel, String reasonCode, String reasonText) {
}
