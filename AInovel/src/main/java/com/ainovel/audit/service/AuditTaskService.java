package com.ainovel.audit.service;

import com.ainovel.common.api.PageResponse;
import com.ainovel.audit.dto.AdminAuditQuery;
import com.ainovel.audit.dto.SubmitTextAuditRequest;
import com.ainovel.audit.vo.AuditTaskVO;

public interface AuditTaskService {

    AuditTaskVO accept(SubmitTextAuditRequest request, String idempotencyKey);

    AuditTaskVO getTask(Long taskId);

    PageResponse<AuditTaskVO> query(AdminAuditQuery query);
}
