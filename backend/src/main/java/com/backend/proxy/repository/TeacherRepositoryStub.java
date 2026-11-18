package com.backend.proxy.repository;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.backend.proxy.dto.task.TeacherDTO;

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
