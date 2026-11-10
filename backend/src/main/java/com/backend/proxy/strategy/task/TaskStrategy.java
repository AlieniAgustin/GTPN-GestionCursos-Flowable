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
}
