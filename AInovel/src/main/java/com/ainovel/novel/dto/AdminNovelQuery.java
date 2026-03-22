package com.ainovel.novel.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

public record AdminNovelQuery(
        String status,
        Long authorId,
        Long categoryId,
        String keyword,
        @Positive @Min(1) int page,
        @Positive @Max(100) int size) {
}
