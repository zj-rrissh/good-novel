package com.ainovel.novel.dto;

import jakarta.validation.constraints.Size;

public record ChangeNovelStatusRequest(@Size(max = 256) String reason) {
}
