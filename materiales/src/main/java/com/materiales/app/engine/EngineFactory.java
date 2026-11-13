package com.materiales.app.engine;

import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;

/**
 * Arma el {@link ProcessEngine} embebido que usa la app (enunciado 4.2.1: API Java, no REST).
 *
 * <p>Dos decisiones importantes:
 * <ul>
 *   <li><b>No crea ni migra tablas</b> ({@code databaseSchemaUpdate = false}): el dueño del
 *       esquema es la app del motor. Si las versiones no coinciden, falla al arrancar en vez
 *       de tocar la base.</li>
 *   <li><b>Async executor apagado</b>: la app no debe competir por los jobs ni por los
 *       temporizadores del motor, solo leer y completar tareas.</li>
 * </ul>
 */
public final class EngineFactory {

    private EngineFactory() {
    }

    public static ProcessEngine create(EngineConfig config) {
        return ProcessEngineConfiguration.createStandaloneProcessEngineConfiguration()
                .setJdbcUrl(config.jdbcUrl())
                .setJdbcDriver(config.jdbcDriver())
                .setJdbcUsername(config.username())
                .setJdbcPassword(config.password())
                .setDatabaseSchemaUpdate(config.schemaUpdate())
                .setAsyncExecutorActivate(false)
                .buildProcessEngine();
    }
}
