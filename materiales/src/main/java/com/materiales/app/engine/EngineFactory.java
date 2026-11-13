package com.materiales.app.engine;

import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;

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
 *   <li><b>Event registry deshabilitado</b>: la app no lo usa, y arrancarlo dispara un
 *       chequeo de Liquibase sobre sus tablas. Sin eso, el arranque no toca el esquema ni
 *       llena la consola de logs.</li>
 * </ul>
 */
public final class EngineFactory {

    private EngineFactory() {
    }

    public static ProcessEngine create(EngineConfig config) {
        ProcessEngineConfigurationImpl engineConfiguration =
                (ProcessEngineConfigurationImpl) ProcessEngineConfiguration
                        .createStandaloneProcessEngineConfiguration();

        engineConfiguration.setJdbcUrl(config.jdbcUrl());
        engineConfiguration.setJdbcDriver(config.jdbcDriver());
        engineConfiguration.setJdbcUsername(config.username());
        engineConfiguration.setJdbcPassword(config.password());
        engineConfiguration.setDatabaseSchemaUpdate(config.schemaUpdate());
        engineConfiguration.setAsyncExecutorActivate(false);
        engineConfiguration.setDisableEventRegistry(true);

        return engineConfiguration.buildProcessEngine();
    }
}
