package com.ainovel.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "currentPassword must not be blank")
        String currentPassword,

        @NotBlank(message = "newPassword must not be blank")
        @Size(min = 8, max = 64, message = "newPassword must be between 8 and 64 characters")
        String newPassword
) {
}
