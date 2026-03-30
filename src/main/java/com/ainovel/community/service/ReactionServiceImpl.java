package com.ainovel.community.service;

import com.ainovel.common.api.StandardErrorCode;
import com.ainovel.community.domain.FollowStatus;
import com.ainovel.community.domain.ReactionStatus;
import com.ainovel.community.domain.ReactionType;
import com.ainovel.community.dto.ToggleReactionRequest;
import com.ainovel.community.entity.ReactionEntity;
import com.ainovel.community.entity.UserFollowEntity;
import com.ainovel.community.mapper.ReactionMapper;
import com.ainovel.community.mapper.UserFollowMapper;
import com.ainovel.infrastructure.exception.BusinessException;
import com.ainovel.novel.mapper.ChapterMapper;
import com.ainovel.novel.mapper.NovelMapper;
import com.ainovel.security.auth.context.CurrentUserHolder;
import com.ainovel.user.mapper.UserAccountMapper;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReactionServiceImpl implements ReactionService {

    private final ReactionMapper reactionMapper;
    private final UserFollowMapper userFollowMapper;
    private final NovelMapper novelMapper;
    private final ChapterMapper chapterMapper;
    private final UserAccountMapper userAccountMapper;

    public ReactionServiceImpl(ReactionMapper reactionMapper,
                               UserFollowMapper userFollowMapper,
                               NovelMapper novelMapper,
                               ChapterMapper chapterMapper,
                               UserAccountMapper userAccountMapper) {
        this.reactionMapper = reactionMapper;
        this.userFollowMapper = userFollowMapper;
        this.novelMapper = novelMapper;
        this.chapterMapper = chapterMapper;
        this.userAccountMapper = userAccountMapper;
    }

    @Override
    @Transactional
    public void like(ToggleReactionRequest request) {
        ensureTargetExists(request);
        toggleReaction(currentUserId(), request, ReactionType.LIKE, ReactionStatus.ACTIVE);
    }

    @Override
    @Transactional
    public void unlike(ToggleReactionRequest request) {
        ensureTargetExists(request);
        toggleReaction(currentUserId(), request, ReactionType.LIKE, ReactionStatus.CANCELED);
    }

    @Override
    @Transactional
    public void favorite(ToggleReactionRequest request) {
        ensureTargetExists(request);
        toggleReaction(currentUserId(), request, ReactionType.FAVORITE, ReactionStatus.ACTIVE);
    }

    @Override
    @Transactional
    public void unfavorite(ToggleReactionRequest request) {
        ensureTargetExists(request);
        toggleReaction(currentUserId(), request, ReactionType.FAVORITE, ReactionStatus.CANCELED);
    }

    @Override
    @Transactional
    public void follow(Long targetUserId) {
        Long userId = currentUserId();
        ensureFollowTargetValid(userId, targetUserId);
        toggleFollow(userId, targetUserId, FollowStatus.ACTIVE);
    }

    @Override
    @Transactional
    public void unfollow(Long targetUserId) {
        Long userId = currentUserId();
        ensureFollowTargetValid(userId, targetUserId);
        toggleFollow(userId, targetUserId, FollowStatus.CANCELED);
    }

    private void ensureTargetExists(ToggleReactionRequest request) {
        boolean exists = switch (request.targetType()) {
            case NOVEL -> novelMapper.findById(request.targetId()) != null;
            case CHAPTER -> chapterMapper.findById(request.targetId()) != null;
        };
        if (!exists) {
            throw new BusinessException(StandardErrorCode.INVALID_REQUEST, "reaction target does not exist");
        }
    }

    private void ensureFollowTargetValid(Long userId, Long targetUserId) {
        if (targetUserId == null || targetUserId <= 0) {
            throw new BusinessException(StandardErrorCode.INVALID_REQUEST, "target user id is invalid");
        }
        if (userId.equals(targetUserId)) {
            throw new BusinessException(StandardErrorCode.BUSINESS_STATE_INVALID, "cannot follow yourself");
        }
        if (userAccountMapper.findById(targetUserId) == null) {
            throw new BusinessException(StandardErrorCode.INVALID_REQUEST, "target user does not exist");
        }
    }

    private void toggleReaction(Long userId,
                                ToggleReactionRequest request,
                                ReactionType reactionType,
                                ReactionStatus targetStatus) {
        Optional<ReactionEntity> existing = reactionMapper.findByUserAndTarget(
                userId,
                reactionType,
                request.targetType(),
                request.targetId());
        if (existing.isEmpty()) {
            if (targetStatus == ReactionStatus.CANCELED) {
                return;
            }
            ReactionEntity entity = new ReactionEntity();
            entity.setReactionType(reactionType);
            entity.setTargetType(request.targetType());
            entity.setTargetId(request.targetId());
            entity.setUserId(userId);
            entity.setStatus(ReactionStatus.ACTIVE);
            reactionMapper.insert(entity);
            return;
        }
        ReactionEntity entity = existing.get();
        if (entity.getStatus() == targetStatus) {
            return;
        }
        reactionMapper.updateStatus(entity.getId(), targetStatus);
    }

    private void toggleFollow(Long userId, Long targetUserId, FollowStatus targetStatus) {
        Optional<UserFollowEntity> existing = userFollowMapper.findByUserAndTarget(userId, targetUserId);
        if (existing.isEmpty()) {
            if (targetStatus == FollowStatus.CANCELED) {
                return;
            }
            UserFollowEntity entity = new UserFollowEntity();
            entity.setUserId(userId);
            entity.setTargetUserId(targetUserId);
            entity.setStatus(FollowStatus.ACTIVE);
            userFollowMapper.insert(entity);
            return;
        }
        UserFollowEntity entity = existing.get();
        if (entity.getStatus() == targetStatus) {
            return;
        }
        userFollowMapper.updateStatus(entity.getId(), targetStatus);
    }

    private Long currentUserId() {
        return CurrentUserHolder.get()
                .map(currentUser -> currentUser.userId())
                .orElseThrow(() -> new BusinessException(StandardErrorCode.UNAUTHENTICATED));
    }
}
