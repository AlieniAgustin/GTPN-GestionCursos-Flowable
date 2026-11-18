package com.flowable.app;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.flowable.app.dto.CourseProcessStartRequestDTO;
import com.flowable.app.dto.CourseProcessStartResponseDTO;
import com.flowable.app.services.CourseProcessService;

/**
 * Entrada del motor para el alta de una solicitud de curso (enunciado 4.1.1).
 *
 * <p>El BFF no conoce la clave del proceso ni los nombres de sus variables: pide
 * "arrancá la solicitud de este curso". Qué proceso se inicia y con qué datos es
 * responsabilidad de este módulo.
 */
@RestController
@RequestMapping("/api/course-requests")
public class CourseProcessController {

    private final CourseProcessService courseProcessService;

    public CourseProcessController(CourseProcessService courseProcessService) {
        this.courseProcessService = courseProcessService;
    }

    @PostMapping
    public CourseProcessStartResponseDTO start(@RequestBody CourseProcessStartRequestDTO request) {
        return new CourseProcessStartResponseDTO(courseProcessService.startCourseRequest(request));
    }
}
