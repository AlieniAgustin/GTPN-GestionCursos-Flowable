package com.backend.proxy.dto.auth;

/**
 * Identidad del usuario autenticado. El {@code role} le dice al cliente que pantallas
 * ofrecer: {@code INSTITUCION} para el personal, {@code EMPRESA} para las empresas.
 */
public record LoginResponseDTO(
    String username,
    String role,
    boolean authenticated
) {}
