package com.ainovel.community.service;

import com.ainovel.common.api.PageResponse;
import com.ainovel.community.domain.TargetType;
import com.ainovel.community.dto.CreateCommentRequest;
import com.ainovel.community.vo.CommentVO;

public interface CommentService {

    CommentVO createComment(CreateCommentRequest request, String idempotencyKey);

    void deleteComment(Long commentId);

    PageResponse<CommentVO> queryComments(TargetType targetType, Long targetId, int page, int size, String sort);
}
