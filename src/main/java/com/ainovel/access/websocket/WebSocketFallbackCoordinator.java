package com.ainovel.access.websocket;

import com.ainovel.access.contract.WebSocketContracts;
import org.springframework.stereotype.Component;

@Component
public class WebSocketFallbackCoordinator {

    public String pollingHint() {
        return WebSocketContracts.DEGRADED_POLLING_HINT;
    }
}
