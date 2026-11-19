package com.backend.proxy.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
import com.backend.proxy.dto.task.FlowableTaskDTO;
import com.backend.proxy.dto.task.TaskDetailResponse;
import com.backend.proxy.service.TaskService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Tareas", description = "Endpoints de bandeja de entrada e inspección/resolución de tareas de Flowable")
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @Operation(summary = "Bandeja de tareas", description = "Lista las tareas asignadas o reclamables por el usuario autenticado")
    @GetMapping
    public ResponseEntity<List<FlowableTaskDTO>> inbox(@AuthenticationPrincipal String username) {
        return ResponseEntity.ok(taskService.findInbox(username));
    }
    // se le envia al usuario lo que necesita para resolver, y a donde tiene que responder 
    @Operation(summary = "Detalle de tarea", description = "Retorna el detalle agregado y contexto para resolver una tarea de Flowable")
    @GetMapping("/{taskId}/detail")
    public ResponseEntity<TaskDetailResponse<?>> getDetail(
            @PathVariable String taskId,
            @AuthenticationPrincipal String username) {
        return ResponseEntity.ok(taskService.getTaskDetail(taskId, username));
    }

    @Operation(summary = "Completar tarea: Definir docente", description = "Asigna el docente responsable del desarrollo del curso")
    @PostMapping("/{taskId}/define-teacher")
    public ResponseEntity<Void> completeDefineTeacher(
            @PathVariable String taskId,
            @Valid @RequestBody DefineTeacherRequestDTO dto,
            @AuthenticationPrincipal String username) {
        taskService.complete(taskId, Map.of("docenteSeleccionado", dto.userId()), username);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Completar tarea: Desarrollar curso", description = "Marca el curso como desarrollado por el docente asignado")
    @PostMapping("/{taskId}/deliver-course")
    public ResponseEntity<Void> completeDeliverCourse(
            @PathVariable String taskId,
            @Valid @RequestBody DeliverCourseRequestDTO dto,
            @AuthenticationPrincipal String username) {
        taskService.complete(taskId, Map.of("cursoDesarrollado", true), username);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Completar tarea: Elaborar informe", description = "Registra el informe de resultados del curso")
    @PostMapping("/{taskId}/prepare-report")
    public ResponseEntity<Void> completePrepareReport(
            @PathVariable String taskId,
            @Valid @RequestBody PrepareReportRequestDTO dto,
            @AuthenticationPrincipal String username) {
        taskService.complete(taskId, Map.of("informe", dto.report()), username);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Completar tarea: Enviar informe y archivar", description = "Confirma el envío del informe a la empresa y archiva el expediente")
    @PostMapping("/{taskId}/send-report")
    public ResponseEntity<Void> completeSendReport(
            @PathVariable String taskId,
            @Valid @RequestBody SendReportRequestDTO dto,
            @AuthenticationPrincipal String username) {
        taskService.complete(taskId, Map.of(
            "informeEnviado", dto.confirmSend(),
            "expedienteArchivado", true
        ), username);
        return ResponseEntity.noContent().build();
    }
}
