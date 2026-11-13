package com.backend.proxy.strategy.task;

import java.util.List;
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
        PrepareReportDataDTO data = new PrepareReportDataDTO(
                variableAsString(processVariables, "courseName"),
                variableAsString(processVariables, "docenteSeleccionado"),
                coveredTopics(processVariables)
        );

        return new TaskDetailResponse<>(
                task.id(),
                task.name(),
                actionUrl(task, "prepare-report"),
                data
        );
    }

    /**
     * El temario se guarda como variable del proceso cuando se elabora la propuesta.
     * Si todavía no está, el jefe docente escribe el informe sin esa ayuda.
     */
    private List<String> coveredTopics(Map<String, Object> processVariables) {
        Object topics = processVariables.get("temario");
        if (topics instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        if (topics != null) {
            return List.of(topics.toString());
        }
        return List.of();
    }
}
