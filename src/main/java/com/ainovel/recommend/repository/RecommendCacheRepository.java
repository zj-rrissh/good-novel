package com.ainovel.recommend.repository;

import com.ainovel.recommend.domain.RecommendResult;
import java.util.Optional;

public interface RecommendCacheRepository {

    Optional<RecommendResult> find(String key);

    void save(String key, RecommendResult result);
}
