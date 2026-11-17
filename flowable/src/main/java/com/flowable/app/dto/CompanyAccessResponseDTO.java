package com.flowable.app.dto;

/**
 * Respuesta del BFF al habilitar el acceso de la empresa (enunciado 4.1.2).
 *
 * @param username usuario con el que la empresa entra al sitio
 * @param created  true si la cuenta se creó ahora, false si se reutilizó una anterior
 */
public record CompanyAccessResponseDTO(
        String username,
        boolean created
) {}
