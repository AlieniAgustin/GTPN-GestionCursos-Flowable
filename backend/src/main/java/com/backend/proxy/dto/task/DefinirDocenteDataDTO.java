package com.backend.proxy.dto.task;

import java.util.List;

/**
 * Contrato de lectura (Query payload) para la tarea 'definirdocentequedesarrollaelcurso'.
 * Agrega el contexto del curso y la nómina de docentes habilitados para que el
 * jefe de equipo docente seleccione al responsable.
 */
public record DefinirDocenteDataDTO(
    String cursoNombre,
    List<DocenteDTO> docentesDisponibles
) {}
