package com.ainovel.novel.vo;

import com.ainovel.novel.domain.NovelStatus;

public record NovelSummaryVO(Long novelId, String title, String coverUrl, Long authorId, NovelStatus status) {
}
