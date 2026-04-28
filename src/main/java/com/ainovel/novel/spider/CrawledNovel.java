package com.ainovel.novel.spider;

import com.ainovel.novel.domain.NovelStatus;
import java.util.List;
import java.util.Set;

public record CrawledNovel(
        Long authorId,
        String title,
        String intro,
        String coverUrl,
        Long categoryId,
        Set<Long> tagIds,
        NovelStatus novelStatus,
        List<CrawledChapter> chapters,
        String sourceUrl) {

    public record CrawledChapter(
            Integer chapterNo,
            String title,
            String content,
            String sourceUrl) {
    }
}
