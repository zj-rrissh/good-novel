package com.ainovel.community.controller;

import com.ainovel.access.contract.ApiPaths;
import com.ainovel.common.api.Result;
import com.ainovel.community.dto.ToggleReactionRequest;
import com.ainovel.community.service.ReactionService;
import com.ainovel.infrastructure.aop.lock.Lock;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping(ApiPaths.API_V1)
public class ReactionController {

    private final ReactionService reactionService;

    public ReactionController(ReactionService reactionService) {
        this.reactionService = reactionService;
    }

    @PostMapping("/reactions/like")
    @Lock(key = "'reaction:like:' + #currentUserId + ':' + #request.targetType + ':' + #request.targetId",
            failMessage = "like request is in progress")
    public Result<Void> like(@Valid @RequestBody ToggleReactionRequest request) {
        reactionService.like(request);
        return Result.success();
    }

    @PostMapping("/reactions/unlike")
    @Lock(key = "'reaction:unlike:' + #currentUserId + ':' + #request.targetType + ':' + #request.targetId",
            failMessage = "unlike request is in progress")
    public Result<Void> unlike(@Valid @RequestBody ToggleReactionRequest request) {
        reactionService.unlike(request);
        return Result.success();
    }

    @PostMapping("/reactions/favorite")
    @Lock(key = "'reaction:favorite:' + #currentUserId + ':' + #request.targetType + ':' + #request.targetId",
            failMessage = "favorite request is in progress")
    public Result<Void> favorite(@Valid @RequestBody ToggleReactionRequest request) {
        reactionService.favorite(request);
        return Result.success();
    }

    @PostMapping("/reactions/unfavorite")
    @Lock(key = "'reaction:unfavorite:' + #currentUserId + ':' + #request.targetType + ':' + #request.targetId",
            failMessage = "unfavorite request is in progress")
    public Result<Void> unfavorite(@Valid @RequestBody ToggleReactionRequest request) {
        reactionService.unfavorite(request);
        return Result.success();
    }

    @PostMapping("/users/{targetUserId}/follow")
    @Lock(key = "'follow:' + #currentUserId + ':' + #targetUserId", failMessage = "follow request is in progress")
    public Result<Void> follow(@PathVariable Long targetUserId) {
        reactionService.follow(targetUserId);
        return Result.success();
    }

    @PostMapping("/users/{targetUserId}/unfollow")
    @Lock(key = "'unfollow:' + #currentUserId + ':' + #targetUserId", failMessage = "unfollow request is in progress")
    public Result<Void> unfollow(@PathVariable Long targetUserId) {
        reactionService.unfollow(targetUserId);
        return Result.success();
    }
}
