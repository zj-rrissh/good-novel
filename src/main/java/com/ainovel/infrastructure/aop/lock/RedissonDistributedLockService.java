package com.ainovel.infrastructure.aop.lock;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

@Service
public class RedissonDistributedLockService implements DistributedLockService {

    private final RedissonClient redissonClient;
    private final ThreadLocal<Map<String, RLock>> heldLocks = ThreadLocal.withInitial(HashMap::new);

    public RedissonDistributedLockService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public boolean tryLock(String key, LockType lockType, Duration waitTime, Duration leaseTime) throws InterruptedException {
        RLock lock = resolveLock(key, lockType);
        boolean acquired = lock.tryLock(normalize(waitTime), normalize(leaseTime), TimeUnit.MILLISECONDS);
        if (acquired) {
            heldLocks.get().put(heldKey(key, lockType), lock);
        }
        return acquired;
    }

    @Override
    public void unlock(String key, LockType lockType) {
        Map<String, RLock> locks = heldLocks.get();
        RLock lock = locks.remove(heldKey(key, lockType));
        if (lock == null) {
            return;
        }
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
        if (locks.isEmpty()) {
            heldLocks.remove();
        }
    }

    private RLock resolveLock(String key, LockType lockType) {
        return switch (lockType) {
            case FAIR -> redissonClient.getFairLock(key);
            case READ -> redissonClient.getReadWriteLock(key).readLock();
            case WRITE -> redissonClient.getReadWriteLock(key).writeLock();
            default -> redissonClient.getLock(key);
        };
    }

    private long normalize(Duration duration) {
        if (duration == null) {
            return -1L;
        }
        long millis = duration.toMillis();
        return millis < 0 ? -1L : millis;
    }

    private String heldKey(String key, LockType lockType) {
        return lockType.name() + ":" + key;
    }
}
