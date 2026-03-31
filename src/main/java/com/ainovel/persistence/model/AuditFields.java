package com.ainovel.persistence.model;

import java.time.LocalDateTime;

public record AuditFields(LocalDateTime createdAt, LocalDateTime updatedAt, Long createdBy, Long updatedBy) {
}
