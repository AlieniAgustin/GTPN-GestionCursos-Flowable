package com.backend.proxy.web;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.backend.proxy.dto.CourseResponseDTO;
import com.backend.proxy.dto.course.ActivityDTO;
import com.backend.proxy.exception.GlobalExceptionHandler;
import com.backend.proxy.exception.ResourceNotFoundException;
import com.backend.proxy.service.CourseProgressService;
import com.backend.proxy.service.CourseService;
import com.backend.proxy.types.CourseStage;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Consulta de cursos desde el sitio (enunciado 4.1.2): cada empresa ve solo los suyos y
 * puede seguir en qué punto del proceso está cada uno.
 */
@WebMvcTest(CourseSiteController.class)
@org.springframework.context.annotation.Import(GlobalExceptionHandler.class)
class CourseSiteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CourseService courseService;

    @MockBean
    private CourseProgressService progressService;

    @Test
    @DisplayName("4.1.2 Listado: a una empresa se le piden solo sus cursos")
    @WithMockUser(username = "acme-sa", roles = {"EMPRESA"})
    void testCompanyListsOnlyItsOwnCourses() throws Exception {
        when(courseService.findVisibleFor(anyString(), anyBoolean())).thenReturn(List.of(course()));
        when(progressService.findActiveActivities()).thenReturn(Map.of());
        when(progressService.resolveStage(any(), any())).thenReturn(CourseStage.EN_PREPARACION);
        when(progressService.visibleActivities(any())).thenReturn(List.of());

        mockMvc.perform(get("/mis-cursos"))
                .andExpect(status().isOk())
                .andExpect(view().name("cursos/listado"))
                .andExpect(model().attribute("esEmpresa", true));

        verify(courseService).findVisibleFor("acme-sa", true);
    }

    @Test
    @DisplayName("4.1.2 Listado: el personal de la institución los ve todos")
    @WithMockUser(username = "jefeadministrativo", roles = {"INSTITUCION"})
    void testStaffListsEveryCourse() throws Exception {
        when(courseService.findVisibleFor(anyString(), anyBoolean())).thenReturn(List.of(course()));
        when(progressService.findActiveActivities()).thenReturn(Map.of());
        when(progressService.resolveStage(any(), any())).thenReturn(CourseStage.EN_PREPARACION);
        when(progressService.visibleActivities(any())).thenReturn(List.of());

        mockMvc.perform(get("/mis-cursos"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("esEmpresa", false));

        verify(courseService).findVisibleFor("jefeadministrativo", false);
    }

    @Test
    @DisplayName("4.1.2 Listado: el motor se consulta una sola vez, no una por curso")
    @WithMockUser(username = "jefeadministrativo", roles = {"INSTITUCION"})
    void testEngineIsQueriedOncePerPage() throws Exception {
        when(courseService.findVisibleFor(anyString(), anyBoolean()))
                .thenReturn(List.of(course(), course(), course()));
        when(progressService.findActiveActivities()).thenReturn(Map.of());
        when(progressService.resolveStage(any(), any())).thenReturn(CourseStage.EN_PREPARACION);
        when(progressService.visibleActivities(any())).thenReturn(List.of());

        mockMvc.perform(get("/mis-cursos")).andExpect(status().isOk());

        verify(progressService, times(1)).findActiveActivities();
    }

    @Test
    @DisplayName("4.1.2 Detalle: muestra el estado y las actividades en curso, incluidas las paralelas")
    @WithMockUser(username = "acme-sa", roles = {"EMPRESA"})
    void testDetailShowsParallelActivities() throws Exception {
        when(courseService.findOneFor(anyLong(), anyString(), anyBoolean())).thenReturn(course());
        when(progressService.findActiveActivities())
                .thenReturn(Map.of("proc-1", List.of(
                        activity("prepararmateriales", "Preparar materiales"),
                        activity("evaluarrecursos", "Evaluar Recursos"))));
        when(progressService.resolveStage(any(), any())).thenReturn(CourseStage.EN_PREPARACION);
        when(progressService.visibleActivities(any())).thenReturn(List.of(
                activity("evaluarrecursos", "Evaluar Recursos"),
                activity("prepararmateriales", "Preparar materiales")));

        mockMvc.perform(get("/mis-cursos/52"))
                .andExpect(status().isOk())
                .andExpect(view().name("cursos/detalle"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Preparar materiales")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Evaluar Recursos")));

        verify(courseService).findOneFor(eq(52L), eq("acme-sa"), eq(true));
    }

    @Test
    @DisplayName("4.1.2 Detalle: el curso de otra empresa da 404")
    @WithMockUser(username = "acme-sa", roles = {"EMPRESA"})
    void testForeignCourseIsNotFound() throws Exception {
        when(courseService.findOneFor(anyLong(), anyString(), anyBoolean()))
                .thenThrow(new ResourceNotFoundException("Course not found: 53"));

        mockMvc.perform(get("/mis-cursos/53")).andExpect(status().isNotFound());
    }

    private CourseResponseDTO course() {
        return new CourseResponseDTO(52L, "proc-1", "Introduccion a BPMN", "Modelado de procesos",
                null, "ACME SA", null, null, LocalDate.of(2027, 3, 1),
                List.of(LocalDate.of(2027, 3, 1)), null);
    }

    private ActivityDTO activity(String id, String name) {
        return new ActivityDTO("proc-1", id, name, "userTask");
    }
}
