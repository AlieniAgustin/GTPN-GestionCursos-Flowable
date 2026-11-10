package com.backend.proxy.dto.task;

import jakarta.validation.constraints.NotBlank;

/**
 * Command payload contract for submitting the results report.
 * The textual report content is persisted into the Flowable dataObject 'informe'.
 */
public record PrepareReportRequestDTO(
    @NotBlank(message = "Report content must not be blank")
    String report,
    String overallRating
) {}
