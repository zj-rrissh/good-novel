package com.ainovel.reading.domain;

import java.time.LocalDateTime;

public record Bookmark(Long userId, Long novelId, Long chapterId, Long pageOffset, LocalDateTime createdAt) {
}
