package com.backend.proxy.exception;

/**
 * Se intentó completar una tarea de grupo que todavía nadie reclamó (enunciado 2).
 */
public class TaskNotClaimedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public TaskNotClaimedException(String message) {
        super(message);
    }
}
