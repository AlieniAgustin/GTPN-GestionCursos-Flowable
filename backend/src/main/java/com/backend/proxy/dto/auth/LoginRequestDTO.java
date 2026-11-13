package com.backend.proxy.dto.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * Credentials submitted to {@code POST /api/auth/login}.
 * They are the user's Flowable credentials (enunciado 3.3): the institution does not
 * keep separate accounts for its own staff.
 */
public record LoginRequestDTO(

    @NotBlank(message = "Username is required")
    String username,

    @NotBlank(message = "Password is required")
    String password
) {}
