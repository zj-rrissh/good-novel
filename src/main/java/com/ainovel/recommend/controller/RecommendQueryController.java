package com.ainovel.recommend.controller;

import com.ainovel.access.contract.ApiPaths;
import com.ainovel.common.api.Result;
import com.ainovel.recommend.service.RecommendQueryService;
import com.ainovel.recommend.vo.RecommendResultVO;
import com.ainovel.security.auth.context.CurrentUserHolder;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping(ApiPaths.API_V1 + "/recommend")
public class RecommendQueryController {

    private final RecommendQueryService recommendQueryService;

    public RecommendQueryController(RecommendQueryService recommendQueryService) {
        this.recommendQueryService = recommendQueryService;
    }

    @GetMapping("/home")
    public Result<RecommendResultVO> home(@RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        Long userId = CurrentUserHolder.get().map(currentUser -> currentUser.userId()).orElse(null);
        return Result.success(recommendQueryService.queryHome(userId, size));
    }

    @GetMapping("/novels/{novelId}/related")
    public Result<RecommendResultVO> related(@PathVariable Long novelId,
                                             @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return Result.success(recommendQueryService.queryRelated(novelId, size));
    }

    @GetMapping("/continue")
    public Result<RecommendResultVO> continueReading(@RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        Long userId = CurrentUserHolder.get().map(currentUser -> currentUser.userId()).orElse(null);
        return Result.success(recommendQueryService.queryContinue(userId, size));
    }
}
