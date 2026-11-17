package com.flowable.app.services;

import org.flowable.engine.RuntimeService;
import org.springframework.stereotype.Service;

@Service
public class CourseProcessService {

    public static final String PROCESS_KEY = "Process_Curso";
    public static final String ACTIVE_COURSE_VARIABLE = "cursoActivo";

    private final RuntimeService runtimeService;

    public CourseProcessService(RuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    public long countActiveCourses() {
        return runtimeService.createProcessInstanceQuery()
                .processDefinitionKey(PROCESS_KEY)
                .variableValueEquals(ACTIVE_COURSE_VARIABLE, true)
                .count();
    }
}
