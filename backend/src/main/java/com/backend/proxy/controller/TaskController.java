package com.backend.proxy.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.backend.proxy.dto.task.DefineTeacherRequestDTO;
import com.backend.proxy.dto.task.DeliverCourseRequestDTO;
import com.backend.proxy.dto.task.PrepareReportRequestDTO;
import com.backend.proxy.dto.task.SendReportRequestDTO;
import com.backend.proxy.dto.task.TaskDetailResponse;
import com.backend.proxy.service.TaskService;

import jakarta.validation.Valid;

/**
 * REST controller for HATEOAS task inspection and strongly-typed task completion
 * for institutional users (development, course delivery, report and archiving).
 */
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    /**
     * Unified aggregated query endpoint (Query).
     * Dispatches dynamically via TaskStrategy based on taskDefinitionKey.
     */
    @GetMapping("/{taskId}/detail")
    public ResponseEntity<TaskDetailResponse<?>> getDetail(@PathVariable String taskId) {
        /*
         * IMPLEMENTATION STEPS:
         * 1. Call taskService.getTaskDetail(taskId).
         * 2. Return 200 OK with TaskDetailResponse<?> containing dynamic actionUrl and typed payload.
         */
        return ResponseEntity.ok(taskService.getTaskDetail(taskId));
    }

    /**
     * Completion action: Define Teacher.
     * Task: 'definirdocentequedesarrollaelcurso' (Teaching team lead).
     */
    @PostMapping("/{taskId}/define-teacher")
    public ResponseEntity<Void> completeDefineTeacher(
            @PathVariable String taskId,
            @Valid @RequestBody DefineTeacherRequestDTO dto) {
        /*
         * IMPLEMENTATION STEPS:
         * 1. Validate Bean Validation annotations (@Valid fail-fast: 400 Bad Request if userId is blank).
         * 2. Persist process variable 'docenteSeleccionado' with provided userId.
         * 3. Complete task in Flowable to trigger pause on intermediate timer 'fechaprogramada'.
         */
        taskService.complete(taskId, Map.of("docenteSeleccionado", dto.userId()));
        return ResponseEntity.noContent().build();
    }

    /**
     * Completion action: Deliver Course.
     * Task: 'desarrollarcurso' (Dynamically assigned teacher).
     */
    @PostMapping("/{taskId}/deliver-course")
    public ResponseEntity<Void> completeDeliverCourse(
            @PathVariable String taskId,
            @Valid @RequestBody DeliverCourseRequestDTO dto) {
        /*
         * IMPLEMENTATION STEPS:
         * 1. Record delivery observations if supplied.
         * 2. Advance execution token towards 'elaborarinformederesultados'.
         */
        taskService.complete(taskId, Map.of("cursoDesarrollado", true));
        return ResponseEntity.noContent().build();
    }

    /**
     * Completion action: Prepare Results Report.
     * Task: 'elaborarinformederesultados' (Teaching team lead).
     */
    @PostMapping("/{taskId}/prepare-report")
    public ResponseEntity<Void> completePrepareReport(
            @PathVariable String taskId,
            @Valid @RequestBody PrepareReportRequestDTO dto) {
        /*
         * IMPLEMENTATION STEPS:
         * 1. Validate that the report content is not blank (@Valid).
         * 2. Persist content into process dataObject 'informe'.
         * 3. Advance execution token towards 'enviarinformeaempresayarchivarexpediente'.
         */
        taskService.complete(taskId, Map.of("informe", dto.report()));
        return ResponseEntity.noContent().build();
    }

    /**
     * Completion action: Send Report and Archive File.
     * Task: 'enviarinformeaempresayarchivarexpediente' (Administrative lead).
     */
    @PostMapping("/{taskId}/send-report")
    public ResponseEntity<Void> completeSendReport(
            @PathVariable String taskId,
            @Valid @RequestBody SendReportRequestDTO dto) {
        /*
         * IMPLEMENTATION STEPS:
         * 1. Dispatch final report/document to client company.
         * 2. Persist audit flags ('informeEnviado', 'expedienteArchivado').
         * 3. Complete task in Flowable, triggering intermediate compensation 'liberarrecurso'
         *    and transitioning to endEvent 'cursofinalizado' with status = 'FINALIZADO'.
         */
        taskService.complete(taskId, Map.of(
            "informeEnviado", dto.confirmSend(),
            "expedienteArchivado", true
        ));
        return ResponseEntity.noContent().build();
    }
}
