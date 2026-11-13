package com.materiales.app.engine;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * La app corre tanto contra el PostgreSQL del stack como contra el de otra máquina de la
 * intranet, así que la conexión tiene que poder cambiarse sin recompilar.
 */
class EngineConfigTest {

    @Test
    @DisplayName("Config: without overrides it takes the values of materiales.properties")
    void testDefaultsComeFromThePropertiesFile() {
        EngineConfig config = EngineConfig.load();

        assertEquals("jdbc:postgresql://localhost:5432/flowable", config.jdbcUrl());
        assertEquals("org.postgresql.Driver", config.jdbcDriver());
        assertEquals("flowable", config.username());
        assertEquals(EngineConfig.SCHEMA_VALIDATE_ONLY, config.schemaUpdate(),
                "The app must never create or migrate the engine schema");
    }

    @Test
    @DisplayName("Config: -Ddb.url wins over the properties file")
    void testSystemPropertyWins() {
        System.setProperty("db.url", "jdbc:postgresql://intranet:5432/flowable");
        try {
            assertEquals("jdbc:postgresql://intranet:5432/flowable", EngineConfig.load().jdbcUrl());
        } finally {
            System.clearProperty("db.url");
        }
    }

    @Test
    @DisplayName("Config: the summary for the log does not leak the password")
    void testDescribeHidesThePassword() {
        System.setProperty("db.password", "s3cr3t-de-la-intranet");
        try {
            EngineConfig config = EngineConfig.load();

            assertEquals("s3cr3t-de-la-intranet", config.password());
            assertFalse(config.describe().contains("s3cr3t-de-la-intranet"),
                    "describe() is what goes to the console: " + config.describe());
        } finally {
            System.clearProperty("db.password");
        }
    }
}
