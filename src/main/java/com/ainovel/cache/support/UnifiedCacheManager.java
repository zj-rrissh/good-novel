package com.ainovel.cache.support;

import com.ainovel.cache.config.CacheLayerProperties;
import com.ainovel.cache.key.CacheKeyPrefixes;
import com.ainovel.cache.l1.LocalCacheStore;
import com.ainovel.cache.l2.RedisCacheStore;
import com.ainovel.cache.metrics.CacheMetricsCollector;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

@Component
public class UnifiedCacheManager {

    private final LocalCacheStore localCacheStore;
    private final RedisCacheStore redisCacheStore;
    private final CacheLockService cacheLockService;
    private final CacheTtlPolicy cacheTtlPolicy;
    private final CacheLayerProperties properties;
    private final CacheMetricsCollector metricsCollector;

    public UnifiedCacheManager(LocalCacheStore localCacheStore,
                               RedisCacheStore redisCacheStore,
                               CacheLockService cacheLockService,
                               CacheTtlPolicy cacheTtlPolicy,
                               CacheLayerProperties properties,
                               CacheMetricsCollector metricsCollector) {
        this.localCacheStore = localCacheStore;
        this.redisCacheStore = redisCacheStore;
        this.cacheLockService = cacheLockService;
        this.cacheTtlPolicy = cacheTtlPolicy;
        this.properties = properties;
        this.metricsCollector = metricsCollector;
    }

    public String key(String prefix, Object... segments) {
        StringJoiner joiner = new StringJoiner(":");
        joiner.add(properties.keyPrefix()).add(prefix);
        if (segments != null) {
            for (Object segment : segments) {
                joiner.add(String.valueOf(segment));
            }
        }
        return joiner.toString();
    }

    public <T> Optional<T> readThrough(String cacheKey,
                                       Class<T> valueType,
                                       CacheTtlLevel ttlLevel,
                                       Supplier<Optional<T>> dbLoader) {
        return readThrough(cacheKey, valueType, resolveBaseTtl(ttlLevel), () -> true, dbLoader);
    }

    public <T> Optional<T> readThrough(String cacheKey,
                                       Class<T> valueType,
                                       Duration baseTtl,
                                       Supplier<Optional<T>> dbLoader) {
        return readThrough(cacheKey, valueType, baseTtl, () -> true, dbLoader);
    }

    public <T> Optional<T> readThrough(String cacheKey,
                                       Class<T> valueType,
                                       CacheTtlLevel ttlLevel,
                                       Supplier<Boolean> mayExistChecker,
                                       Supplier<Optional<T>> dbLoader) {
        return readThrough(cacheKey, valueType, resolveBaseTtl(ttlLevel), mayExistChecker, dbLoader);
    }

    public <T> Optional<T> readThrough(String cacheKey,
                                       Class<T> valueType,
                                       Duration baseTtl,
                                       Supplier<Boolean> mayExistChecker,
                                       Supplier<Optional<T>> dbLoader) {
        long start = System.currentTimeMillis();

        CacheLookup<T> l1 = localCacheStore.get(cacheKey, valueType);
        if (l1.hit()) {
            metricsCollector.record(cacheKey, "GET", "L1", elapsed(start), "NONE");
            return l1.value();
        }

        CacheLookup<T> l2 = redisCacheStore.get(cacheKey, valueType);
        if (l2.hit()) {
            writeLocalMirror(cacheKey, l2);
            metricsCollector.record(cacheKey, "GET", "L2", elapsed(start), "NONE");
            return l2.value();
        }

        if (!mayExistChecker.get()) {
            cacheNull(cacheKey);
            metricsCollector.record(cacheKey, "GET", "MISS", elapsed(start), "BLOOM_BLOCK");
            return Optional.empty();
        }

        String lockKey = lockKey(cacheKey);
        boolean locked = cacheLockService.tryLock(lockKey, properties.lockTtl());
        if (!locked) {
            CacheLookup<T> retryLookup = retryFromRedis(cacheKey, valueType);
            if (retryLookup.hit()) {
                writeLocalMirror(cacheKey, retryLookup);
                metricsCollector.record(cacheKey, "GET", "L2_RETRY", elapsed(start), "NONE");
                return retryLookup.value();
            }
        }

        try {
            if (locked) {
                CacheLookup<T> secondCheck = redisCacheStore.get(cacheKey, valueType);
                if (secondCheck.hit()) {
                    writeLocalMirror(cacheKey, secondCheck);
                    metricsCollector.record(cacheKey, "GET", "L2_AFTER_LOCK", elapsed(start), "NONE");
                    return secondCheck.value();
                }
            }

            Optional<T> fromDb = dbLoader.get();
            if (fromDb.isPresent()) {
                Duration dataTtl = withJitter(baseTtl);
                redisCacheStore.put(cacheKey, fromDb.get(), dataTtl);
                localCacheStore.put(cacheKey, fromDb.get());
                metricsCollector.record(cacheKey, "GET", "MISS", elapsed(start), "DB");
                return fromDb;
            }

            cacheNull(cacheKey);
            metricsCollector.record(cacheKey, "GET", "MISS", elapsed(start), "NULL");
            return Optional.empty();
        } finally {
            if (locked) {
                cacheLockService.unlock(lockKey);
            }
        }
    }

    public <T> T writeDbThenInvalidate(String cacheKey, Supplier<T> dbWriter) {
        return writeDbThenInvalidate(List.of(cacheKey), dbWriter);
    }

    public <T> T writeDbThenInvalidate(Collection<String> cacheKeys, Supplier<T> dbWriter) {
        T result = dbWriter.get();
        invalidate(cacheKeys);
        return result;
    }

    public void invalidate(String cacheKey) {
        invalidate(List.of(cacheKey));
    }

    public void invalidate(Collection<String> cacheKeys) {
        for (String cacheKey : cacheKeys) {
            redisCacheStore.evict(cacheKey);
            localCacheStore.evict(cacheKey);
            metricsCollector.record(cacheKey, "DEL", "L1+L2", 0, "WRITE_INVALIDATE");
        }
    }

    public Duration withJitter(Duration baseTtl) {
        return cacheTtlPolicy.withJitter(baseTtl, properties.ttlJitterPercent());
    }

    private Duration resolveBaseTtl(CacheTtlLevel ttlLevel) {
        if (ttlLevel == CacheTtlLevel.LOCAL_SHORT) {
            return properties.localTtl();
        }
        if (ttlLevel == CacheTtlLevel.CHAPTER_CONTENT) {
            return properties.chapterContentTtl();
        }
        return properties.detailTtl();
    }

    private void cacheNull(String cacheKey) {
        Duration nullTtl = withJitter(properties.nullValueTtl());
        redisCacheStore.putNull(cacheKey, nullTtl);
        localCacheStore.putNull(cacheKey);
    }

    private <T> CacheLookup<T> retryFromRedis(String cacheKey, Class<T> valueType) {
        for (int i = 0; i < properties.lockRetryTimes(); i++) {
            sleepQuietly(properties.lockRetryInterval());
            CacheLookup<T> lookup = redisCacheStore.get(cacheKey, valueType);
            if (lookup.hit()) {
                return lookup;
            }
        }
        return CacheLookup.miss();
    }

    private <T> void writeLocalMirror(String cacheKey, CacheLookup<T> lookup) {
        if (lookup.value().isPresent()) {
            localCacheStore.put(cacheKey, lookup.value().get());
        } else {
            localCacheStore.putNull(cacheKey);
        }
    }

    private String lockKey(String cacheKey) {
        return key(CacheKeyPrefixes.LOCK, cacheKey);
    }

    private long elapsed(long start) {
        return System.currentTimeMillis() - start;
    }

    private void sleepQuietly(Duration waitDuration) {
        long sleepMillis = Math.max(1, waitDuration.toMillis());
        try {
            Thread.sleep(sleepMillis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
