package com.ainovel.reading.controller;

import com.ainovel.access.contract.ApiPaths;
import com.ainovel.common.api.Result;
import com.ainovel.reading.service.ChapterReadService;
import com.ainovel.reading.vo.ChapterContentVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.API_V1)
public class ReadingChapterController {

    private final ChapterReadService chapterReadService;

    public ReadingChapterController(ChapterReadService chapterReadService) {
        this.chapterReadService = chapterReadService;
    }

    @GetMapping("/chapters/{chapterId}/content")
    public Result<ChapterContentVO> getChapterContent(@PathVariable Long chapterId,
                                                      @RequestHeader(value = "X-Device-Id", required = false)
                                                      String deviceId) {
        return Result.success(chapterReadService.getChapterContent(chapterId));
    }
}
