package com.ainovel.recommend.vo;

import com.ainovel.recommend.domain.RecommendItem;
import java.util.List;

public record RecommendResultVO(String scene, String subject, String source, String degradeLevel, List<RecommendItem> items) {
}
