package com.backend.proxy.context;

import java.io.Serializable;

/**
 * Contexto de la sesión de Flowable asociado a la sesión del usuario en el Backend.
 *
 * @param username Identificador del empleado institucional.
 * @param flowableJSessionId Cookie JSESSIONID emitida por Flowable tras autenticarlo.
 */
public record FlowableSessionContext(
    String username,
    String flowableJSessionId
) implements Serializable {}
