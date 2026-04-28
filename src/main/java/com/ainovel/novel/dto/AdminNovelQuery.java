package com.ainovel.novel.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;

public record AdminNovelQuery(
        @Pattern(
                regexp = "DRAFT|PENDING_AUDIT|REJECTED|PUBLISHED|ON_SHELF|OFF_SHELF|BANNED",
                message = "status must be one of DRAFT,PENDING_AUDIT,REJECTED,PUBLISHED,ON_SHELF,OFF_SHELF,BANNED")
        String status,
        Long authorId,
        Long categoryId,
        String keyword,
        @Positive @Min(1) int page,
        @Positive @Max(100) int size) {
}
