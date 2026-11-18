package com.backend.proxy.web;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.backend.proxy.dto.task.FlowableTaskDTO;
import com.backend.proxy.exception.ResourceNotFoundException;
import com.backend.proxy.exception.TaskAlreadyClaimedException;
import com.backend.proxy.exception.TaskNotClaimedException;
import com.backend.proxy.service.CourseService;
import com.backend.proxy.service.TaskService;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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
 * Bandeja de tareas del sitio (enunciado 4.1.3).
 *
 * <p>El enunciado separa dos casos: las tareas con grupo candidato hay que reclamarlas
 * antes de completarlas, y las asignadas directamente a un usuario no requieren reclamo
 * y solo las completa esa persona.
 */
@WebMvcTest(TaskSiteController.class)
class TaskSiteControllerTest {

    private static final String TASK_ID = "task-1";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TaskService taskService;

    @MockBean
    private CourseService courseService;

    @Test
    @DisplayName("4.1.3 Bandeja: separa las asignadas de las que hay que reclamar")
    @WithMockUser(username = "administrativo1", roles = {"INSTITUCION"})
    void testInboxSplitsAssignedFromClaimable() throws Exception {
        when(courseService.findVisibleFor(anyString(), anyBoolean())).thenReturn(List.of());
        when(taskService.findInbox("administrativo1")).thenReturn(List.of(
                task("t1", "Preparar materiales", "prepararmateriales", "administrativo1"),
                task("t2", "Evaluar Recursos", "evaluarrecursos", null),
                task("t3", "Confeccionar lista definitiva", "confeccionarlistadefinitiva", null)));

        mockMvc.perform(get("/tareas"))
                .andExpect(status().isOk())
                .andExpect(view().name("tareas/bandeja"))
                .andExpect(model().attribute("mias", hasSize(1)))
                .andExpect(model().attribute("delGrupo", hasSize(2)));
    }

    @Test
    @DisplayName("4.1.3 Bandeja: una tarea de otro integrante no aparece en ninguna de las dos listas")
    @WithMockUser(username = "administrativo1", roles = {"INSTITUCION"})
    void testTaskClaimedBySomebodyElseIsNotListed() throws Exception {
        when(courseService.findVisibleFor(anyString(), anyBoolean())).thenReturn(List.of());
        when(taskService.findInbox("administrativo1")).thenReturn(List.of(
                task("t1", "Preparar materiales", "prepararmateriales", "jefeadministrativo")));

        mockMvc.perform(get("/tareas"))
                .andExpect(model().attribute("mias", hasSize(0)))
                .andExpect(model().attribute("delGrupo", hasSize(0)));
    }

    @Test
    @DisplayName("4.1.3 Reclamar: deja la tarea en el usuario y avisa")
    @WithMockUser(username = "administrativo1", roles = {"INSTITUCION"})
    void testClaim() throws Exception {
        mockMvc.perform(post("/tareas/{id}/reclamar", TASK_ID).with(csrf()))
                .andExpect(redirectedUrl("/tareas"))
                .andExpect(flash().attributeExists("mensaje"));

        verify(taskService).claim(TASK_ID, "administrativo1");
    }

    @Test
    @DisplayName("4.1.3 Reclamar: si otro se la quedó primero, se explica en castellano")
    @WithMockUser(username = "administrativo1", roles = {"INSTITUCION"})
    void testClaimConflict() throws Exception {
        doThrow(new TaskAlreadyClaimedException("La tarea ya la tiene jefeadministrativo"))
                .when(taskService).claim(anyString(), anyString());

        mockMvc.perform(post("/tareas/{id}/reclamar", TASK_ID).with(csrf()))
                .andExpect(flash().attribute("error",
                        containsString("ya la tiene jefeadministrativo")));
    }

    @Test
    @DisplayName("4.1.3 Completar: una tarea de confirmación se completa con sus variables")
    @WithMockUser(username = "docente1", roles = {"INSTITUCION"})
    void testCompleteDirectTask() throws Exception {
        when(taskService.findTaskDefinitionKey(anyString(), anyString())).thenReturn("desarrollarcurso");

        mockMvc.perform(post("/tareas/{id}/completar", TASK_ID).with(csrf()))
                .andExpect(flash().attributeExists("mensaje"));

        verify(taskService).complete(eq(TASK_ID), anyMap(), eq("docente1"));
    }

    @Test
    @DisplayName("4.1.3 Completar: una tarea con formulario propio todavía no se completa acá")
    @WithMockUser(username = "jefeadministrativo", roles = {"INSTITUCION"})
    void testTaskNeedingAFormIsRejectedForNow() throws Exception {
        when(taskService.findTaskDefinitionKey(anyString(), anyString()))
                .thenReturn("registraryelaborarpropuesta");

        mockMvc.perform(post("/tareas/{id}/completar", TASK_ID).with(csrf()))
                .andExpect(flash().attribute("error", containsString("necesita cargar datos")));

        verify(taskService, never()).complete(anyString(), anyMap(), anyString());
    }

    @Test
    @DisplayName("4.1.3 Completar: sin reclamar primero, se avisa qué falta")
    @WithMockUser(username = "administrativo1", roles = {"INSTITUCION"})
    void testCompleteWithoutClaiming() throws Exception {
        when(taskService.findTaskDefinitionKey(anyString(), anyString())).thenReturn("prepararmateriales");
        doThrow(new TaskNotClaimedException("Primero tenés que reclamar la tarea 'Preparar materiales'"))
                .when(taskService).complete(anyString(), anyMap(), anyString());

        mockMvc.perform(post("/tareas/{id}/completar", TASK_ID).with(csrf()))
                .andExpect(flash().attribute("error", containsString("Primero tenés que reclamar")));
    }

    @Test
    @DisplayName("4.1.3 Completar: una tarea ajena no filtra el mensaje técnico de la API")
    @WithMockUser(username = "docente2", roles = {"INSTITUCION"})
    void testForeignTaskGetsAFriendlyMessage() throws Exception {
        when(taskService.findTaskDefinitionKey(anyString(), anyString()))
                .thenThrow(new ResourceNotFoundException(
                        "Task task-1 is not assigned to docente2 nor to any of their groups"));

        mockMvc.perform(post("/tareas/{id}/completar", TASK_ID).with(csrf()))
                .andExpect(flash().attribute("error", "Esa tarea ya no está disponible para vos."));
    }

    @Test
    @DisplayName("4.1.3 Bandeja: cada tarea muestra a qué curso pertenece")
    @WithMockUser(username = "jefeadministrativo", roles = {"INSTITUCION"})
    void testTasksShowTheirCourse() throws Exception {
        when(courseService.findVisibleFor(anyString(), anyBoolean())).thenReturn(List.of(
                new com.backend.proxy.dto.CourseResponseDTO(1L, "proc-1", "Introduccion a BPMN",
                        "Desc", null, "ACME SA", null, null, null, List.of(), null)));
        when(taskService.findInbox(anyString())).thenReturn(List.of(
                task("t1", "Archivar solicitud", "archivarsolicitud", "jefeadministrativo")));

        mockMvc.perform(get("/tareas"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Archivar solicitud")))
                .andExpect(content().string(containsString("Introduccion a BPMN")));
    }

    @Test
    @DisplayName("4.1.3 Bandeja: cada tarea con datos lleva a su formulario del sitio")
    @WithMockUser(username = "jefedocente", roles = {"INSTITUCION"})
    void testTasksWithSiteFormLinkToIt() throws Exception {
        when(courseService.findVisibleFor(anyString(), anyBoolean())).thenReturn(List.of());
        when(taskService.findInbox(anyString())).thenReturn(List.of(
                task("t1", "Evaluar Recursos", "evaluarrecursos", "jefedocente"),
                task("t2", "Definir Docente", "definirdocentequedesarrollaelcurso", "jefedocente"),
                task("t3", "Elaborar informe", "elaborarinformederesultados", "jefedocente"),
                task("t4", "Enviar informe", "enviarinformeaempresayarchivarexpediente", "jefedocente")));

        // Ya no queda ninguna tarea con formulario derivada a Flowable UI
        mockMvc.perform(get("/tareas"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/tareas/t1/evaluar-recursos")))
                .andExpect(content().string(containsString("/tareas/t2/definir-docente")))
                .andExpect(content().string(containsString("/tareas/t3/elaborar-informe")))
                .andExpect(content().string(containsString("/tareas/t4/enviar-informe")))
                .andExpect(content().string(
                        org.hamcrest.Matchers.not(containsString("Se completa desde Flowable UI"))));
    }

    private FlowableTaskDTO task(String id, String nombre, String key, String assignee) {
        return new FlowableTaskDTO(id, nombre, key, assignee, "proc-1", "exec-1");
    }
}
