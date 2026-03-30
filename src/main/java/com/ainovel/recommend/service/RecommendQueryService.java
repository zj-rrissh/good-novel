package com.ainovel.recommend.service;

import com.ainovel.recommend.vo.RecommendResultVO;

public interface RecommendQueryService {

    RecommendResultVO queryHome(Long userId, int size);

    RecommendResultVO queryRelated(Long novelId, int size);

    RecommendResultVO queryContinue(Long userId, int size);
}
