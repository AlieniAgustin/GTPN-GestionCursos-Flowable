package com.backend.proxy.strategy.task;

import java.util.Map;

import com.backend.proxy.dto.task.FlowableTaskDTO;
import com.backend.proxy.dto.task.TaskDetailResponse;

/**
 * Strategy interface for context aggregation and dynamic HATEOAS generation for Flowable User Tasks.
 */
public interface TaskStrategy {

    /**
     * Determines whether this strategy supports the given task definition key.
     *
     * @param taskDefinitionKey BPMN task definition key (e.g., 'definirdocentequedesarrollaelcurso')
     * @return true if applicable, false otherwise
     */
    boolean supports(String taskDefinitionKey);

    /**
     * Builds the aggregated query response containing domain context and the dynamic actionUrl.
     *
     * @param task Current Flowable task details
     * @param processVariables Current process instance variables
     * @return Aggregated response with dynamic HATEOAS link and typed payload
     */
    TaskDetailResponse<?> buildDetail(FlowableTaskDTO task, Map<String, Object> processVariables);

    /**
     * Reads a process variable as text. Variables that the process has not set yet come back
     * as {@code null} from Flowable, so callers get {@code null} rather than "null".
     */
    default String variableAsString(Map<String, Object> processVariables, String name) {
        Object value = processVariables.get(name);
        return value != null ? value.toString() : null;
    }

    /**
     * Builds the HATEOAS action URL the client must POST to in order to complete this task.
     */
    default String actionUrl(FlowableTaskDTO task, String action) {
        return "/api/tasks/" + task.id() + "/" + action;
    }
}
