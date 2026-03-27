package com.ainovel.community.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record AdminCommentQuery(
        String targetType,
        Long targetId,
        String status,
        Long userId,
        String keyword,
        @Min(1) int page,
        @Min(1) @Max(100) int size) {
}
