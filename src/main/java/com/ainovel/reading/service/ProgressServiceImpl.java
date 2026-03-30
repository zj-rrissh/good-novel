package com.ainovel.reading.service;

import com.ainovel.cache.key.CacheKeyFactory;
import com.ainovel.cache.support.CacheTtlLevel;
import com.ainovel.cache.support.UnifiedCacheManager;
import com.ainovel.common.api.StandardErrorCode;
import com.ainovel.infrastructure.exception.BusinessException;
import com.ainovel.novel.entity.ChapterEntity;
import com.ainovel.novel.mapper.ChapterMapper;
import com.ainovel.reading.dto.UpdateReadingProgressRequest;
import com.ainovel.reading.entity.ReadingProgressEntity;
import com.ainovel.reading.mapper.ReadingProgressMapper;
import com.ainovel.reading.vo.ReadingProgressVO;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProgressServiceImpl implements ProgressService {

    private final ReadingProgressMapper readingProgressMapper;
    private final ChapterMapper chapterMapper;
    private final UnifiedCacheManager unifiedCacheManager;
    private final CacheKeyFactory cacheKeyFactory;

    public ProgressServiceImpl(ReadingProgressMapper readingProgressMapper,
                               ChapterMapper chapterMapper,
                               UnifiedCacheManager unifiedCacheManager,
                               CacheKeyFactory cacheKeyFactory) {
        this.readingProgressMapper = readingProgressMapper;
        this.chapterMapper = chapterMapper;
        this.unifiedCacheManager = unifiedCacheManager;
        this.cacheKeyFactory = cacheKeyFactory;
    }

    @Override
    @Transactional
    public ReadingProgressVO saveProgress(Long userId, UpdateReadingProgressRequest request, String idempotencyKey) {
        ChapterEntity chapter = chapterMapper.findById(request.chapterId());
        if (chapter == null || !chapter.getNovelId().equals(request.novelId())) {
            throw new BusinessException(StandardErrorCode.INVALID_REQUEST, "chapter does not belong to novel");
        }
        String cacheKey = cacheKeyFactory.readingProgress(userId, request.novelId());
        return unifiedCacheManager.writeDbThenInvalidate(cacheKey, () -> upsertProgress(userId, request));
    }

    @Override
    public ReadingProgressVO getProgress(Long userId, Long novelId) {
        return unifiedCacheManager.readThrough(
                        cacheKeyFactory.readingProgress(userId, novelId),
                        ReadingProgressVO.class,
                        CacheTtlLevel.DETAIL,
                        () -> Optional.ofNullable(readingProgressMapper.findByUserAndNovel(userId, novelId)).map(this::toVO))
                .orElse(null);
    }

    private ReadingProgressVO upsertProgress(Long userId, UpdateReadingProgressRequest request) {
        ReadingProgressEntity existing = readingProgressMapper.findByUserAndNovel(userId, request.novelId());
        if (existing == null) {
            ReadingProgressEntity entity = new ReadingProgressEntity();
            entity.setUserId(userId);
            entity.setNovelId(request.novelId());
            entity.setChapterId(request.chapterId());
            entity.setProgressPercent(request.progressPercent());
            entity.setPageOffset(Optional.ofNullable(request.pageOffset()).orElse(0L));
            readingProgressMapper.insert(entity);
            return toVO(readingProgressMapper.findByUserAndNovel(userId, request.novelId()));
        }
        existing.setChapterId(request.chapterId());
        existing.setProgressPercent(request.progressPercent());
        existing.setPageOffset(Optional.ofNullable(request.pageOffset()).orElse(0L));
        readingProgressMapper.update(existing);
        return toVO(readingProgressMapper.findByUserAndNovel(userId, request.novelId()));
    }

    private ReadingProgressVO toVO(ReadingProgressEntity entity) {
        return new ReadingProgressVO(
                entity.getNovelId(),
                entity.getChapterId(),
                entity.getProgressPercent(),
                entity.getPageOffset(),
                entity.getUpdatedAt());
    }
}
