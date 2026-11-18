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
     * Ya reclamada y con pantalla propia en el sitio: la bandeja ofrece ir al formulario.
     */
    public boolean tieneFormularioEnSitio() {
        return !sePuedeReclamar() && TaskCatalog.formularioEnSitio(tarea.taskDefinitionKey()) != null;
    }

    /**
     * Tramo de la URL del formulario ({@code /tareas/{id}/<tramo>}), si existe.
     */
    public String formulario() {
        return TaskCatalog.formularioEnSitio(tarea.taskDefinitionKey());
    }

    /**
     * Ya reclamada, pero su pantalla todavía no existe: se completa desde Flowable UI.
     */
    public boolean necesitaFormulario() {
        return !sePuedeReclamar() && !tieneFormularioEnSitio()
                && TaskCatalog.necesitaFormulario(tarea.taskDefinitionKey());
    }
}
