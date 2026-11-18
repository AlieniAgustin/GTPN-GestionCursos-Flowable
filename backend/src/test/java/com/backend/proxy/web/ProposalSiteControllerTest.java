package com.backend.proxy.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.backend.proxy.dto.task.FlowableTaskDTO;
import com.backend.proxy.exception.ResourceNotFoundException;
import com.backend.proxy.service.CourseService;
import com.backend.proxy.service.ProposalService;
import com.backend.proxy.service.TaskService;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Pantalla de <em>Registrar y elaborar propuesta</em> y descarga del PDF (enunciado 4.1.3).
 */
@WebMvcTest(ProposalSiteController.class)
class ProposalSiteControllerTest {

    private static final String TASK_ID = "task-1";
    private static final String FECHA_FUTURA = LocalDate.now().plusMonths(2).toString();

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TaskService taskService;

    @MockBean
    private ProposalService proposalService;

    @MockBean
    private CourseService courseService;

    @BeforeEach
    void setUp() {
        when(taskService.findTaskDefinitionKey(TASK_ID, "jefeadministrativo"))
                .thenReturn("registraryelaborarpropuesta");
        when(taskService.findInbox("jefeadministrativo")).thenReturn(List.of(
                new FlowableTaskDTO(TASK_ID, "Registrar y Elaborar propuesta",
                        "registraryelaborarpropuesta", "jefeadministrativo", "proc-1", "exec-1")));
        when(proposalService.contextoDe("proc-1")).thenReturn(new ProposalService.Contexto(
                52L, "Introduccion a BPMN", "ACME SA", "Modelado de procesos",
                List.of(LocalDate.of(2027, 3, 1)), 1));
    }

    @Test
    @DisplayName("El formulario muestra la solicitud y arranca con las fechas que pidió la empresa")
    @WithMockUser(username = "jefeadministrativo", roles = {"INSTITUCION"})
    void testFormShowsRequest() throws Exception {
        mockMvc.perform(get("/tareas/{id}/elaborar-propuesta", TASK_ID))
                .andExpect(status().isOk())
                .andExpect(view().name("tareas/elaborar-propuesta"))
                .andExpect(content().string(containsString("ACME SA")))
                .andExpect(content().string(containsString("Introduccion a BPMN")))
                .andExpect(model().attribute("fechas", "2027-03-01"));
    }

    @Test
    @DisplayName("Con datos válidos genera la propuesta y avisa con el enlace al PDF")
    @WithMockUser(username = "jefeadministrativo", roles = {"INSTITUCION"})
    void testValidProposalIsSent() throws Exception {
        when(proposalService.enviarPropuesta(anyString(), anyString(), anyString(), any(), anyList(), anyString()))
                .thenReturn(1);

        mockMvc.perform(post("/tareas/{id}/elaborar-propuesta", TASK_ID).with(csrf())
                        .param("programa", "4 módulos, 16 horas")
                        .param("presupuesto", "180000,50")
                        .param("fechas", FECHA_FUTURA))
                .andExpect(redirectedUrl("/tareas"))
                .andExpect(flash().attribute("mensaje", containsString("versión 1")))
                .andExpect(flash().attribute("enlace", "/mis-cursos/52/propuestas/1.pdf"));

        verify(proposalService).enviarPropuesta(eq(TASK_ID), eq("proc-1"), eq("4 módulos, 16 horas"),
                eq(new BigDecimal("180000.50")), eq(List.of(LocalDate.parse(FECHA_FUTURA))), eq("jefeadministrativo"));
    }

    @Test
    @DisplayName("Sin programa, con presupuesto inválido o fecha pasada vuelve al formulario y no envía nada")
    @WithMockUser(username = "jefeadministrativo", roles = {"INSTITUCION"})
    void testInvalidFormIsRejected() throws Exception {
        mockMvc.perform(post("/tareas/{id}/elaborar-propuesta", TASK_ID).with(csrf())
                        .param("programa", " ")
                        .param("presupuesto", "mucho")
                        .param("fechas", "2020-01-01"))
                .andExpect(status().isOk())
                .andExpect(view().name("tareas/elaborar-propuesta"))
                .andExpect(content().string(containsString("Escribí el programa")))
                .andExpect(content().string(containsString("como número")))
                .andExpect(content().string(containsString("tienen que ser futuras")))
                // Lo que escribió no se pierde
                .andExpect(model().attribute("presupuesto", "mucho"));

        verify(proposalService, never()).enviarPropuesta(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Un presupuesto en cero o negativo se rechaza")
    @WithMockUser(username = "jefeadministrativo", roles = {"INSTITUCION"})
    void testNonPositiveBudget() throws Exception {
        mockMvc.perform(post("/tareas/{id}/elaborar-propuesta", TASK_ID).with(csrf())
                        .param("programa", "4 módulos")
                        .param("presupuesto", "0")
                        .param("fechas", FECHA_FUTURA))
                .andExpect(view().name("tareas/elaborar-propuesta"))
                .andExpect(content().string(containsString("mayor que cero")));

        verify(proposalService, never()).enviarPropuesta(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Con otra tarea no se envía nada")
    @WithMockUser(username = "jefeadministrativo", roles = {"INSTITUCION"})
    void testWrongTaskKey() throws Exception {
        when(taskService.findTaskDefinitionKey(TASK_ID, "jefeadministrativo")).thenReturn("archivarsolicitud");

        mockMvc.perform(post("/tareas/{id}/elaborar-propuesta", TASK_ID).with(csrf())
                        .param("programa", "4 módulos")
                        .param("presupuesto", "1000")
                        .param("fechas", FECHA_FUTURA))
                .andExpect(redirectedUrl("/tareas"))
                .andExpect(flash().attribute("error", containsString("no corresponde")));

        verify(proposalService, never()).enviarPropuesta(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Revisar: muestra las observaciones y arranca con la versión anterior")
    @WithMockUser(username = "jefeadministrativo", roles = {"INSTITUCION"})
    void testReviewShowsObservationsAndPreviousVersion() throws Exception {
        when(taskService.findTaskDefinitionKey(TASK_ID, "jefeadministrativo")).thenReturn("revisarpropuesta");
        when(taskService.findVariablesOfTask(TASK_ID, "jefeadministrativo"))
                .thenReturn(java.util.Map.of("observaciones", "Bajar el presupuesto a 150000"));
        when(proposalService.ultimaVersion(52L)).thenReturn(java.util.Optional.of(new ProposalService.Version(
                1, "4 módulos, 16 horas", new BigDecimal("180000"), List.of(LocalDate.of(2027, 3, 1)))));

        mockMvc.perform(get("/tareas/{id}/revisar-propuesta", TASK_ID))
                .andExpect(status().isOk())
                .andExpect(view().name("tareas/elaborar-propuesta"))
                .andExpect(model().attribute("revisar", true))
                .andExpect(content().string(containsString("Bajar el presupuesto a 150000")))
                .andExpect(model().attribute("programa", "4 módulos, 16 horas"))
                .andExpect(model().attribute("presupuesto", "180000"))
                .andExpect(model().attribute("fechas", "2027-03-01"));
    }

    @Test
    @DisplayName("Revisar: genera la versión siguiente y avisa con el enlace a ese PDF")
    @WithMockUser(username = "jefeadministrativo", roles = {"INSTITUCION"})
    void testReviewSendsNextVersion() throws Exception {
        when(taskService.findTaskDefinitionKey(TASK_ID, "jefeadministrativo")).thenReturn("revisarpropuesta");
        when(proposalService.enviarPropuesta(anyString(), anyString(), anyString(), any(), anyList(), anyString()))
                .thenReturn(2);

        mockMvc.perform(post("/tareas/{id}/revisar-propuesta", TASK_ID).with(csrf())
                        .param("programa", "4 módulos, 12 horas")
                        .param("presupuesto", "150000")
                        .param("fechas", FECHA_FUTURA))
                .andExpect(redirectedUrl("/tareas"))
                .andExpect(flash().attribute("mensaje", containsString("Nueva versión")))
                .andExpect(flash().attribute("enlace", "/mis-cursos/52/propuestas/2.pdf"));

        verify(proposalService).enviarPropuesta(eq(TASK_ID), eq("proc-1"), eq("4 módulos, 12 horas"),
                eq(new BigDecimal("150000")), anyList(), eq("jefeadministrativo"));
    }

    @Test
    @DisplayName("Revisar: la pantalla de elaborar no sirve para completar una revisión, ni al revés")
    @WithMockUser(username = "jefeadministrativo", roles = {"INSTITUCION"})
    void testReviewAndCreateAreNotInterchangeable() throws Exception {
        // La tarea es "elaborar" (setUp), pero se postea a la URL de revisar
        mockMvc.perform(post("/tareas/{id}/revisar-propuesta", TASK_ID).with(csrf())
                        .param("programa", "4 módulos")
                        .param("presupuesto", "1000")
                        .param("fechas", FECHA_FUTURA))
                .andExpect(redirectedUrl("/tareas"))
                .andExpect(flash().attribute("error", containsString("no corresponde")));

        verify(proposalService, never()).enviarPropuesta(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("PDF: se descarga como application/pdf")
    @WithMockUser(username = "jefeadministrativo", roles = {"INSTITUCION"})
    void testDownloadPdf() throws Exception {
        when(proposalService.pdfDe(52L, 1)).thenReturn("%PDF-1.4 prueba".getBytes());

        mockMvc.perform(get("/mis-cursos/52/propuestas/1.pdf"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    @Test
    @DisplayName("PDF: el de un curso de otra empresa da 404")
    @WithMockUser(username = "acme-sa", roles = {"EMPRESA"})
    void testCompanyCannotDownloadOtherCompanyPdf() throws Exception {
        when(courseService.findOneFor(anyLong(), anyString(), anyBoolean()))
                .thenThrow(new ResourceNotFoundException("Course not found: 53"));

        mockMvc.perform(get("/mis-cursos/53/propuestas/1.pdf"))
                .andExpect(status().isNotFound());

        verify(proposalService, never()).pdfDe(anyLong(), anyInt());
    }
}
