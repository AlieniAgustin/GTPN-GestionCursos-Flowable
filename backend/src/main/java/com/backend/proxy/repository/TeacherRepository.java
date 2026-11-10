package com.backend.proxy.repository;

import java.util.List;

import com.backend.proxy.dto.task.TeacherDTO;

/**
 * Repository interface for institutional teachers catalog lookup.
 */
public interface TeacherRepository {
    List<TeacherDTO> findAvailableTeachers();
}
