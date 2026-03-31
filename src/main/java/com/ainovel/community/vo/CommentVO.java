package com.ainovel.community.vo;

import java.time.LocalDateTime;
import java.util.List;

public record CommentVO(
        Long commentId,
        Long userId,
        String content,
        String status,
        LocalDateTime createdAt,
        List<CommentVO> replies) {
}
