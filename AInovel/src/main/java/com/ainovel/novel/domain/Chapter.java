package com.ainovel.novel.domain;

import java.time.LocalDateTime;

public record Chapter(
        Long id,
        Long novelId,
        Integer chapterNo,
        String title,
        String content,
        ChapterStatus status,
        String auditTaskId,
        LocalDateTime publishedAt,
        LocalDateTime updatedAt) {
}
