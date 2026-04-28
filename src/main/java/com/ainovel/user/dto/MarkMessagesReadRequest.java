package com.ainovel.user.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.Set;

public record MarkMessagesReadRequest(@NotEmpty Set<Long> messageIds) {
}
