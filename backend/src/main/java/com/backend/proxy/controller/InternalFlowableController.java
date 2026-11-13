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
import com.backend.proxy.service.CompanyAccessService;
import com.backend.proxy.service.CourseService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/flowable")
@RequiredArgsConstructor
public class InternalFlowableController {
    private final CourseService courseService;
    private final CompanyAccessService companyAccessService;

    @PutMapping("/courses/{processId}")
    public ResponseEntity<Void> updateStatus(
            @PathVariable String processId,
            @Valid @RequestBody CourseStatusUpdateDTO dto
    ) {
        courseService.updateCourse(processId, dto.status());
        return ResponseEntity.noContent().build();
    }

    /**
     * Vuelca a la BD de la institucion los datos que el proceso fue produciendo
     * (enunciado 3.2): programa y presupuesto de la propuesta, fecha acordada e informe.
     * La invoca el listener {@code syncCourseDetails} al cerrar cada etapa.
     */
    @PutMapping("/courses/{processId}/details")
    public ResponseEntity<Void> updateDetails(
            @PathVariable String processId,
            @RequestBody CourseDetailsUpdateDTO details
    ) {
        courseService.updateCourseDetails(processId, details);
        return ResponseEntity.noContent().build();
    }

    /**
     * Habilita el acceso de la empresa duena del curso (enunciado 4.1.2).
     * La invoca la actividad automatica "Habilitar acceso de la empresa" del proceso.
     */
    @PostMapping("/courses/{processId}/company-access")
    public ResponseEntity<CompanyAccessResponseDTO> enableCompanyAccess(@PathVariable String processId) {
        return ResponseEntity.ok(companyAccessService.enableAccess(processId));
    }
}
