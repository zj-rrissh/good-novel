package com.ainovel.user.dto;

import com.ainovel.user.domain.MessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record DeliverMessageRequest(
        @NotNull @Positive Long toUserId,
        @NotNull MessageType type,
        @NotBlank @Size(max = 128) String title,
        @NotBlank @Size(max = 1000) String content,
        @NotBlank @Size(max = 64) String bizType,
        @NotNull Long bizId,
        @NotBlank @Size(max = 64) String producer,
        @NotBlank @Size(max = 64) String traceId) {
}
