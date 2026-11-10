package com.backend.proxy.strategy.task;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.backend.proxy.dto.task.DefinirDocenteDataDTO;
import com.backend.proxy.dto.task.FlowableTaskDTO;
import com.backend.proxy.dto.task.TaskDetailResponse;
import com.backend.proxy.repository.DocenteRepository;

/**
 * Estrategia de consulta agregada para la tarea 'definirdocentequedesarrollaelcurso'.
 * Consulta el catálogo institucional de docentes y genera la actionUrl para la asignación.
 */
@Component
public class DefinirDocenteStrategy implements TaskStrategy {

    public static final String TASK_KEY = "definirdocentequedesarrollaelcurso";

    private final DocenteRepository docenteRepository;

    public DefinirDocenteStrategy(DocenteRepository docenteRepository) {
        this.docenteRepository = docenteRepository;
    }

    @Override
    public boolean supports(String taskDefinitionKey) {
        return TASK_KEY.equalsIgnoreCase(taskDefinitionKey);
    }

    @Override
    public TaskDetailResponse<DefinirDocenteDataDTO> buildDetail(FlowableTaskDTO task, Map<String, Object> processVariables) {
        /*
         * PASOS A IMPLEMENTAR:
         * 1. Consultar el catálogo de docentes habilitados mediante docenteRepository.findDocentesHabilitados().
         * 2. Recuperar el nombre del curso desde 'processVariables' (clave 'courseName').
         * 3. Construir el DTO DefinirDocenteDataDTO con los docentes y el contexto del curso.
         * 4. Retornar TaskDetailResponse con:
         *    - taskId = task.id()
         *    - taskName = task.name()
         *    - actionUrl = "/api/tasks/" + task.id() + "/definir-docente"
         *    - data = DefinirDocenteDataDTO
         */
        return null;
    }
}
