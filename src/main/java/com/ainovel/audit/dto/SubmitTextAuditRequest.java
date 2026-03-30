package com.ainovel.audit.dto;

import com.ainovel.audit.domain.BizType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record SubmitTextAuditRequest(
        @NotNull BizType bizType,
        @NotNull @Positive Long bizId,
        @NotBlank @Size(max = 100000) String content,
        Long operatorId,
        @Size(max = 32) String source) {
}
