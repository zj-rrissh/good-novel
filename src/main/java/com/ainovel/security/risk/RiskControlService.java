package com.ainovel.security.risk;

import com.ainovel.common.api.StandardErrorCode;
import com.ainovel.infrastructure.exception.BusinessException;
import java.time.Duration;
import java.time.Instant;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RiskControlService {

    private final StringRedisTemplate redisTemplate;

    public RiskControlService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public RiskDecision decide(String policyId, String subject, int windowSeconds, int threshold) {
        if (threshold <= 0 || windowSeconds <= 0) {
            return RiskDecision.PASS;
        }
        String normalizedSubject = normalizeSubject(subject);
        long bucket = Instant.now().getEpochSecond() / windowSeconds;
        String key = "security:risk:" + policyId + ":" + normalizedSubject + ":" + bucket;
        try {
            String raw = redisTemplate.opsForValue().get(key);
            int count = raw == null || raw.isBlank() ? 0 : Integer.parseInt(raw);
            count++;
            redisTemplate.opsForValue().set(key, String.valueOf(count), Duration.ofSeconds(windowSeconds));
            return count > threshold ? RiskDecision.BLOCK : RiskDecision.PASS;
        } catch (RuntimeException ex) {
            throw new BusinessException(StandardErrorCode.DEPENDENCY_UNAVAILABLE, "redis risk control unavailable");
        }
    }

    private String normalizeSubject(String subject) {
        if (subject == null || subject.isBlank()) {
            return "anonymous";
        }
        return subject.trim();
    }
}
