package com.ainovel.cache.support;

import com.ainovel.infrastructure.aop.lock.DistributedLockService;
import com.ainovel.infrastructure.aop.lock.LockType;
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class RedisCacheLockService implements CacheLockService {

    private final DistributedLockService distributedLockService;

    public RedisCacheLockService(DistributedLockService distributedLockService) {
        this.distributedLockService = distributedLockService;
    }

    @Override
    public boolean tryLock(String key, Duration ttl) {
        try {
            return distributedLockService.tryLock(key, LockType.REENTRANT, Duration.ZERO, normalizeTtl(ttl));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public void unlock(String key) {
        distributedLockService.unlock(key, LockType.REENTRANT);
    }

    private Duration normalizeTtl(Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            return Duration.ofSeconds(5);
        }
        return ttl;
    }
}
