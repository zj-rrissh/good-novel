package com.ainovel.community.service;

import com.ainovel.common.api.StandardErrorCode;
import com.ainovel.community.domain.CommentStatus;
import com.ainovel.community.entity.CommentEntity;
import com.ainovel.community.mapper.CommentMapper;
import com.ainovel.infrastructure.exception.BusinessException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminCommunityServiceImpl implements AdminCommunityService {

    private final CommentMapper commentMapper;

    public AdminCommunityServiceImpl(CommentMapper commentMapper) {
        this.commentMapper = commentMapper;
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
}
