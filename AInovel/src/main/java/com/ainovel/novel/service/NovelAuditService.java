package com.ainovel.novel.service;

import com.ainovel.novel.dto.SubmitNovelAuditRequest;

public interface NovelAuditService {

    String submitAudit(Long novelId, SubmitNovelAuditRequest request, String idempotencyKey);

    void applyAuditResult(String auditTaskId, String decision, String reasonCode, String reasonText);
}
