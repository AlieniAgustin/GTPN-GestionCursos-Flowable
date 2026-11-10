package com.backend.proxy.dto.task;

/**
 * Standard HATEOAS wrapper for aggregated User Task queries.
 * Provides the task identifier, readable name, dynamic actionUrl for decoupled submission,
 * and the strongly-typed payload needed by the client.
 */
public record TaskDetailResponse<T>(
    String taskId,
    String taskName,
    String actionUrl,
    T data
) {}
