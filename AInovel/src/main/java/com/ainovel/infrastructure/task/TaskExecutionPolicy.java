package com.ainovel.infrastructure.task;

import java.time.Duration;

public record TaskExecutionPolicy(String taskName, Duration timeout, int maxRetries, Duration retryInterval) {
}
