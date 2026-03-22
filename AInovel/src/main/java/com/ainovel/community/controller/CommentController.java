package com.ainovel.community.controller;

import com.ainovel.access.contract.ApiPaths;
import com.ainovel.access.contract.RequestHeaders;
import com.ainovel.common.api.PageResponse;
import com.ainovel.common.api.Result;
import com.ainovel.community.service.CommentService;
import com.ainovel.community.domain.TargetType;
import com.ainovel.community.dto.CreateCommentRequest;
import com.ainovel.community.vo.CommentVO;
import com.ainovel.infrastructure.aop.idempotent.Idempotent;
import com.ainovel.infrastructure.aop.idempotent.IdempotentStrategy;
import com.ainovel.infrastructure.aop.lock.Lock;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping("/comments")
    @Idempotent(strategy = IdempotentStrategy.TOKEN, tokenParam = "idempotencyKey", optional = true, ttl = 10)
    @Lock(key = "'comment:create:' + #currentUserId + ':' + #request.targetType + ':' + #request.targetId + ':' + #request.content.hashCode()",
            failMessage = "comment submission is in progress")
    public Result<CommentVO> createComment(@Valid @RequestBody CreateCommentRequest request,
                                           @RequestHeader(value = RequestHeaders.IDEMPOTENCY_KEY, required = false)
                                           String idempotencyKey) {
        return Result.success(commentService.createComment(request, idempotencyKey));
    }

    @DeleteMapping("/comments/{commentId}")
    @Lock(key = "'comment:delete:' + #currentUserId + ':' + #commentId", failMessage = "comment delete is in progress")
    public Result<Void> deleteComment(@PathVariable Long commentId) {
        commentService.deleteComment(commentId);
        return Result.success();
    }

    @GetMapping("/{targetType}/{targetId}/comments")
    public Result<PageResponse<CommentVO>> queryComments(@PathVariable TargetType targetType,
                                                         @PathVariable Long targetId,
                                                         @RequestParam(defaultValue = "1") @Min(1) int page,
                                                         @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size,
                                                         @RequestParam(defaultValue = "new") String sort) {
        return Result.success(commentService.queryComments(targetType, targetId, page, size, sort));
    }
}
