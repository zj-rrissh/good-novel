package com.ainovel.novel.service;

import com.ainovel.common.api.StandardErrorCode;
import com.ainovel.infrastructure.exception.BusinessException;
import com.ainovel.novel.domain.NovelStatus;
import com.ainovel.novel.dto.CreateNovelRequest;
import com.ainovel.novel.dto.UpdateNovelRequest;
import com.ainovel.novel.entity.NovelEntity;
import com.ainovel.novel.mapper.NovelMapper;
import com.ainovel.novel.service.support.NovelDomainSupport;
import com.ainovel.novel.vo.NovelDetailVO;
import com.ainovel.persistence.support.DelimitedValueCodec;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NovelDraftServiceImpl implements NovelDraftService {

    private final NovelMapper novelMapper;
    private final NovelDomainSupport novelDomainSupport;

    public NovelDraftServiceImpl(NovelMapper novelMapper, NovelDomainSupport novelDomainSupport) {
        this.novelMapper = novelMapper;
        this.novelDomainSupport = novelDomainSupport;
    }

    @Override
    @Transactional
    public NovelDetailVO createNovel(CreateNovelRequest request, String idempotencyKey) {
        NovelEntity entity = new NovelEntity();
        entity.setAuthorId(novelDomainSupport.currentAuthorId());
        entity.setTitle(request.title().trim());
        entity.setIntro(normalizeOptionalText(request.intro()));
        entity.setCoverUrl(request.coverUrl());
        entity.setCategoryId(request.categoryId());
        entity.setTagIds(DelimitedValueCodec.formatLongSet(request.tagIds()));
        entity.setStatus(NovelStatus.DRAFT);
        entity.setLatestChapterId(null);
        entity.setWordCount(0L);
        entity.setAuditTaskId(null);
        try {
            novelMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(StandardErrorCode.INVALID_REQUEST, "novel create failed");
        }
        return novelDomainSupport.toNovelDetail(novelMapper.findById(entity.getId()));
    }

    @Override
    @Transactional
    public NovelDetailVO updateNovel(Long novelId, UpdateNovelRequest request) {
        NovelEntity existing = novelDomainSupport.requireOwnedNovel(novelId);
        if (!List.of(NovelStatus.DRAFT, NovelStatus.REJECTED).contains(existing.getStatus())) {
            throw new BusinessException(StandardErrorCode.BUSINESS_STATE_INVALID, "novel status does not allow editing");
        }

        existing.setTitle(request.title().trim());
        existing.setIntro(normalizeOptionalText(request.intro()));
        existing.setCoverUrl(request.coverUrl());
        existing.setCategoryId(request.categoryId());
        existing.setTagIds(DelimitedValueCodec.formatLongSet(request.tagIds()));
        novelMapper.updateDraft(existing);
        novelDomainSupport.invalidateNovelCaches(novelId);
        return novelDomainSupport.toNovelDetail(novelMapper.findById(novelId));
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
