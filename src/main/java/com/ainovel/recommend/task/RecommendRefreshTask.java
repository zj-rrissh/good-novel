package com.ainovel.recommend.task;

import com.ainovel.recommend.domain.RecommendScene;

public interface RecommendRefreshTask {

    void refreshForScene(RecommendScene scene, String subject);

    void schedulePeriodicRefresh(RecommendScene scene);
}
