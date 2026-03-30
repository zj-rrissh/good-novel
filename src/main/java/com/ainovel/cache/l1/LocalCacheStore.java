package com.ainovel.cache.l1;

import com.ainovel.cache.guard.NullValueMarker;
import com.ainovel.cache.support.CacheLookup;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

@Component
public class LocalCacheStore {

    public static final String LOCAL_CACHE_NAME = "ainovel-local-cache";

    private final CacheManager cacheManager;

    public LocalCacheStore(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public <T> CacheLookup<T> get(String key, Class<T> valueType) {
        Cache cache = resolveCache();
        if (cache == null) {
            return CacheLookup.miss();
        }
        Cache.ValueWrapper wrapper = cache.get(key);
        if (wrapper == null) {
            return CacheLookup.miss();
        }
        Object value = wrapper.get();
        if (NullValueMarker.VALUE.equals(value)) {
            return CacheLookup.hitNull();
        }
        if (value == null) {
            return CacheLookup.hitNull();
        }
        if (!valueType.isInstance(value)) {
            return CacheLookup.miss();
        }
        return CacheLookup.hit(valueType.cast(value));
    }

    public void put(String key, Object value) {
        Cache cache = resolveCache();
        if (cache == null) {
            return;
        }
        cache.put(key, value);
    }

    public void putNull(String key) {
        Cache cache = resolveCache();
        if (cache == null) {
            return;
        }
        cache.put(key, NullValueMarker.VALUE);
    }

    public void evict(String key) {
        Cache cache = resolveCache();
        if (cache == null) {
            return;
        }
        cache.evict(key);
    }

    private Cache resolveCache() {
        return cacheManager.getCache(LOCAL_CACHE_NAME);
    }
}
