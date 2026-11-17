package com.backend.proxy.repository;

import java.util.List;

import com.backend.proxy.dto.task.TeacherDTO;

public interface TeacherRepository {
    List<TeacherDTO> findAvailableTeachers();
}
