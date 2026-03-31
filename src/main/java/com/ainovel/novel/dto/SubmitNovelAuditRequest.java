package com.ainovel.novel.dto;

import java.util.Set;

public record SubmitNovelAuditRequest(Set<Long> chapterIds, String reason) {
}
