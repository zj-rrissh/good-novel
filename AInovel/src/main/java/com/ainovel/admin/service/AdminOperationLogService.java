package com.ainovel.admin.service;

import com.ainovel.admin.dto.AdminOperationLogQuery;
import com.ainovel.admin.vo.AdminOperationLogVO;
import com.ainovel.common.api.PageResponse;
import com.ainovel.infrastructure.log.AuditAction;

public interface AdminOperationLogService {

    void record(AuditAction action, String bizType, Long bizId, String fromStatus, String toStatus, String reason);

    PageResponse<AdminOperationLogVO> query(AdminOperationLogQuery query);
}
