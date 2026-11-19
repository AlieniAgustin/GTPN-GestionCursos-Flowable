package com.backend.proxy.web;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.backend.proxy.dto.CourseRequestDTO;
import com.backend.proxy.dto.CourseResponseDTO;
import com.backend.proxy.service.CourseService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Sección pública del sitio (enunciado 4.1.1): cualquier empresa puede registrar una
 * solicitud sin tener cuenta, y al enviarla se inicia la instancia del proceso.
 */
@WebMvcTest(PublicSiteController.class)
@AutoConfigureMockMvc(addFilters = false)
class PublicSiteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CourseService courseService;

    @Test
    @DisplayName("4.1.1 Portada: muestra el formulario de solicitud sin pedir login")
    void testHomeShowsForm() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("publico/inicio"))
                .andExpect(model().attributeExists("solicitud"));
    }

    @Test
    @DisplayName("4.1.1 Alta: una solicitud válida registra el curso e inicia la instancia")
    void testValidRequestStartsTheProcess() throws Exception {
        when(courseService.processCourse(any())).thenReturn(course());

        mockMvc.perform(post("/solicitudes").params(validForm()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/solicitudes/enviada"))
                .andExpect(flash().attributeExists("curso"));

        verify(courseService).processCourse(any(CourseRequestDTO.class));
    }

    @Test
    @DisplayName("4.1.1 Alta: un email inválido vuelve al formulario sin tocar el motor")
    void testInvalidEmailStaysOnTheForm() throws Exception {
        var form = validForm();
        form.set("companyEmail", "no-es-un-email");

        mockMvc.perform(post("/solicitudes").params(form))
                .andExpect(status().isOk())
                .andExpect(view().name("publico/inicio"))
                .andExpect(model().attributeHasFieldErrors("solicitud", "companyEmail"));

        verify(courseService, never()).processCourse(any());
    }

    @Test
    @DisplayName("4.1.1 Alta: una fecha con formato raro se explica sobre el campo")
    void testUnparseableDateIsReported() throws Exception {
        var form = validForm();
        form.set("preferredDates", "01/03/2027");

        mockMvc.perform(post("/solicitudes").params(form))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("solicitud", "preferredDates"));

        verify(courseService, never()).processCourse(any());
    }

    @Test
    @DisplayName("4.1.1 Alta: una fecha pasada se rechaza")
    void testPastDateIsRejected() throws Exception {
        var form = validForm();
        form.set("preferredDates", "2020-01-01");

        mockMvc.perform(post("/solicitudes").params(form))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("solicitud", "preferredDates"));

        verify(courseService, never()).processCourse(any());
    }

    @Test
    @DisplayName("4.1.1 Alta: si el motor falla, se avisa sin perder lo que cargó el usuario")
    void testEngineFailureKeepsTheForm() throws Exception {
        when(courseService.processCourse(any())).thenThrow(new RuntimeException("motor caido"));

        mockMvc.perform(post("/solicitudes").params(validForm()))
                .andExpect(status().isOk())
                .andExpect(view().name("publico/inicio"))
                .andExpect(model().attributeExists("errorGeneral"));
    }

    @Test
    @DisplayName("4.1.1 Confirmación: recargar la página sin datos vuelve a la portada")
    void testConfirmationWithoutFlashRedirects() throws Exception {
        mockMvc.perform(get("/solicitudes/enviada"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    private org.springframework.util.MultiValueMap<String, String> validForm() {
        var form = new org.springframework.util.LinkedMultiValueMap<String, String>();
        form.add("companyName", "Textiles Andinas");
        form.add("companyEmail", "rrhh@textilesandinas.com");
        form.add("companyPhone", "+543588889900");
        form.add("name", "Liderazgo de Equipos");
        form.add("description", "Delegacion, feedback y gestion de conflictos");
        form.add("participantCount", "15");
        form.add("preferredDates", "2027-05-10, 2027-05-24");
        return form;
    }

    private CourseResponseDTO course() {
        return new CourseResponseDTO(102L, "proc-1", "Liderazgo de Equipos", "Descripcion",
                null, "Textiles Andinas", null, null, null, List.of(LocalDate.of(2027, 5, 10)), null, List.of());
    }
}
