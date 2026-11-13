package com.backend.proxy.dto;

import com.backend.proxy.entity.Course;
import com.backend.proxy.types.CourseStatus;

/**
 * Course view returned to clients. Keeps the JPA entity out of the HTTP layer so the
 * lazy {@code company} association is never serialized by accident.
 */
public record CourseResponseDTO(
    Long id,
    String processInstanceId,
    String name,
    String description,
    CourseStatus status,
    String companyName
) {

    public static CourseResponseDTO from(Course course) {
        return new CourseResponseDTO(
                course.getId(),
                course.getProcessInstanceId(),
                course.getName(),
                course.getDescription(),
                course.getStatus(),
                course.getCompany() != null ? course.getCompany().getName() : null
        );
    }
}
