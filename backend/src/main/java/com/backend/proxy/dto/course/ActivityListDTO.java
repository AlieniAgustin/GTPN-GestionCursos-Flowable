package com.backend.proxy.dto.course;

import java.util.List;

/**
 * Respuesta paginada del historial de actividades de Flowable. Solo interesa {@code data}.
 */
public record ActivityListDTO(
    List<ActivityDTO> data,
    int total
) {}
