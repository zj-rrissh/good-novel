package com.ainovel.user.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(@NotBlank String refreshToken, @NotBlank String deviceId) {
}
