package com.ainovel.community.dto;

import com.ainovel.community.domain.TargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(
        @NotNull TargetType targetType,
        @NotNull @Positive Long targetId,
        @NotBlank @Size(max = 2000) String content,
        Long parentId,
        Long replyToUserId) {
}
