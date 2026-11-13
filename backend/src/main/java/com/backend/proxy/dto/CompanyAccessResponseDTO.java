package com.backend.proxy.dto;

/**
 * Resultado de la actividad automatica "Habilitar acceso de la empresa" (enunciado 4.1.2).
 *
 * @param companyId         empresa de la institucion a la que quedo vinculado el curso
 * @param companyName       razon social, para los logs del motor
 * @param username          usuario con el que la empresa entra al sitio
 * @param temporaryPassword contrasena en claro, solo cuando la cuenta se acaba de crear;
 *                          el enunciado permite simular su comunicacion a la empresa
 * @param created           true si la cuenta se creo ahora, false si ya existia de una
 *                          contratacion anterior y se reutilizo
 */
public record CompanyAccessResponseDTO(
        Long companyId,
        String companyName,
        String username,
        String temporaryPassword,
        boolean created
) {}
