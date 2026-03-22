package com.ainovel.user.controller;

import com.ainovel.access.contract.ApiPaths;
import com.ainovel.access.contract.RequestHeaders;
import com.ainovel.common.api.Result;
import com.ainovel.infrastructure.aop.idempotent.Idempotent;
import com.ainovel.infrastructure.aop.idempotent.IdempotentStrategy;
import com.ainovel.infrastructure.aop.lock.Lock;
import com.ainovel.user.dto.DeliverMessageRequest;
import com.ainovel.user.service.UserMessageService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping(ApiPaths.API_V1 + "/internal/messages")
public class InternalMessageController {

    private final UserMessageService userMessageService;

    public InternalMessageController(UserMessageService userMessageService) {
        this.userMessageService = userMessageService;
    }

    @PostMapping
    @Idempotent(strategy = IdempotentStrategy.TOKEN, tokenParam = "idempotencyKey", ttl = 10)
    @Lock(key = "'internal:message:' + #request.producer + ':' + #request.bizType + ':' + #request.bizId",
            failMessage = "message delivery is in progress")
    public Result<Void> deliver(@Valid @RequestBody DeliverMessageRequest request,
                                @RequestHeader(RequestHeaders.IDEMPOTENCY_KEY) String idempotencyKey) {
        userMessageService.deliver(idempotencyKey, request);
        return Result.success();
    }
}
