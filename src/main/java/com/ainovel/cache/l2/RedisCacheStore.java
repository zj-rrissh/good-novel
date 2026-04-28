package com.ainovel.cache.l2;

import com.ainovel.cache.guard.NullValueMarker;
import com.ainovel.cache.support.CacheLookup;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisCacheStore {

    private static final Logger log = LoggerFactory.getLogger(RedisCacheStore.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisCacheStore(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public <T> CacheLookup<T> get(String key, Class<T> valueType) {
        try {
            String value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                return CacheLookup.miss();
            }
            if (NullValueMarker.VALUE.equals(value)) {
                return CacheLookup.hitNull();
            }
            return CacheLookup.hit(objectMapper.readValue(value, valueType));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to deserialize cache value for key: " + key, ex);
        } catch (RuntimeException ex) {
            log.debug("Redis GET failed, fallback as miss. key={}", key, ex);
            return CacheLookup.miss();
        }
    }

    public void put(String key, Object value, Duration ttl) {
        try {
            String serialized = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, serialized, normalizeTtl(ttl));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize cache value for key: " + key, ex);
        } catch (RuntimeException ex) {
            log.debug("Redis PUT failed, skip cache write. key={}", key, ex);
        }
    }

    public void putNull(String key, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, NullValueMarker.VALUE, normalizeTtl(ttl));
        } catch (RuntimeException ex) {
            log.debug("Redis PUT_NULL failed, skip null marker write. key={}", key, ex);
        }
    }

    public void evict(String key) {
        try {
            redisTemplate.delete(key);
        } catch (RuntimeException ex) {
            log.debug("Redis EVICT failed, skip delete. key={}", key, ex);
        }
    }

    private Duration normalizeTtl(Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            return Duration.ofSeconds(1);
        }
        return ttl;
    }
}
