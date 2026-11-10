package com.backend.proxy.dto.task;

/**
 * Envoltorio estándar para la consulta agregada de una tarea de usuario.
 * Proporciona el identificador, nombre legible, URL de acción para el submit
 * desacoplado y el payload fuertemente tipado necesario para la UI.
 */
public record TaskDetailResponse<T>(
    String taskId,
    String taskName,
    String actionUrl,
    T data
) {}
