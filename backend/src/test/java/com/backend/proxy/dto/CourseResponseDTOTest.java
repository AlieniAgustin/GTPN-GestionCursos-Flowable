package com.backend.proxy.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.backend.proxy.entity.Company;
import com.backend.proxy.entity.Course;
import com.backend.proxy.entity.ProposalDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El curso devuelve todas sus propuestas (relación 1 a N), sin los bytes del PDF.
 */
class CourseResponseDTOTest {

    private static final List<LocalDate> FECHAS = List.of(LocalDate.of(2027, 3, 1));

    @Test
    @DisplayName("El curso trae todas sus versiones de la propuesta, con el enlace a cada PDF")
    void testCourseIncludesItsProposals() {
        Course curso = new Course(new Company("ACME SA", "+54358", "rrhh@acme.com"),
                "Introduccion a BPMN", "Modelado de procesos", FECHAS);
        curso.setId(52L);
        curso.setProposals(List.of(
                new ProposalDocument(curso, 2, "v2", new BigDecimal("120000"), FECHAS, "jefeadministrativo", new byte[] {1}),
                new ProposalDocument(curso, 1, "v1", new BigDecimal("150000"), FECHAS, "jefeadministrativo", new byte[] {1})));

        CourseResponseDTO dto = CourseResponseDTO.from(curso);

        assertEquals(2, dto.proposals().size());
        assertEquals(2, dto.proposals().get(0).version());
        assertEquals(new BigDecimal("120000"), dto.proposals().get(0).budget());
        assertEquals("/mis-cursos/52/propuestas/2.pdf", dto.proposals().get(0).pdfUrl());
        assertEquals(1, dto.proposals().get(1).version());
    }

    @Test
    @DisplayName("Un curso sin propuestas devuelve una lista vacía, no null")
    void testCourseWithoutProposals() {
        Course curso = new Course(new Company("ACME SA", "+54358", "rrhh@acme.com"),
                "Introduccion a BPMN", "Modelado de procesos", FECHAS);

        assertTrue(CourseResponseDTO.from(curso).proposals().isEmpty());
    }
}
