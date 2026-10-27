package group16.webapp.common.dto;

import group16.webapp.common.exception.ErrorCode;

import java.time.Instant;

/** Unified response envelope for every endpoint. */
public record ApiResponse<T>(boolean success, String code, String message, T data, Instant timestamp) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "SUCCESS", "OK", data, Instant.now());
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, "SUCCESS", message, data, Instant.now());
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        return new ApiResponse<>(false, errorCode.name(), errorCode.getMessage(), null, Instant.now());
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode, String message, T data) {
        return new ApiResponse<>(false, errorCode.name(), message, data, Instant.now());
    }
}
