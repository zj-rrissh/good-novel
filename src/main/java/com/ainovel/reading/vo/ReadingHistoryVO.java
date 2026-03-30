package com.ainovel.reading.vo;

import java.time.LocalDateTime;

public record ReadingHistoryVO(
        Long novelId,
        Long chapterId,
        LocalDateTime lastReadAt) {
}
