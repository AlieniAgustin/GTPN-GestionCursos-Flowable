package com.backend.proxy.dto.task;

public record FlowableTaskDTO(
    String id,
    String name,
    String taskDefinitionKey,
    String assignee,
    String processInstanceId,
    String executionId
) {}
