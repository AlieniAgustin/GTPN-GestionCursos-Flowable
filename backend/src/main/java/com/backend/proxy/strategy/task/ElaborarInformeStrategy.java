package com.backend.proxy.strategy.task;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.backend.proxy.dto.task.ElaborarInformeDataDTO;
import com.backend.proxy.dto.task.FlowableTaskDTO;
import com.backend.proxy.dto.task.TaskDetailResponse;

/**
 * Estrategia de consulta agregada para la tarea 'elaborarinformederesultados'.
 * Stub para la elaboración del reporte/informe final por parte del jefe de equipo docente.
 */
@Component
public class ElaborarInformeStrategy implements TaskStrategy {

    public static final String TASK_KEY = "elaborarinformederesultados";

    @Override
    public boolean supports(String taskDefinitionKey) {
        return TASK_KEY.equalsIgnoreCase(taskDefinitionKey);
    }

    @Override
    public TaskDetailResponse<ElaborarInformeDataDTO> buildDetail(FlowableTaskDTO task, Map<String, Object> processVariables) {
        /*
         * PASOS A IMPLEMENTAR:
         * 1. Extraer de 'processVariables' los datos de ejecución del curso:
         *    - 'courseName': Nombre del curso finalizado.
         *    - 'docenteSeleccionado': Docente que estuvo a cargo del dictado.
         * 2. Consultar o inferir los contenidos y temas cubiertos durante la capacitación.
         * 3. Construir el DTO ElaborarInformeDataDTO con los insumos para que el jefe docente
         *    redacte las conclusiones del dictado.
         * 4. Retornar TaskDetailResponse con:
         *    - taskId = task.id()
         *    - taskName = task.name()
         *    - actionUrl = "/api/tasks/" + task.id() + "/elaborar-informe"
         *    - data = ElaborarInformeDataDTO
         */
        return null;
    }
}
