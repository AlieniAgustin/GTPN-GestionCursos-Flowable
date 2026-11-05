package com.backend.proxy.exception;

import java.time.Instant;

/**
 * Estructura estándar y expresiva para respuestas de error de la API.
 */
public record ApiErrorResponse(
    Instant timestamp,
    int status,
    String error,
    String message,
    String path
) {}
