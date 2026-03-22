package com.ainovel.common.api;

public enum StandardErrorCode implements ErrorCode {
    INVALID_REQUEST(1000, "invalid request"),
    VALIDATION_FAILED(1001, "validation failed"),
    INVALID_CLIENT_HEADER(1002, "invalid client header"),
    IDEMPOTENCY_KEY_REQUIRED(1003, "idempotency key required"),
    UNAUTHENTICATED(2001, "unauthenticated"),
    TOKEN_EXPIRED(2002, "token expired"),
    FORBIDDEN(2003, "forbidden"),
    ACCOUNT_DISABLED(2004, "account disabled"),
    RATE_LIMITED(2101, "rate limited"),
    RISK_BLOCKED(2102, "risk blocked"),
    BUSINESS_STATE_INVALID(3000, "business state invalid"),
    IDEMPOTENT_CONFLICT(3001, "idempotent conflict"),
    CONTENT_NOT_VISIBLE(3002, "content not visible"),
    LOCK_ACQUIRE_FAILED(3003, "lock acquire failed"),
    INTERNAL_ERROR(5000, "internal server error"),
    DEPENDENCY_UNAVAILABLE(5001, "dependency unavailable"),
    TASK_EXECUTION_FAILED(5002, "task execution failed");

    private final int code;
    private final String message;

    StandardErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
