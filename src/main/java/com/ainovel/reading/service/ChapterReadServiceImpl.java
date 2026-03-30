package com.ainovel.reading.service;

import com.ainovel.cache.key.CacheKeyFactory;
import com.ainovel.cache.support.CacheTtlLevel;
import com.ainovel.cache.support.UnifiedCacheManager;
import com.ainovel.common.api.StandardErrorCode;
import com.ainovel.infrastructure.exception.BusinessException;
import com.ainovel.novel.domain.NovelStatus;
import com.ainovel.novel.entity.ChapterEntity;
import com.ainovel.novel.entity.NovelEntity;
import com.ainovel.novel.mapper.ChapterMapper;
import com.ainovel.novel.mapper.NovelMapper;
import com.ainovel.reading.vo.ChapterContentVO;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class ChapterReadServiceImpl implements ChapterReadService {

    private final ChapterMapper chapterMapper;
    private final NovelMapper novelMapper;
    private final UnifiedCacheManager unifiedCacheManager;
    private final CacheKeyFactory cacheKeyFactory;

    public ChapterReadServiceImpl(ChapterMapper chapterMapper,
                                  NovelMapper novelMapper,
                                  UnifiedCacheManager unifiedCacheManager,
                                  CacheKeyFactory cacheKeyFactory) {
        this.chapterMapper = chapterMapper;
        this.novelMapper = novelMapper;
        this.unifiedCacheManager = unifiedCacheManager;
        this.cacheKeyFactory = cacheKeyFactory;
    }

    @Override
    public ChapterContentVO getChapterContent(Long chapterId) {
        return unifiedCacheManager.readThrough(
                        cacheKeyFactory.chapterContent(chapterId),
                        ChapterContentVO.class,
                        CacheTtlLevel.CHAPTER_CONTENT,
                        () -> loadVisibleChapterContent(chapterId))
                .orElseThrow(() -> new BusinessException(StandardErrorCode.CONTENT_NOT_VISIBLE));
    }

    private Optional<ChapterContentVO> loadVisibleChapterContent(Long chapterId) {
        ChapterEntity chapter = chapterMapper.findPublishedById(chapterId);
        if (chapter == null) {
            return Optional.empty();
        }
        NovelEntity novel = novelMapper.findById(chapter.getNovelId());
        if (novel == null || novel.getStatus() != NovelStatus.ON_SHELF) {
            return Optional.empty();
        }
        return Optional.of(new ChapterContentVO(
                chapter.getId(),
                chapter.getNovelId(),
                chapter.getTitle(),
                chapter.getContent(),
                chapter.getContent() == null ? 0 : chapter.getContent().length(),
                chapter.getPublishedAt()));
    }
}
