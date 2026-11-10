package com.backend.proxy.dto.task;

import jakarta.validation.constraints.NotBlank;

/**
 * Contrato de escritura (Command payload) para completar la tarea de asignación docente.
 * Se valida de forma fail-fast antes de avanzar el token en Flowable.
 */
public record DefinirDocenteRequestDTO(
    @NotBlank(message = "El identificador del docente es obligatorio")
    String userId
) {}
