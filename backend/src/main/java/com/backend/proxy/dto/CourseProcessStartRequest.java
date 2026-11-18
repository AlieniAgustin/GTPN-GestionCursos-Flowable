package com.backend.proxy.dto;

/**
 * Contexto del curso que el motor necesita para arrancar su proceso.
 */
public record CourseProcessStartRequest(
        Long courseId,
        String courseName,
        String companyName,
        String companyEmail
) {}
