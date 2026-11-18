package com.flowable.app.services;

import java.util.HashMap;
import java.util.Map;

import org.flowable.engine.RuntimeService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.flowable.app.dto.CourseProcessStartRequestDTO;

@Service
public class CourseProcessService {

    public static final String PROCESS_KEY = "Process_Curso";

    /** Interfaz pública del modelo: el arranque se pide por mensaje, no por clave. */
    public static final String MESSAGE_COURSE_REQUEST = "Solicitud de curso";

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

    /**
     * Arranca la instancia de una solicitud. La clave del proceso y los nombres de las
     * variables viven solo acá: el BFF manda datos del curso, no del BPMN.
     *
     * @return id de la instancia creada
     */
    public String startCourseRequest(CourseProcessStartRequestDTO request) {
        if (request == null || request.courseId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "courseId is required");
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("courseId", request.courseId());
        variables.put("courseName", request.courseName());
        variables.put("companyName", request.companyName());
        variables.put("companyEmail", request.companyEmail());

        return runtimeService.startProcessInstanceByMessage(
                MESSAGE_COURSE_REQUEST,
                String.valueOf(request.courseId()),
                variables
        ).getId();
    }
}
