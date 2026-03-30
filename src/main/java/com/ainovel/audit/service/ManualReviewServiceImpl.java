package com.ainovel.audit.service;

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
import com.ainovel.novel.service.NovelAuditService;
import com.ainovel.security.auth.context.CurrentUserHolder;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ManualReviewServiceImpl implements ManualReviewService {

    private final AuditTaskMapper auditTaskMapper;
    private final NovelAuditService novelAuditService;

    public ManualReviewServiceImpl(AuditTaskMapper auditTaskMapper, NovelAuditService novelAuditService) {
        this.auditTaskMapper = auditTaskMapper;
        this.novelAuditService = novelAuditService;
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

        AuditTaskEntity reviewed = auditTaskMapper.findById(taskId);
        return new AuditTaskVO(
                reviewed.getTaskId(),
                reviewed.getBizType(),
                reviewed.getBizId(),
                reviewed.getAuditStatus(),
                reviewed.getRiskLevel(),
                reviewed.getReasonCode(),
                reviewed.getReasonText(),
                reviewed.getReviewerId(),
                reviewed.getReviewedAt(),
                reviewed.getCreatedAt());
    }
}
