package com.ainovel.infrastructure.task;

import java.util.concurrent.Callable;
import org.springframework.stereotype.Component;

@Component
public class TaskExecutionGuard {

    public <T> T execute(TaskExecutionPolicy policy, Callable<T> callable) throws Exception {
        return callable.call();
    }
}
