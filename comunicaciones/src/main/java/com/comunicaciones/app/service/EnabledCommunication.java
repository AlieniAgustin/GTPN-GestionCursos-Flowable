package com.comunicaciones.app.service;

/**
 * Comunicación que la instancia está esperando ahora mismo, con la ejecución suscripta al
 * mensaje. El enunciado 4.2.2 pide mostrar solo estas.
 *
 * @param communication comunicación habilitada
 * @param executionId   ejecución suscripta al mensaje, a la que hay que mandárselo
 * @param activityId    actividad del BPMN que está esperando, para mostrarla
 */
public record EnabledCommunication(Communication communication, String executionId, String activityId) {
}
