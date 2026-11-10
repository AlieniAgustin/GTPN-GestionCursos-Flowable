package com.backend.proxy.dto.task;

/**
 * Contrato de escritura para completar el envío del informe y archivado del expediente.
 */
public record EnviarInformeRequestDTO(
    boolean confirmarEnvio,
    String observacionesArchivo
) {}
