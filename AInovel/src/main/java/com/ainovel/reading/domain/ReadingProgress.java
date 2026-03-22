package com.ainovel.reading.domain;

import java.time.LocalDateTime;

public record ReadingProgress(
        Long userId,
        Long novelId,
        Long chapterId,
        Integer progressPercent,
        Long pageOffset,
        LocalDateTime updatedAt) {
}
