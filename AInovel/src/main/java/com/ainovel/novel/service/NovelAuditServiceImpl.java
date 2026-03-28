package com.ainovel.novel.service;

import com.ainovel.audit.domain.AuditStatus;
import com.ainovel.audit.domain.BizType;
import com.ainovel.audit.domain.RiskLevel;
import com.ainovel.audit.entity.AuditTaskEntity;
import com.ainovel.audit.mapper.AuditTaskMapper;
import com.ainovel.common.api.StandardErrorCode;
import com.ainovel.infrastructure.exception.BusinessException;
import com.ainovel.novel.domain.ChapterStatus;
import com.ainovel.novel.domain.NovelStatus;
import com.ainovel.novel.dto.SubmitNovelAuditRequest;
import com.ainovel.novel.entity.ChapterEntity;
import com.ainovel.novel.entity.NovelEntity;
import com.ainovel.novel.mapper.ChapterMapper;
import com.ainovel.novel.mapper.NovelMapper;
import com.ainovel.novel.service.support.NovelDomainSupport;
import com.ainovel.persistence.support.HashingSupport;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NovelAuditServiceImpl implements NovelAuditService {

    private final AuditTaskMapper auditTaskMapper;
    private final NovelMapper novelMapper;
    private final ChapterMapper chapterMapper;
    private final NovelDomainSupport novelDomainSupport;

    public NovelAuditServiceImpl(AuditTaskMapper auditTaskMapper,
                                 NovelMapper novelMapper,
                                 ChapterMapper chapterMapper,
                                 NovelDomainSupport novelDomainSupport) {
        this.auditTaskMapper = auditTaskMapper;
        this.novelMapper = novelMapper;
        this.chapterMapper = chapterMapper;
        this.novelDomainSupport = novelDomainSupport;
    }

    @Override
    @Transactional
    public String submitAudit(Long novelId, SubmitNovelAuditRequest request, String idempotencyKey) {
        NovelEntity novel = novelDomainSupport.requireOwnedNovel(novelId);
        Set<Long> chapterIds = resolveChapterIds(novelId, request.chapterIds());
        List<ChapterEntity> chapters = chapterMapper.findByNovelIdAndIds(novelId, chapterIds);
        if (chapters.isEmpty()) {
            throw new BusinessException(StandardErrorCode.BUSINESS_STATE_INVALID, "at least one chapter is required for audit");
        }
        if (!chapters.stream().allMatch(chapter -> List.of(ChapterStatus.DRAFT, ChapterStatus.REJECTED).contains(chapter.getStatus()))) {
            throw new BusinessException(StandardErrorCode.BUSINESS_STATE_INVALID, "only draft or rejected chapters can be submitted");
        }

        String snapshot = buildSnapshot(novel, chapters, request.reason());
        String contentHash = HashingSupport.sha256(snapshot);
        AuditTaskEntity existing = auditTaskMapper.findLatestByBizHash(BizType.BIZ_NOVEL_INTRO, novelId, contentHash);
        if (existing != null && List.of(AuditStatus.PENDING, AuditStatus.MANUAL_REVIEW).contains(existing.getAuditStatus())) {
            return String.valueOf(existing.getTaskId());
        }
        if (existing != null && List.of(AuditStatus.PASS, AuditStatus.REJECT, AuditStatus.FAILED).contains(existing.getAuditStatus())) {
            return String.valueOf(existing.getTaskId());
        }

        AuditTaskEntity entity = new AuditTaskEntity();
        entity.setBizType(BizType.BIZ_NOVEL_INTRO);
        entity.setBizId(novelId);
        entity.setContentSnapshot(snapshot);
        entity.setContentHash(contentHash);
        entity.setAuditStatus(AuditStatus.PENDING);
        entity.setRiskLevel(RiskLevel.MEDIUM);
        entity.setRetryCount(0);
        entity.setRuleVersion("v1");
        auditTaskMapper.insert(entity);

        String auditTaskId = String.valueOf(entity.getTaskId());
        novelMapper.markPendingAudit(novelId, auditTaskId, NovelStatus.PENDING_AUDIT, List.of(NovelStatus.DRAFT, NovelStatus.REJECTED));
        chapterMapper.markPendingAudit(
                novelId,
                chapterIds,
                auditTaskId,
                ChapterStatus.PENDING_AUDIT,
                List.of(ChapterStatus.DRAFT, ChapterStatus.REJECTED));
        novelDomainSupport.invalidateNovelCaches(novelId, chapterIds);
        return auditTaskId;
    }

    @Override
    @Transactional
    public void applyAuditResult(String auditTaskId, String decision, String reasonCode, String reasonText) {
        if (auditTaskId == null || auditTaskId.isBlank()) {
            throw new BusinessException(StandardErrorCode.INVALID_REQUEST, "audit task id is required");
        }
        NovelEntity novel = novelMapper.findByAuditTaskId(auditTaskId);
        if (novel == null) {
            return;
        }

        if ("PASS".equalsIgnoreCase(decision)) {
            chapterMapper.applyAuditResultByTaskId(auditTaskId, ChapterStatus.PUBLISHED, LocalDateTime.now());
            novelMapper.applyAuditResultByTaskId(auditTaskId, NovelStatus.PUBLISHED);
            novelDomainSupport.refreshNovelStatistics(novel.getId());
        } else {
            chapterMapper.applyAuditResultByTaskId(auditTaskId, ChapterStatus.REJECTED, null);
            novelMapper.applyAuditResultByTaskId(auditTaskId, NovelStatus.REJECTED);
        }
        novelDomainSupport.invalidateNovelCaches(novel.getId());
    }

    private Set<Long> resolveChapterIds(Long novelId, Set<Long> requestedIds) {
        if (requestedIds != null && !requestedIds.isEmpty()) {
            return requestedIds.stream()
                    .filter(id -> id != null && id > 0)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
        return chapterMapper.findByNovelId(novelId).stream()
                .filter(chapter -> List.of(ChapterStatus.DRAFT, ChapterStatus.REJECTED).contains(chapter.getStatus()))
                .map(ChapterEntity::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String buildSnapshot(NovelEntity novel, List<ChapterEntity> chapters, String submitReason) {
        String chapterBlock = chapters.stream()
                .map(chapter -> "#" + chapter.getChapterNo() + " " + chapter.getTitle() + "\n" + chapter.getContent())
                .collect(Collectors.joining("\n\n"));
        return """
                title:%s
                intro:%s
                reason:%s
                chapters:
                %s
                """.formatted(novel.getTitle(), novel.getIntro(), submitReason == null ? "" : submitReason.trim(), chapterBlock);
    }
}
