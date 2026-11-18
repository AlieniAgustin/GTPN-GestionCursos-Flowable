package com.flowable.app.dto;

/**
 * Alta de una solicitud de curso (enunciado 4.1.1), tal como la manda el BFF.
 */
public record CourseProcessStartRequestDTO(
        Long courseId,
        String courseName,
        String companyName,
        String companyEmail
) {}
