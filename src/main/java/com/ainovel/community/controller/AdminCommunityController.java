package com.ainovel.community.controller;

import com.ainovel.access.contract.ApiPaths;
import com.ainovel.common.api.PageResponse;
import com.ainovel.common.api.Result;
import com.ainovel.community.dto.AdminCommentQuery;
import com.ainovel.community.service.AdminCommunityService;
import com.ainovel.community.vo.AdminCommentVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

@Validated
@RestController
@RequestMapping({ApiPaths.API_ADMIN_V1 + "/community", ApiPaths.API_V1_ADMIN + "/community"})
public class AdminCommunityController {

    private final AdminCommunityService adminCommunityService;

    public AdminCommunityController(AdminCommunityService adminCommunityService) {
        this.adminCommunityService = adminCommunityService;
    }

    @GetMapping("/comments")
    public Result<PageResponse<AdminCommentVO>> queryComments(@Valid AdminCommentQuery query) {
        return Result.success(adminCommunityService.queryComments(query));
    }

    @PostMapping("/comments/{commentId}/hide")
    public Result<Void> hideComment(@PathVariable Long commentId) {
        adminCommunityService.hideComment(commentId);
        return Result.success();
    }
}
