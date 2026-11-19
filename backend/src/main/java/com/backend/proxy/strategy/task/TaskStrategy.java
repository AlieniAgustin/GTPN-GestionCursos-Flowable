package com.backend.proxy.strategy.task;

import java.util.Map;

import com.backend.proxy.dto.task.FlowableTaskDTO;
import com.backend.proxy.dto.task.TaskDetailResponse;

/**
 * Strategy interface for context aggregation and dynamic HATEOAS generation for Flowable User Tasks.
 */
public interface TaskStrategy {

    boolean supports(String taskDefinitionKey);

    TaskDetailResponse<?> buildDetail(FlowableTaskDTO task, Map<String, Object> processVariables);

    default String variableAsString(Map<String, Object> processVariables, String name) {
        Object value = processVariables.get(name);
        return value != null ? value.toString() : null;
    }

    default String actionUrl(FlowableTaskDTO task, String action) {
        return "/api/tasks/" + task.id() + "/" + action;
    }
}
