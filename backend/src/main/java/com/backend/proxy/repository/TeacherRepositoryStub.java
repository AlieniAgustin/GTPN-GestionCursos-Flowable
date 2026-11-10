package com.backend.proxy.repository;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.backend.proxy.dto.task.TeacherDTO;

/**
 * In-memory stub implementation of {@link TeacherRepository} for decoupling and tests.
 */
@Repository
public class TeacherRepositoryStub implements TeacherRepository {

    @Override
    public List<TeacherDTO> findAvailableTeachers() {
        /*
         * IMPLEMENTATION STEPS:
         * 1. Query the institutional database or HR service for teaching staff.
         * 2. Filter by active teaching status and schedule availability.
         * 3. Map to TeacherDTO(userId, name) and return.
         */
        return List.of();
    }
}
