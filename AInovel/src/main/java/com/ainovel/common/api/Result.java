package com.ainovel.common.api;

public record Result<T>(int code, String message, T data) {

    public static <T> Result<T> success(T data) {
        return new Result<>(0, "success", data);
    }

    public static Result<Void> success() {
        return new Result<>(0, "success", null);
    }

    public static <T> Result<T> failure(ErrorCode errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static <T> Result<T> failure(int code, String message) {
        return new Result<>(code, message, null);
    }
}
