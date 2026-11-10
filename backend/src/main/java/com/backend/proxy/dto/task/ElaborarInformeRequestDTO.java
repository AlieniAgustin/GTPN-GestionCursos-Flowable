package com.backend.proxy.dto.task;

import jakarta.validation.constraints.NotBlank;

/**
 * Contrato de escritura para completar la elaboración del informe de resultados.
 * El contenido textual del informe se persiste en Flowable como el dataObject 'informe'.
 */
public record ElaborarInformeRequestDTO(
    @NotBlank(message = "El contenido del informe no puede estar vacío")
    String informe,
    String calificacionGeneral
) {}
