package com.ainovel;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.mockito.Mockito;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@Configuration(proxyBeanMethods = false)
@Profile("test")
public class TestInfrastructureConfiguration {

    @Bean
    @Primary
    public StringRedisTemplate stringRedisTemplate() {
        Map<String, String> store = new ConcurrentHashMap<>();
        Map<String, Instant> expiry = new ConcurrentHashMap<>();
        StringRedisTemplate redisTemplate = Mockito.mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = Mockito.mock(ValueOperations.class);
        Mockito.when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        Mockito.when(valueOperations.get(Mockito.anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            if (isExpired(key, expiry, store)) {
                return null;
            }
            return store.get(key);
        });
        Mockito.doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            String value = invocation.getArgument(1);
            Duration ttl = invocation.getArgument(2);
            store.put(key, value);
            expiry.put(key, Instant.now().plus(ttl));
            return null;
        }).when(valueOperations).set(Mockito.anyString(), Mockito.anyString(), Mockito.any(Duration.class));
        Mockito.doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            String value = invocation.getArgument(1);
            store.put(key, value);
            expiry.remove(key);
            return null;
        }).when(valueOperations).set(Mockito.anyString(), Mockito.anyString());
        Mockito.when(valueOperations.setIfAbsent(Mockito.anyString(), Mockito.anyString(), Mockito.any(Duration.class)))
                .thenAnswer(invocation -> {
                    String key = invocation.getArgument(0);
                    String value = invocation.getArgument(1);
                    Duration ttl = invocation.getArgument(2);
                    if (isExpired(key, expiry, store)) {
                        store.put(key, value);
                        expiry.put(key, Instant.now().plus(ttl));
                        return Boolean.TRUE;
                    }
                    if (store.containsKey(key)) {
                        return Boolean.FALSE;
                    }
                    store.put(key, value);
                    expiry.put(key, Instant.now().plus(ttl));
                    return Boolean.TRUE;
                });
        Mockito.when(redisTemplate.delete(Mockito.anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            store.remove(key);
            expiry.remove(key);
            return true;
        });
        Mockito.when(redisTemplate.hasKey(Mockito.anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            return !isExpired(key, expiry, store) && store.containsKey(key);
        });
        return redisTemplate;
    }

    @Bean
    @Primary
    public RedissonClient redissonClient() {
        RedissonClient redissonClient = Mockito.mock(RedissonClient.class);
        RLock lock = Mockito.mock(RLock.class);
        RReadWriteLock readWriteLock = Mockito.mock(RReadWriteLock.class);
        Mockito.when(redissonClient.getLock(Mockito.anyString())).thenReturn(lock);
        Mockito.when(redissonClient.getFairLock(Mockito.anyString())).thenReturn(lock);
        Mockito.when(redissonClient.getReadWriteLock(Mockito.anyString())).thenReturn(readWriteLock);
        Mockito.when(readWriteLock.readLock()).thenReturn(lock);
        Mockito.when(readWriteLock.writeLock()).thenReturn(lock);
        try {
            Mockito.when(lock.tryLock(Mockito.anyLong(), Mockito.anyLong(), Mockito.any())).thenReturn(true);
        } catch (InterruptedException ex) {
            throw new IllegalStateException(ex);
        }
        Mockito.when(lock.isHeldByCurrentThread()).thenReturn(true);
        return redissonClient;
    }

    private static boolean isExpired(String key, Map<String, Instant> expiry, Map<String, String> store) {
        Instant expireAt = expiry.get(key);
        if (expireAt != null && Instant.now().isAfter(expireAt)) {
            expiry.remove(key);
            store.remove(key);
            return true;
        }
        return false;
    }
}
