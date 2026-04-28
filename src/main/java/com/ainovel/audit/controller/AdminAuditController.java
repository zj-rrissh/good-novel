package com.ainovel.audit.controller;

import com.ainovel.access.contract.ApiPaths;
import com.ainovel.audit.dto.AdminAuditQuery;
import com.ainovel.audit.dto.ReviewAuditTaskRequest;
import com.ainovel.audit.service.AuditTaskService;
import com.ainovel.audit.service.ManualReviewService;
import com.ainovel.audit.vo.AuditTaskVO;
import com.ainovel.common.api.PageResponse;
import com.ainovel.common.api.Result;
import com.ainovel.common.api.StandardErrorCode;
import com.ainovel.infrastructure.aop.lock.Lock;
import com.ainovel.infrastructure.exception.BusinessException;
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
@RequestMapping({ApiPaths.API_ADMIN_V1 + "/audit/tasks", ApiPaths.API_V1_ADMIN + "/audit/tasks"})
public class AdminAuditController {

    private final AuditTaskService auditTaskService;
    private final ManualReviewService manualReviewService;

    public AdminAuditController(AuditTaskService auditTaskService, ManualReviewService manualReviewService) {
        this.auditTaskService = auditTaskService;
        this.manualReviewService = manualReviewService;
    }

    @GetMapping
    public Result<PageResponse<AuditTaskVO>> query(@Valid AdminAuditQuery query) {
        return Result.success(auditTaskService.query(query));
    }

    @GetMapping("/{taskId}")
    public Result<AuditTaskVO> getTask(@PathVariable Long taskId) {
        AuditTaskVO task = auditTaskService.getTask(taskId);
        if (task == null) {
            throw new BusinessException(StandardErrorCode.INVALID_REQUEST, "audit task not found");
        }
        return Result.success(task);
    }

    @PostMapping("/{taskId}/review")
    @Lock(key = "'audit:review:' + #taskId", failMessage = "audit review is in progress")
    public Result<AuditTaskVO> review(@PathVariable Long taskId, @Valid @RequestBody ReviewAuditTaskRequest request) {
        return Result.success(manualReviewService.review(taskId, request));
    }
}
