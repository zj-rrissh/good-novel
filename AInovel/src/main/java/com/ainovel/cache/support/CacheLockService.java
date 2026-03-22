package com.ainovel.cache.support;

import java.time.Duration;

public interface CacheLockService {

    boolean tryLock(String key, Duration ttl);

    void unlock(String key);
}
