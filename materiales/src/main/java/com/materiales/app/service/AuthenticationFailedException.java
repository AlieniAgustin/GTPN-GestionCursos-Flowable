package com.materiales.app.service;

/** Login rechazado: credenciales inválidas o usuario sin la responsabilidad requerida. */
public class AuthenticationFailedException extends Exception {

    private static final long serialVersionUID = 1L;

    public AuthenticationFailedException(String message) {
        super(message);
    }
}
