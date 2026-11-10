package com.backend.proxy.strategy.task;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.backend.proxy.dto.task.FlowableTaskDTO;
import com.backend.proxy.dto.task.SendReportDataDTO;
import com.backend.proxy.dto.task.TaskDetailResponse;

/**
 * Strategy for aggregating context and building action URL for 'enviarinformeaempresayarchivarexpediente'.
 */
@Component
public class SendReportStrategy implements TaskStrategy {

    public static final String TASK_KEY = "enviarinformeaempresayarchivarexpediente";

    @Override
    public boolean supports(String taskDefinitionKey) {
        return TASK_KEY.equalsIgnoreCase(taskDefinitionKey);
    }

    @Override
    public TaskDetailResponse<SendReportDataDTO> buildDetail(FlowableTaskDTO task, Map<String, Object> processVariables) {
        /*
         * IMPLEMENTATION STEPS:
         * 1. Extract context from 'processVariables':
         *    - 'courseName': Name of the course.
         *    - 'companyName' & 'companyEmail': Client company contact info.
         *    - 'informe': Content written in 'elaborarinformederesultados'.
         * 2. Construct SendReportDataDTO for administrative review before dispatch.
         * 3. Return TaskDetailResponse with:
         *    - taskId = task.id()
         *    - taskName = task.name()
         *    - actionUrl = "/api/tasks/" + task.id() + "/send-report"
         *    - data = SendReportDataDTO
         */
        return null;
    }
}
