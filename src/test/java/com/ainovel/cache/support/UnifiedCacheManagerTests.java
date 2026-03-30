package com.ainovel.cache.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import com.ainovel.cache.config.CacheLayerProperties;
import com.ainovel.cache.l1.LocalCacheStore;
import com.ainovel.cache.l2.RedisCacheStore;
import com.ainovel.cache.metrics.CacheMetricsCollector;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UnifiedCacheManagerTests {

    @Mock
    private LocalCacheStore localCacheStore;

    @Mock
    private RedisCacheStore redisCacheStore;

    @Mock
    private CacheLockService cacheLockService;

    @Mock
    private CacheTtlPolicy cacheTtlPolicy;

    @Mock
    private CacheMetricsCollector metricsCollector;

    private UnifiedCacheManager unifiedCacheManager;

    @BeforeEach
    void setUp() {
        CacheLayerProperties properties = new CacheLayerProperties(
                Duration.ofMinutes(1),
                Duration.ofMinutes(5),
                Duration.ofMinutes(30),
                Duration.ofSeconds(60),
                10,
                "ainovel",
                Duration.ofSeconds(5),
                Duration.ofMillis(1),
                1);

        lenient().when(cacheTtlPolicy.withJitter(any(Duration.class), anyInt()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        unifiedCacheManager = new UnifiedCacheManager(
                localCacheStore,
                redisCacheStore,
                cacheLockService,
                cacheTtlPolicy,
                properties,
                metricsCollector);
    }

    @Test
    void shouldReturnL1HitWithoutTouchingL2OrDb() {
        when(localCacheStore.get("cache:key:l1", String.class)).thenReturn(CacheLookup.hit("l1-value"));

        Optional<String> result = unifiedCacheManager.readThrough(
                "cache:key:l1",
                String.class,
                Duration.ofSeconds(30),
                () -> Optional.of("db-value"));

        assertTrue(result.isPresent());
        assertEquals("l1-value", result.get());
        verify(redisCacheStore, never()).get(anyString(), eq(String.class));
        verify(cacheLockService, never()).tryLock(anyString(), any(Duration.class));
    }

    @Test
    void shouldReturnL2HitAndMirrorToL1() {
        when(localCacheStore.get("cache:key:l2", String.class)).thenReturn(CacheLookup.miss());
        when(redisCacheStore.get("cache:key:l2", String.class)).thenReturn(CacheLookup.hit("l2-value"));

        Optional<String> result = unifiedCacheManager.readThrough(
                "cache:key:l2",
                String.class,
                Duration.ofSeconds(30),
                () -> Optional.of("db-value"));

        assertTrue(result.isPresent());
        assertEquals("l2-value", result.get());
        verify(localCacheStore).put("cache:key:l2", "l2-value");
        verify(cacheLockService, never()).tryLock(anyString(), any(Duration.class));
    }

    @Test
    void shouldLoadFromDbAndPopulateL1AndL2OnCacheMiss() {
        when(localCacheStore.get("cache:key:db", String.class)).thenReturn(CacheLookup.miss());
        when(redisCacheStore.get("cache:key:db", String.class)).thenReturn(CacheLookup.miss(), CacheLookup.miss());
        when(cacheLockService.tryLock(anyString(), any(Duration.class))).thenReturn(true);

        Optional<String> result = unifiedCacheManager.readThrough(
                "cache:key:db",
                String.class,
                Duration.ofSeconds(45),
                () -> Optional.of("db-value"));

        assertTrue(result.isPresent());
        assertEquals("db-value", result.get());
        verify(redisCacheStore).put(eq("cache:key:db"), eq("db-value"), eq(Duration.ofSeconds(45)));
        verify(localCacheStore).put("cache:key:db", "db-value");
        verify(cacheLockService).unlock("ainovel:lock:cache:key:db");
    }

    @Test
    void shouldCacheNullWhenMayExistCheckRejects() {
        when(localCacheStore.get("cache:key:null", String.class)).thenReturn(CacheLookup.miss());
        when(redisCacheStore.get("cache:key:null", String.class)).thenReturn(CacheLookup.miss());

        Optional<String> result = unifiedCacheManager.readThrough(
                "cache:key:null",
                String.class,
                Duration.ofSeconds(45),
                () -> false,
                Optional::empty);

        assertFalse(result.isPresent());
        verify(redisCacheStore).putNull(eq("cache:key:null"), eq(Duration.ofSeconds(60)));
        verify(localCacheStore).putNull("cache:key:null");
        verify(cacheLockService, never()).tryLock(anyString(), any(Duration.class));
    }

    @Test
    void shouldRetryFromL2WhenLockNotAcquired() {
        when(localCacheStore.get("cache:key:retry", String.class)).thenReturn(CacheLookup.miss());
        when(redisCacheStore.get("cache:key:retry", String.class)).thenReturn(CacheLookup.miss(), CacheLookup.hit("retry-value"));
        when(cacheLockService.tryLock(anyString(), any(Duration.class))).thenReturn(false);

        Optional<String> result = unifiedCacheManager.readThrough(
                "cache:key:retry",
                String.class,
                Duration.ofSeconds(30),
                () -> Optional.of("db-value"));

        assertTrue(result.isPresent());
        assertEquals("retry-value", result.get());
        verify(localCacheStore).put("cache:key:retry", "retry-value");
        verify(redisCacheStore, times(2)).get("cache:key:retry", String.class);
        verify(cacheLockService, never()).unlock(anyString());
    }

    @Test
    void shouldInvalidateL1AndL2AfterWrite() {
        String result = unifiedCacheManager.writeDbThenInvalidate("cache:key:invalidate", () -> "ok");

        assertEquals("ok", result);
        verify(redisCacheStore).evict("cache:key:invalidate");
        verify(localCacheStore).evict("cache:key:invalidate");
    }
}
