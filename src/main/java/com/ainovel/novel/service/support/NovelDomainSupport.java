package com.ainovel.novel.service.support;

import com.ainovel.cache.key.CacheKeyFactory;
import com.ainovel.cache.support.UnifiedCacheManager;
import com.ainovel.common.api.StandardErrorCode;
import com.ainovel.infrastructure.exception.BusinessException;
import com.ainovel.novel.entity.ChapterEntity;
import com.ainovel.novel.entity.NovelEntity;
import com.ainovel.novel.mapper.ChapterMapper;
import com.ainovel.novel.mapper.NovelMapper;
import com.ainovel.novel.vo.NovelChapterVO;
import com.ainovel.novel.vo.NovelDetailVO;
import com.ainovel.persistence.support.DelimitedValueCodec;
import com.ainovel.security.auth.context.CurrentUser;
import com.ainovel.security.auth.context.CurrentUserHolder;
import com.ainovel.security.auth.rbac.RoleType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class NovelDomainSupport {

    private final NovelMapper novelMapper;
    private final ChapterMapper chapterMapper;
    private final UnifiedCacheManager unifiedCacheManager;
    private final CacheKeyFactory cacheKeyFactory;

    public NovelDomainSupport(NovelMapper novelMapper,
                              ChapterMapper chapterMapper,
                              UnifiedCacheManager unifiedCacheManager,
                              CacheKeyFactory cacheKeyFactory) {
        this.novelMapper = novelMapper;
        this.chapterMapper = chapterMapper;
        this.unifiedCacheManager = unifiedCacheManager;
        this.cacheKeyFactory = cacheKeyFactory;
    }

    public Long currentAuthorId() {
        CurrentUser currentUser = CurrentUserHolder.get()
                .orElseThrow(() -> new BusinessException(StandardErrorCode.UNAUTHENTICATED));
        if (!currentUser.hasRole(RoleType.AUTHOR) && !currentUser.hasRole(RoleType.ADMIN)) {
            throw new BusinessException(StandardErrorCode.FORBIDDEN);
        }
        return currentUser.userId();
    }

    public NovelEntity requireNovel(Long novelId) {
        NovelEntity novel = novelMapper.findById(novelId);
        if (novel == null) {
            throw new BusinessException(StandardErrorCode.INVALID_REQUEST, "novel not found");
        }
        return novel;
    }

    public NovelEntity requireOwnedNovel(Long novelId) {
        NovelEntity novel = requireNovel(novelId);
        if (!novel.getAuthorId().equals(currentAuthorId())) {
            throw new BusinessException(StandardErrorCode.FORBIDDEN);
        }
        return novel;
    }

    public ChapterEntity requireOwnedChapter(Long chapterId) {
        ChapterEntity chapter = chapterMapper.findById(chapterId);
        if (chapter == null) {
            throw new BusinessException(StandardErrorCode.INVALID_REQUEST, "chapter not found");
        }
        NovelEntity novel = requireOwnedNovel(chapter.getNovelId());
        if (!novel.getId().equals(chapter.getNovelId())) {
            throw new BusinessException(StandardErrorCode.FORBIDDEN);
        }
        return chapter;
    }

    public NovelDetailVO toNovelDetail(NovelEntity novel) {
        List<NovelChapterVO> chapters = chapterMapper.findByNovelId(novel.getId()).stream()
                .map(this::toChapterVO)
                .toList();
        return new NovelDetailVO(
                novel.getId(),
                novel.getTitle(),
                novel.getIntro(),
                novel.getCoverUrl(),
                novel.getCategoryId(),
                DelimitedValueCodec.parseLongSet(novel.getTagIds()),
                novel.getStatus(),
                novel.getLatestChapterId(),
                Optional.ofNullable(novel.getWordCount()).orElse(0L),
                chapters);
    }

    public NovelChapterVO toChapterVO(ChapterEntity chapter) {
        return new NovelChapterVO(
                chapter.getId(),
                chapter.getChapterNo(),
                chapter.getTitle(),
                chapter.getStatus(),
                chapter.getPublishedAt(),
                chapter.getContent() == null ? 0 : chapter.getContent().length());
    }

    public void refreshNovelStatistics(Long novelId) {
        Long wordCount = Optional.ofNullable(chapterMapper.sumPublishedWordCount(novelId)).orElse(0L);
        Long latestChapterId = chapterMapper.findLatestPublishedChapterId(novelId);
        novelMapper.updateStatistics(novelId, latestChapterId, wordCount);
    }

    public void invalidateNovelCaches(Long novelId) {
        invalidateNovelCaches(novelId, chapterMapper.findIdsByNovelId(novelId));
    }

    public void invalidateNovelCaches(Long novelId, Collection<Long> chapterIds) {
        List<String> cacheKeys = new ArrayList<>();
        cacheKeys.add(cacheKeyFactory.novelDetail(novelId));
        cacheKeys.add(cacheKeyFactory.novelChapters(novelId));
        if (chapterIds != null) {
            chapterIds.stream()
                    .filter(id -> id != null && id > 0)
                    .map(cacheKeyFactory::chapterContent)
                    .forEach(cacheKeys::add);
        }
        unifiedCacheManager.invalidate(cacheKeys);
    }
}
