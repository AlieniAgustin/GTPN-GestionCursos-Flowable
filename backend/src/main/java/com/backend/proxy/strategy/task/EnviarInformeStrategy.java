package com.backend.proxy.strategy.task;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.backend.proxy.dto.task.EnviarInformeDataDTO;
import com.backend.proxy.dto.task.FlowableTaskDTO;
import com.backend.proxy.dto.task.TaskDetailResponse;

/**
 * Estrategia de consulta agregada para la tarea 'enviarinformeaempresayarchivarexpediente'.
 * Stub para la revisión final, envío del informe y archivo por parte del jefe administrativo.
 */
@Component
public class EnviarInformeStrategy implements TaskStrategy {

    public static final String TASK_KEY = "enviarinformeaempresayarchivarexpediente";

    @Override
    public boolean supports(String taskDefinitionKey) {
        return TASK_KEY.equalsIgnoreCase(taskDefinitionKey);
    }

    @Override
    public TaskDetailResponse<EnviarInformeDataDTO> buildDetail(FlowableTaskDTO task, Map<String, Object> processVariables) {
        /*
         * PASOS A IMPLEMENTAR:
         * 1. Extraer de 'processVariables':
         *    - 'courseName': Nombre del curso.
         *    - 'companyName' y 'companyEmail': Datos de contacto de la empresa solicitante.
         *    - 'informe': Contenido redactado previamente en 'elaborarinformederesultados'.
         * 2. Construir el DTO EnviarInformeDataDTO para que el jefe administrativo revise
         *    el documento final antes de despacharlo a la empresa.
         * 3. Retornar TaskDetailResponse con:
         *    - taskId = task.id()
         *    - taskName = task.name()
         *    - actionUrl = "/api/tasks/" + task.id() + "/enviar-informe"
         *    - data = EnviarInformeDataDTO
         */
        return null;
    }
}
