package com.backend.proxy.exception;

/**
 * La tarea ya tiene responsable: otro integrante del grupo se la quedó primero, o es una
 * tarea asignada directamente a otra persona (enunciado 2).
 */
public class TaskAlreadyClaimedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public TaskAlreadyClaimedException(String message) {
        super(message);
    }
}
