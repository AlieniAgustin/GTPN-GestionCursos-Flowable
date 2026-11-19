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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Cursos", description = "Endpoints de gestión y consulta de cursos")
@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @Operation(summary = "Crear nuevo curso", description = "Procesa una nueva solicitud de curso e inicia el proceso correspondiente")
    @PostMapping
    public ResponseEntity<CourseResponseDTO> create(@Valid @RequestBody CourseRequestDTO courseRequest) {
        CourseResponseDTO course = courseService.processCourse(courseRequest);
        URI location = URI.create("/api/courses/" + course.id());
        return ResponseEntity.created(location).body(course);
    }

    @Operation(summary = "Listar cursos", description = "Retorna los cursos visibles para el usuario autenticado")
    @GetMapping
    public ResponseEntity<List<CourseResponseDTO>> list(Authentication authentication) {
        return ResponseEntity.ok(courseService.findVisibleFor(
                authentication.getName(), Roles.isCompany(authentication)));
    }

    @Operation(summary = "Detalle de curso", description = "Retorna el detalle del curso solicitado si el usuario tiene permisos")
    @GetMapping("/{courseId}")
    public ResponseEntity<CourseResponseDTO> detail(
            @PathVariable Long courseId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(courseService.findOneFor(
                courseId, authentication.getName(), Roles.isCompany(authentication)));
    }
}
