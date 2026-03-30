package com.ainovel.reading.vo;

import java.time.LocalDateTime;

public record BookmarkVO(
        Long id,
        Long novelId,
        Long chapterId,
        Long pageOffset,
        String note,
        LocalDateTime createdAt) {
}
