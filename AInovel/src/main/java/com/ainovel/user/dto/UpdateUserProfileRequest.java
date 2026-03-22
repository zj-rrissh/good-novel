package com.ainovel.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserProfileRequest(
        @Size(max = 32) String nickname,
        @Pattern(regexp = "(^$|^https://.*)", message = "must be empty or start with https://") String avatarUrl,
        @Size(max = 1000) String bio) {
}
