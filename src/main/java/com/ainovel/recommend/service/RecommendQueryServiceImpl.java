package com.ainovel.recommend.service;

import com.ainovel.community.domain.ReactionType;
import com.ainovel.community.domain.TargetType;
import com.ainovel.community.mapper.ReactionMapper;
import com.ainovel.novel.domain.Category;
import com.ainovel.novel.domain.NovelStatus;
import com.ainovel.novel.entity.NovelEntity;
import com.ainovel.novel.mapper.NovelMapper;
import com.ainovel.reading.mapper.ReadingProgressMapper;
import com.ainovel.recommend.domain.RecommendItem;
import com.ainovel.recommend.vo.RecommendResultVO;
import com.ainovel.user.entity.UserProfileEntity;
import com.ainovel.user.mapper.UserProfileMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class RecommendQueryServiceImpl implements RecommendQueryService {

    private static final String DEGRADE_NONE = "NONE";
    private static final String DEGRADE_LEVEL_1 = "LEVEL_1";
    private static final String DEGRADE_LEVEL_2 = "LEVEL_2";

    private static final String SOURCE_HOME = "db-home";
    private static final String SOURCE_RELATED = "db-related";
    private static final String SOURCE_CONTINUE = "db-continue";

    private final NovelMapper novelMapper;
    private final ReadingProgressMapper readingProgressMapper;
    private final ReactionMapper reactionMapper;
    private final UserProfileMapper userProfileMapper;

    public RecommendQueryServiceImpl(NovelMapper novelMapper,
                                     ReadingProgressMapper readingProgressMapper,
                                     ReactionMapper reactionMapper,
                                     UserProfileMapper userProfileMapper) {
        this.novelMapper = novelMapper;
        this.readingProgressMapper = readingProgressMapper;
        this.reactionMapper = reactionMapper;
        this.userProfileMapper = userProfileMapper;
    }

    @Override
    public RecommendResultVO queryHome(Long userId, int size) {
        List<NovelEntity> candidates = novelMapper.queryOnShelf(Math.min(200, Math.max(size * 4, size)));
        List<RecommendItem> items = rankAndBuildItems(candidates, size);
        String subject = userId == null ? "anon" : String.valueOf(userId);
        String degradeLevel = items.isEmpty() ? DEGRADE_LEVEL_2 : DEGRADE_NONE;
        return new RecommendResultVO("HOME", subject, SOURCE_HOME, degradeLevel, items);
    }

    @Override
    public RecommendResultVO queryRelated(Long novelId, int size) {
        NovelEntity anchorNovel = novelMapper.findById(novelId);
        if (anchorNovel == null) {
            return new RecommendResultVO("RELATED", String.valueOf(novelId), SOURCE_RELATED, DEGRADE_LEVEL_2, List.of());
        }

        List<NovelEntity> sameCategoryCandidates = anchorNovel.getCategoryId() == null
                ? List.of()
                : novelMapper.queryOnShelfByCategoryExclude(anchorNovel.getCategoryId(), novelId, Math.max(size * 4, size));

        List<RecommendItem> primaryItems = rankAndBuildItems(sameCategoryCandidates, size);
        if (primaryItems.size() >= size) {
            return new RecommendResultVO("RELATED", String.valueOf(novelId), SOURCE_RELATED, DEGRADE_NONE, primaryItems);
        }

        List<Long> excludeIds = new ArrayList<>();
        excludeIds.add(novelId);
        primaryItems.forEach(item -> excludeIds.add(normalizeId(item.novelId())));

        List<NovelEntity> fallbackCandidates = novelMapper.queryOnShelfExcludeIds(excludeIds, Math.max(size * 4, size));
        List<RecommendItem> fallbackItems = rankAndBuildItems(fallbackCandidates, size - primaryItems.size());

        List<RecommendItem> combinedItems = new ArrayList<>(primaryItems);
        combinedItems.addAll(fallbackItems);
        if (combinedItems.size() > size) {
            combinedItems = combinedItems.subList(0, size);
        }

        String degradeLevel = combinedItems.isEmpty()
                ? DEGRADE_LEVEL_2
                : (fallbackItems.isEmpty() ? DEGRADE_NONE : DEGRADE_LEVEL_1);
        return new RecommendResultVO("RELATED", String.valueOf(novelId), SOURCE_RELATED, degradeLevel, combinedItems);
    }

    @Override
    public RecommendResultVO queryContinue(Long userId, int size) {
        if (userId == null) {
            return new RecommendResultVO("CONTINUE", "anon", SOURCE_CONTINUE, DEGRADE_LEVEL_2, List.of());
        }

        List<Long> recentNovelIds = readingProgressMapper.findRecentNovelIdsByUser(userId, Math.max(size * 3, size));
        if (recentNovelIds.isEmpty()) {
            return new RecommendResultVO("CONTINUE", String.valueOf(userId), SOURCE_CONTINUE, DEGRADE_LEVEL_2, List.of());
        }

        List<NovelEntity> onShelfNovels = novelMapper.queryOnShelfByIds(recentNovelIds);
        Map<Long, NovelEntity> novelById = new LinkedHashMap<>();
        for (NovelEntity novel : onShelfNovels) {
            if (novel != null && novel.getId() != null) {
                novelById.put(novel.getId(), novel);
            }
        }

        Map<Long, String> authorNameCache = new LinkedHashMap<>();
        List<RecommendItem> items = new ArrayList<>();
        for (Long novelId : recentNovelIds) {
            NovelEntity novel = novelById.get(novelId);
            if (novel == null) {
                continue;
            }
            items.add(toRecommendItem(novel, authorNameCache));
            if (items.size() >= size) {
                break;
            }
        }

        String degradeLevel = items.isEmpty() ? DEGRADE_LEVEL_2 : DEGRADE_NONE;
        return new RecommendResultVO("CONTINUE", String.valueOf(userId), SOURCE_CONTINUE, degradeLevel, items);
    }

    private List<RecommendItem> rankAndBuildItems(List<NovelEntity> novels, int size) {
        if (novels == null || novels.isEmpty() || size <= 0) {
            return List.of();
        }

        Map<Long, NovelEntity> uniqueNovels = new LinkedHashMap<>();
        for (NovelEntity novel : novels) {
            if (novel == null || novel.getId() == null) {
                continue;
            }
            uniqueNovels.putIfAbsent(novel.getId(), novel);
        }

        Map<Long, String> authorNameCache = new LinkedHashMap<>();
        List<RankedItem> rankedItems = uniqueNovels.values().stream()
                .map(novel -> new RankedItem(novel, toRecommendItem(novel, authorNameCache)))
                .sorted(Comparator.comparingDouble((RankedItem ranked) -> hotScoreValue(ranked.item())).reversed()
                        .thenComparing((RankedItem ranked) -> normalizeTime(ranked.novel().getUpdatedAt()), Comparator.reverseOrder())
                        .thenComparing((RankedItem ranked) -> normalizeId(ranked.novel().getId()), Comparator.reverseOrder()))
                .toList();

        List<RecommendItem> items = new ArrayList<>();
        for (RankedItem rankedItem : rankedItems) {
            items.add(rankedItem.item());
            if (items.size() >= size) {
                break;
            }
        }
        return items;
    }

    private RecommendItem toRecommendItem(NovelEntity novel, Map<Long, String> authorNameCache) {
        String authorName = authorNameCache.computeIfAbsent(novel.getAuthorId(), this::resolveAuthorName);
        return new RecommendItem(
                novel.getId(),
                novel.getTitle(),
                novel.getCoverUrl(),
                authorName,
                formatCategory(novel.getCategoryId()),
                formatTagsSummary(novel.getStatus(), novel.getWordCount()),
                calculateHotScore(novel));
    }

    private String resolveAuthorName(Long authorId) {
        if (authorId == null) {
            return "佚名";
        }
        UserProfileEntity profile = userProfileMapper.findByUserId(authorId);
        if (profile != null && StringUtils.hasText(profile.getNickname())) {
            return profile.getNickname();
        }
        return "user-" + authorId;
    }

    private String formatCategory(Long categoryId) {
        return Category.getNameById(categoryId);
    }

    private String formatTagsSummary(NovelStatus status, Long wordCount) {
        return statusLabel(status) + " · " + formatWordCount(wordCount);
    }

    private String statusLabel(NovelStatus status) {
        if (status == NovelStatus.PUBLISHED || status == NovelStatus.ON_SHELF) {
            return "连载中";
        }
        if (status == NovelStatus.OFF_SHELF) {
            return "已下架";
        }
        if (status == NovelStatus.BANNED) {
            return "已封禁";
        }
        return "连载中";
    }

    private String formatWordCount(Long wordCount) {
        long safeWordCount = wordCount == null ? 0L : Math.max(0L, wordCount);
        double inTenThousands = safeWordCount / 10_000D;
        String display = isWholeNumber(inTenThousands)
                ? String.format(Locale.ROOT, "%.0f", inTenThousands)
                : String.format(Locale.ROOT, "%.1f", inTenThousands);
        return display + "万字";
    }

    private boolean isWholeNumber(double value) {
        return Math.abs(value - Math.rint(value)) < 0.000001D;
    }

    private Double calculateHotScore(NovelEntity novel) {
        if (novel == null || novel.getId() == null) {
            return 0D;
        }
        long likeCount = reactionMapper.countActiveByTarget(ReactionType.LIKE, TargetType.NOVEL, novel.getId());
        long favoriteCount = reactionMapper.countActiveByTarget(ReactionType.FAVORITE, TargetType.NOVEL, novel.getId());
        double wordCountScore = Math.min((novel.getWordCount() == null ? 0L : novel.getWordCount()) / 50_000D, 20D);
        double rawScore = 55D + likeCount * 7D + favoriteCount * 11D + wordCountScore;
        double clamped = Math.max(0D, Math.min(99.9D, rawScore));
        return Math.round(clamped * 10D) / 10D;
    }

    private double hotScoreValue(RecommendItem item) {
        return item == null || item.hotScore() == null ? 0D : item.hotScore();
    }

    private Long normalizeId(Object id) {
        if (id == null) {
            return 0L;
        }
        if (id instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(Objects.toString(id, "0"));
    }

    private LocalDateTime normalizeTime(LocalDateTime time) {
        return time == null ? LocalDateTime.MIN : time;
    }

    private record RankedItem(NovelEntity novel, RecommendItem item) {
    }
}
