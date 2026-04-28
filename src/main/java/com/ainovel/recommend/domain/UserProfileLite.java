package com.ainovel.recommend.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public record UserProfileLite(
        Long userId,
        Set<String> preferCategories,
        Set<String> preferTags,
        List<Long> recentNovels,
        LocalDateTime lastUpdatedAt,
        String version) {
}
