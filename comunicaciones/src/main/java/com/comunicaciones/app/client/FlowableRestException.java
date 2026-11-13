package com.comunicaciones.app.client;

/** Falla al hablar con la API REST: red caída, credenciales, o un 4xx/5xx del motor. */
public class FlowableRestException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int statusCode;

    public FlowableRestException(String message) {
        this(message, 0);
    }

    public FlowableRestException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public FlowableRestException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 0;
    }

    /** Código HTTP de la respuesta, o 0 si no hubo respuesta. */
    public int statusCode() {
        return statusCode;
    }
}
