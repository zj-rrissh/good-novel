package com.ainovel.infrastructure.aop.lock;

import com.ainovel.cache.config.CacheLayerProperties;
import com.ainovel.common.api.StandardErrorCode;
import com.ainovel.infrastructure.aop.spel.SpelExpressionEvaluator;
import com.ainovel.infrastructure.config.RedissonProperties;
import com.ainovel.infrastructure.exception.BusinessException;
import java.lang.reflect.Method;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Order(2)
public class LockAspect {

    private final DistributedLockService distributedLockService;
    private final RedissonProperties redissonProperties;
    private final CacheLayerProperties cacheLayerProperties;
    private final SpelExpressionEvaluator spelEvaluator;

    public LockAspect(DistributedLockService distributedLockService,
                      RedissonProperties redissonProperties,
                      CacheLayerProperties cacheLayerProperties,
                      SpelExpressionEvaluator spelEvaluator) {
        this.distributedLockService = distributedLockService;
        this.redissonProperties = redissonProperties;
        this.cacheLayerProperties = cacheLayerProperties;
        this.spelEvaluator = spelEvaluator;
    }

    @Around("@annotation(lock)")
    public Object around(ProceedingJoinPoint joinPoint, Lock lock) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        String resolvedKey = spelEvaluator.evaluate(lock.key(), method, joinPoint.getArgs());
        String redisKey = cacheLayerProperties.keyPrefix() + ":lock:" + resolvedKey;

        long waitTime = resolveWaitTime(lock);
        long leaseTime = resolveLeaseTime(lock);

        boolean acquired;
        try {
            acquired = distributedLockService.tryLock(
                    redisKey,
                    lock.type(),
                    java.time.Duration.ofMillis(waitTime),
                    java.time.Duration.ofMillis(leaseTime));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(StandardErrorCode.LOCK_ACQUIRE_FAILED, "lock interrupted");
        }

        if (!acquired) {
            String message = lock.failMessage().isEmpty()
                    ? StandardErrorCode.LOCK_ACQUIRE_FAILED.getMessage()
                    : lock.failMessage();
            throw new BusinessException(StandardErrorCode.LOCK_ACQUIRE_FAILED, message);
        }

        try {
            return joinPoint.proceed();
        } finally {
            distributedLockService.unlock(redisKey, lock.type());
        }
    }

    private long resolveWaitTime(Lock lock) {
        if (lock.waitTime() >= 0) {
            return lock.timeUnit().toMillis(lock.waitTime());
        }
        return redissonProperties.lockWaitTime().toMillis();
    }

    private long resolveLeaseTime(Lock lock) {
        if (lock.leaseTime() >= 0) {
            return lock.timeUnit().toMillis(lock.leaseTime());
        }
        return redissonProperties.lockLeaseTime().toMillis();
    }
}
