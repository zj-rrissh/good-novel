package com.ainovel.recommend.task;

import com.ainovel.recommend.domain.RecommendScene;

public interface RecommendAsyncTrigger {

    void trigger(RecommendScene scene, String subject);
}
