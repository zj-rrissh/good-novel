package com.ainovel.community.service;

import com.ainovel.common.api.PageResponse;
import com.ainovel.common.api.StandardErrorCode;
import com.ainovel.community.domain.Comment;
import com.ainovel.community.domain.CommentStatus;
import com.ainovel.community.domain.TargetType;
import com.ainovel.community.dto.CreateCommentRequest;
import com.ainovel.community.mapper.CommentMapper;
import com.ainovel.community.vo.CommentVO;
import com.ainovel.infrastructure.exception.BusinessException;
import com.ainovel.security.auth.context.CurrentUserHolder;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class CommentServiceImpl implements CommentService {

    private static final Duration DUPLICATE_WINDOW = Duration.ofSeconds(30);

    private final ObjectProvider<CommentMapper> commentMapperProvider;
    private final AtomicLong commentIdGenerator = new AtomicLong(1);
    private final ConcurrentMap<Long, Comment> localComments = new ConcurrentHashMap<>();

    public CommentServiceImpl(ObjectProvider<CommentMapper> commentMapperProvider) {
        this.commentMapperProvider = commentMapperProvider;
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

        Comment comment = new Comment(
                commentIdGenerator.getAndIncrement(),
                request.targetType(),
                request.targetId(),
                userId,
                request.parentId(),
                request.replyToUserId(),
                normalizedContent,
                CommentStatus.VISIBLE,
                now,
                0L);

        CommentMapper mapper = commentMapperProvider.getIfAvailable();
        if (mapper != null) {
            mapper.insert(comment);
        }
        localComments.put(comment.id(), comment);
        return toVO(comment);
    }

    @Override
    public void deleteComment(Long commentId) {
        Long userId = currentUserId();
        Comment current = localComments.get(commentId);

        CommentMapper mapper = commentMapperProvider.getIfAvailable();
        if (current == null && mapper != null) {
            current = mapper.findById(commentId).orElse(null);
        }
        if (current == null || current.status() == CommentStatus.DELETED) {
            return;
        }
        if (!Objects.equals(current.userId(), userId)) {
            throw new BusinessException(StandardErrorCode.FORBIDDEN);
        }

        if (mapper != null) {
            int updated = mapper.softDelete(commentId, current.version());
            if (updated == 0) {
                throw new BusinessException(StandardErrorCode.BUSINESS_STATE_INVALID, "comment changed concurrently");
            }
        }

        Comment deleted = new Comment(
                current.id(),
                current.targetType(),
                current.targetId(),
                current.userId(),
                current.parentId(),
                current.replyToUserId(),
                current.content(),
                CommentStatus.DELETED,
                current.createdAt(),
                current.version() == null ? 1L : current.version() + 1);
        localComments.put(commentId, deleted);
    }

    @Override
    public PageResponse<CommentVO> queryComments(TargetType targetType, Long targetId, int page, int size, String sort) {
        List<CommentVO> records = localComments.values().stream()
                .filter(comment -> comment.targetType() == targetType)
                .filter(comment -> Objects.equals(comment.targetId(), targetId))
                .filter(comment -> comment.status() == CommentStatus.VISIBLE)
                .sorted(Comparator.comparing(Comment::createdAt).reversed())
                .skip((long) (page - 1) * size)
                .limit(size)
                .map(this::toVO)
                .collect(Collectors.toList());

        long total = localComments.values().stream()
                .filter(comment -> comment.targetType() == targetType)
                .filter(comment -> Objects.equals(comment.targetId(), targetId))
                .filter(comment -> comment.status() == CommentStatus.VISIBLE)
                .count();

        return PageResponse.of(records, total, page, size);
    }

    private boolean existsRecentDuplicate(Long userId,
                                          TargetType targetType,
                                          Long targetId,
                                          String normalizedContent,
                                          LocalDateTime threshold) {
        CommentMapper mapper = commentMapperProvider.getIfAvailable();
        if (mapper != null && mapper.existsRecentDuplicate(userId, targetType, targetId, normalizedContent, threshold)) {
            return true;
        }
        return localComments.values().stream()
                .filter(comment -> Objects.equals(comment.userId(), userId))
                .filter(comment -> comment.targetType() == targetType)
                .filter(comment -> Objects.equals(comment.targetId(), targetId))
                .filter(comment -> comment.status() != CommentStatus.DELETED)
                .filter(comment -> Objects.equals(comment.content(), normalizedContent))
                .anyMatch(comment -> !comment.createdAt().isBefore(threshold));
    }

    private Long currentUserId() {
        return CurrentUserHolder.get()
                .map(currentUser -> currentUser.userId())
                .orElseThrow(() -> new BusinessException(StandardErrorCode.UNAUTHENTICATED));
    }

    private String normalizeContent(String content) {
        return content == null ? "" : content.trim().replaceAll("\\s+", " ");
    }

    private CommentVO toVO(Comment comment) {
        return new CommentVO(
                comment.id(),
                comment.userId(),
                comment.content(),
                comment.status().name(),
                comment.createdAt(),
                List.of());
    }
}
