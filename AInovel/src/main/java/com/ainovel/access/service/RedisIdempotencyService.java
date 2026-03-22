package com.ainovel.access.service;

import com.ainovel.access.contract.IdempotencyScope;
import com.ainovel.cache.config.CacheLayerProperties;
import com.ainovel.cache.key.CacheKeyPrefixes;
import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisIdempotencyService implements IdempotencyService {

    private final StringRedisTemplate redisTemplate;
    private final CacheLayerProperties cacheLayerProperties;

    public RedisIdempotencyService(StringRedisTemplate redisTemplate,
                                   CacheLayerProperties cacheLayerProperties) {
        this.redisTemplate = redisTemplate;
        this.cacheLayerProperties = cacheLayerProperties;
    }

    @Override
    public boolean record(IdempotencyScope scope, Duration ttl) {
        String key = buildKey(scope);
        Boolean result = redisTemplate.opsForValue().setIfAbsent(key, "1", ttl);
        return Boolean.TRUE.equals(result);
    }

    @Override
    public void release(IdempotencyScope scope) {
        redisTemplate.delete(buildKey(scope));
    }

    private String buildKey(IdempotencyScope scope) {
        StringBuilder sb = new StringBuilder();
        sb.append(cacheLayerProperties.keyPrefix())
          .append(':')
          .append(CacheKeyPrefixes.IDEMPOTENT);

        if (scope.userId() != null) {
            sb.append(':').append(scope.userId());
        }
        if (scope.path() != null && !scope.path().isEmpty()) {
            sb.append(':').append(scope.path());
        }
        sb.append(':').append(scope.key());
        return sb.toString();
    }
}
