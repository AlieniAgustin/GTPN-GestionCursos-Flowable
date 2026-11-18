package com.backend.proxy.web;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.backend.proxy.dto.CourseResponseDTO;
import com.backend.proxy.dto.task.FlowableTaskDTO;
import com.backend.proxy.dto.task.TeacherDTO;
import com.backend.proxy.exception.ResourceNotFoundException;
import com.backend.proxy.exception.TaskNotClaimedException;
import com.backend.proxy.repository.TeacherRepository;
import com.backend.proxy.service.CourseService;
import com.backend.proxy.service.TaskService;

import static org.hamcrest.Matchers.containsString;
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
 * Pantallas del sitio para <em>Evaluar recursos</em>, <em>Definir docente</em>,
 * <em>Elaborar informe</em> y <em>Enviar informe y archivar</em>
 * (enunciado 4.1.3 y 4.1.4).
 */
@WebMvcTest(TaskFormSiteController.class)
class TaskFormSiteControllerTest {

    private static final String TASK_ID = "task-1";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TaskService taskService;

    @MockBean
    private CourseService courseService;

    @MockBean
    private TeacherRepository teacherRepository;

    @BeforeEach
    void setUp() {
        when(teacherRepository.findAvailableTeachers()).thenReturn(List.of(
                new TeacherDTO("jefedocente", "Jefe del equipo docente"),
                new TeacherDTO("docente1", "Docente 1"),
                new TeacherDTO("docente2", "Docente 2")));
        when(taskService.findInbox(anyString())).thenReturn(List.of(
                new FlowableTaskDTO(TASK_ID, "Tarea", "x", "alguien", "proc-1", "exec-1")));
        when(courseService.findVisibleFor(anyString(), anyBoolean())).thenReturn(List.of(
                new CourseResponseDTO(1L, "proc-1", "Introduccion a BPMN",
                        "Desc", null, "ACME SA", null, null, null, List.of(), null)));
    }

    // ---------------------------------------------------------------- Evaluar recursos

    @Test
    @DisplayName("Evaluar recursos: el formulario muestra las tres opciones y el curso")
    @WithMockUser(username = "administrativo1", roles = {"INSTITUCION"})
    void testResourcesFormShows() throws Exception {
        when(taskService.findTaskDefinitionKey(TASK_ID, "administrativo1")).thenReturn("evaluarrecursos");

        mockMvc.perform(get("/tareas/{id}/evaluar-recursos", TASK_ID))
                .andExpect(status().isOk())
                .andExpect(view().name("tareas/evaluar-recursos"))
                .andExpect(content().string(containsString("Aula física")))
                .andExpect(content().string(containsString("Aula virtual")))
                .andExpect(content().string(containsString("Equipamiento especial")))
                .andExpect(content().string(containsString("Introduccion a BPMN")));
    }

    @Test
    @DisplayName("Evaluar recursos: sin elegir nada se vuelve al formulario y no se completa")
    @WithMockUser(username = "administrativo1", roles = {"INSTITUCION"})
    void testResourcesWithoutSelection() throws Exception {
        when(taskService.findTaskDefinitionKey(TASK_ID, "administrativo1")).thenReturn("evaluarrecursos");

        mockMvc.perform(post("/tareas/{id}/evaluar-recursos", TASK_ID).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("tareas/evaluar-recursos"))
                .andExpect(model().attribute("error", containsString("al menos un recurso")));

        verify(taskService, never()).complete(anyString(), anyMap(), anyString());
    }

    @ParameterizedTest(name = "aula={0} plataforma={1} equipamiento={2}")
    @CsvSource({
            "true,false,false",
            "false,true,false",
            "false,false,true",
            "true,true,true"
    })
    @DisplayName("Evaluar recursos: siempre viajan las tres variables del gateway")
    @WithMockUser(username = "administrativo1", roles = {"INSTITUCION"})
    void testResourcesSendsTheThreeBooleans(boolean aula, boolean plataforma, boolean equipamiento)
            throws Exception {
        when(taskService.findTaskDefinitionKey(TASK_ID, "administrativo1")).thenReturn("evaluarrecursos");

        MockHttpServletRequestBuilder request = post("/tareas/{id}/evaluar-recursos", TASK_ID).with(csrf());
        if (aula) request.param("requiereAula", "true");
        if (plataforma) request.param("requierePlataforma", "true");
        if (equipamiento) request.param("requiereEquipamiento", "true");

        mockMvc.perform(request)
                .andExpect(redirectedUrl("/tareas"))
                .andExpect(flash().attributeExists("mensaje"));

        verify(taskService).complete(TASK_ID, Map.of(
                "requiereAula", aula,
                "requierePlataforma", plataforma,
                "requiereEquipamiento", equipamiento), "administrativo1");
    }

    @Test
    @DisplayName("Evaluar recursos: sin reclamarla primero se avisa qué falta")
    @WithMockUser(username = "administrativo1", roles = {"INSTITUCION"})
    void testResourcesNotClaimed() throws Exception {
        when(taskService.findTaskDefinitionKey(TASK_ID, "administrativo1")).thenReturn("evaluarrecursos");
        doThrow(new TaskNotClaimedException("Primero tenés que reclamar la tarea 'Evaluar Recursos'"))
                .when(taskService).complete(anyString(), anyMap(), anyString());

        mockMvc.perform(post("/tareas/{id}/evaluar-recursos", TASK_ID).with(csrf())
                        .param("requiereAula", "true"))
                .andExpect(redirectedUrl("/tareas"))
                .andExpect(flash().attribute("error", containsString("Primero tenés que reclamar")));
    }

    @Test
    @DisplayName("Evaluar recursos: con otra tarea no se completa nada")
    @WithMockUser(username = "administrativo1", roles = {"INSTITUCION"})
    void testResourcesWrongTaskKey() throws Exception {
        when(taskService.findTaskDefinitionKey(TASK_ID, "administrativo1")).thenReturn("prepararmateriales");

        mockMvc.perform(post("/tareas/{id}/evaluar-recursos", TASK_ID).with(csrf())
                        .param("requiereAula", "true"))
                .andExpect(redirectedUrl("/tareas"))
                .andExpect(flash().attribute("error", containsString("no corresponde")));

        mockMvc.perform(get("/tareas/{id}/evaluar-recursos", TASK_ID))
                .andExpect(redirectedUrl("/tareas"));

        verify(taskService, never()).complete(anyString(), anyMap(), anyString());
    }

    // ----------------------------------------------------------------- Definir docente

    @Test
    @DisplayName("Definir docente: el formulario lista el equipo docente, jefe incluido")
    @WithMockUser(username = "jefedocente", roles = {"INSTITUCION"})
    void testTeacherFormListsTeachers() throws Exception {
        when(taskService.findTaskDefinitionKey(TASK_ID, "jefedocente"))
                .thenReturn("definirdocentequedesarrollaelcurso");

        mockMvc.perform(get("/tareas/{id}/definir-docente", TASK_ID))
                .andExpect(status().isOk())
                .andExpect(view().name("tareas/definir-docente"))
                .andExpect(content().string(containsString("value=\"jefedocente\"")))
                .andExpect(content().string(containsString("value=\"docente1\"")))
                .andExpect(content().string(containsString("value=\"docente2\"")));
    }

    @Test
    @DisplayName("Definir docente: completa con docenteSeleccionado")
    @WithMockUser(username = "jefedocente", roles = {"INSTITUCION"})
    void testTeacherCompletes() throws Exception {
        when(taskService.findTaskDefinitionKey(TASK_ID, "jefedocente"))
                .thenReturn("definirdocentequedesarrollaelcurso");

        mockMvc.perform(post("/tareas/{id}/definir-docente", TASK_ID).with(csrf())
                        .param("userId", "docente1"))
                .andExpect(redirectedUrl("/tareas"))
                .andExpect(flash().attributeExists("mensaje"));

        verify(taskService).complete(TASK_ID, Map.of("docenteSeleccionado", "docente1"), "jefedocente");
    }

    @Test
    @DisplayName("Definir docente: alguien fuera del equipo docente se rechaza")
    @WithMockUser(username = "jefedocente", roles = {"INSTITUCION"})
    void testTeacherInvalid() throws Exception {
        when(taskService.findTaskDefinitionKey(TASK_ID, "jefedocente"))
                .thenReturn("definirdocentequedesarrollaelcurso");

        mockMvc.perform(post("/tareas/{id}/definir-docente", TASK_ID).with(csrf())
                        .param("userId", "administrativo1"))
                .andExpect(status().isOk())
                .andExpect(view().name("tareas/definir-docente"))
                .andExpect(model().attribute("error", containsString("de la lista")));

        mockMvc.perform(post("/tareas/{id}/definir-docente", TASK_ID).with(csrf()))
                .andExpect(view().name("tareas/definir-docente"));

        verify(taskService, never()).complete(anyString(), anyMap(), anyString());
    }

    @Test
    @DisplayName("Definir docente: con otra tarea no se completa nada")
    @WithMockUser(username = "jefedocente", roles = {"INSTITUCION"})
    void testTeacherWrongTaskKey() throws Exception {
        when(taskService.findTaskDefinitionKey(TASK_ID, "jefedocente")).thenReturn("elaborarinformederesultados");

        mockMvc.perform(post("/tareas/{id}/definir-docente", TASK_ID).with(csrf())
                        .param("userId", "docente1"))
                .andExpect(redirectedUrl("/tareas"))
                .andExpect(flash().attribute("error", containsString("no corresponde")));

        verify(taskService, never()).complete(anyString(), anyMap(), anyString());
    }

    @Test
    @DisplayName("Definir docente: una tarea ajena da el mensaje amigable")
    @WithMockUser(username = "docente2", roles = {"INSTITUCION"})
    void testTeacherForeignTask() throws Exception {
        when(taskService.findTaskDefinitionKey(anyString(), eq("docente2")))
                .thenThrow(new ResourceNotFoundException("Task task-1 is not assigned to docente2"));

        mockMvc.perform(get("/tareas/{id}/definir-docente", TASK_ID))
                .andExpect(redirectedUrl("/tareas"))
                .andExpect(flash().attribute("error", "Esa tarea ya no está disponible para vos."));
    }

    // ------------------------------------------------------------- Elaborar informe

    @Test
    @DisplayName("Elaborar informe: el formulario arranca vacío y nombra el curso")
    @WithMockUser(username = "jefedocente", roles = {"INSTITUCION"})
    void testReportFormShows() throws Exception {
        when(taskService.findTaskDefinitionKey(TASK_ID, "jefedocente"))
                .thenReturn("elaborarinformederesultados");
        when(taskService.findVariablesOfTask(anyString(), anyString())).thenReturn(Map.of());

        mockMvc.perform(get("/tareas/{id}/elaborar-informe", TASK_ID))
                .andExpect(status().isOk())
                .andExpect(view().name("tareas/elaborar-informe"))
                .andExpect(content().string(containsString("Introduccion a BPMN")));
    }

    @Test
    @DisplayName("Elaborar informe: un informe en blanco vuelve al formulario sin completar")
    @WithMockUser(username = "jefedocente", roles = {"INSTITUCION"})
    void testReportBlankIsRejected() throws Exception {
        when(taskService.findTaskDefinitionKey(TASK_ID, "jefedocente"))
                .thenReturn("elaborarinformederesultados");
        when(taskService.findVariablesOfTask(anyString(), anyString())).thenReturn(Map.of());

        mockMvc.perform(post("/tareas/{id}/elaborar-informe", TASK_ID).with(csrf())
                        .param("informe", "   "))
                .andExpect(status().isOk())
                .andExpect(view().name("tareas/elaborar-informe"))
                .andExpect(model().attribute("error", containsString("Escribí el informe")));

        verify(taskService, never()).complete(anyString(), anyMap(), anyString());
    }

    @Test
    @DisplayName("Elaborar informe: se completa con la variable informe, sin espacios de más")
    @WithMockUser(username = "jefedocente", roles = {"INSTITUCION"})
    void testReportIsStored() throws Exception {
        when(taskService.findTaskDefinitionKey(TASK_ID, "jefedocente"))
                .thenReturn("elaborarinformederesultados");

        mockMvc.perform(post("/tareas/{id}/elaborar-informe", TASK_ID).with(csrf())
                        .param("informe", "  18 de 20 participantes aprobaron.  "))
                .andExpect(redirectedUrl("/tareas"))
                .andExpect(flash().attribute("mensaje", containsString("Informe elaborado")));

        verify(taskService).complete(TASK_ID,
                Map.of("informe", "18 de 20 participantes aprobaron."), "jefedocente");
    }

    // ---------------------------------------------------------------- Enviar informe

    @Test
    @DisplayName("Enviar informe: muestra el informe que dejó el equipo docente")
    @WithMockUser(username = "jefeadministrativo", roles = {"INSTITUCION"})
    void testSendFormShowsTheReport() throws Exception {
        when(taskService.findTaskDefinitionKey(TASK_ID, "jefeadministrativo"))
                .thenReturn("enviarinformeaempresayarchivarexpediente");
        when(taskService.findVariablesOfTask(anyString(), anyString()))
                .thenReturn(Map.of("informe", "18 de 20 participantes aprobaron."));

        mockMvc.perform(get("/tareas/{id}/enviar-informe", TASK_ID))
                .andExpect(status().isOk())
                .andExpect(view().name("tareas/enviar-informe"))
                .andExpect(content().string(containsString("18 de 20 participantes aprobaron.")));
    }

    @Test
    @DisplayName("Enviar informe: si todavía no hay informe, lo avisa en vez de mostrar vacío")
    @WithMockUser(username = "jefeadministrativo", roles = {"INSTITUCION"})
    void testSendFormWithoutReportYet() throws Exception {
        when(taskService.findTaskDefinitionKey(TASK_ID, "jefeadministrativo"))
                .thenReturn("enviarinformeaempresayarchivarexpediente");
        when(taskService.findVariablesOfTask(anyString(), anyString())).thenReturn(Map.of());

        mockMvc.perform(get("/tareas/{id}/enviar-informe", TASK_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("todavía no cargó el informe")));
    }

    @Test
    @DisplayName("Enviar informe: sin confirmar no se cierra el curso")
    @WithMockUser(username = "jefeadministrativo", roles = {"INSTITUCION"})
    void testSendRequiresConfirmation() throws Exception {
        when(taskService.findTaskDefinitionKey(TASK_ID, "jefeadministrativo"))
                .thenReturn("enviarinformeaempresayarchivarexpediente");
        when(taskService.findVariablesOfTask(anyString(), anyString()))
                .thenReturn(Map.of("informe", "Informe"));

        mockMvc.perform(post("/tareas/{id}/enviar-informe", TASK_ID).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("tareas/enviar-informe"))
                .andExpect(model().attribute("error", containsString("Confirmá el envío")));

        verify(taskService, never()).complete(anyString(), anyMap(), anyString());
    }

    @Test
    @DisplayName("Enviar informe: al confirmar marca el envío y el archivado")
    @WithMockUser(username = "jefeadministrativo", roles = {"INSTITUCION"})
    void testSendCompletesTheCourse() throws Exception {
        when(taskService.findTaskDefinitionKey(TASK_ID, "jefeadministrativo"))
                .thenReturn("enviarinformeaempresayarchivarexpediente");

        mockMvc.perform(post("/tareas/{id}/enviar-informe", TASK_ID).with(csrf())
                        .param("confirmar", "true"))
                .andExpect(redirectedUrl("/tareas"))
                .andExpect(flash().attribute("mensaje", containsString("finalizado")));

        verify(taskService).complete(TASK_ID,
                Map.of("informeEnviado", true, "expedienteArchivado", true), "jefeadministrativo");
    }

    @Test
    @DisplayName("Enviar informe: con otra tarea no se completa nada")
    @WithMockUser(username = "jefeadministrativo", roles = {"INSTITUCION"})
    void testSendWrongTaskKey() throws Exception {
        when(taskService.findTaskDefinitionKey(TASK_ID, "jefeadministrativo"))
                .thenReturn("archivarsolicitud");

        mockMvc.perform(post("/tareas/{id}/enviar-informe", TASK_ID).with(csrf())
                        .param("confirmar", "true"))
                .andExpect(redirectedUrl("/tareas"))
                .andExpect(flash().attribute("error", containsString("no corresponde")));

        verify(taskService, never()).complete(anyString(), anyMap(), anyString());
    }
}
