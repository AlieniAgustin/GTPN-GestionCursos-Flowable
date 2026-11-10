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
        /*
         * IMPLEMENTATION STEPS:
         * 1. Query available teachers via teacherRepository.findAvailableTeachers().
         * 2. Retrieve course name from 'processVariables' (key 'courseName').
         * 3. Construct DefineTeacherDataDTO with course context and teacher candidates.
         * 4. Return TaskDetailResponse with:
         *    - taskId = task.id()
         *    - taskName = task.name()
         *    - actionUrl = "/api/tasks/" + task.id() + "/define-teacher"
         *    - data = DefineTeacherDataDTO
         */
        return null;
    }
}
