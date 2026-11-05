package com.backend.proxy.exception;

import java.time.Instant;

/**
 * Standard structure for API error responses.
 */
public record ApiErrorResponse(
    Instant timestamp,
    String error,
    String message,
    String path
) {
    public static ApiErrorResponse of(String error, String message, String path) {
        return new ApiErrorResponse(Instant.now(), error, message, path);
    }
}
