package com.backend.proxy.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.backend.proxy.dto.CourseStatusUpdateDTO;
import com.backend.proxy.service.CourseService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/flowable")
@RequiredArgsConstructor
public class InternalFlowableController {
    private final CourseService courseService;

    @PutMapping("/courses/{processId}")
    public ResponseEntity<Void> updateStatus(
            @PathVariable String processId,
            @Valid @RequestBody CourseStatusUpdateDTO dto
    ) {
        courseService.updateCourse(processId, dto.status());
        return ResponseEntity.noContent().build();
    }
}
