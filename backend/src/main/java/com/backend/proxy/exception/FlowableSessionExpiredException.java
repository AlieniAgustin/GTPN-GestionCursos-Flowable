package com.backend.proxy.exception;

/**
 * Excepción lanzada cuando Flowable responde 401 Unauthorized a una petición
 * autenticada con la cookie de sesión delegada.
 */
public class FlowableSessionExpiredException extends RuntimeException {

    private final String username;

    public FlowableSessionExpiredException(String message) {
        super(message);
        this.username = null;
    }

    public FlowableSessionExpiredException(String message, String username) {
        super(message);
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
}
