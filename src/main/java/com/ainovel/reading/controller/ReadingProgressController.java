package com.ainovel.reading.controller;

import com.ainovel.access.contract.ApiPaths;
import com.ainovel.access.contract.RequestHeaders;
import com.ainovel.common.api.Result;
import com.ainovel.common.api.StandardErrorCode;
import com.ainovel.infrastructure.aop.idempotent.Idempotent;
import com.ainovel.infrastructure.aop.idempotent.IdempotentStrategy;
import com.ainovel.infrastructure.aop.lock.Lock;
import com.ainovel.infrastructure.exception.BusinessException;
import com.ainovel.reading.service.ProgressService;
import com.ainovel.reading.service.ReadingHistoryService;
import com.ainovel.reading.dto.UpdateReadingProgressRequest;
import com.ainovel.reading.vo.ReadingProgressVO;
import com.ainovel.security.auth.context.CurrentUserHolder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping(ApiPaths.API_V1)
public class ReadingProgressController {

    private final ProgressService progressService;
    private final ReadingHistoryService readingHistoryService;

    public ReadingProgressController(ProgressService progressService, ReadingHistoryService readingHistoryService) {
        this.progressService = progressService;
        this.readingHistoryService = readingHistoryService;
    }

    @GetMapping("/reading-progress")
    public Result<ReadingProgressVO> getProgress(@RequestParam @Positive Long novelId) {
        return Result.success(progressService.getProgress(currentUserId(), novelId));
    }

    @PostMapping("/reading-progress")
    @Idempotent(strategy = IdempotentStrategy.TOKEN, tokenParam = "idempotencyKey", optional = true, ttl = 10)
    @Lock(key = "'reading:progress:' + #currentUserId + ':' + #request.novelId", failMessage = "progress update is in progress")
    public Result<ReadingProgressVO> saveProgress(@Valid @RequestBody UpdateReadingProgressRequest request,
                                                  @RequestHeader(value = RequestHeaders.IDEMPOTENCY_KEY, required = false)
                                                  String idempotencyKey) {
        Long userId = currentUserId();
        ReadingProgressVO result = progressService.saveProgress(userId, request, idempotencyKey);
        readingHistoryService.recordHistory(userId, request.novelId(), request.chapterId());
        return Result.success(result);
    }

    private Long currentUserId() {
        return CurrentUserHolder.get()
                .map(currentUser -> currentUser.userId())
                .orElseThrow(() -> new BusinessException(StandardErrorCode.UNAUTHENTICATED));
    }
}
