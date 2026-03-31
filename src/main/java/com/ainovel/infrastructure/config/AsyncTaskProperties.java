package com.ainovel.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ainovel.task")
public record AsyncTaskProperties(
        int corePoolSize,
        int maxPoolSize,
        int queueCapacity,
        int keepAliveSeconds,
        String threadNamePrefix) {

    public AsyncTaskProperties {
        corePoolSize = corePoolSize > 0 ? corePoolSize : 4;
        maxPoolSize = maxPoolSize > 0 ? maxPoolSize : 8;
        queueCapacity = queueCapacity > 0 ? queueCapacity : 200;
        keepAliveSeconds = keepAliveSeconds > 0 ? keepAliveSeconds : 60;
        threadNamePrefix = threadNamePrefix == null || threadNamePrefix.isBlank()
                ? "ainovel-async-"
                : threadNamePrefix;
    }
}
