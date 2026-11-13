package com.comunicaciones.app.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * Hace de API REST de Flowable en los tests: responde lo que se le configure y guarda lo
 * que recibió. Es el servidor HTTP del JDK, así que no agrega dependencias.
 */
public final class FlowableStubServer implements AutoCloseable {

    /** Petición tal como llegó al stub. */
    public record RecordedRequest(
            String method,
            String path,
            Map<String, String> query,
            String authorization,
            String body
    ) {
    }

    /** Respuesta que devuelve el stub. */
    public record StubResponse(int status, String body) {

        public static StubResponse json(String body) {
            return new StubResponse(200, body);
        }

        public static StubResponse noContent() {
            return new StubResponse(204, "");
        }
    }

    private final HttpServer server;
    private final List<RecordedRequest> requests = new ArrayList<>();
    private final Map<String, Function<RecordedRequest, StubResponse>> routes = new LinkedHashMap<>();

    public FlowableStubServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/process-api", this::handle);
        server.start();
    }

    public String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/process-api";
    }

    public RestConfig config() {
        return new RestConfig(baseUrl(), "admin", "test");
    }

    /** Registers what to answer for requests whose path starts with the given prefix. */
    public void on(String method, String pathPrefix, Function<RecordedRequest, StubResponse> responder) {
        routes.put(method + " " + pathPrefix, responder);
    }

    public void on(String method, String pathPrefix, StubResponse response) {
        on(method, pathPrefix, request -> response);
    }

    public List<RecordedRequest> requests() {
        return List.copyOf(requests);
    }

    public RecordedRequest lastRequest() {
        if (requests.isEmpty()) {
            throw new IllegalStateException("El stub no recibió ninguna petición");
        }
        return requests.get(requests.size() - 1);
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath().substring("/process-api".length());
        RecordedRequest request = new RecordedRequest(
                exchange.getRequestMethod(),
                path,
                parseQuery(exchange.getRequestURI().getRawQuery()),
                exchange.getRequestHeaders().getFirst("Authorization"),
                readBody(exchange.getRequestBody()));
        requests.add(request);

        StubResponse response = resolve(request);
        byte[] body = response.body().getBytes(StandardCharsets.UTF_8);

        if (body.length == 0) {
            exchange.sendResponseHeaders(response.status(), -1);
        } else {
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(response.status(), body.length);
            exchange.getResponseBody().write(body);
        }
        exchange.close();
    }

    private StubResponse resolve(RecordedRequest request) {
        return routes.entrySet().stream()
                .filter(route -> {
                    String[] parts = route.getKey().split(" ", 2);
                    return parts[0].equals(request.method()) && request.path().startsWith(parts[1]);
                })
                .map(Map.Entry::getValue)
                .findFirst()
                .map(responder -> responder.apply(request))
                .orElse(new StubResponse(404, "{\"message\":\"sin ruta en el stub: "
                        + request.method() + " " + request.path() + "\"}"));
    }

    private Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> query = new LinkedHashMap<>();
        if (rawQuery == null || rawQuery.isBlank()) {
            return query;
        }
        for (String pair : rawQuery.split("&")) {
            int separator = pair.indexOf('=');
            if (separator < 0) {
                query.put(decode(pair), "");
            } else {
                query.put(decode(pair.substring(0, separator)), decode(pair.substring(separator + 1)));
            }
        }
        return query;
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private String readBody(InputStream in) throws IOException {
        return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
}
