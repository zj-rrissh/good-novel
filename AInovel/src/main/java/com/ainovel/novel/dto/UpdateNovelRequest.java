package com.ainovel.novel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record UpdateNovelRequest(
        @NotBlank @Size(max = 128) String title,
        @Size(max = 4000) String intro,
        @Size(max = 512) String coverUrl,
        Long categoryId,
        Set<Long> tagIds) {
}
