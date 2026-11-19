package com.backend.proxy.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.backend.proxy.client.FlowableClient;
import com.backend.proxy.dto.FlowableVariable;
import com.backend.proxy.dto.task.FlowableTaskDTO;
import com.backend.proxy.dto.task.FlowableTaskListDTO;
import com.backend.proxy.dto.task.TaskDetailResponse;
import com.backend.proxy.exception.ResourceNotFoundException;
import com.backend.proxy.exception.TaskAlreadyClaimedException;
import com.backend.proxy.exception.TaskNotClaimedException;
import com.backend.proxy.strategy.task.TaskStrategy;

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

    public TaskDetailResponse<?> getTaskDetail(String taskId, String username) {
        FlowableTaskDTO task = findTask(taskId);
        assertVisibleFor(task, username);

        TaskStrategy strategy = strategies.stream()
                .filter(candidate -> candidate.supports(task.taskDefinitionKey()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No strategy available for task type: " + task.taskDefinitionKey()));

        return strategy.buildDetail(task, findProcessVariables(task.processInstanceId()));
    }

    public List<FlowableTaskDTO> findInbox(String username) {
        FlowableTaskListDTO tasks = flowableClient
                .get("/process-api/runtime/tasks?candidateOrAssigned=" + encode(username) + "&size=100")
                .retrieve()
                .body(FlowableTaskListDTO.class);

        return tasks != null && tasks.data() != null ? tasks.data() : List.of();
    }

    /**
     * Una tarea solo la ve, y la completa, quien la tiene asignada o puede reclamarla.
     * Es el mismo criterio que el enunciado pide para las pantallas (4.1.3).
     */
    private void assertVisibleFor(FlowableTaskDTO task, String username) {
        boolean visible = findInbox(username).stream()
                .anyMatch(candidate -> candidate.id().equals(task.id()));

        if (!visible) {
            throw new ResourceNotFoundException(
                    "Task " + task.id() + " is not assigned to " + username + " nor to any of their groups");
        }
    }

    /**
     * Una tarea sin responsable no se completa: primero hay que reclamarla (enunciado 2).
     * Y una que ya tiene responsable solo la completa esa persona.
     */
    private void assertClaimedBy(FlowableTaskDTO task, String username) {
        if (task.assignee() == null || task.assignee().isBlank()) {
            throw new TaskNotClaimedException(
                    "Primero tenés que reclamar la tarea '" + task.name() + "'");
        }
        if (!task.assignee().equals(username)) {
            throw new TaskAlreadyClaimedException(
                    "La tarea la tiene " + task.assignee() + ", no la podés completar");
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /**
     * Tipo de tarea, para saber qué hace falta para completarla. Valida la visibilidad,
     * así que no sirve para espiar tareas ajenas.
     */
    public String findTaskDefinitionKey(String taskId, String username) {
        FlowableTaskDTO task = findTask(taskId);
        assertVisibleFor(task, username);
        return task.taskDefinitionKey();
    }

    public void claim(String taskId, String username) {
        FlowableTaskDTO task = findTask(taskId);
        assertVisibleFor(task, username);

        if (task.assignee() != null && !task.assignee().isBlank()) {
            throw new TaskAlreadyClaimedException(
                    "La tarea ya la tiene " + task.assignee());
        }

        flowableClient.post(TASKS_URI + taskId, Map.of("action", "claim", "assignee", username))
                .retrieve()
                .toBodilessEntity();
    }

  
    public void complete(String taskId, Map<String, Object> variables, String username) {
        FlowableTaskDTO task = findTask(taskId);
        assertVisibleFor(task, username);
        assertClaimedBy(task, username);

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

   
    public Map<String, Object> findVariablesOfTask(String taskId, String username) {
        FlowableTaskDTO task = findTask(taskId);
        assertVisibleFor(task, username);
        return findProcessVariables(task.processInstanceId());
    }

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
