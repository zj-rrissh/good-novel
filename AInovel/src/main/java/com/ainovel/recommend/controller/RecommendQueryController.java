package com.ainovel.recommend.controller;

import com.ainovel.access.contract.ApiPaths;
import com.ainovel.common.api.Result;
import com.ainovel.recommend.domain.RecommendItem;
import com.ainovel.recommend.vo.RecommendResultVO;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
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

    @GetMapping("/home")
    public Result<RecommendResultVO> home(@RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return Result.success(sample("HOME", "anon"));
    }

    @GetMapping("/novels/{novelId}/related")
    public Result<RecommendResultVO> related(@PathVariable Long novelId,
                                             @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return Result.success(sample("RELATED", String.valueOf(novelId)));
    }

    @GetMapping("/continue")
    public Result<RecommendResultVO> continueReading(@RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return Result.success(sample("CONTINUE", "current-user"));
    }

    private RecommendResultVO sample(String scene, String subject) {
        return new RecommendResultVO(scene, subject, "cache", "LEVEL_1", List.of(
                new RecommendItem(1L, "sample novel", "https://example.com/cover.png", "author", "fantasy",
                        "growth · hot", 98.6)));
    }
}
