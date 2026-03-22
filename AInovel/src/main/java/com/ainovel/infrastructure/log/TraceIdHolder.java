package com.ainovel.infrastructure.log;

import java.util.Optional;

public final class TraceIdHolder {

    private static final ThreadLocal<String> TRACE_ID = new ThreadLocal<>();

    private TraceIdHolder() {
    }

    public static void set(String traceId) {
        TRACE_ID.set(traceId);
    }

    public static Optional<String> get() {
        return Optional.ofNullable(TRACE_ID.get());
    }

    public static void clear() {
        TRACE_ID.remove();
    }
}
