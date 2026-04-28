package com.ainovel.audit.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record AdminAuditQuery(String status, String bizType, String riskLevel, @Min(1) int page, @Min(1) @Max(100) int size) {
}
