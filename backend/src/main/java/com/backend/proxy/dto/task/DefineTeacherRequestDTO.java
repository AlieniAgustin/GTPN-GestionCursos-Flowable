package com.backend.proxy.dto.task;

import jakarta.validation.constraints.NotBlank;

/**
 * Command payload contract to complete the teacher assignment task.
 * Validated fail-fast before advancing the execution token in Flowable.
 */
public record DefineTeacherRequestDTO(
    @NotBlank(message = "Teacher user ID is required")
    String userId
) {}
