package com.ainovel.reading.vo;

import java.time.LocalDateTime;

public record ChapterContentVO(
        Long chapterId,
        Long novelId,
        String title,
        String content,
        Integer wordCount,
        LocalDateTime publishedAt) {
}
