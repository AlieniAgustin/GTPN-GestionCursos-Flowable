package com.backend.proxy.dto.task;

/**
 * Contrato de lectura para la tarea 'enviarinformeaempresayarchivarexpediente'.
 * Muestra al jefe administrativo el informe elaborado y los datos de destino (empresa)
 * antes del envío y cierre final del expediente.
 */
public record EnviarInformeDataDTO(
    String cursoNombre,
    String empresaNombre,
    String empresaEmail,
    String informe
) {}
