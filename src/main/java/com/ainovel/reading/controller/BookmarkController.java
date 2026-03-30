package com.ainovel.reading.controller;

import com.ainovel.access.contract.ApiPaths;
import com.ainovel.access.contract.RequestHeaders;
import com.ainovel.common.api.PageResponse;
import com.ainovel.common.api.Result;
import com.ainovel.common.api.StandardErrorCode;
import com.ainovel.infrastructure.aop.idempotent.Idempotent;
import com.ainovel.infrastructure.aop.idempotent.IdempotentStrategy;
import com.ainovel.infrastructure.aop.lock.Lock;
import com.ainovel.infrastructure.exception.BusinessException;
import com.ainovel.reading.dto.AddBookmarkRequest;
import com.ainovel.reading.service.BookmarkService;
import com.ainovel.reading.vo.BookmarkVO;
import com.ainovel.security.auth.context.CurrentUserHolder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping(ApiPaths.API_V1)
public class BookmarkController {

    private final BookmarkService bookmarkService;

    public BookmarkController(BookmarkService bookmarkService) {
        this.bookmarkService = bookmarkService;
    }

    @PostMapping("/bookmarks")
    @Idempotent(strategy = IdempotentStrategy.TOKEN, tokenParam = "idempotencyKey", optional = true, ttl = 10)
    @Lock(key = "'bookmark:add:' + #userId + ':' + #request.chapterId()", failMessage = "bookmark add is in progress")
    public Result<BookmarkVO> addBookmark(@Valid @RequestBody AddBookmarkRequest request,
                                          @RequestHeader(value = RequestHeaders.IDEMPOTENCY_KEY, required = false)
                                          String idempotencyKey) {
        return Result.success(bookmarkService.addBookmark(currentUserId(), request));
    }

    @DeleteMapping("/bookmarks/{bookmarkId}")
    @Lock(key = "'bookmark:del:' + #userId + ':' + #bookmarkId", failMessage = "bookmark delete is in progress")
    public Result<Void> removeBookmark(@PathVariable @Positive Long bookmarkId) {
        bookmarkService.removeBookmark(currentUserId(), bookmarkId);
        return Result.success();
    }

    @GetMapping("/bookmarks")
    public Result<PageResponse<BookmarkVO>> listBookmarks(
            @RequestParam(required = false) @Positive Long novelId,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return Result.success(bookmarkService.listBookmarks(currentUserId(), novelId, page, size));
    }

    private Long currentUserId() {
        return CurrentUserHolder.get()
                .map(currentUser -> currentUser.userId())
                .orElseThrow(() -> new BusinessException(StandardErrorCode.UNAUTHENTICATED));
    }
}
