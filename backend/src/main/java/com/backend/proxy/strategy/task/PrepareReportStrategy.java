package com.backend.proxy.strategy.task;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.backend.proxy.dto.task.FlowableTaskDTO;
import com.backend.proxy.dto.task.PrepareReportDataDTO;
import com.backend.proxy.dto.task.TaskDetailResponse;

/**
 * Strategy for aggregating context and building action URL for 'elaborarinformederesultados'.
 */
@Component
public class PrepareReportStrategy implements TaskStrategy {

    public static final String TASK_KEY = "elaborarinformederesultados";

    @Override
    public boolean supports(String taskDefinitionKey) {
        return TASK_KEY.equalsIgnoreCase(taskDefinitionKey);
    }

    @Override
    public TaskDetailResponse<PrepareReportDataDTO> buildDetail(FlowableTaskDTO task, Map<String, Object> processVariables) {
        /*
         * IMPLEMENTATION STEPS:
         * 1. Extract course execution data from 'processVariables':
         *    - 'courseName': Name of the completed course.
         *    - 'docenteSeleccionado': Teacher responsible for course delivery.
         * 2. Gather covered syllabus topics and outcomes.
         * 3. Construct PrepareReportDataDTO for the teaching lead to write the final summary.
         * 4. Return TaskDetailResponse with:
         *    - taskId = task.id()
         *    - taskName = task.name()
         *    - actionUrl = "/api/tasks/" + task.id() + "/prepare-report"
         *    - data = PrepareReportDataDTO
         */
        return null;
    }
}
