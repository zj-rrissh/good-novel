package com.ainovel.reading.vo;

import java.time.LocalDateTime;

public record ReadingProgressVO(
        Long novelId,
        Long chapterId,
        Integer progressPercent,
        Long pageOffset,
        LocalDateTime updatedAt) {
}
