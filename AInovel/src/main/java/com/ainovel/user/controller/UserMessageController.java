package com.ainovel.user.controller;

import com.ainovel.access.contract.ApiPaths;
import com.ainovel.common.api.PageResponse;
import com.ainovel.common.api.Result;
import com.ainovel.common.api.StandardErrorCode;
import com.ainovel.infrastructure.aop.lock.Lock;
import com.ainovel.infrastructure.exception.BusinessException;
import com.ainovel.security.auth.context.CurrentUserHolder;
import com.ainovel.user.service.UserMessageService;
import com.ainovel.user.domain.MessageType;
import com.ainovel.user.dto.MarkMessagesReadRequest;
import com.ainovel.user.vo.UserMessageVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping(ApiPaths.API_V1 + "/users/me/messages")
public class UserMessageController {

    private final UserMessageService userMessageService;

    public UserMessageController(UserMessageService userMessageService) {
        this.userMessageService = userMessageService;
    }

    @GetMapping
    public Result<PageResponse<UserMessageVO>> queryMessages(@RequestParam(defaultValue = "1") @Min(1) int page,
                                                             @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size,
                                                             @RequestParam(required = false) String type,
                                                             @RequestParam(required = false) Boolean readStatus) {
        return Result.success(userMessageService.queryMessages(currentUserId(), page, size, type, readStatus));
    }

    @PostMapping("/read")
    @Lock(key = "'message:read:' + #currentUserId", failMessage = "message read request is in progress")
    public Result<Void> markRead(@Valid @RequestBody MarkMessagesReadRequest request) {
        userMessageService.markRead(currentUserId(), request);
        return Result.success();
    }

    private Long currentUserId() {
        return CurrentUserHolder.get()
                .map(currentUser -> currentUser.userId())
                .orElseThrow(() -> new BusinessException(StandardErrorCode.UNAUTHENTICATED));
    }
}
