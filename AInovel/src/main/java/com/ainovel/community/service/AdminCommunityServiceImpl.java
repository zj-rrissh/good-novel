package com.ainovel.community.service;

import com.ainovel.admin.service.AdminOperationLogService;
import com.ainovel.common.api.PageResponse;
import com.ainovel.common.api.StandardErrorCode;
import com.ainovel.community.dto.AdminCommentQuery;
import com.ainovel.community.domain.CommentStatus;
import com.ainovel.community.entity.CommentEntity;
import com.ainovel.community.mapper.CommentMapper;
import com.ainovel.community.vo.AdminCommentVO;
import com.ainovel.infrastructure.exception.BusinessException;
import com.ainovel.infrastructure.log.AuditAction;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminCommunityServiceImpl implements AdminCommunityService {

    private final CommentMapper commentMapper;
    private final AdminOperationLogService adminOperationLogService;

    public AdminCommunityServiceImpl(CommentMapper commentMapper, AdminOperationLogService adminOperationLogService) {
        this.commentMapper = commentMapper;
        this.adminOperationLogService = adminOperationLogService;
    }

    @Override
    public PageResponse<AdminCommentVO> queryComments(AdminCommentQuery query) {
        int page = query.page() <= 0 ? 1 : query.page();
        int size = query.size() <= 0 ? 20 : query.size();
        int offset = (page - 1) * size;
        long total = commentMapper.countAdminQuery(
                query.targetType(),
                query.targetId(),
                query.status(),
                query.userId(),
                query.keyword());
        return PageResponse.of(
                commentMapper.queryAdmin(
                                query.targetType(),
                                query.targetId(),
                                query.status(),
                                query.userId(),
                                query.keyword(),
                                offset,
                                size)
                        .stream()
                        .map(this::toAdminVO)
                        .toList(),
                total,
                page,
                size);
    }

    @Override
    @Transactional
    public void hideComment(Long commentId) {
        CommentEntity target = commentMapper.findById(commentId)
                .orElseThrow(() -> new BusinessException(StandardErrorCode.INVALID_REQUEST));
        if (target.getStatus() == CommentStatus.HIDDEN || target.getStatus() == CommentStatus.DELETED) {
            return;
        }
        hideCommentTree(commentId);
        adminOperationLogService.record(
                AuditAction.COMMENT_HIDE,
                "COMMENT",
                commentId,
                target.getStatus().name(),
                CommentStatus.HIDDEN.name(),
                null);
    }

    private void hideCommentTree(Long rootCommentId) {
        Set<Long> visited = new HashSet<>();
        List<Long> frontier = List.of(rootCommentId);
        while (!frontier.isEmpty()) {
            List<Long> batch = new ArrayList<>();
            for (Long commentId : frontier) {
                if (commentId != null && visited.add(commentId)) {
                    batch.add(commentId);
                }
            }
            if (batch.isEmpty()) {
                return;
            }
            commentMapper.batchHideByIds(batch);
            frontier = commentMapper.queryChildIdsByParentIds(batch);
        }
    }

    private AdminCommentVO toAdminVO(CommentEntity entity) {
        return new AdminCommentVO(
                entity.getId(),
                entity.getTargetType(),
                entity.getTargetId(),
                entity.getUserId(),
                entity.getParentId(),
                entity.getReplyToUserId(),
                entity.getContent(),
                entity.getStatus(),
                entity.getCreatedAt());
    }
}
