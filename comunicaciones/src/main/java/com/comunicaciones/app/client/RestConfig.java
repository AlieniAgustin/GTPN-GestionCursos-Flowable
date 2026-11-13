package com.comunicaciones.app.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;

/**
 * Dónde está la API REST de Flowable y con qué cuenta se la llama.
 *
 * <p>Orden de precedencia: propiedad de sistema ({@code -Dflowable.base-url=...}),
 * variable de entorno ({@code FLOWABLE_BASE_URL}) y por último
 * {@code comunicaciones.properties} del classpath.
 *
 * <p>La empresa no es usuario del motor (el enunciado 3.3 solo pide usuarios para el
 * personal), así que la app usa la misma cuenta técnica que el BFF.
 */
public record RestConfig(String baseUrl, String username, String password) {

    private static final String PROPERTIES_FILE = "/comunicaciones.properties";

    public RestConfig(String baseUrl, String username, String password) {
        this.baseUrl = baseUrl == null ? null : baseUrl.replaceAll("/+$", "");
        this.username = username;
        this.password = password;
    }

    public static RestConfig load() {
        Properties file = readPropertiesFile();
        return new RestConfig(
                resolve("flowable.base-url", "FLOWABLE_BASE_URL", file, "http://localhost:8080/process-api"),
                resolve("flowable.username", "FLOWABLE_USERNAME", file, "admin"),
                resolve("flowable.password", "FLOWABLE_PASSWORD", file, "test"));
    }

    private static Properties readPropertiesFile() {
        Properties properties = new Properties();
        try (InputStream in = RestConfig.class.getResourceAsStream(PROPERTIES_FILE)) {
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

    /** Resumen para el log y la ventana, sin la contraseña. */
    public String describe() {
        return username + "@" + baseUrl;
    }
}
