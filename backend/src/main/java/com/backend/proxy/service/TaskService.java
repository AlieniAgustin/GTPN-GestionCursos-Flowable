package com.backend.proxy.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.backend.proxy.dto.task.TaskDetailResponse;
import com.backend.proxy.strategy.task.TaskStrategy;

/**
 * Orchestration service for human tasks (User Tasks).
 * Coordinates polymorphic HATEOAS strategy resolution and Flowable task completion.
 */
@Service
public class TaskService {

    private final List<TaskStrategy> strategies;

    public TaskService(List<TaskStrategy> strategies) {
        this.strategies = strategies;
    }

    /**
     * Retrieves task contextual details and HATEOAS metadata using the matched strategy.
     *
     * @param taskId Flowable runtime task identifier
     * @return Aggregated response with dynamic actionUrl and strongly-typed DTO
     */
    public TaskDetailResponse<?> getTaskDetail(String taskId) {
        /*
         * IMPLEMENTATION STEPS:
         * 1. Query task metadata from Flowable REST API:
         *    GET /process-api/runtime/tasks/{taskId}
         *    Map to FlowableTaskDTO (id, name, taskDefinitionKey, assignee, processInstanceId).
         *
         * 2. Query process instance variables:
         *    GET /process-api/runtime/process-instances/{processInstanceId}/variables
         *    Transform to Map<String, Object> (e.g., courseName, docenteSeleccionado, fechaCurso, informe).
         *
         * 3. Resolve strategy:
         *    Find first strategy in 'strategies' where strategy.supports(task.taskDefinitionKey()) == true.
         *    Throw ResourceNotFoundException if no strategy matches.
         *
         * 4. Build and return detail:
         *    Invoke strategy.buildDetail(task, processVariables) to assemble the domain DTO and actionUrl.
         */
        return null;
    }

    /**
     * Completes a task in Flowable engine injecting the necessary process variables.
     *
     * @param taskId Flowable task identifier
     * @param variables Process variables to persist upon advancing the execution token
     */
    public void complete(String taskId, Map<String, Object> variables) {
        /*
         * IMPLEMENTATION STEPS:
         * 1. Validate task existence and assignee state in Flowable.
         *
         * 2. Map 'variables' to the Flowable REST completion payload:
         *    {
         *      "action": "complete",
         *      "variables": [ { "name": "...", "value": "..." } ]
         *    }
         *
         * 3. Send completion request to Flowable:
         *    POST /process-api/runtime/tasks/{taskId}
         *    via FlowableClient using technical service account credentials.
         *
         * 4. Handle exceptions (404 Not Found, 409 Conflict) and log audit events.
         */
    }
}
