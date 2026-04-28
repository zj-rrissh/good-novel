package com.ainovel.admin.controller;

import com.ainovel.access.contract.ApiPaths;
import com.ainovel.admin.dto.AdminOperationLogQuery;
import com.ainovel.admin.service.AdminOperationLogService;
import com.ainovel.admin.vo.AdminOperationLogVO;
import com.ainovel.common.api.PageResponse;
import com.ainovel.common.api.Result;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping({ApiPaths.API_ADMIN_V1 + "/operation-logs", ApiPaths.API_V1_ADMIN + "/operation-logs"})
public class AdminOperationLogController {

    private final AdminOperationLogService adminOperationLogService;

    public AdminOperationLogController(AdminOperationLogService adminOperationLogService) {
        this.adminOperationLogService = adminOperationLogService;
    }

    @GetMapping
    public Result<PageResponse<AdminOperationLogVO>> query(@Valid AdminOperationLogQuery query) {
        return Result.success(adminOperationLogService.query(query));
    }
}
