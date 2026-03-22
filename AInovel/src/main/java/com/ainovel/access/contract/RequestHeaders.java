package com.ainovel.access.contract;

public final class RequestHeaders {

    public static final String AUTHORIZATION = "Authorization";
    public static final String X_CLIENT = "X-Client";
    public static final String X_APP_VERSION = "X-App-Version";
    public static final String X_DEVICE_ID = "X-Device-Id";
    public static final String X_REQUEST_ID = "X-Request-Id";
    public static final String IDEMPOTENCY_KEY = "Idempotency-Key";

    private RequestHeaders() {
    }
}
