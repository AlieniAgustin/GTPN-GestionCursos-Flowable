package com.backend.proxy.dto.task;

/**
 * Immutable representation of a user task retrieved from the Flowable engine.
 */
public record FlowableTaskDTO(
    String id,
    String name,
    String taskDefinitionKey,
    String assignee,
    String processInstanceId,
    String executionId
) {}
