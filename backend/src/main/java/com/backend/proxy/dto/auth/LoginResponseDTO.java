package com.backend.proxy.dto.auth;

/**
 * Identity of the authenticated user, returned after a successful login
 * so the client can decide which screens to offer.
 */
public record LoginResponseDTO(
    String username,
    boolean authenticated
) {}
