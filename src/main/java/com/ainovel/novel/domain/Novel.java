package com.ainovel.novel.domain;

import java.time.LocalDateTime;
import java.util.Set;

public record Novel(
        Long id,
        Long authorId,
        String title,
        String intro,
        String coverUrl,
        Long categoryId,
        Set<Long> tagIds,
        NovelStatus status,
        Long latestChapterId,
        long wordCount,
        String auditTaskId,
        LocalDateTime updatedAt) {
}
