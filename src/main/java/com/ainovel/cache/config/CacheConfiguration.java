package com.ainovel.cache.config;

import com.ainovel.cache.l1.LocalCacheStore;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfiguration {

    @Bean
    public CacheManager cacheManager(CacheLayerProperties properties) {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(properties.localTtl().toSeconds(), TimeUnit.SECONDS)
                .maximumSize(1_000));
        cacheManager.setAllowNullValues(false);
        cacheManager.setCacheNames(List.of(LocalCacheStore.LOCAL_CACHE_NAME));
        return cacheManager;
    }
}
