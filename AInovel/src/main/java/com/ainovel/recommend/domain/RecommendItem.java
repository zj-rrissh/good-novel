package com.ainovel.recommend.domain;

public record RecommendItem(
        Long novelId,
        String title,
        String coverUrl,
        String authorName,
        String category,
        String tagsSummary,
        Double hotScore) {
}
