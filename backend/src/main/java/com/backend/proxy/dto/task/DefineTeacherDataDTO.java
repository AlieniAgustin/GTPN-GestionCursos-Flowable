package com.backend.proxy.dto.task;

import java.util.List;

/**
 * Query payload contract for 'definirdocentequedesarrollaelcurso'.
 * Aggregates course context and eligible teachers so the teaching team lead can pick one.
 */
public record DefineTeacherDataDTO(
    String courseName,
    List<TeacherDTO> availableTeachers
) {}
