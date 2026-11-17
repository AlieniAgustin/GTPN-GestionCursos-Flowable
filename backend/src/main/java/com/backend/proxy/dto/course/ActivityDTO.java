package com.backend.proxy.dto.course;

/**
 * Una actividad del proceso que está en curso o pendiente.
 *
 * <p>Se corresponde con una fila de
 * {@code GET /process-api/history/historic-activity-instances?finished=false}.
 */
public record ActivityDTO(
    String processInstanceId,
    String activityId,
    String activityName,
    String activityType
) {

    /**
     * El subproceso contenedor y las compuertas aparecen como actividades activas pero no
     * son algo que la empresa esté esperando: lo que importa son las tareas y los eventos.
     */
    public boolean isVisibleForCompany() {
        if (activityName == null || activityName.isBlank()) {
            return false;
        }
        return switch (activityType) {
            case "userTask", "serviceTask", "intermediateCatchEvent", "receiveTask" -> true;
            default -> false;
        };
    }
}
