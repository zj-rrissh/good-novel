package com.ainovel.security.rate.policy;

import com.ainovel.security.rate.RateLimitDimension;

public record RateLimitPolicy(String policyId, RateLimitDimension dimension, int windowSeconds, int threshold) {
}
