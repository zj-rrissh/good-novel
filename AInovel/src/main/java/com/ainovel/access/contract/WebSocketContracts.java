package com.ainovel.access.contract;

public final class WebSocketContracts {

    public static final String CONNECT_ENDPOINT = ApiPaths.WS_V1 + "/connect";
    public static final String AUTH_MESSAGE_TYPE = "AUTH";
    public static final String HEARTBEAT_MESSAGE_TYPE = "PING";
    public static final String DEGRADED_POLLING_HINT = "fallback-to-polling";

    private WebSocketContracts() {
    }
}
