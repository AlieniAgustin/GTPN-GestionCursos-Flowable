package com.backend.proxy.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.backend.proxy.dto.CourseRequestDTO;
import com.backend.proxy.dto.CourseStatusUpdateDTO;
import com.backend.proxy.entity.Course;
import com.backend.proxy.service.CourseService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @PostMapping
    public ResponseEntity<Course> updateStatus(
            @PathVariable String processId,
            @Valid @RequestBody CourseRequestDTO courseRequest
    ) {
        courseService.processCourse(courseRequest);
        URI location = URI.create("/api/courses/mock");
        return ResponseEntity.created(location).body(null);
    }
}
