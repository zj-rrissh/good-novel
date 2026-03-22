package com.ainovel.cache.l2;

import com.ainovel.cache.guard.NullValueMarker;
import com.ainovel.cache.support.CacheLookup;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisCacheStore {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisCacheStore(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public <T> CacheLookup<T> get(String key, Class<T> valueType) {
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return CacheLookup.miss();
        }
        if (NullValueMarker.VALUE.equals(value)) {
            return CacheLookup.hitNull();
        }
        try {
            return CacheLookup.hit(objectMapper.readValue(value, valueType));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to deserialize cache value for key: " + key, ex);
        }
    }

    public void put(String key, Object value, Duration ttl) {
        try {
            String serialized = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, serialized, normalizeTtl(ttl));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize cache value for key: " + key, ex);
        }
    }

    public void putNull(String key, Duration ttl) {
        redisTemplate.opsForValue().set(key, NullValueMarker.VALUE, normalizeTtl(ttl));
    }

    public void evict(String key) {
        redisTemplate.delete(key);
    }

    private Duration normalizeTtl(Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            return Duration.ofSeconds(1);
        }
        return ttl;
    }
}
