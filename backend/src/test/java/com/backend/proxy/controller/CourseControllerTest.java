package com.backend.proxy.controller;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.backend.proxy.dto.CourseResponseDTO;
import com.backend.proxy.exception.GlobalExceptionHandler;
import com.backend.proxy.exception.ResourceNotFoundException;
import com.backend.proxy.service.CourseService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contrato HTTP del alta y la consulta de cursos (enunciado 4.1.1 y 4.1.2).
 *
 * <p>Corre con la cadena de filtros puesta, así el usuario de {@code @WithMockUser} llega
 * al controlador y se puede comprobar que cada rol consulta lo que le corresponde. Quién
 * puede autenticarse se ejercita aparte, en {@code AuthenticationServiceTest}.
 */
@WebMvcTest(CourseController.class)
@org.springframework.context.annotation.Import(GlobalExceptionHandler.class)
class CourseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CourseService courseService;

    @Test
    @DisplayName("4.1.1 Alta: devuelve 201 con Location y el curso creado")
    @WithMockUser(username = "jefeadministrativo")
    void testCreateReturnsCreated() throws Exception {
        when(courseService.processCourse(any())).thenReturn(course(10L));

        mockMvc.perform(post("/api/courses")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/courses/10"))
                .andExpect(jsonPath("$.processInstanceId").value("proc-1"));
    }

    @Test
    @DisplayName("4.1.1 Alta: un email inválido se rechaza con 400 antes de tocar el motor")
    @WithMockUser(username = "jefeadministrativo")
    void testInvalidEmailIsRejected() throws Exception {
        mockMvc.perform(post("/api/courses")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest().replace("contacto@acme.com", "no-es-un-email")))
                .andExpect(status().isBadRequest());

        verify(courseService, org.mockito.Mockito.never()).processCourse(any());
    }

    @Test
    @DisplayName("4.1.1 Alta: una fecha en el pasado se rechaza con 400")
    @WithMockUser(username = "jefeadministrativo")
    void testPastDateIsRejected() throws Exception {
        mockMvc.perform(post("/api/courses")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest().replace("2027-03-01", "2020-01-01")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("4.1.2 Consulta: a una empresa se le piden solo sus cursos")
    @WithMockUser(username = "acme-sa", roles = {"EMPRESA"})
    void testCompanySeesOnlyItsOwnCourses() throws Exception {
        when(courseService.findVisibleFor(anyString(), anyBoolean())).thenReturn(List.of(course(10L)));

        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(courseService).findVisibleFor("acme-sa", true);
    }

    @Test
    @DisplayName("4.1.2 Consulta: el personal de la institución los ve todos")
    @WithMockUser(username = "jefeadministrativo", roles = {"INSTITUCION"})
    void testStaffSeesEveryCourse() throws Exception {
        when(courseService.findVisibleFor(anyString(), anyBoolean()))
                .thenReturn(List.of(course(10L), course(11L)));

        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        verify(courseService).findVisibleFor("jefeadministrativo", false);
    }

    @Test
    @DisplayName("4.1.2 Consulta: un curso que no corresponde da 404")
    @WithMockUser(username = "acme-sa", roles = {"EMPRESA"})
    void testForeignCourseIsNotFound() throws Exception {
        when(courseService.findOneFor(anyLong(), anyString(), anyBoolean()))
                .thenThrow(new ResourceNotFoundException("Course not found: 7"));

        mockMvc.perform(get("/api/courses/7")).andExpect(status().isNotFound());

        verify(courseService).findOneFor(eq(7L), eq("acme-sa"), eq(true));
    }

    private CourseResponseDTO course(Long id) {
        return new CourseResponseDTO(id, "proc-1", "Curso de Flowable", "Descripcion",
                null, "ACME SA", null, null, null, List.of(LocalDate.of(2027, 3, 1)), null);
    }

    private String validRequest() {
        return """
            {
              "contact": {"name":"ACME SA","email":"contacto@acme.com","phone":"+543581234567"},
              "name": "Curso de Flowable",
              "description": "Capacitacion en BPMN",
              "participantCount": 12,
              "possibleDates": ["2027-03-01"]
            }
            """;
    }
}
