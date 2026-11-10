package com.backend.proxy.strategy.task;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.backend.proxy.dto.task.DesarrollarCursoDataDTO;
import com.backend.proxy.dto.task.FlowableTaskDTO;
import com.backend.proxy.dto.task.TaskDetailResponse;

/**
 * Estrategia de consulta agregada para la tarea 'desarrollarcurso'.
 * Stub para la visualización del dictado asignado al docente.
 */
@Component
public class DesarrollarCursoStrategy implements TaskStrategy {

    public static final String TASK_KEY = "desarrollarcurso";

    @Override
    public boolean supports(String taskDefinitionKey) {
        return TASK_KEY.equalsIgnoreCase(taskDefinitionKey);
    }

    @Override
    public TaskDetailResponse<DesarrollarCursoDataDTO> buildDetail(FlowableTaskDTO task, Map<String, Object> processVariables) {
        /*
         * PASOS A IMPLEMENTAR:
         * 1. Extraer de 'processVariables' los metadatos relevantes para el dictado:
         *    - 'courseName': Nombre del curso.
         *    - 'docenteSeleccionado': Identificador del docente asignado dinámicamente.
         *    - 'fechaCurso': Fecha efectiva acordada para el inicio del dictado.
         * 2. Construir el DTO DesarrollarCursoDataDTO con dicha información de contexto.
         * 3. Retornar TaskDetailResponse con:
         *    - taskId = task.id()
         *    - taskName = task.name()
         *    - actionUrl = "/api/tasks/" + task.id() + "/desarrollar-curso"
         *    - data = DesarrollarCursoDataDTO
         */
        return null;
    }
}
