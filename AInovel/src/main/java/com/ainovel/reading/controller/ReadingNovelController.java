package com.ainovel.reading.controller;

import com.ainovel.access.contract.ApiPaths;
import com.ainovel.common.api.PageResponse;
import com.ainovel.common.api.Result;
import com.ainovel.reading.service.NovelReadService;
import com.ainovel.reading.vo.ChapterMetaVO;
import com.ainovel.reading.vo.ReadingNovelDetailVO;
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
@RequestMapping(ApiPaths.API_V1)
public class ReadingNovelController {

    private final NovelReadService novelReadService;

    public ReadingNovelController(NovelReadService novelReadService) {
        this.novelReadService = novelReadService;
    }

    @GetMapping("/novels/{novelId}")
    public Result<ReadingNovelDetailVO> getNovelDetail(@PathVariable Long novelId) {
        return Result.success(novelReadService.getNovelDetail(novelId));
    }

    @GetMapping("/novels/{novelId}/chapters")
    public Result<PageResponse<ChapterMetaVO>> getChapterPage(@PathVariable Long novelId,
                                                              @RequestParam(defaultValue = "1") @Min(1) int page,
                                                              @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return Result.success(novelReadService.getChapterPage(novelId, page, size));
    }
}
