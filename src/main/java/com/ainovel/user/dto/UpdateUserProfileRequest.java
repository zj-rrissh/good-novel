package com.ainovel.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserProfileRequest(
        @Size(max = 32) String nickname,
        @Pattern(
                regexp = "(?i)(|data:image/(png|jpe?g);base64,.+)",
                message = "avatarUrl must be empty or a base64-encoded PNG or JPEG") String avatarUrl,
        @Size(max = 1000) String bio) {
}
