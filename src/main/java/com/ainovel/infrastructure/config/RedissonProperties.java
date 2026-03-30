package com.ainovel.infrastructure.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ainovel.redisson")
public record RedissonProperties(
        String address,
        String username,
        String password,
        Integer database,
        Duration connectTimeout,
        Duration timeout,
        Duration lockWaitTime,
        Duration lockLeaseTime) {

    public RedissonProperties {
        address = address == null || address.isBlank() ? null : address;
        database = database == null ? 0 : database;
        connectTimeout = connectTimeout == null ? Duration.ofSeconds(3) : connectTimeout;
        timeout = timeout == null ? Duration.ofSeconds(3) : timeout;
        lockWaitTime = lockWaitTime == null ? Duration.ofMillis(200) : lockWaitTime;
        lockLeaseTime = lockLeaseTime == null ? Duration.ofSeconds(5) : lockLeaseTime;
    }
}
