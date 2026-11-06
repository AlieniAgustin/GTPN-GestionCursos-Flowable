package com.backend.proxy.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.backend.proxy.dto.CourseRequestDTO;
import com.backend.proxy.service.CourseService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @PostMapping
    public ResponseEntity<Void> createCourse(@Valid @RequestBody CourseRequestDTO courseRequest) {
        courseService.processCourse(courseRequest);
        URI location = URI.create("/api/courses/mock");
        return ResponseEntity.created(location).build();
    }
}
