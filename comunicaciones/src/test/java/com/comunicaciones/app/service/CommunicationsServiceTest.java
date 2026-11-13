package com.comunicaciones.app.service;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.comunicaciones.app.client.FlowableRestClient;
import com.comunicaciones.app.client.FlowableStubServer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Las operaciones del enunciado 4.2.2 contra un stub de la API REST: listar solicitudes,
 * saber qué comunicaciones están habilitadas y enviarlas con sus datos.
 */
class CommunicationsServiceTest {

    private static final String INSTANCES_JSON = """
            {
              "total": 2,
              "data": [
                {
                  "id": "inst-1",
                  "businessKey": "7",
                  "startTime": "2026-03-01T09:30:00.000+00:00",
                  "variables": [
                    {"name": "courseName", "value": "Flowable Avanzado"},
                    {"name": "companyName", "value": "ACME S.A."}
                  ]
                },
                {
                  "id": "inst-2",
                  "businessKey": "",
                  "startTime": "2026-02-20T11:00:00.000+00:00",
                  "variables": []
                }
              ]
            }
            """;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private FlowableStubServer server;
    private CommunicationsService communicationsService;

    @BeforeEach
    void startServer() throws IOException {
        server = new FlowableStubServer();
        communicationsService = new CommunicationsService(new FlowableRestClient(server.config()));
    }

    @AfterEach
    void stopServer() {
        server.close();
    }

    @Test
    @DisplayName("4.2.2 Requests: the running instances are listed with their course and company")
    void testFindRequests() {
        server.on("GET", "/runtime/process-instances",
                FlowableStubServer.StubResponse.json(INSTANCES_JSON));

        List<CourseRequest> requests = communicationsService.findRequests();

        assertEquals(2, requests.size());
        CourseRequest first = requests.get(0);
        assertEquals("inst-1", first.processInstanceId());
        assertEquals("7", first.businessKey());
        assertEquals("Flowable Avanzado", first.courseName());
        assertEquals("ACME S.A.", first.companyName());
        assertEquals("Flowable Avanzado — ACME S.A.", first.describe());

        // Una instancia iniciada desde Flowable UI no tiene esas variables: no debe romper
        assertEquals("", requests.get(1).courseName());
        assertEquals("Instancia inst-2", requests.get(1).describe());

        FlowableStubServer.RecordedRequest sent = server.lastRequest();
        assertEquals("Process_Curso", sent.query().get("processDefinitionKey"));
        assertEquals("true", sent.query().get("includeProcessVariables"),
                "The course and company come from the instance variables");
    }

    @Test
    @DisplayName("4.2.2 Enabled: while waiting for an answer, only the three negotiation ones show")
    void testEnabledWhileWaitingForTheAnswer() {
        stubSubscriptions(Set.of("Aceptación", "Rechazo", "Solicitud de modificaciones"));

        List<EnabledCommunication> enabled = communicationsService.findEnabled("inst-1");

        assertEquals(
                List.of(Communication.ACCEPTANCE, Communication.REJECTION, Communication.MODIFICATIONS),
                enabled.stream().map(EnabledCommunication::communication).collect(Collectors.toList()));
        assertEquals("exec-aceptacion", enabled.get(0).executionId());
        assertFalse(enabled.get(0).activityId().isBlank());
    }

    @Test
    @DisplayName("4.2.2 Enabled: during preparation only the participants list and the cancellation")
    void testEnabledDuringPreparation() {
        stubSubscriptions(Set.of("Nómina de participantes", "Cancelación"));

        List<EnabledCommunication> enabled = communicationsService.findEnabled("inst-1");

        assertEquals(List.of(Communication.PARTICIPANTS, Communication.CANCELLATION),
                enabled.stream().map(EnabledCommunication::communication).collect(Collectors.toList()));
    }

    @Test
    @DisplayName("4.2.2 Enabled: an instance waiting for nothing offers nothing")
    void testNoEnabledCommunications() {
        stubSubscriptions(Set.of());

        assertTrue(communicationsService.findEnabled("inst-1").isEmpty());
    }

    @Test
    @DisplayName("4.2.2 Acceptance: the message carries the agreed date")
    void testSendAcceptanceWithDate() throws Exception {
        server.on("PUT", "/runtime/executions", FlowableStubServer.StubResponse.noContent());

        communicationsService.send(
                new EnabledCommunication(Communication.ACCEPTANCE, "exec-1", "aceptacion"),
                "2026-03-01T09:00");

        FlowableStubServer.RecordedRequest sent = server.lastRequest();
        assertEquals("/runtime/executions/exec-1", sent.path());

        JsonNode body = objectMapper.readTree(sent.body());
        assertEquals("messageEventReceived", body.path("action").asText());
        assertEquals("Aceptación", body.path("messageName").asText());
        assertEquals("fechaCurso", body.path("variables").path(0).path("name").asText());
        assertEquals("2026-03-01T09:00", body.path("variables").path(0).path("value").asText());
    }

    @Test
    @DisplayName("4.2.2 Rejection: it travels with no variables")
    void testSendRejectionWithoutVariables() throws Exception {
        server.on("PUT", "/runtime/executions", FlowableStubServer.StubResponse.noContent());

        communicationsService.send(
                new EnabledCommunication(Communication.REJECTION, "exec-2", "rechazo"), null);

        JsonNode body = objectMapper.readTree(server.lastRequest().body());
        assertEquals("Rechazo", body.path("messageName").asText());
        assertTrue(body.path("variables").isMissingNode(), "Rejection carries no data");
    }

    @Test
    @DisplayName("4.2.2 Modifications: the observations travel in the message")
    void testSendModificationsWithObservations() throws Exception {
        server.on("PUT", "/runtime/executions", FlowableStubServer.StubResponse.noContent());

        communicationsService.send(
                new EnabledCommunication(Communication.MODIFICATIONS, "exec-3", "modificaciones"),
                "  Bajar el presupuesto y correr la fecha  ");

        JsonNode body = objectMapper.readTree(server.lastRequest().body());
        assertEquals("observaciones", body.path("variables").path(0).path("name").asText());
        assertEquals("Bajar el presupuesto y correr la fecha",
                body.path("variables").path(0).path("value").asText());
    }

    @Test
    @DisplayName("4.2.2 Participants: the list is normalized before sending it")
    void testSendParticipantsNormalizesTheList() throws Exception {
        server.on("PUT", "/runtime/executions", FlowableStubServer.StubResponse.noContent());

        communicationsService.send(
                new EnabledCommunication(Communication.PARTICIPANTS, "exec-4", "recibirnomina"),
                "Ana Pérez\nJuan Gómez,  Luis Díaz\n\n");

        JsonNode body = objectMapper.readTree(server.lastRequest().body());
        assertEquals("participantes", body.path("variables").path(0).path("name").asText());
        assertEquals("Ana Pérez, Juan Gómez, Luis Díaz",
                body.path("variables").path(0).path("value").asText());
    }

    @Test
    @DisplayName("4.2.2 Validation: a bad date is caught before calling the engine")
    void testInvalidDateIsRejectedLocally() {
        server.on("PUT", "/runtime/executions", FlowableStubServer.StubResponse.noContent());

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> communicationsService.send(
                        new EnabledCommunication(Communication.ACCEPTANCE, "exec-1", "aceptacion"),
                        "01/03/2026"));

        assertTrue(error.getMessage().contains("AAAA-MM-DD"), error.getMessage());
        assertTrue(server.requests().isEmpty(), "Nothing should have been sent to the engine");
    }

    @Test
    @DisplayName("4.2.2 Validation: a communication that needs data is not sent empty")
    void testMissingDataIsRejectedLocally() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> communicationsService.send(
                        new EnabledCommunication(Communication.MODIFICATIONS, "exec-3", "modificaciones"),
                        "   "));

        assertTrue(error.getMessage().contains("Falta el dato"), error.getMessage());
        assertTrue(server.requests().isEmpty());
    }

    /**
     * Makes the stub answer as an engine whose instance is subscribed to exactly these messages.
     */
    private void stubSubscriptions(Set<String> subscribedMessages) {
        server.on("GET", "/runtime/executions", request -> {
            String message = request.query().get("messageEventSubscriptionName");
            if (!subscribedMessages.contains(message)) {
                return FlowableStubServer.StubResponse.json("{\"total\":0,\"data\":[]}");
            }
            String executionId = "exec-" + message.toLowerCase().split(" ")[0]
                    .replace("ó", "o").replace("á", "a");
            return FlowableStubServer.StubResponse.json("{\"total\":1,\"data\":[{\"id\":\"" + executionId
                    + "\",\"activityId\":\"" + executionId.substring(5) + "\"}]}");
        });
    }
}
