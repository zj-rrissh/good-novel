package com.ainovel.community.service;

import com.ainovel.common.api.PageResponse;
import com.ainovel.common.api.StandardErrorCode;
import com.ainovel.community.domain.CommentStatus;
import com.ainovel.community.domain.TargetType;
import com.ainovel.community.dto.CreateCommentRequest;
import com.ainovel.community.entity.CommentEntity;
import com.ainovel.community.mapper.CommunityPartitionMapper;
import com.ainovel.community.mapper.CommentMapper;
import com.ainovel.community.vo.CommentVO;
import com.ainovel.infrastructure.exception.BusinessException;
import com.ainovel.novel.mapper.ChapterMapper;
import com.ainovel.novel.mapper.NovelMapper;
import com.ainovel.security.auth.context.CurrentUserHolder;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class CommentServiceImpl implements CommentService {

    private static final Duration DUPLICATE_WINDOW = Duration.ofSeconds(30);

    private final CommentMapper commentMapper;
    private final CommunityPartitionMapper communityPartitionMapper;
    private final NovelMapper novelMapper;
    private final ChapterMapper chapterMapper;

    public CommentServiceImpl(CommentMapper commentMapper,
                              CommunityPartitionMapper communityPartitionMapper,
                              NovelMapper novelMapper,
                              ChapterMapper chapterMapper) {
        this.commentMapper = commentMapper;
        this.communityPartitionMapper = communityPartitionMapper;
        this.novelMapper = novelMapper;
        this.chapterMapper = chapterMapper;
    }

    @Override
    public CommentVO createComment(CreateCommentRequest request, String idempotencyKey) {
        Long userId = currentUserId();
        validateTargetExists(request.targetType(), request.targetId());
        validateReplyRelation(request);
        String normalizedContent = normalizeContent(request.content());
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold = now.minus(DUPLICATE_WINDOW);

        if (existsRecentDuplicate(userId, request.targetType(), request.targetId(), normalizedContent, threshold)) {
            throw new BusinessException(StandardErrorCode.IDEMPOTENT_CONFLICT, "duplicate comment submission");
        }

        CommentEntity comment = new CommentEntity();
        comment.setTargetType(request.targetType());
        comment.setTargetId(request.targetId());
        comment.setUserId(userId);
        comment.setParentId(request.parentId());
        comment.setReplyToUserId(request.replyToUserId());
        comment.setContent(normalizedContent);
        comment.setStatus(CommentStatus.VISIBLE);
        comment.setCreatedAt(now);
        comment.setVersion(0L);
        commentMapper.insert(comment);
        return toVO(comment, List.of());
    }

    @Override
    public void deleteComment(Long commentId) {
        Long userId = currentUserId();
        CommentEntity current = commentMapper.findById(commentId).orElse(null);
        if (current == null || current.getStatus() == CommentStatus.DELETED) {
            return;
        }
        if (!Objects.equals(current.getUserId(), userId)) {
            throw new BusinessException(StandardErrorCode.FORBIDDEN);
        }

        int updated = commentMapper.softDelete(commentId, current.getVersion());
        if (updated == 0) {
            throw new BusinessException(StandardErrorCode.BUSINESS_STATE_INVALID, "comment changed concurrently");
        }
    }

    @Override
    public PageResponse<CommentVO> queryComments(TargetType targetType, Long targetId, int page, int size, String sort) {
        validateTargetExists(targetType, targetId);
        int offset = (page - 1) * size;
        List<CommentEntity> rootComments = queryRootBySort(sort, targetType, targetId, offset, size);
        Map<Long, List<CommentVO>> repliesByParent = loadRepliesByParent(rootComments);
        List<CommentVO> records = rootComments.stream()
                .map(comment -> toVO(comment, repliesByParent.getOrDefault(comment.getId(), List.of())))
                .toList();

        long total = commentMapper.countVisibleRootByTarget(targetType, targetId);
        return PageResponse.of(records, total, page, size);
    }

    private boolean existsRecentDuplicate(Long userId,
                                          TargetType targetType,
                                          Long targetId,
                                          String normalizedContent,
                                          LocalDateTime threshold) {
        return commentMapper.existsRecentDuplicate(userId, targetType, targetId, normalizedContent, threshold);
    }

    private List<CommentEntity> queryRootBySort(String sort,
                                                TargetType targetType,
                                                Long targetId,
                                                int offset,
                                                int size) {
        if ("old".equalsIgnoreCase(sort)) {
            return commentMapper.queryVisibleRootByTargetOrderOld(targetType, targetId, offset, size);
        }
        return commentMapper.queryVisibleRootByTargetOrderNew(targetType, targetId, offset, size);
    }

    private Map<Long, List<CommentVO>> loadRepliesByParent(List<CommentEntity> rootComments) {
        if (rootComments.isEmpty()) {
            return Map.of();
        }
        List<Long> rootIds = rootComments.stream().map(CommentEntity::getId).toList();
        List<CommentEntity> replies = commentMapper.queryVisibleByParentIds(rootIds);
        Map<Long, List<CommentVO>> repliesByParent = new HashMap<>();
        for (CommentEntity reply : replies) {
            repliesByParent.computeIfAbsent(reply.getParentId(), key -> new ArrayList<>())
                    .add(toVO(reply, List.of()));
        }
        return repliesByParent;
    }

    private void validateTargetExists(TargetType targetType, Long targetId) {
        boolean exists = switch (targetType) {
            case NOVEL -> novelMapper.findById(targetId) != null;
            case CHAPTER -> chapterMapper.findById(targetId) != null;
            case PARTITION -> communityPartitionMapper.existsActiveById(targetId);
        };
        if (!exists) {
            throw new BusinessException(StandardErrorCode.INVALID_REQUEST);
        }
    }

    private void validateReplyRelation(CreateCommentRequest request) {
        if (request.parentId() == null) {
            if (request.replyToUserId() != null) {
                throw new BusinessException(StandardErrorCode.INVALID_REQUEST);
            }
            return;
        }

        CommentEntity parent = commentMapper.findById(request.parentId())
                .orElseThrow(() -> new BusinessException(StandardErrorCode.INVALID_REQUEST));
        if (parent.getStatus() == CommentStatus.DELETED || parent.getStatus() == CommentStatus.HIDDEN) {
            throw new BusinessException(StandardErrorCode.INVALID_REQUEST);
        }
        if (parent.getTargetType() != request.targetType() || !Objects.equals(parent.getTargetId(), request.targetId())) {
            throw new BusinessException(StandardErrorCode.INVALID_REQUEST);
        }
        if (request.replyToUserId() != null && !Objects.equals(request.replyToUserId(), parent.getUserId())) {
            throw new BusinessException(StandardErrorCode.INVALID_REQUEST);
        }
    }

    private Long currentUserId() {
        return CurrentUserHolder.get()
                .map(currentUser -> currentUser.userId())
                .orElseThrow(() -> new BusinessException(StandardErrorCode.UNAUTHENTICATED));
    }

    private String normalizeContent(String content) {
        return content == null ? "" : content.trim().replaceAll("\\s+", " ");
    }

    private CommentVO toVO(CommentEntity comment, List<CommentVO> replies) {
        return new CommentVO(
                comment.getId(),
                comment.getUserId(),
                comment.getContent(),
                comment.getStatus().name(),
                comment.getCreatedAt(),
                replies);
    }
}
