package com.ainovel.novel.vo;

import com.ainovel.novel.domain.NovelStatus;
import java.util.List;
import java.util.Set;

public record NovelDetailVO(
        Long novelId,
        String title,
        String intro,
        String coverUrl,
        Long categoryId,
        Set<Long> tagIds,
        NovelStatus status,
        Long latestChapterId,
        long wordCount,
        List<NovelChapterVO> chapters) {
}
