package com.ainovel.infrastructure.monitor;

import org.springframework.stereotype.Component;

@Component
public class ApplicationHealthAdvisor {

    public ApplicationHealthState evaluate(boolean databaseUp, boolean redisUp, boolean taskBacklogHealthy) {
        if (!databaseUp) {
            return ApplicationHealthState.DOWN;
        }
        if (!redisUp || !taskBacklogHealthy) {
            return ApplicationHealthState.DEGRADED;
        }
        return ApplicationHealthState.UP;
    }
}
