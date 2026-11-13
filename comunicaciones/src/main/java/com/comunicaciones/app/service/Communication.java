package com.comunicaciones.app.service;

/**
 * Las cinco comunicaciones de la empresa que pide simular el enunciado 4.2.2, con el
 * nombre del mensaje tal como está definido en el BPMN y el dato que cada una tiene que
 * aportar.
 */
public enum Communication {

    ACCEPTANCE(
            "Aceptación",
            "Aceptar la propuesta",
            "fechaCurso",
            "Fecha acordada de inicio del curso (AAAA-MM-DD o AAAA-MM-DDTHH:MM):",
            InputKind.DATE),

    REJECTION(
            "Rechazo",
            "Rechazar la propuesta",
            null, null, InputKind.NONE),

    MODIFICATIONS(
            "Solicitud de modificaciones",
            "Pedir modificaciones",
            "observaciones",
            "Observaciones para la nueva propuesta:",
            InputKind.TEXT),

    PARTICIPANTS(
            "Nómina de participantes",
            "Enviar la nómina de participantes",
            "participantes",
            "Participantes, uno por línea o separados por comas:",
            InputKind.LIST),

    CANCELLATION(
            "Cancelación",
            "Cancelar el curso",
            null, null, InputKind.NONE);

    /** Qué dato hay que pedirle al usuario antes de enviar la comunicación. */
    public enum InputKind {
        /** Nada: la comunicación se envía sola. */
        NONE,
        /** Texto libre obligatorio. */
        TEXT,
        /** Lista de nombres, que se normaliza separada por comas. */
        LIST,
        /** Fecha ISO-8601, con o sin hora. */
        DATE
    }

    private final String messageName;
    private final String label;
    private final String variableName;
    private final String prompt;
    private final InputKind inputKind;

    Communication(String messageName, String label, String variableName, String prompt, InputKind inputKind) {
        this.messageName = messageName;
        this.label = label;
        this.variableName = variableName;
        this.prompt = prompt;
        this.inputKind = inputKind;
    }

    /** Nombre del mensaje en el BPMN, el que espera la API REST. */
    public String messageName() {
        return messageName;
    }

    /** Texto del botón. */
    public String label() {
        return label;
    }

    /** Variable del proceso donde viaja el dato, o null si la comunicación no lleva datos. */
    public String variableName() {
        return variableName;
    }

    /** Pregunta que se le hace al usuario, o null si no hace falta ningún dato. */
    public String prompt() {
        return prompt;
    }

    public InputKind inputKind() {
        return inputKind;
    }

    public boolean needsInput() {
        return inputKind != InputKind.NONE;
    }
}
