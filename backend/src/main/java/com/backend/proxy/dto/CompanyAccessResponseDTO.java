package com.backend.proxy.dto;

/**
 * Resultado de la actividad automática "Habilitar acceso de la empresa" (enunciado 4.1.2).
 *
 * @param username usuario con el que la empresa entra al sitio
 * @param created  true si la cuenta se creó ahora, false si ya existía y se reutilizó
 */
public record CompanyAccessResponseDTO(
        String username,
        boolean created
) {}
