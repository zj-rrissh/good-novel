package com.ainovel.audit.service;

import com.ainovel.audit.domain.AuditStatus;
import com.ainovel.audit.domain.AuditTask;
import com.ainovel.audit.domain.BizType;
import com.ainovel.audit.engine.AuditExecutionResult;
import com.ainovel.audit.engine.NovelIntroAuditExecutor;
import com.ainovel.audit.entity.AuditTaskEntity;
import com.ainovel.audit.mapper.AuditTaskMapper;
import com.ainovel.novel.service.NovelAuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditTaskExecutionService {

    private final AuditTaskMapper auditTaskMapper;
    private final NovelIntroAuditExecutor novelIntroAuditExecutor;
    private final NovelAuditService novelAuditService;

    public AuditTaskExecutionService(AuditTaskMapper auditTaskMapper,
                                     NovelIntroAuditExecutor novelIntroAuditExecutor,
                                     NovelAuditService novelAuditService) {
        this.auditTaskMapper = auditTaskMapper;
        this.novelIntroAuditExecutor = novelIntroAuditExecutor;
        this.novelAuditService = novelAuditService;
    }

    @Transactional
    public void execute(Long taskId) {
        AuditTaskEntity task = auditTaskMapper.findById(taskId);
        if (task == null) {
            return;
        }
        if (task.getAuditStatus() != AuditStatus.PENDING) {
            return;
        }
        if (task.getBizType() != BizType.BIZ_NOVEL_INTRO) {
            return;
        }

        AuditExecutionResult result = novelIntroAuditExecutor.execute(toDomain(task));
        int updatedRows = auditTaskMapper.updateExecutionResult(
                taskId,
                result.auditStatus(),
                result.riskLevel(),
                result.reasonCode(),
                result.reasonText());
        if (updatedRows == 0) {
            return;
        }

        if (result.auditStatus() == AuditStatus.PASS || result.auditStatus() == AuditStatus.REJECT) {
            novelAuditService.applyAuditResult(
                    String.valueOf(taskId),
                    result.auditStatus().name(),
                    result.reasonCode(),
                    result.reasonText());
        }
    }

    private AuditTask toDomain(AuditTaskEntity entity) {
        return new AuditTask(
                entity.getTaskId(),
                entity.getBizType(),
                entity.getBizId(),
                entity.getContentSnapshot(),
                entity.getContentHash(),
                entity.getAuditStatus(),
                entity.getRiskLevel(),
                entity.getReasonCode(),
                entity.getReasonText(),
                entity.getReviewerId(),
                entity.getRetryCount(),
                entity.getRuleVersion(),
                entity.getReviewedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
