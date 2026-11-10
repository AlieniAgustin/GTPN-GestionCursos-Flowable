package com.backend.proxy.strategy.task;

import java.util.Map;

import com.backend.proxy.dto.task.FlowableTaskDTO;
import com.backend.proxy.dto.task.TaskDetailResponse;

/**
 * Estrategia de agregación de contexto y generación de hipermedios para una userTask en Flowable.
 */
public interface TaskStrategy {

    /**
     * Determina si esta estrategia soporta la clave de definición de la tarea.
     *
     * @param taskDefinitionKey Identificador de definición en el BPMN (ej. 'definirdocentequedesarrollaelcurso')
     * @return true si aplica a la tarea, false en caso contrario
     */
    boolean supports(String taskDefinitionKey);

    /**
     * Construye la respuesta agregada de lectura (Query) con los datos de negocio y la actionUrl correspondiente.
     *
     * @param task Datos de la tarea en Flowable
     * @param processVariables Variables actuales del proceso
     * @return Respuesta con hipermedio y payload tipado
     */
    TaskDetailResponse<?> buildDetail(FlowableTaskDTO task, Map<String, Object> processVariables);
}
