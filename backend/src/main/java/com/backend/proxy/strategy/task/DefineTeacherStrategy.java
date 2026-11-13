package com.backend.proxy.strategy.task;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.backend.proxy.dto.task.DefineTeacherDataDTO;
import com.backend.proxy.dto.task.FlowableTaskDTO;
import com.backend.proxy.dto.task.TaskDetailResponse;
import com.backend.proxy.repository.TeacherRepository;

/**
 * Strategy for aggregating context and building action URL for 'definirdocentequedesarrollaelcurso'.
 */
@Component
public class DefineTeacherStrategy implements TaskStrategy {

    public static final String TASK_KEY = "definirdocentequedesarrollaelcurso";

    private final TeacherRepository teacherRepository;

    public DefineTeacherStrategy(TeacherRepository teacherRepository) {
        this.teacherRepository = teacherRepository;
    }

    @Override
    public boolean supports(String taskDefinitionKey) {
        return TASK_KEY.equalsIgnoreCase(taskDefinitionKey);
    }

    @Override
    public TaskDetailResponse<DefineTeacherDataDTO> buildDetail(FlowableTaskDTO task, Map<String, Object> processVariables) {
        DefineTeacherDataDTO data = new DefineTeacherDataDTO(
                variableAsString(processVariables, "courseName"),
                teacherRepository.findAvailableTeachers()
        );

        return new TaskDetailResponse<>(
                task.id(),
                task.name(),
                actionUrl(task, "define-teacher"),
                data
        );
    }
}
