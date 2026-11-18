package com.backend.proxy.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.backend.proxy.dto.CompanyAccessResponseDTO;
import com.backend.proxy.dto.CourseDetailsUpdateDTO;
import com.backend.proxy.dto.CourseStatusUpdateDTO;
import com.backend.proxy.service.AccessService;
import com.backend.proxy.service.CourseService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/flowable")
@RequiredArgsConstructor
public class InternalFlowableController {
    private final CourseService courseService;
    private final AccessService accessService;

    @PutMapping("/courses/{processId}")
    public ResponseEntity<Void> updateStatus(
            @PathVariable String processId,
            @Valid @RequestBody CourseStatusUpdateDTO dto
    ) {
        courseService.updateCourse(processId, dto.status());
        return ResponseEntity.noContent().build();
    }

   
    @PutMapping("/courses/{processId}/details")
    public ResponseEntity<Void> updateDetails(
            @PathVariable String processId,
            @RequestBody CourseDetailsUpdateDTO details
    ) {
        courseService.updateCourseDetails(processId, details);
        return ResponseEntity.noContent().build();
    }


    @PostMapping("/courses/{processId}/company-access")
    public ResponseEntity<CompanyAccessResponseDTO> enableCompanyAccess(@PathVariable String processId) {
        return ResponseEntity.ok(accessService.enableAccess(processId));
    }
}
