package com.courseplatform.backend.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record ApiResponse<T>(
        int code,
        String message,
        T data,
        String traceId,
        Instant timestamp
) {
    public static <T> ApiResponse<T> success(T data, String traceId) {
        return new ApiResponse<>(0, "success", data, traceId, Instant.now());
    }

    public static ApiResponse<Void> failure(int code, String message, String traceId) {
        return new ApiResponse<>(code, message, null, traceId, Instant.now());
    }
}
