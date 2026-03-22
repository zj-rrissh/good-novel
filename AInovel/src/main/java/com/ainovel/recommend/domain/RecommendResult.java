package com.ainovel.recommend.domain;

import java.time.LocalDateTime;
import java.util.List;

public record RecommendResult(
        RecommendScene scene,
        String subject,
        List<RecommendItem> items,
        String version,
        String algorithm,
        LocalDateTime generatedAt,
        LocalDateTime expireAt,
        String degradeLevel) {
}
