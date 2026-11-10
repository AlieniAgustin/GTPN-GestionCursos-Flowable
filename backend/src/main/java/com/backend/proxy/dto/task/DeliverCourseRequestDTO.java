package com.backend.proxy.dto.task;

/**
 * Command payload contract for completing course delivery by the assigned teacher.
 */
public record DeliverCourseRequestDTO(
    String observations
) {}
