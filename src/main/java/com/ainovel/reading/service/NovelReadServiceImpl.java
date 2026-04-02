package com.ainovel.reading.service;

import com.ainovel.cache.key.CacheKeyFactory;
import com.ainovel.cache.support.CacheTtlLevel;
import com.ainovel.cache.support.UnifiedCacheManager;
import com.ainovel.common.api.PageResponse;
import com.ainovel.common.api.StandardErrorCode;
import com.ainovel.infrastructure.exception.BusinessException;
import com.ainovel.novel.domain.Category;
import com.ainovel.novel.domain.NovelStatus;
import com.ainovel.novel.entity.ChapterEntity;
import com.ainovel.novel.entity.NovelEntity;
import com.ainovel.novel.mapper.ChapterMapper;
import com.ainovel.novel.mapper.NovelMapper;
import com.ainovel.persistence.support.DelimitedValueCodec;
import com.ainovel.reading.vo.ChapterMetaVO;
import com.ainovel.reading.vo.ReadingNovelDetailVO;
import com.ainovel.user.entity.UserAccountEntity;
import com.ainovel.user.entity.UserProfileEntity;
import com.ainovel.user.mapper.UserAccountMapper;
import com.ainovel.user.mapper.UserProfileMapper;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class NovelReadServiceImpl implements NovelReadService {

    private final NovelMapper novelMapper;
    private final ChapterMapper chapterMapper;
    private final UserAccountMapper userAccountMapper;
    private final UserProfileMapper userProfileMapper;
    private final UnifiedCacheManager unifiedCacheManager;
    private final CacheKeyFactory cacheKeyFactory;

    public NovelReadServiceImpl(NovelMapper novelMapper,
                                ChapterMapper chapterMapper,
                                UserAccountMapper userAccountMapper,
                                UserProfileMapper userProfileMapper,
                                UnifiedCacheManager unifiedCacheManager,
                                CacheKeyFactory cacheKeyFactory) {
        this.novelMapper = novelMapper;
        this.chapterMapper = chapterMapper;
        this.userAccountMapper = userAccountMapper;
        this.userProfileMapper = userProfileMapper;
        this.unifiedCacheManager = unifiedCacheManager;
        this.cacheKeyFactory = cacheKeyFactory;
    }

    @Override
    public ReadingNovelDetailVO getNovelDetail(Long novelId) {
        return unifiedCacheManager.readThrough(
                        cacheKeyFactory.novelDetail(novelId),
                        ReadingNovelDetailVO.class,
                        CacheTtlLevel.DETAIL,
                        () -> loadVisibleNovelDetail(novelId))
                .orElseThrow(() -> new BusinessException(StandardErrorCode.CONTENT_NOT_VISIBLE));
    }

    @Override
    public PageResponse<ChapterMetaVO> getChapterPage(Long novelId, int page, int size) {
        ChapterMetaVO[] chapters = unifiedCacheManager.readThrough(
                        cacheKeyFactory.novelChapters(novelId),
                        ChapterMetaVO[].class,
                        CacheTtlLevel.DETAIL,
                        () -> loadVisibleChapterCatalog(novelId))
                .orElseThrow(() -> new BusinessException(StandardErrorCode.CONTENT_NOT_VISIBLE));

        int offset = (page - 1) * size;
        if (offset >= chapters.length) {
            return PageResponse.of(List.of(), chapters.length, page, size);
        }
        int end = Math.min(chapters.length, offset + size);
        return PageResponse.of(Arrays.asList(chapters).subList(offset, end), chapters.length, page, size);
    }

    private Optional<ReadingNovelDetailVO> loadVisibleNovelDetail(Long novelId) {
        NovelEntity novel = novelMapper.findById(novelId);
        if (novel == null || novel.getStatus() != NovelStatus.ON_SHELF) {
            return Optional.empty();
        }
        UserProfileEntity authorProfile = userProfileMapper.findByUserId(novel.getAuthorId());
        UserAccountEntity authorAccount = userAccountMapper.findById(novel.getAuthorId());
        String authorName = authorProfile != null && authorProfile.getNickname() != null && !authorProfile.getNickname().isBlank()
                ? authorProfile.getNickname()
                : authorAccount == null ? "unknown" : authorAccount.getUsername();

        Set<String> tags = DelimitedValueCodec.parseLongSet(novel.getTagIds()).stream()
                .map(tagId -> "tag-" + tagId)
                .collect(Collectors.toSet());

        return Optional.of(new ReadingNovelDetailVO(
                novel.getId(),
                novel.getTitle(),
                novel.getIntro(),
                novel.getCoverUrl(),
                authorName,
                Category.getNameById(novel.getCategoryId()),
                tags,
                novel.getLatestChapterId()));
    }

    private Optional<ChapterMetaVO[]> loadVisibleChapterCatalog(Long novelId) {
        NovelEntity novel = novelMapper.findById(novelId);
        if (novel == null || novel.getStatus() != NovelStatus.ON_SHELF) {
            return Optional.empty();
        }
        List<ChapterMetaVO> chapters = chapterMapper.findPublishedByNovelId(novelId).stream()
                .map(this::toChapterMeta)
                .toList();
        return Optional.of(chapters.toArray(new ChapterMetaVO[0]));
    }

    private ChapterMetaVO toChapterMeta(ChapterEntity entity) {
        return new ChapterMetaVO(
                entity.getId(),
                entity.getChapterNo(),
                entity.getTitle(),
                entity.getStatus().name(),
                entity.getPublishedAt(),
                entity.getContent() == null ? 0 : entity.getContent().length());
    }
}
