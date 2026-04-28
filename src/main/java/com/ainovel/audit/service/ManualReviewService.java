package com.ainovel.audit.service;

import com.ainovel.audit.dto.ReviewAuditTaskRequest;
import com.ainovel.audit.vo.AuditTaskVO;

public interface ManualReviewService {

    AuditTaskVO review(Long taskId, ReviewAuditTaskRequest request);
}
