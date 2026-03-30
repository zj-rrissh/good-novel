package com.ainovel.cache.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CacheMetricsCollector {

    private static final Logger log = LoggerFactory.getLogger(CacheMetricsCollector.class);

    public void record(String cacheKey, String operation, String hitLayer, long elapsedMs, String fallback) {
        log.debug("cache metrics key={} op={} hit={} elapsedMs={} fallback={}",
                Integer.toHexString(cacheKey.hashCode()), operation, hitLayer, elapsedMs, fallback);
    }
}
