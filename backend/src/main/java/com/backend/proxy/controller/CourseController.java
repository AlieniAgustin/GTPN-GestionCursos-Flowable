package com.backend.proxy.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.backend.proxy.dto.CourseRequestDTO;
import com.backend.proxy.dto.CourseResponseDTO;
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
     * Registers a training request and starts its process instance (enunciado 4.1.1).
     */
    @PostMapping
    public ResponseEntity<CourseResponseDTO> create(@Valid @RequestBody CourseRequestDTO courseRequest) {
        CourseResponseDTO course = courseService.processCourse(courseRequest);
        URI location = URI.create("/api/courses/" + course.id());
        return ResponseEntity.created(location).body(course);
    }
}
