package com.backend.proxy.dto.task;

import java.util.List;

/**
 * Respuesta paginada de {@code GET /process-api/runtime/tasks}. Solo se usa el contenido
 * de {@code data}; el resto de los campos de paginacion se ignoran.
 */
public record FlowableTaskListDTO(
    List<FlowableTaskDTO> data,
    int total
) {}
