package com.ainovel.novel.service;

import com.ainovel.common.api.StandardErrorCode;
import com.ainovel.infrastructure.exception.BusinessException;
import com.ainovel.novel.domain.ChapterStatus;
import com.ainovel.novel.domain.NovelStatus;
import com.ainovel.novel.dto.CreateChapterRequest;
import com.ainovel.novel.dto.UpdateChapterRequest;
import com.ainovel.novel.entity.ChapterEntity;
import com.ainovel.novel.entity.NovelEntity;
import com.ainovel.novel.mapper.ChapterMapper;
import com.ainovel.novel.service.support.NovelDomainSupport;
import com.ainovel.novel.vo.NovelChapterVO;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChapterManagementServiceImpl implements ChapterManagementService {

    private final ChapterMapper chapterMapper;
    private final NovelDomainSupport novelDomainSupport;

    public ChapterManagementServiceImpl(ChapterMapper chapterMapper,
                                        NovelDomainSupport novelDomainSupport) {
        this.chapterMapper = chapterMapper;
        this.novelDomainSupport = novelDomainSupport;
    }

    @Override
    @Transactional
    public NovelChapterVO createChapter(Long novelId, CreateChapterRequest request, String idempotencyKey) {
        NovelEntity novel = novelDomainSupport.requireOwnedNovel(novelId);
        if (novel.getStatus() == NovelStatus.BANNED) {
            throw new BusinessException(StandardErrorCode.BUSINESS_STATE_INVALID, "novel is banned");
        }

        ChapterEntity entity = new ChapterEntity();
        entity.setNovelId(novelId);
        entity.setChapterNo(request.chapterNo());
        entity.setTitle(request.title().trim());
        entity.setContent(request.content().trim());
        entity.setStatus(ChapterStatus.DRAFT);
        try {
            chapterMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(StandardErrorCode.INVALID_REQUEST, "chapter number already exists");
        }
        novelDomainSupport.invalidateNovelCaches(novelId, List.of(entity.getId()));
        return novelDomainSupport.toChapterVO(chapterMapper.findById(entity.getId()));
    }

    @Override
    @Transactional
    public NovelChapterVO updateChapter(Long chapterId, UpdateChapterRequest request) {
        ChapterEntity existing = novelDomainSupport.requireOwnedChapter(chapterId);
        if (!List.of(ChapterStatus.DRAFT, ChapterStatus.REJECTED).contains(existing.getStatus())) {
            throw new BusinessException(StandardErrorCode.BUSINESS_STATE_INVALID, "chapter status does not allow editing");
        }

        existing.setChapterNo(request.chapterNo());
        existing.setTitle(request.title().trim());
        existing.setContent(request.content().trim());
        try {
            chapterMapper.updateDraft(existing);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(StandardErrorCode.INVALID_REQUEST, "chapter number already exists");
        }
        novelDomainSupport.invalidateNovelCaches(existing.getNovelId(), List.of(chapterId));
        return novelDomainSupport.toChapterVO(chapterMapper.findById(chapterId));
    }
}
