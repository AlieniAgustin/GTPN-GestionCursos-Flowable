package com.materiales.app.service;

import java.util.Date;

/**
 * Una tarea <i>Preparar materiales</i> tal como la muestra la app.
 *
 * @param id                id de la tarea en el motor
 * @param name              nombre de la tarea en el BPMN
 * @param assignee          usuario asignado, o null si está disponible para el grupo
 * @param processInstanceId instancia del proceso a la que pertenece
 * @param courseName        nombre del curso, de las variables de la instancia
 * @param companyName       empresa solicitante, de las variables de la instancia
 * @param createTime        cuándo la creó el motor
 */
public record MaterialTask(
        String id,
        String name,
        String assignee,
        String processInstanceId,
        String courseName,
        String companyName,
        Date createTime
) {

    /** Disponible para el grupo: hay que reclamarla antes de completarla (enunciado 2). */
    public boolean claimable() {
        return assignee == null || assignee.isBlank();
    }

    public boolean assignedTo(String userId) {
        return assignee != null && assignee.equals(userId);
    }
}
