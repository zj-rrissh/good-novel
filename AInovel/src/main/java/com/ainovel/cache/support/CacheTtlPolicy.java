package com.ainovel.cache.support;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

@Component
public class CacheTtlPolicy {

    public Duration withJitter(Duration baseTtl, int jitterPercent) {
        if (baseTtl == null || baseTtl.isZero() || baseTtl.isNegative()) {
            return Duration.ZERO;
        }
        long baseMillis = baseTtl.toMillis();
        long maxJitter = Math.max(1, baseMillis * jitterPercent / 100);
        long jitter = ThreadLocalRandom.current().nextLong(maxJitter + 1);
        return Duration.ofMillis(baseMillis + jitter);
    }
}
