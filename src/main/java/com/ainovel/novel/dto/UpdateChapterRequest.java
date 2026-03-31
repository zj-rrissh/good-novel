package com.ainovel.novel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UpdateChapterRequest(
        @Positive Integer chapterNo,
        @NotBlank @Size(max = 128) String title,
        @NotBlank @Size(max = 100000) String content) {
}
