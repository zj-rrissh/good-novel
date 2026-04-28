package com.ainovel.audit.service;

import com.ainovel.admin.service.AdminOperationLogService;
import com.ainovel.audit.domain.AuditStatus;
import com.ainovel.audit.domain.BizType;
import com.ainovel.audit.domain.ReviewDecision;
import com.ainovel.audit.domain.RiskLevel;
import com.ainovel.audit.dto.ReviewAuditTaskRequest;
import com.ainovel.audit.entity.AuditTaskEntity;
import com.ainovel.audit.mapper.AuditTaskMapper;
import com.ainovel.audit.vo.AuditTaskVO;
import com.ainovel.common.api.StandardErrorCode;
import com.ainovel.infrastructure.exception.BusinessException;
import com.ainovel.infrastructure.log.AuditAction;
import com.ainovel.novel.service.NovelAuditService;
import com.ainovel.security.auth.context.CurrentUserHolder;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ManualReviewServiceImpl implements ManualReviewService {

    private final AuditTaskMapper auditTaskMapper;
    private final NovelAuditService novelAuditService;
    private final AdminOperationLogService adminOperationLogService;

    public ManualReviewServiceImpl(AuditTaskMapper auditTaskMapper,
                                   NovelAuditService novelAuditService,
                                   AdminOperationLogService adminOperationLogService) {
        this.auditTaskMapper = auditTaskMapper;
        this.novelAuditService = novelAuditService;
        this.adminOperationLogService = adminOperationLogService;
    }

    @Override
    @Transactional
    public AuditTaskVO review(Long taskId, ReviewAuditTaskRequest request) {
        AuditTaskEntity task = auditTaskMapper.findById(taskId);
        if (task == null) {
            throw new BusinessException(StandardErrorCode.INVALID_REQUEST, "audit task not found");
        }
        if (task.getAuditStatus() != AuditStatus.PENDING && task.getAuditStatus() != AuditStatus.MANUAL_REVIEW) {
            throw new BusinessException(StandardErrorCode.BUSINESS_STATE_INVALID, "audit task already reviewed");
        }

        AuditStatus targetStatus = request.decision() == ReviewDecision.PASS ? AuditStatus.PASS : AuditStatus.REJECT;
        RiskLevel riskLevel = request.decision() == ReviewDecision.PASS ? RiskLevel.LOW : RiskLevel.HIGH;
        Long reviewerId = CurrentUserHolder.get().map(currentUser -> currentUser.userId()).orElse(null);
        LocalDateTime reviewedAt = LocalDateTime.now();
        auditTaskMapper.review(
                taskId,
                targetStatus,
                riskLevel,
                request.rejectReasonCode(),
                request.rejectReasonText(),
                reviewerId,
                reviewedAt);

        if (task.getBizType() == BizType.BIZ_NOVEL_INTRO || task.getBizType() == BizType.BIZ_CHAPTER_CONTENT) {
            novelAuditService.applyAuditResult(
                    String.valueOf(taskId),
                    request.decision().name(),
                    request.rejectReasonCode(),
                    request.rejectReasonText());
        }

        adminOperationLogService.record(
                AuditAction.AUDIT_MANUAL_DECIDED,
                "AUDIT_TASK",
                taskId,
                task.getAuditStatus().name(),
                targetStatus.name(),
                request.rejectReasonText() != null && !request.rejectReasonText().isBlank()
                        ? request.rejectReasonText()
                        : request.rejectReasonCode());

        AuditTaskEntity reviewed = auditTaskMapper.findById(taskId);
        return new AuditTaskVO(
                reviewed.getTaskId(),
                reviewed.getBizType(),
                reviewed.getBizId(),
                reviewed.getAuditStatus(),
                reviewed.getRiskLevel(),
                reviewed.getContentSnapshot(),
                reviewed.getContentHash(),
                reviewed.getReasonCode(),
                reviewed.getReasonText(),
                reviewed.getReviewerId(),
                reviewed.getRetryCount(),
                reviewed.getRuleVersion(),
                reviewed.getReviewedAt(),
                reviewed.getCreatedAt(),
                reviewed.getUpdatedAt());
    }
}
