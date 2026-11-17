package com.backend.proxy.dto.task;

public record TaskDetailResponse<T>(
    String taskId,
    String taskName,
    String actionUrl,
    T data
) {}
