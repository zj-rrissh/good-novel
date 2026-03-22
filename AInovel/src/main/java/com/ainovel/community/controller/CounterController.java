package com.ainovel.community.controller;

import com.ainovel.access.contract.ApiPaths;
import com.ainovel.common.api.Result;
import com.ainovel.community.domain.TargetType;
import com.ainovel.community.vo.CounterSummaryVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.API_V1)
public class CounterController {

    @GetMapping("/counters")
    public Result<CounterSummaryVO> getCounters(@RequestParam TargetType targetType, @RequestParam Long targetId) {
        return Result.success(new CounterSummaryVO(targetType.name(), targetId, 1, 2, 3, 4));
    }
}
