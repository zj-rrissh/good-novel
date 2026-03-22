package com.ainovel.reading.domain;

import java.time.LocalDateTime;

public record ReadingHistory(Long userId, Long novelId, Long chapterId, LocalDateTime lastReadAt) {
}
