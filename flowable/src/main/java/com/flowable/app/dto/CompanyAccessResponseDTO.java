package com.flowable.app.dto;

/**
 * Respuesta del BFF al habilitar el acceso de la empresa (enunciado 4.1.2).
 *
 * @param companyId         empresa de la base de la institución a la que quedó vinculado el curso
 * @param companyName       razón social
 * @param username          usuario con el que la empresa entra al sitio
 * @param temporaryPassword contraseña en claro, solo cuando la cuenta se acaba de crear
 * @param created           true si la cuenta se creó ahora, false si se reutilizó una anterior
 */
public record CompanyAccessResponseDTO(
        Long companyId,
        String companyName,
        String username,
        String temporaryPassword,
        boolean created
) {}
