package com.ainovel.community.service;

import com.ainovel.common.api.PageResponse;
import com.ainovel.common.api.StandardErrorCode;
import com.ainovel.community.domain.CommentStatus;
import com.ainovel.community.domain.TargetType;
import com.ainovel.community.dto.CreateCommentRequest;
import com.ainovel.community.entity.CommentEntity;
import com.ainovel.community.mapper.CommentMapper;
import com.ainovel.community.vo.CommentVO;
import com.ainovel.infrastructure.exception.BusinessException;
import com.ainovel.security.auth.context.CurrentUserHolder;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class CommentServiceImpl implements CommentService {

    private static final Duration DUPLICATE_WINDOW = Duration.ofSeconds(30);

    private final CommentMapper commentMapper;

    public CommentServiceImpl(CommentMapper commentMapper) {
        this.commentMapper = commentMapper;
    }

    @Override
    public CommentVO createComment(CreateCommentRequest request, String idempotencyKey) {
        Long userId = currentUserId();
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
        return toVO(comment);
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
        int offset = (page - 1) * size;
        List<CommentVO> records = queryBySort(sort, targetType, targetId, offset, size).stream()
                .map(this::toVO)
                .toList();

        long total = commentMapper.countVisibleByTarget(targetType, targetId);
        return PageResponse.of(records, total, page, size);
    }

    private boolean existsRecentDuplicate(Long userId,
                                          TargetType targetType,
                                          Long targetId,
                                          String normalizedContent,
                                          LocalDateTime threshold) {
        return commentMapper.existsRecentDuplicate(userId, targetType, targetId, normalizedContent, threshold);
    }

    private List<CommentEntity> queryBySort(String sort,
                                            TargetType targetType,
                                            Long targetId,
                                            int offset,
                                            int size) {
        if ("old".equalsIgnoreCase(sort)) {
            return commentMapper.queryVisibleByTargetOrderOld(targetType, targetId, offset, size);
        }
        return commentMapper.queryVisibleByTargetOrderNew(targetType, targetId, offset, size);
    }

    private Long currentUserId() {
        return CurrentUserHolder.get()
                .map(currentUser -> currentUser.userId())
                .orElseThrow(() -> new BusinessException(StandardErrorCode.UNAUTHENTICATED));
    }

    private String normalizeContent(String content) {
        return content == null ? "" : content.trim().replaceAll("\\s+", " ");
    }

    private CommentVO toVO(CommentEntity comment) {
        return new CommentVO(
                comment.getId(),
                comment.getUserId(),
                comment.getContent(),
                comment.getStatus().name(),
                comment.getCreatedAt(),
                List.of());
    }
}
