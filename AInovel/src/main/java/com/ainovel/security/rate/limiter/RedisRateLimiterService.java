package com.ainovel.security.rate.limiter;

import com.ainovel.common.api.StandardErrorCode;
import com.ainovel.infrastructure.exception.BusinessException;
import com.ainovel.security.rate.policy.RateLimitPolicy;
import java.time.Duration;
import java.time.Instant;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisRateLimiterService implements RateLimiterService {

    private final StringRedisTemplate redisTemplate;

    public RedisRateLimiterService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean allow(String subject, String path, RateLimitPolicy policy) {
        if (policy == null || policy.threshold() <= 0 || policy.windowSeconds() <= 0) {
            return true;
        }
        String normalizedSubject = normalizeSubject(subject);
        long bucket = Instant.now().getEpochSecond() / policy.windowSeconds();
        String key = "security:rate:" + policy.policyId() + ":" + normalizedSubject + ":" + bucket;
        try {
            String raw = redisTemplate.opsForValue().get(key);
            int count = raw == null || raw.isBlank() ? 0 : Integer.parseInt(raw);
            count++;
            redisTemplate.opsForValue().set(key, String.valueOf(count), Duration.ofSeconds(policy.windowSeconds()));
            return count <= policy.threshold();
        } catch (RuntimeException ex) {
            throw new BusinessException(StandardErrorCode.DEPENDENCY_UNAVAILABLE, "redis rate limit unavailable");
        }
    }

    private String normalizeSubject(String subject) {
        if (subject == null || subject.isBlank()) {
            return "anonymous";
        }
        return subject.trim();
    }
}
