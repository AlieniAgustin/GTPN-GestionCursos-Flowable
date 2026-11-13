package com.materiales.app.service;

import java.util.List;

/**
 * Integrante de la institución ya identificado contra el IDM de Flowable (enunciado 3.3).
 *
 * @param id          ID de usuario de Flowable, el que asigna y reclama tareas
 * @param displayName nombre y apellido si los tiene cargados; si no, el ID
 * @param groups      grupos a los que pertenece, para resolver las tareas reclamables
 */
public record StaffUser(String id, String displayName, List<String> groups) {

    public StaffUser(String id, String displayName, List<String> groups) {
        this.id = id;
        this.displayName = displayName;
        this.groups = List.copyOf(groups);
    }

    public boolean belongsTo(String groupId) {
        return groups.contains(groupId);
    }
}
