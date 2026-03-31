package com.ainovel.novel.controller;

import com.ainovel.access.contract.ApiPaths;
import com.ainovel.access.contract.RequestHeaders;
import com.ainovel.common.api.Result;
import com.ainovel.infrastructure.aop.idempotent.Idempotent;
import com.ainovel.infrastructure.aop.idempotent.IdempotentStrategy;
import com.ainovel.infrastructure.aop.lock.Lock;
import com.ainovel.novel.dto.ChangeNovelStatusRequest;
import com.ainovel.novel.dto.CreateChapterRequest;
import com.ainovel.novel.dto.CreateNovelRequest;
import com.ainovel.novel.dto.SubmitNovelAuditRequest;
import com.ainovel.novel.dto.UpdateChapterRequest;
import com.ainovel.novel.dto.UpdateNovelRequest;
import com.ainovel.novel.service.ChapterManagementService;
import com.ainovel.novel.service.NovelAuditService;
import com.ainovel.novel.service.NovelDraftService;
import com.ainovel.novel.service.NovelShelfService;
import com.ainovel.novel.vo.NovelChapterVO;
import com.ainovel.novel.vo.NovelDetailVO;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping(ApiPaths.API_V1)
public class AuthorNovelController {

    private final NovelDraftService novelDraftService;
    private final ChapterManagementService chapterManagementService;
    private final NovelAuditService novelAuditService;
    private final NovelShelfService novelShelfService;

    public AuthorNovelController(NovelDraftService novelDraftService,
                                 ChapterManagementService chapterManagementService,
                                 NovelAuditService novelAuditService,
                                 NovelShelfService novelShelfService) {
        this.novelDraftService = novelDraftService;
        this.chapterManagementService = chapterManagementService;
        this.novelAuditService = novelAuditService;
        this.novelShelfService = novelShelfService;
    }

    @PostMapping("/novels")
    @Idempotent(strategy = IdempotentStrategy.TOKEN, tokenParam = "idempotencyKey", optional = true, ttl = 10)
    @Lock(key = "'novel:create:' + #currentUserId", failMessage = "novel create is in progress")
    public Result<NovelDetailVO> createNovel(@Valid @RequestBody CreateNovelRequest request,
                                             @RequestHeader(value = RequestHeaders.IDEMPOTENCY_KEY, required = false)
                                             String idempotencyKey) {
        return Result.success(novelDraftService.createNovel(request, idempotencyKey));
    }

    @PutMapping("/novels/{novelId}")
    @Lock(key = "'novel:update:' + #currentUserId + ':' + #novelId", failMessage = "novel update is in progress")
    public Result<NovelDetailVO> updateNovel(@PathVariable Long novelId, @Valid @RequestBody UpdateNovelRequest request) {
        return Result.success(novelDraftService.updateNovel(novelId, request));
    }

    @PostMapping("/novels/{novelId}/chapters")
    @Idempotent(strategy = IdempotentStrategy.TOKEN, tokenParam = "idempotencyKey", optional = true, ttl = 10)
    @Lock(key = "'chapter:create:' + #currentUserId + ':' + #novelId", failMessage = "chapter create is in progress")
    public Result<NovelChapterVO> createChapter(@PathVariable Long novelId,
                                                @Valid @RequestBody CreateChapterRequest request,
                                                @RequestHeader(value = RequestHeaders.IDEMPOTENCY_KEY, required = false)
                                                String idempotencyKey) {
        return Result.success(chapterManagementService.createChapter(novelId, request, idempotencyKey));
    }

    @PutMapping("/chapters/{chapterId}")
    @Lock(key = "'chapter:update:' + #currentUserId + ':' + #chapterId", failMessage = "chapter update is in progress")
    public Result<NovelChapterVO> updateChapter(@PathVariable Long chapterId,
                                                @Valid @RequestBody UpdateChapterRequest request) {
        return Result.success(chapterManagementService.updateChapter(chapterId, request));
    }

    @PostMapping("/novels/{novelId}/submit-audit")
    @Idempotent(strategy = IdempotentStrategy.TOKEN, tokenParam = "idempotencyKey", optional = true, ttl = 10)
    @Lock(key = "'novel:audit:' + #currentUserId + ':' + #novelId", failMessage = "audit submit is in progress")
    public Result<String> submitAudit(@PathVariable Long novelId,
                                      @Valid @RequestBody SubmitNovelAuditRequest request,
                                      @RequestHeader(value = RequestHeaders.IDEMPOTENCY_KEY, required = false)
                                      String idempotencyKey) {
        return Result.success(novelAuditService.submitAudit(novelId, request, idempotencyKey));
    }

    @PostMapping("/novels/{novelId}/on-shelf")
    @Lock(key = "'novel:on-shelf:' + #currentUserId + ':' + #novelId", failMessage = "novel status update is in progress")
    public Result<Void> onShelf(@PathVariable Long novelId, @Valid @RequestBody ChangeNovelStatusRequest request) {
        novelShelfService.onShelf(novelId, request.reason());
        return Result.success();
    }

    @PostMapping("/novels/{novelId}/off-shelf")
    @Lock(key = "'novel:off-shelf:' + #currentUserId + ':' + #novelId", failMessage = "novel status update is in progress")
    public Result<Void> offShelf(@PathVariable Long novelId, @Valid @RequestBody ChangeNovelStatusRequest request) {
        novelShelfService.offShelf(novelId, request.reason());
        return Result.success();
    }
}
