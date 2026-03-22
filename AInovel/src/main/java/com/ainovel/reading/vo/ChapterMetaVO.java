package com.ainovel.reading.vo;

import java.time.LocalDateTime;

public record ChapterMetaVO(
        Long chapterId,
        Integer chapterNo,
        String title,
        String status,
        LocalDateTime publishedAt,
        Integer wordCount) {
}
