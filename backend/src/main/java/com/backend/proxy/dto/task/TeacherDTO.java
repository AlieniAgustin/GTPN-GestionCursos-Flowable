package com.backend.proxy.dto.task;

/**
 * Minimal representation of an institutional teacher available for assignment.
 */
public record TeacherDTO(
    String userId,
    String name
) {}
