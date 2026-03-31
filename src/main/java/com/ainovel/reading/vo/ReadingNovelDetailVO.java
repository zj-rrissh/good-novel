package com.ainovel.reading.vo;

import java.util.Set;

public record ReadingNovelDetailVO(
        Long novelId,
        String title,
        String intro,
        String coverUrl,
        String authorName,
        String category,
        Set<String> tags,
        Long latestChapterId) {
}
