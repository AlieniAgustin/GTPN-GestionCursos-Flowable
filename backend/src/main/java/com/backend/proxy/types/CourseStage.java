package com.backend.proxy.types;

import com.backend.proxy.types.CourseStatus;

public enum CourseStage {

    EN_EVALUACION(
        "En evaluación",
        "La institución está evaluando la viabilidad y preparando la propuesta."),

    PROPUESTA_ENVIADA(
        "Propuesta enviada",
        "Te enviamos la propuesta y estamos esperando tu respuesta."),

    EN_PREPARACION(
        "En preparación",
        "Aceptaste la propuesta y estamos preparando el curso."),

    CONFIRMADO_PENDIENTE_INICIO(
        "Confirmado, pendiente de inicio",
        "La preparación terminó. El curso arranca en la fecha acordada."),

    EN_DESARROLLO(
        "En desarrollo",
        "El curso se está dictando."),

    EN_ELABORACION_INFORME(
        "En elaboración del informe",
        "Terminó el dictado y se está elaborando el informe de resultados."),

    FINALIZADO(
        "Finalizado",
        "El curso terminó y te enviamos el informe de resultados."),

    CANCELADO(
        "Cancelado",
        "El curso se canceló y se liberaron los recursos reservados."),

    RECHAZADO(
        "Propuesta rechazada",
        "Rechazaste la propuesta y se archivó la solicitud."),

    NO_VIABLE(
        "No viable",
        "No es posible dictar el curso: la institución ya tiene cinco cursos en ejecución."),

    SIN_RESPUESTA(
        "Cerrado por falta de respuesta",
        "Se archivó la solicitud porque no recibimos respuesta a la propuesta.");

    private final String label;
    private final String description;

    CourseStage(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public boolean isClosed() {
        return switch (this) {
            case FINALIZADO, CANCELADO, RECHAZADO, NO_VIABLE, SIN_RESPUESTA -> true;
            default -> false;
        };
    }

    /**
     * Para una instancia ya cerrada, el estado sale del resultado que registró el proceso.
     */
    public static CourseStage from(CourseStatus status) {
        return switch (status) {
            case FINALIZADO -> FINALIZADO;
            case CANCELADO -> CANCELADO;
            case RECHAZADO -> RECHAZADO;
            case NO_VIABLE -> NO_VIABLE;
            case SIN_RESPUESTA -> SIN_RESPUESTA;
        };
    }
}
