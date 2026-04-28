package com.ainovel.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record AdminOperationLogQuery(
        String action,
        String bizType,
        Long bizId,
        Long operatorId,
        @Min(1) int page,
        @Min(1) @Max(100) int size) {
}
