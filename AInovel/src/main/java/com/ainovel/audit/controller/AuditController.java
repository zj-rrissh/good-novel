package com.ainovel.audit.controller;

import com.ainovel.access.contract.ApiPaths;
import com.ainovel.access.contract.RequestHeaders;
import com.ainovel.audit.dto.SubmitTextAuditRequest;
import com.ainovel.audit.service.AuditTaskService;
import com.ainovel.audit.vo.AuditTaskVO;
import com.ainovel.common.api.Result;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping(ApiPaths.API_V1 + "/audit")
public class AuditController {

    private final AuditTaskService auditTaskService;

    public AuditController(AuditTaskService auditTaskService) {
        this.auditTaskService = auditTaskService;
    }

    @PostMapping("/text")
    public Result<AuditTaskVO> submit(@Valid @RequestBody SubmitTextAuditRequest request,
                                      @RequestHeader(value = RequestHeaders.IDEMPOTENCY_KEY, required = false)
                                      String idempotencyKey) {
        return Result.success(auditTaskService.accept(request, idempotencyKey));
    }

    @GetMapping("/tasks/{taskId}")
    public Result<AuditTaskVO> getTask(@PathVariable Long taskId) {
        return Result.success(auditTaskService.getTask(taskId));
    }
}
