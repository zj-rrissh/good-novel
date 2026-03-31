package com.ainovel.security.rate.limiter;

import com.ainovel.security.rate.policy.RateLimitPolicy;

public interface RateLimiterService {

    boolean allow(String subject, String path, RateLimitPolicy policy);
}
