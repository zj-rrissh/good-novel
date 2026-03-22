package com.ainovel.audit.service;

import com.ainovel.audit.domain.AuditStatus;
import com.ainovel.audit.domain.RiskLevel;
import com.ainovel.audit.dto.AdminAuditQuery;
import com.ainovel.audit.dto.SubmitTextAuditRequest;
import com.ainovel.audit.entity.AuditTaskEntity;
import com.ainovel.audit.mapper.AuditTaskMapper;
import com.ainovel.audit.vo.AuditTaskVO;
import com.ainovel.common.api.PageResponse;
import com.ainovel.persistence.support.HashingSupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditTaskServiceImpl implements AuditTaskService {

    private final AuditTaskMapper auditTaskMapper;

    public AuditTaskServiceImpl(AuditTaskMapper auditTaskMapper) {
        this.auditTaskMapper = auditTaskMapper;
    }

    @Override
    @Transactional
    public AuditTaskVO accept(SubmitTextAuditRequest request, String idempotencyKey) {
        String contentHash = HashingSupport.sha256(request.content());
        AuditTaskEntity existing = auditTaskMapper.findLatestByBizHash(request.bizType(), request.bizId(), contentHash);
        if (existing != null) {
            return toVO(existing);
        }

        AuditTaskEntity entity = new AuditTaskEntity();
        entity.setBizType(request.bizType());
        entity.setBizId(request.bizId());
        entity.setContentSnapshot(request.content());
        entity.setContentHash(contentHash);
        entity.setAuditStatus(AuditStatus.PENDING);
        entity.setRiskLevel(RiskLevel.MEDIUM);
        entity.setRetryCount(0);
        entity.setRuleVersion("v1");
        auditTaskMapper.insert(entity);
        return toVO(auditTaskMapper.findById(entity.getTaskId()));
    }

    @Override
    public AuditTaskVO getTask(Long taskId) {
        AuditTaskEntity entity = auditTaskMapper.findById(taskId);
        return entity == null ? null : toVO(entity);
    }

    @Override
    public PageResponse<AuditTaskVO> query(AdminAuditQuery query) {
        int page = query.page() <= 0 ? 1 : query.page();
        int size = query.size() <= 0 ? 20 : query.size();
        int offset = (page - 1) * size;
        long total = auditTaskMapper.countQuery(query.status(), query.bizType(), query.riskLevel());
        return PageResponse.of(
                auditTaskMapper.query(query.status(), query.bizType(), query.riskLevel(), offset, size)
                        .stream()
                        .map(this::toVO)
                        .toList(),
                total,
                page,
                size);
    }

    private AuditTaskVO toVO(AuditTaskEntity entity) {
        return new AuditTaskVO(
                entity.getTaskId(),
                entity.getBizType(),
                entity.getBizId(),
                entity.getAuditStatus(),
                entity.getRiskLevel(),
                entity.getReasonCode(),
                entity.getReasonText(),
                entity.getReviewerId(),
                entity.getReviewedAt(),
                entity.getCreatedAt());
    }
}
