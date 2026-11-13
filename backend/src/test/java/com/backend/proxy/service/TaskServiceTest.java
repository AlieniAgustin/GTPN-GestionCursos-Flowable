package com.backend.proxy.service;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.client.RestClient;

import com.backend.proxy.client.FlowableClient;
import com.backend.proxy.dto.FlowableVariable;
import com.backend.proxy.dto.task.DefineTeacherDataDTO;
import com.backend.proxy.dto.task.FlowableTaskDTO;
import com.backend.proxy.dto.task.FlowableTaskListDTO;
import com.backend.proxy.dto.task.TaskDetailResponse;
import com.backend.proxy.exception.ResourceNotFoundException;
import com.backend.proxy.repository.TeacherRepositoryStub;
import com.backend.proxy.strategy.task.DefineTeacherStrategy;
import com.backend.proxy.strategy.task.DeliverCourseStrategy;
import com.backend.proxy.strategy.task.PrepareReportStrategy;
import com.backend.proxy.strategy.task.SendReportStrategy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Enunciado 4.1.3: cada usuario consulta las tareas asignadas a él y las disponibles para
 * su grupo, y solo puede completar las que le corresponden según las responsabilidades.
 */
class TaskServiceTest {

    private static final String TASK_ID = "task-1";
    private static final String INBOX_PREFIX = "/process-api/runtime/tasks?candidateOrAssigned=";

    private FlowableClient flowableClient;
    private TaskService taskService;

    @BeforeEach
    void setUp() {
        flowableClient = Mockito.mock(FlowableClient.class, Mockito.RETURNS_DEEP_STUBS);
        taskService = new TaskService(
                List.of(
                        new DefineTeacherStrategy(new TeacherRepositoryStub()),
                        new DeliverCourseStrategy(),
                        new PrepareReportStrategy(),
                        new SendReportStrategy()),
                flowableClient);
    }

    @Test
    @DisplayName("4.1.3 Detalle: resuelve la estrategia por taskDefinitionKey y arma el actionUrl")
    void testDetailResolvesStrategy() {
        stubTask(task("definirdocentequedesarrollaelcurso"));
        stubInboxContaining(TASK_ID);
        stubVariables(List.of(new FlowableVariable("courseName", "Curso de Flowable")));

        TaskDetailResponse<?> detail = taskService.getTaskDetail(TASK_ID, "jefedocente");

        assertEquals("/api/tasks/" + TASK_ID + "/define-teacher", detail.actionUrl());
        DefineTeacherDataDTO data = (DefineTeacherDataDTO) detail.data();
        assertEquals("Curso de Flowable", data.courseName());
        assertEquals(3, data.availableTeachers().size(), "El equipo docente son tres (enunciado 2)");
    }

    @Test
    @DisplayName("4.1.3 Detalle: una tarea sin estrategia asociada da 404 con el motivo")
    void testTaskWithoutStrategy() {
        stubTask(task("registraryelaborarpropuesta"));
        stubInboxContaining(TASK_ID);

        ResourceNotFoundException error = assertThrows(ResourceNotFoundException.class,
                () -> taskService.getTaskDetail(TASK_ID, "jefeadministrativo"));
        assertTrue(error.getMessage().contains("registraryelaborarpropuesta"));
    }

    @Test
    @DisplayName("4.1.3 Detalle: una tarea que no está en la bandeja del usuario no se ve")
    void testTaskOutsideTheInboxIsNotVisible() {
        stubTask(task("definirdocentequedesarrollaelcurso"));
        stubInboxContaining("otra-tarea");

        assertThrows(ResourceNotFoundException.class,
                () -> taskService.getTaskDetail(TASK_ID, "docente1"));
    }

    @Test
    @DisplayName("4.1.3 Completar: una tarea ajena no se completa y no se llama al motor")
    void testCompletingSomebodyElsesTaskIsRejected() {
        stubTask(task("desarrollarcurso"));
        stubInboxContaining("otra-tarea");

        assertThrows(ResourceNotFoundException.class,
                () -> taskService.complete(TASK_ID, Map.of("cursoDesarrollado", true), "docente2"));

        verify(flowableClient, never()).post(contains("/runtime/tasks/" + TASK_ID), any());
    }

    @Test
    @DisplayName("4.1.3 Completar: la tarea propia se completa con sus variables")
    void testCompleteOwnTask() {
        stubTask(task("desarrollarcurso"));
        stubInboxContaining(TASK_ID);

        taskService.complete(TASK_ID, Map.of("cursoDesarrollado", true), "docente1");

        verify(flowableClient).post(contains("/runtime/tasks/" + TASK_ID), any());
    }

    @Test
    @DisplayName("4.1.3 Bandeja: trae lo asignado y lo reclamable, y tolera una respuesta vacía")
    void testInbox() {
        stubInboxContaining(TASK_ID, "task-2");
        assertEquals(2, taskService.findInbox("administrativo1").size());

        when(flowableClient.get(anyString()).retrieve().body(FlowableTaskListDTO.class)).thenReturn(null);
        assertTrue(taskService.findInbox("administrativo1").isEmpty(), "Sin datos la bandeja va vacía");
    }

    private FlowableTaskDTO task(String definitionKey) {
        return new FlowableTaskDTO(TASK_ID, "Una tarea", definitionKey, null, "proc-1", "exec-1");
    }

    private void stubTask(FlowableTaskDTO task) {
        RestClient.RequestHeadersSpec<?> spec = flowableClient.get("/process-api/runtime/tasks/" + TASK_ID);
        when(spec.retrieve().body(FlowableTaskDTO.class)).thenReturn(task);
    }

    private void stubInboxContaining(String... taskIds) {
        List<FlowableTaskDTO> tasks = List.of(taskIds).stream()
                .map(id -> new FlowableTaskDTO(id, "Una tarea", "x", null, "proc-1", "exec-1"))
                .toList();

        RestClient.RequestHeadersSpec<?> spec = flowableClient.get(contains(INBOX_PREFIX));
        when(spec.retrieve().body(FlowableTaskListDTO.class))
                .thenReturn(new FlowableTaskListDTO(tasks, tasks.size()));
    }

    private void stubVariables(List<FlowableVariable> variables) {
        RestClient.RequestHeadersSpec<?> spec =
                flowableClient.get("/process-api/runtime/process-instances/proc-1/variables");
        when(spec.retrieve().body(Mockito.<org.springframework.core.ParameterizedTypeReference<List<FlowableVariable>>>any()))
                .thenReturn(variables);
    }
}
