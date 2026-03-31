package com.ainovel.access.service;

import com.ainovel.access.contract.IdempotencyScope;
import java.time.Duration;

public interface IdempotencyService {

    boolean record(IdempotencyScope scope, Duration ttl);

    void release(IdempotencyScope scope);
}
