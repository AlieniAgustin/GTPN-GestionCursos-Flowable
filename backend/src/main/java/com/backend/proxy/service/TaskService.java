package com.backend.proxy.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.backend.proxy.client.FlowableClient;
import com.backend.proxy.dto.FlowableVariable;
import com.backend.proxy.dto.task.FlowableTaskDTO;
import com.backend.proxy.dto.task.TaskDetailResponse;
import com.backend.proxy.exception.ResourceNotFoundException;
import com.backend.proxy.strategy.task.TaskStrategy;

/**
 * Orchestration service for human tasks (User Tasks).
 * Coordinates polymorphic HATEOAS strategy resolution and Flowable task completion.
 */
@Service
public class TaskService {

    private static final String TASKS_URI = "/process-api/runtime/tasks/";
    private static final String PROCESS_INSTANCES_URI = "/process-api/runtime/process-instances/";

    private final List<TaskStrategy> strategies;
    private final FlowableClient flowableClient;

    public TaskService(List<TaskStrategy> strategies, FlowableClient flowableClient) {
        this.strategies = strategies;
        this.flowableClient = flowableClient;
    }

    /**
     * Retrieves task contextual details and HATEOAS metadata using the matched strategy.
     *
     * @param taskId Flowable runtime task identifier
     * @return Aggregated response with dynamic actionUrl and strongly-typed DTO
     */
    public TaskDetailResponse<?> getTaskDetail(String taskId) {
        FlowableTaskDTO task = findTask(taskId);

        TaskStrategy strategy = strategies.stream()
                .filter(candidate -> candidate.supports(task.taskDefinitionKey()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No strategy available for task type: " + task.taskDefinitionKey()));

        return strategy.buildDetail(task, findProcessVariables(task.processInstanceId()));
    }

    /**
     * Completes a task in Flowable engine injecting the necessary process variables.
     *
     * @param taskId Flowable task identifier
     * @param variables Process variables to persist upon advancing the execution token
     */
    public void complete(String taskId, Map<String, Object> variables) {
        // Falla con 404 antes de intentar completar, para no devolver un error del motor
        findTask(taskId);

        List<FlowableVariable> payloadVariables = new ArrayList<>();
        variables.forEach((name, value) -> payloadVariables.add(new FlowableVariable(name, value)));

        Map<String, Object> payload = Map.of(
                "action", "complete",
                "variables", payloadVariables
        );

        flowableClient.post(TASKS_URI + taskId, payload)
                .retrieve()
                .toBodilessEntity();
    }

    private FlowableTaskDTO findTask(String taskId) {
        FlowableTaskDTO task = flowableClient.get(TASKS_URI + taskId)
                .retrieve()
                .body(FlowableTaskDTO.class);

        if (task == null) {
            throw new ResourceNotFoundException("Task not found: " + taskId);
        }
        return task;
    }

    /**
     * Flowable returns variables as a list of {@code {name, type, value, scope}} objects;
     * the strategies only care about name and value.
     */
    private Map<String, Object> findProcessVariables(String processInstanceId) {
        List<FlowableVariable> variables = flowableClient
                .get(PROCESS_INSTANCES_URI + processInstanceId + "/variables")
                .retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<List<FlowableVariable>>() {});

        Map<String, Object> result = new HashMap<>();
        if (variables != null) {
            variables.stream()
                    .filter(variable -> variable.name() != null)
                    .forEach(variable -> result.put(variable.name(), variable.value()));
        }
        return result;
    }
}
