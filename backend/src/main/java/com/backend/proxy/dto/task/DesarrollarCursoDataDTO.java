package com.backend.proxy.dto.task;

/**
 * Contrato de lectura para la tarea 'desarrollarcurso'.
 * Muestra al docente asignado los datos del curso a dictar una vez alcanzada la fecha programada.
 */
public record DesarrollarCursoDataDTO(
    String cursoNombre,
    String docenteAsignado,
    String fechaInicio
) {}
