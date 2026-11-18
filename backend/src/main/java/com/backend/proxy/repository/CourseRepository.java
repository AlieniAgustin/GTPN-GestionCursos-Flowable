package com.backend.proxy.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.backend.proxy.entity.Course;

public interface CourseRepository extends JpaRepository<Course, Long> {
    Optional<Course> findByName(String name);
    Optional<Course> findByprocessInstanceId(String processInstanceId);

    @Query("SELECT c FROM Course c JOIN c.company comp JOIN comp.access ca WHERE ca.username = :accessUsername ORDER BY c.id DESC")
    List<Course> findByCompanyAccessUsernameOrderByIdDesc(@Param("accessUsername") String accessUsername);

    List<Course> findAllByOrderByIdDesc();
}
