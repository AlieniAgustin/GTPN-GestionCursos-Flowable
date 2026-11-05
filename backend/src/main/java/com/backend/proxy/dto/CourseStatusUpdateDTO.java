package com.backend.proxy.dto;

import com.backend.proxy.types.CourseStatus;

import jakarta.validation.constraints.NotNull;

public record CourseStatusUpdateDTO(
    @NotNull(message = "Status is required")
    CourseStatus status
) {}
