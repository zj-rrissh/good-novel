package com.ainovel.recommend.service;

import com.ainovel.recommend.domain.RecommendScene;

public interface RecommendComputeService {

    void compute(RecommendScene scene, String subject);
}
