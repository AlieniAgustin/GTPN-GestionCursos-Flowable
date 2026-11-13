package com.backend.proxy.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.backend.proxy.entity.Course;

public interface CourseRepository extends JpaRepository<Course, Long> {
    Optional<Course> findByName(String name);
    Optional<Course> findByprocessInstanceId(String processInstanceId);

    /** Los cursos de una empresa, para que solo vea los propios (enunciado 4.1.2). */
    List<Course> findByCompanyAccessUsernameOrderByIdDesc(String accessUsername);

    List<Course> findAllByOrderByIdDesc();
}
