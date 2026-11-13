package com.materiales.app.engine;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;

/**
 * Datos de conexión a la base del motor Flowable.
 *
 * <p>Orden de precedencia: propiedad de sistema ({@code -Ddb.url=...}), variable de
 * entorno ({@code DB_URL}) y por último {@code materiales.properties} del classpath.
 */
public record EngineConfig(
        String jdbcUrl,
        String jdbcDriver,
        String username,
        String password,
        String schemaUpdate
) {

    /** No toca el esquema del motor: solo valida que la versión coincida. */
    public static final String SCHEMA_VALIDATE_ONLY = "false";

    private static final String PROPERTIES_FILE = "/materiales.properties";

    public static EngineConfig load() {
        Properties file = readPropertiesFile();
        return new EngineConfig(
                resolve("db.url", "DB_URL", file, "jdbc:postgresql://localhost:5432/flowable"),
                resolve("db.driver", "DB_DRIVER", file, "org.postgresql.Driver"),
                resolve("db.username", "DB_USERNAME", file, "flowable"),
                resolve("db.password", "DB_PASSWORD", file, "flowable"),
                SCHEMA_VALIDATE_ONLY);
    }

    /** Configuración para los tests: motor embebido sobre H2, que sí crea el esquema. */
    public static EngineConfig inMemory(String databaseName) {
        return new EngineConfig(
                "jdbc:h2:mem:" + databaseName + ";DB_CLOSE_DELAY=-1",
                "org.h2.Driver",
                "sa",
                "",
                "true");
    }

    private static Properties readPropertiesFile() {
        Properties properties = new Properties();
        try (InputStream in = EngineConfig.class.getResourceAsStream(PROPERTIES_FILE)) {
            if (in != null) {
                properties.load(in);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer " + PROPERTIES_FILE, e);
        }
        return properties;
    }

    private static String resolve(String propertyKey, String envKey, Properties file, String fallback) {
        String fromSystem = System.getProperty(propertyKey);
        if (isSet(fromSystem)) {
            return fromSystem;
        }
        String fromEnv = System.getenv(envKey);
        if (isSet(fromEnv)) {
            return fromEnv;
        }
        String fromFile = file.getProperty(propertyKey);
        return isSet(fromFile) ? fromFile : fallback;
    }

    private static boolean isSet(String value) {
        return value != null && !value.isBlank();
    }

    /** Resumen para el log, sin la contraseña. */
    public String describe() {
        return username + "@" + jdbcUrl;
    }
}
