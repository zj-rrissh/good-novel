package com.ainovel.novel.vo;

import com.ainovel.novel.domain.ChapterStatus;
import java.time.LocalDateTime;

public record NovelChapterVO(
        Long chapterId,
        Integer chapterNo,
        String title,
        ChapterStatus status,
        LocalDateTime publishedAt,
        Integer wordCount) {
}
