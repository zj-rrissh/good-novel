package com.ainovel.access.contract;

public record IdempotencyScope(Long userId, String path, String key) {
}
