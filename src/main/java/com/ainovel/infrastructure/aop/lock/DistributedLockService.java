package com.ainovel.infrastructure.aop.lock;

import java.time.Duration;

public interface DistributedLockService {

    boolean tryLock(String key, LockType lockType, Duration waitTime, Duration leaseTime) throws InterruptedException;

    void unlock(String key, LockType lockType);
}
