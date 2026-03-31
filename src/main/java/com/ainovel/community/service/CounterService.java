package com.ainovel.community.service;

import com.ainovel.community.domain.TargetType;
import com.ainovel.community.vo.CounterSummaryVO;

public interface CounterService {

    CounterSummaryVO getCounters(TargetType targetType, Long targetId);
}
