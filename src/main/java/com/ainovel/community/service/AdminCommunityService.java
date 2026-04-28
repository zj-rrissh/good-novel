package com.ainovel.community.service;

import com.ainovel.common.api.PageResponse;
import com.ainovel.community.dto.AdminCommentQuery;
import com.ainovel.community.vo.AdminCommentVO;

public interface AdminCommunityService {

    PageResponse<AdminCommentVO> queryComments(AdminCommentQuery query);

    void hideComment(Long commentId);
}
