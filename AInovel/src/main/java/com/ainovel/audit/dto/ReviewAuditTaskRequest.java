package com.ainovel.audit.dto;

import com.ainovel.audit.domain.ReviewDecision;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewAuditTaskRequest(
        @NotNull ReviewDecision decision,
        @Size(max = 64) String rejectReasonCode,
        @Size(max = 256) String rejectReasonText) {
}
