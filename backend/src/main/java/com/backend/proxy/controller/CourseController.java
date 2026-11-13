package com.backend.proxy.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.backend.proxy.dto.CourseRequestDTO;
import com.backend.proxy.dto.CourseResponseDTO;
import com.backend.proxy.security.Roles;
import com.backend.proxy.service.CourseService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    /**
     * Registra una solicitud de capacitacion e inicia su instancia (enunciado 4.1.1).
     */
    @PostMapping
    public ResponseEntity<CourseResponseDTO> create(@Valid @RequestBody CourseRequestDTO courseRequest) {
        CourseResponseDTO course = courseService.processCourse(courseRequest);
        URI location = URI.create("/api/courses/" + course.id());
        return ResponseEntity.created(location).body(course);
    }

    /**
     * Cursos visibles para quien consulta: una empresa ve solo los suyos (enunciado
     * 4.1.2), el personal de la institucion ve todos.
     */
    @GetMapping
    public ResponseEntity<List<CourseResponseDTO>> list(Authentication authentication) {
        return ResponseEntity.ok(courseService.findVisibleFor(
                authentication.getName(), isCompany(authentication)));
    }

    @GetMapping("/{courseId}")
    public ResponseEntity<CourseResponseDTO> detail(
            @PathVariable Long courseId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(courseService.findOneFor(
                courseId, authentication.getName(), isCompany(authentication)));
    }

    private boolean isCompany(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> Roles.ROLE_EMPRESA.equals(authority.getAuthority()));
    }
}
