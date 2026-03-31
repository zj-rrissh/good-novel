package com.ainovel.novel.controller;

import com.ainovel.access.contract.ApiPaths;
import com.ainovel.common.api.PageResponse;
import com.ainovel.common.api.Result;
import com.ainovel.infrastructure.aop.lock.Lock;
import com.ainovel.novel.dto.AdminNovelQuery;
import com.ainovel.novel.dto.ChangeNovelStatusRequest;
import com.ainovel.novel.service.NovelQueryService;
import com.ainovel.novel.service.NovelShelfService;
import com.ainovel.novel.vo.NovelSummaryVO;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping({ApiPaths.API_ADMIN_V1 + "/novels", ApiPaths.API_V1_ADMIN + "/novels"})
public class AdminNovelController {

    private final NovelQueryService novelQueryService;
    private final NovelShelfService novelShelfService;

    public AdminNovelController(NovelQueryService novelQueryService, NovelShelfService novelShelfService) {
        this.novelQueryService = novelQueryService;
        this.novelShelfService = novelShelfService;
    }

    @GetMapping
    public Result<PageResponse<NovelSummaryVO>> queryNovels(@Valid AdminNovelQuery query) {
        return Result.success(novelQueryService.queryAdminNovels(query));
    }

    @PostMapping("/{novelId}/ban")
    @Lock(key = "'novel:ban:' + #novelId", failMessage = "novel ban is in progress")
    public Result<Void> banNovel(@PathVariable Long novelId, @Valid @RequestBody ChangeNovelStatusRequest request) {
        novelShelfService.ban(novelId, request.reason());
        return Result.success();
    }
}
