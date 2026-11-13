package com.backend.proxy.repository;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.backend.proxy.dto.task.TeacherDTO;

/**
 * Teaching staff catalog.
 *
 * <p>The enunciado (section 2) fixes the team at three people, the team lead included, and
 * section 3.3 says institutional staff authenticate with their Flowable credentials rather
 * than with accounts of the institution's own database. So the catalog mirrors the user ids
 * seeded into the engine; there is no separate teachers table to query.
 */
@Repository
public class TeacherRepositoryStub implements TeacherRepository {

    private static final List<TeacherDTO> TEACHING_TEAM = List.of(
            new TeacherDTO("jefedocente", "Jefe del equipo docente"),
            new TeacherDTO("docente1", "Docente 1"),
            new TeacherDTO("docente2", "Docente 2")
    );

    @Override
    public List<TeacherDTO> findAvailableTeachers() {
        return TEACHING_TEAM;
    }
}
