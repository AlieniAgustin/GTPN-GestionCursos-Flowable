package com.backend.proxy.dto.task;

/**
 * Representación inmutable de una tarea de usuario obtenida desde el motor Flowable.
 */
public record FlowableTaskDTO(
    String id,
    String name,
    String taskDefinitionKey,
    String assignee,
    String processInstanceId,
    String executionId
) {}
