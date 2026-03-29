package com.ainovel.reading.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AddBookmarkRequest(
        @NotNull @Positive Long novelId,
        @NotNull @Positive Long chapterId,
        @Min(0) Long pageOffset,
        @Size(max = 500) String note) {
}
