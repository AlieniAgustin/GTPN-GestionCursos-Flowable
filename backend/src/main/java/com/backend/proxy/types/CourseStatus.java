package com.backend.proxy.types;

public enum CourseStatus {
    NO_VIABLE,
    RECHAZADO,
    SIN_RESPUESTA,
    FINALIZADO,
    CANCELADO;

    public boolean isTerminal() {
        return switch (this) {
            case NO_VIABLE, RECHAZADO, SIN_RESPUESTA, FINALIZADO, CANCELADO -> true;
        };
    }
}
