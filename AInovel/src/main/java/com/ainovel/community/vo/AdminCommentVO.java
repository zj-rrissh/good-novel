package com.ainovel.community.vo;

import com.ainovel.community.domain.CommentStatus;
import com.ainovel.community.domain.TargetType;
import java.time.LocalDateTime;

public record AdminCommentVO(
        Long commentId,
        TargetType targetType,
        Long targetId,
        Long userId,
        Long parentId,
        Long replyToUserId,
        String content,
        CommentStatus status,
        LocalDateTime createdAt) {
}
