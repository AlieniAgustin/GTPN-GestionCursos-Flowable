package com.comunicaciones.app.client;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Cliente mínimo de la API REST de Flowable (enunciado 4.2.2: las comunicaciones se envían
 * por REST). Solo necesita GET y PUT de JSON, con HTTP Basic.
 */
public class FlowableRestClient {

    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    private final RestConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FlowableRestClient(RestConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();
    }

    public RestConfig config() {
        return config;
    }

    public ObjectMapper objectMapper() {
        return objectMapper;
    }

    /**
     * GET de un recurso, con los parámetros de query ya escapados acá.
     */
    public JsonNode get(String path, Map<String, String> queryParams) {
        HttpRequest request = authenticated(HttpRequest.newBuilder(uri(path, queryParams)))
                .GET()
                .build();
        return send(request, "GET " + path);
    }

    /**
     * PUT con cuerpo JSON. Flowable responde 200 con el recurso o 204 sin cuerpo, según
     * el endpoint; un 204 devuelve un nodo nulo.
     */
    public JsonNode put(String path, String jsonBody) {
        HttpRequest request = authenticated(HttpRequest.newBuilder(uri(path, Map.of())))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();
        return send(request, "PUT " + path);
    }

    private JsonNode send(HttpRequest request, String description) {
        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new FlowableRestException(
                    "No se pudo contactar al motor en " + config.baseUrl() + ": " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FlowableRestException("Llamada interrumpida: " + description, e);
        }

        int status = response.statusCode();
        if (status == 401 || status == 403) {
            throw new FlowableRestException(
                    "El motor rechazó las credenciales de " + config.describe(), status);
        }
        if (status >= 400) {
            throw new FlowableRestException(
                    description + " devolvió " + status + ": " + summarize(response.body()), status);
        }

        if (response.body() == null || response.body().isBlank()) {
            return objectMapper.nullNode();
        }
        try {
            return objectMapper.readTree(response.body());
        } catch (IOException e) {
            throw new FlowableRestException(description + " devolvió un JSON ilegible", e);
        }
    }

    private HttpRequest.Builder authenticated(HttpRequest.Builder builder) {
        String credentials = config.username() + ":" + config.password();
        String encoded = Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        return builder
                .header("Authorization", "Basic " + encoded)
                .header("Accept", "application/json")
                .timeout(TIMEOUT);
    }

    private URI uri(String path, Map<String, String> queryParams) {
        StringBuilder url = new StringBuilder(config.baseUrl()).append(path);
        if (!queryParams.isEmpty()) {
            Map<String, String> ordered = new LinkedHashMap<>(queryParams);
            url.append('?');
            boolean first = true;
            for (Map.Entry<String, String> param : ordered.entrySet()) {
                if (!first) {
                    url.append('&');
                }
                url.append(encode(param.getKey())).append('=').append(encode(param.getValue()));
                first = false;
            }
        }
        return URI.create(url.toString());
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String summarize(String body) {
        if (body == null || body.isBlank()) {
            return "(sin cuerpo)";
        }
        String flat = body.replaceAll("\\s+", " ").trim();
        return flat.length() <= 300 ? flat : flat.substring(0, 300) + "…";
    }
}
