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
        SendReportDataDTO data = new SendReportDataDTO(
                variableAsString(processVariables, "courseName"),
                variableAsString(processVariables, "companyName"),
                variableAsString(processVariables, "companyEmail"),
                variableAsString(processVariables, "informe")
        );

        return new TaskDetailResponse<>(
                task.id(),
                task.name(),
                actionUrl(task, "send-report"),
                data
        );
    }
}
