package com.backend.proxy.strategy.task;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.backend.proxy.dto.task.DeliverCourseDataDTO;
import com.backend.proxy.dto.task.FlowableTaskDTO;
import com.backend.proxy.dto.task.TaskDetailResponse;

/**
 * Strategy for aggregating context and building action URL for 'desarrollarcurso'.
 */
@Component
public class DeliverCourseStrategy implements TaskStrategy {

    public static final String TASK_KEY = "desarrollarcurso";

    @Override
    public boolean supports(String taskDefinitionKey) {
        return TASK_KEY.equalsIgnoreCase(taskDefinitionKey);
    }

    @Override
    public TaskDetailResponse<DeliverCourseDataDTO> buildDetail(FlowableTaskDTO task, Map<String, Object> processVariables) {
        DeliverCourseDataDTO data = new DeliverCourseDataDTO(
                variableAsString(processVariables, "courseName"),
                variableAsString(processVariables, "docenteSeleccionado"),
                variableAsString(processVariables, "fechaCurso")
        );

        return new TaskDetailResponse<>(
                task.id(),
                task.name(),
                actionUrl(task, "deliver-course"),
                data
        );
    }
}
