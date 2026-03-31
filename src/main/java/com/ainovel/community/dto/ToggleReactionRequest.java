package com.ainovel.community.dto;

import com.ainovel.community.domain.TargetType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ToggleReactionRequest(@NotNull TargetType targetType, @NotNull @Positive Long targetId) {
}
