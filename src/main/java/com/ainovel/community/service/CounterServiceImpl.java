package com.ainovel.community.service;

import com.ainovel.common.api.StandardErrorCode;
import com.ainovel.community.domain.ReactionType;
import com.ainovel.community.domain.TargetType;
import com.ainovel.community.mapper.CommunityPartitionMapper;
import com.ainovel.community.mapper.CommentMapper;
import com.ainovel.community.mapper.ReactionMapper;
import com.ainovel.community.mapper.UserFollowMapper;
import com.ainovel.community.vo.CounterSummaryVO;
import com.ainovel.infrastructure.exception.BusinessException;
import com.ainovel.novel.entity.ChapterEntity;
import com.ainovel.novel.entity.NovelEntity;
import com.ainovel.novel.mapper.ChapterMapper;
import com.ainovel.novel.mapper.NovelMapper;
import com.ainovel.security.auth.context.CurrentUserHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CounterServiceImpl implements CounterService {

    private final CommentMapper commentMapper;
    private final ReactionMapper reactionMapper;
    private final UserFollowMapper userFollowMapper;
    private final CommunityPartitionMapper communityPartitionMapper;
    private final NovelMapper novelMapper;
    private final ChapterMapper chapterMapper;

    public CounterServiceImpl(CommentMapper commentMapper,
                              ReactionMapper reactionMapper,
                              UserFollowMapper userFollowMapper,
                              CommunityPartitionMapper communityPartitionMapper,
                              NovelMapper novelMapper,
                              ChapterMapper chapterMapper) {
        this.commentMapper = commentMapper;
        this.reactionMapper = reactionMapper;
        this.userFollowMapper = userFollowMapper;
        this.communityPartitionMapper = communityPartitionMapper;
        this.novelMapper = novelMapper;
        this.chapterMapper = chapterMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public CounterSummaryVO getCounters(TargetType targetType, Long targetId) {
        Long authorId = resolveTargetAuthorId(targetType, targetId);
        long commentCount = commentMapper.countVisibleByTarget(targetType, targetId);
        long likeCount = reactionMapper.countActiveByTarget(ReactionType.LIKE, targetType, targetId);
        long favoriteCount = reactionMapper.countActiveByTarget(ReactionType.FAVORITE, targetType, targetId);
        long followerCount = authorId == null ? 0L : userFollowMapper.countActiveFollowers(authorId);

        Long currentUserId = CurrentUserHolder.get().map(currentUser -> currentUser.userId()).orElse(null);
        boolean liked = currentUserId != null
                && reactionMapper.existsActiveByUserAndTarget(currentUserId, ReactionType.LIKE, targetType, targetId);
        boolean favorited = currentUserId != null
                && reactionMapper.existsActiveByUserAndTarget(currentUserId, ReactionType.FAVORITE, targetType, targetId);
        boolean followed = currentUserId != null
                && authorId != null
                && userFollowMapper.existsActiveFollow(currentUserId, authorId);

        return new CounterSummaryVO(
                targetType.name(),
                targetId,
                commentCount,
                likeCount,
                favoriteCount,
                followerCount,
                liked,
                favorited,
                followed);
    }

    private Long resolveTargetAuthorId(TargetType targetType, Long targetId) {
        return switch (targetType) {
            case NOVEL -> {
                NovelEntity novel = novelMapper.findById(targetId);
                if (novel == null) {
                    throw new BusinessException(StandardErrorCode.INVALID_REQUEST, "novel target does not exist");
                }
                yield novel.getAuthorId();
            }
            case CHAPTER -> {
                ChapterEntity chapter = chapterMapper.findById(targetId);
                if (chapter == null) {
                    throw new BusinessException(StandardErrorCode.INVALID_REQUEST, "chapter target does not exist");
                }
                NovelEntity novel = novelMapper.findById(chapter.getNovelId());
                if (novel == null) {
                    throw new BusinessException(StandardErrorCode.INVALID_REQUEST, "chapter novel does not exist");
                }
                yield novel.getAuthorId();
            }
            case PARTITION -> {
                if (!communityPartitionMapper.existsActiveById(targetId)) {
                    throw new BusinessException(StandardErrorCode.INVALID_REQUEST, "partition target does not exist");
                }
                yield null;
            }
        };
    }
}
