package com.backend.proxy.web.view;

import com.backend.proxy.dto.task.FlowableTaskDTO;
import com.backend.proxy.types.TaskCatalog;

/**
 * Una tarea de la bandeja, con lo que el sitio necesita para saber qué botón ofrecer
 * (enunciado 4.1.3).
 */
public record TareaVista(
    FlowableTaskDTO tarea,
    String curso
) {

    public String id() {
        return tarea.id();
    }

    public String nombre() {
        return tarea.name();
    }

    /**
     * Sin responsable: es una tarea del grupo y hay que reclamarla antes de completarla.
     */
    public boolean sePuedeReclamar() {
        return tarea.assignee() == null || tarea.assignee().isBlank();
    }

    public boolean sePuedeCompletar() {
        return !sePuedeReclamar() && TaskCatalog.seCompletaDirecto(tarea.taskDefinitionKey());
    }

    /**
     * Ya reclamada, pero su pantalla todavía no existe: se completa desde Flowable UI.
     */
    public boolean necesitaFormulario() {
        return !sePuedeReclamar() && TaskCatalog.necesitaFormulario(tarea.taskDefinitionKey());
    }
}
