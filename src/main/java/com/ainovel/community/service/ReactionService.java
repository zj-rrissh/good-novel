package com.ainovel.community.service;

import com.ainovel.community.dto.ToggleReactionRequest;

public interface ReactionService {

    void like(ToggleReactionRequest request);

    void unlike(ToggleReactionRequest request);

    void favorite(ToggleReactionRequest request);

    void unfavorite(ToggleReactionRequest request);

    void follow(Long targetUserId);

    void unfollow(Long targetUserId);
}
