package com.backend.proxy.dto.task;

/**
 * Representación mínima de un docente institucional disponible para asignación.
 */
public record DocenteDTO(
    String userId,
    String name
) {}
