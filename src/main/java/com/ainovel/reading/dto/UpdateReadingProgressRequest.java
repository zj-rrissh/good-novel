package com.ainovel.reading.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UpdateReadingProgressRequest(
        @NotNull @Positive Long novelId,
        @NotNull @Positive Long chapterId,
        @NotNull @Min(0) @Max(100) Integer progressPercent,
        Long clientTs,
        String deviceId,
        Long pageOffset) {
}
