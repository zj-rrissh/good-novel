package com.ainovel.access.controller;

import com.ainovel.access.contract.ApiPaths;
import com.ainovel.access.contract.WebSocketContracts;
import com.ainovel.common.api.Result;
import com.ainovel.infrastructure.log.TraceIdHolder;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.API_V1 + "/gateway")
public class AccessGatewayController {

    @GetMapping("/contract")
    public Result<Map<String, String>> contract() {
        return Result.success(Map.of(
                "traceId", TraceIdHolder.get().orElse(""),
                "wsConnect", WebSocketContracts.CONNECT_ENDPOINT,
                "fallback", WebSocketContracts.DEGRADED_POLLING_HINT));
    }
}
