package com.ainovel.reading.service;

import com.ainovel.common.api.StandardErrorCode;
import com.ainovel.infrastructure.exception.BusinessException;
import com.ainovel.novel.domain.ChapterStatus;
import com.ainovel.novel.domain.NovelStatus;
import com.ainovel.novel.entity.ChapterEntity;
import com.ainovel.novel.entity.NovelEntity;
import com.ainovel.novel.mapper.ChapterMapper;
import com.ainovel.novel.mapper.NovelMapper;
import org.springframework.stereotype.Service;

@Service
public class ReadAccessGuardServiceImpl implements ReadAccessGuardService {

    private final NovelMapper novelMapper;
    private final ChapterMapper chapterMapper;

    public ReadAccessGuardServiceImpl(NovelMapper novelMapper, ChapterMapper chapterMapper) {
        this.novelMapper = novelMapper;
        this.chapterMapper = chapterMapper;
    }

    @Override
    public void verifyNovelVisible(Long novelId) {
        NovelEntity novel = novelMapper.findById(novelId);
        if (novel == null || novel.getStatus() != NovelStatus.ON_SHELF) {
            throw new BusinessException(StandardErrorCode.CONTENT_NOT_VISIBLE);
        }
    }

    @Override
    public void verifyChapterVisible(Long chapterId) {
        ChapterEntity chapter = chapterMapper.findById(chapterId);
        if (chapter == null || chapter.getStatus() != ChapterStatus.PUBLISHED) {
            throw new BusinessException(StandardErrorCode.CONTENT_NOT_VISIBLE);
        }
        verifyNovelVisible(chapter.getNovelId());
    }
}
