package com.comunicaciones.app.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlowableRestClientTest {

    private FlowableStubServer server;
    private FlowableRestClient client;

    @BeforeEach
    void startServer() throws IOException {
        server = new FlowableStubServer();
        client = new FlowableRestClient(server.config());
    }

    @AfterEach
    void stopServer() {
        server.close();
    }

    @Test
    @DisplayName("GET: sends HTTP Basic and escapes the query parameters")
    void testGetSendsBasicAuthAndEscapesParams() {
        server.on("GET", "/runtime/executions", FlowableStubServer.StubResponse.json("{\"total\":0,\"data\":[]}"));

        JsonNode response = client.get("/runtime/executions",
                Map.of("messageEventSubscriptionName", "Nómina de participantes"));

        assertEquals(0, response.path("total").asInt());

        FlowableStubServer.RecordedRequest request = server.lastRequest();
        assertEquals("GET", request.method());
        assertEquals("Nómina de participantes", request.query().get("messageEventSubscriptionName"),
                "The accented name should arrive intact");

        String expectedAuth = "Basic " + Base64.getEncoder()
                .encodeToString("admin:test".getBytes(StandardCharsets.UTF_8));
        assertEquals(expectedAuth, request.authorization());
    }

    @Test
    @DisplayName("PUT: sends the JSON body and accepts a 204 with no content")
    void testPutAcceptsNoContent() {
        server.on("PUT", "/runtime/executions", FlowableStubServer.StubResponse.noContent());

        JsonNode response = client.put("/runtime/executions/exec-1", "{\"action\":\"messageEventReceived\"}");

        assertTrue(response.isNull(), "A 204 should come back as a null node, not blow up");
        assertEquals("{\"action\":\"messageEventReceived\"}", server.lastRequest().body());
    }

    @Test
    @DisplayName("Errors: a 401 points at the credentials being used")
    void testUnauthorizedMentionsTheAccount() {
        server.on("GET", "/runtime", new FlowableStubServer.StubResponse(401, ""));

        FlowableRestException error = assertThrows(FlowableRestException.class,
                () -> client.get("/runtime/process-instances", Map.of()));

        assertEquals(401, error.statusCode());
        assertTrue(error.getMessage().contains("admin@"), error.getMessage());
    }

    @Test
    @DisplayName("Errors: a 500 reports the status and what the engine answered")
    void testServerErrorIsReported() {
        server.on("GET", "/runtime", new FlowableStubServer.StubResponse(500,
                "{\"message\":\"Internal server error\"}"));

        FlowableRestException error = assertThrows(FlowableRestException.class,
                () -> client.get("/runtime/process-instances", Map.of()));

        assertEquals(500, error.statusCode());
        assertTrue(error.getMessage().contains("500"), error.getMessage());
        assertTrue(error.getMessage().contains("Internal server error"), error.getMessage());
    }

    @Test
    @DisplayName("Errors: with the engine down it says where it tried to connect")
    void testConnectionRefusedMentionsTheBaseUrl() {
        FlowableRestClient offline = new FlowableRestClient(
                new RestConfig("http://127.0.0.1:1/process-api", "admin", "test"));

        FlowableRestException error = assertThrows(FlowableRestException.class,
                () -> offline.get("/runtime/process-instances", Map.of()));

        assertTrue(error.getMessage().contains("http://127.0.0.1:1/process-api"), error.getMessage());
    }

    @Test
    @DisplayName("Config: a trailing slash in the base url does not duplicate separators")
    void testBaseUrlTrailingSlashIsTrimmed() {
        assertEquals("http://localhost:8080/process-api",
                new RestConfig("http://localhost:8080/process-api/", "admin", "test").baseUrl());
    }
}
