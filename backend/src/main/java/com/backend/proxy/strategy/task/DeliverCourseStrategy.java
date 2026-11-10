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
        /*
         * IMPLEMENTATION STEPS:
         * 1. Retrieve delivery context from 'processVariables':
         *    - 'courseName': Name of the course.
         *    - 'docenteSeleccionado': Dynamically assigned teacher ID.
         *    - 'fechaCurso': Scheduled starting date.
         * 2. Construct DeliverCourseDataDTO with retrieved context.
         * 3. Return TaskDetailResponse with:
         *    - taskId = task.id()
         *    - taskName = task.name()
         *    - actionUrl = "/api/tasks/" + task.id() + "/deliver-course"
         *    - data = DeliverCourseDataDTO
         */
        return null;
    }
}
