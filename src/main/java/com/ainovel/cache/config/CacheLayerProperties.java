package com.ainovel.cache.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ainovel.cache")
public record CacheLayerProperties(
        Duration localTtl,
        Duration detailTtl,
        Duration chapterContentTtl,
        Duration nullValueTtl,
        int ttlJitterPercent,
        String keyPrefix,
        Duration lockTtl,
        Duration lockRetryInterval,
        int lockRetryTimes) {

    public CacheLayerProperties {
        localTtl = localTtl == null ? Duration.ofMinutes(1) : localTtl;
        detailTtl = detailTtl == null ? Duration.ofMinutes(5) : detailTtl;
        chapterContentTtl = chapterContentTtl == null ? Duration.ofMinutes(30) : chapterContentTtl;
        nullValueTtl = nullValueTtl == null ? Duration.ofSeconds(60) : nullValueTtl;
        ttlJitterPercent = ttlJitterPercent <= 0 ? 10 : ttlJitterPercent;
        keyPrefix = keyPrefix == null || keyPrefix.isBlank() ? "ainovel" : keyPrefix;
        lockTtl = lockTtl == null ? Duration.ofSeconds(5) : lockTtl;
        lockRetryInterval = lockRetryInterval == null ? Duration.ofMillis(80) : lockRetryInterval;
        lockRetryTimes = lockRetryTimes <= 0 ? 2 : lockRetryTimes;
    }
}
