package com.backend.proxy.dto.task;

import java.util.List;

/**
 * Query payload contract for 'elaborarinformederesultados'.
 * Provides course summary details so the teaching team lead can prepare the results report.
 */
public record PrepareReportDataDTO(
    String courseName,
    String teacher,
    List<String> coveredTopics
) {}
