package com.ainovel.community.controller;

import com.ainovel.access.contract.ApiPaths;
import com.ainovel.common.api.Result;
import com.ainovel.community.domain.TargetType;
import com.ainovel.community.service.CounterService;
import com.ainovel.community.vo.CounterSummaryVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.API_V1)
public class CounterController {

    private final CounterService counterService;

    public CounterController(CounterService counterService) {
        this.counterService = counterService;
    }

    @GetMapping("/counters")
    public Result<CounterSummaryVO> getCounters(@RequestParam TargetType targetType, @RequestParam Long targetId) {
        return Result.success(counterService.getCounters(targetType, targetId));
    }
}
