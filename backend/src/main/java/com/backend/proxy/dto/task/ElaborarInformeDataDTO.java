package com.backend.proxy.dto.task;

import java.util.List;

/**
 * Contrato de lectura para la tarea 'elaborarinformederesultados'.
 * Proporciona el resumen del curso dictado y los datos necesarios para que el
 * jefe docente confeccione el informe de resultados.
 */
public record ElaborarInformeDataDTO(
    String cursoNombre,
    String docente,
    List<String> contenidosTratados
) {}
