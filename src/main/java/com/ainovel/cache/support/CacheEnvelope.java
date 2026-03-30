package com.ainovel.cache.support;

import java.time.Instant;

public record CacheEnvelope<T>(T value, Instant expireAt) {

    public boolean expired(Instant now) {
        return expireAt != null && expireAt.isBefore(now);
    }
}
