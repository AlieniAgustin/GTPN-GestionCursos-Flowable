package com.backend.proxy.dto.task;

/**
 * Query payload contract for 'desarrollarcurso'.
 * Displays course delivery context to the assigned teacher once the scheduled date arrives.
 */
public record DeliverCourseDataDTO(
    String courseName,
    String assignedTeacher,
    String startDate
) {}
