package com.ainovel.cache.support;

import java.time.Duration;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

@Component
public class CacheAsideExecutor {

    private final UnifiedCacheManager unifiedCacheManager;

    public CacheAsideExecutor(UnifiedCacheManager unifiedCacheManager) {
        this.unifiedCacheManager = unifiedCacheManager;
    }

    public <T> Optional<T> readThrough(String cacheKey,
                                       Class<T> valueType,
                                       Duration redisBaseTtl,
                                       Supplier<Optional<T>> dbLoader) {
        return unifiedCacheManager.readThrough(cacheKey, valueType, redisBaseTtl, dbLoader);
    }

    public <T> T writeDbThenInvalidate(String cacheKey, Supplier<T> dbWriter) {
        return unifiedCacheManager.writeDbThenInvalidate(cacheKey, dbWriter);
    }

    public <T> T writeDbThenInvalidate(Collection<String> cacheKeys, Supplier<T> dbWriter) {
        return unifiedCacheManager.writeDbThenInvalidate(cacheKeys, dbWriter);
    }

    public void evict(String cacheKey) {
        unifiedCacheManager.invalidate(cacheKey);
    }

    public void evict(Collection<String> cacheKeys) {
        unifiedCacheManager.invalidate(cacheKeys);
    }

    public Duration computeTtl(Duration baseTtl, CacheTtlPolicy ttlPolicy, int jitterPercent) {
        return ttlPolicy.withJitter(baseTtl, jitterPercent);
    }
}
