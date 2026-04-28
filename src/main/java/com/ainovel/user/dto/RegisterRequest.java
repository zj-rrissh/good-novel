package com.ainovel.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(min = 1, max = 32) String username,
        @NotBlank @Size(min = 8, max = 64) String password,
        @Size(max = 32) String nickname,
        @Pattern(regexp = "(^$|^https://.*)", message = "must be empty or start with https://") String avatarUrl,
        @Size(max = 128) String deviceId) {
}
