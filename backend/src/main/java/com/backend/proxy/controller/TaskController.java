package com.backend.proxy.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.backend.proxy.dto.task.DefinirDocenteRequestDTO;
import com.backend.proxy.dto.task.DesarrollarCursoRequestDTO;
import com.backend.proxy.dto.task.ElaborarInformeRequestDTO;
import com.backend.proxy.dto.task.EnviarInformeRequestDTO;
import com.backend.proxy.dto.task.TaskDetailResponse;
import com.backend.proxy.service.TaskService;

import jakarta.validation.Valid;

/**
 * Controlador REST para la consulta de detalles (HATEOAS) y resolución tipada
 * de tareas asignadas al personal de la institución (desarrollo, dictado y reporte/cierre).
 */
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    /**
     * Endpoint unificado de consulta agregada (Query).
     * Despacha mediante TaskStrategy según la taskDefinitionKey de la tarea en Flowable.
     */
    @GetMapping("/{taskId}/detail")
    public ResponseEntity<TaskDetailResponse<?>> getDetail(@PathVariable String taskId) {
        /*
         * PASOS A IMPLEMENTAR:
         * 1. Invocar a taskService.getTaskDetail(taskId).
         * 2. Retornar 200 OK con el envoltorio TaskDetailResponse<?> conteniendo
         *    actionUrl dinámica y el payload de datos tipado.
         */
        return ResponseEntity.ok(taskService.getTaskDetail(taskId));
    }

    /**
     * Acción de completitud: Definir Docente.
     * Tarea: 'definirdocentequedesarrollaelcurso' (Jefe de Equipo Docente).
     */
    @PostMapping("/{taskId}/definir-docente")
    public ResponseEntity<Void> completarDefinirDocente(
            @PathVariable String taskId,
            @Valid @RequestBody DefinirDocenteRequestDTO dto) {
        /*
         * PASOS A IMPLEMENTAR:
         * 1. Validar anotaciones Bean Validation (@Valid fail-fast: 400 Bad Request si userId es blanco).
         * 2. Persistir variable de proceso 'docenteSeleccionado' con el userId provisto.
         * 3. Completar la tarea en Flowable para habilitar la espera en el timer 'fechaprogramada'.
         */
        taskService.complete(taskId, Map.of("docenteSeleccionado", dto.userId()));
        return ResponseEntity.noContent().build();
    }

    /**
     * Acción de completitud: Desarrollar Curso.
     * Tarea: 'desarrollarcurso' (Docente Asignado dinámicamente).
     */
    @PostMapping("/{taskId}/desarrollar-curso")
    public ResponseEntity<Void> completarDesarrollarCurso(
            @PathVariable String taskId,
            @Valid @RequestBody DesarrollarCursoRequestDTO dto) {
        /*
         * PASOS A IMPLEMENTAR:
         * 1. Registrar observaciones del dictado si fueran provistas.
         * 2. Avanzar el token de ejecución hacia 'elaborarinformederesultados'.
         */
        taskService.complete(taskId, Map.of("cursoDesarrollado", true));
        return ResponseEntity.noContent().build();
    }

    /**
     * Acción de completitud: Elaborar Informe de Resultados.
     * Tarea: 'elaborarinformederesultados' (Jefe de Equipo Docente).
     */
    @PostMapping("/{taskId}/elaborar-informe")
    public ResponseEntity<Void> completarElaborarInforme(
            @PathVariable String taskId,
            @Valid @RequestBody ElaborarInformeRequestDTO dto) {
        /*
         * PASOS A IMPLEMENTAR:
         * 1. Validar que el informe no sea nulo ni vacío (@Valid).
         * 2. Persistir el contenido en el dataObject 'informe' del proceso en Flowable.
         * 3. Avanzar el token hacia 'enviarinformeaempresayarchivarexpediente'.
         */
        taskService.complete(taskId, Map.of("informe", dto.informe()));
        return ResponseEntity.noContent().build();
    }

    /**
     * Acción de completitud: Enviar Informe y Archivar Expediente.
     * Tarea: 'enviarinformeaempresayarchivarexpediente' (Jefe Administrativo).
     */
    @PostMapping("/{taskId}/enviar-informe")
    public ResponseEntity<Void> completarEnviarInforme(
            @PathVariable String taskId,
            @Valid @RequestBody EnviarInformeRequestDTO dto) {
        /*
         * PASOS A IMPLEMENTAR:
         * 1. Despachar el informe/documento a la empresa solicitante.
         * 2. Persistir flags de auditoría ('informeEnviado', 'expedienteArchivado').
         * 3. Completar la tarea en Flowable, lo que desencadenará el evento de compensación
         *    'liberarrecurso' (liberando el aula reservada) y la transición al endEvent
         *    'cursofinalizado' con status = 'FINALIZADO'.
         */
        taskService.complete(taskId, Map.of(
            "informeEnviado", dto.confirmarEnvio(),
            "expedienteArchivado", true
        ));
        return ResponseEntity.noContent().build();
    }
}
