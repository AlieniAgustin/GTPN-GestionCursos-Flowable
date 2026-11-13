package com.comunicaciones.app.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.comunicaciones.app.client.FlowableRestClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Las operaciones del enunciado 4.2.2, todas por la API REST de Flowable: listar las
 * solicitudes, ver qué comunicaciones están habilitadas en el estado actual de cada una y
 * enviarlas con los datos que correspondan.
 */
public class CommunicationsService {

    public static final String PROCESS_KEY = "Process_Curso";

    private static final String INSTANCES_PATH = "/runtime/process-instances";
    private static final String EXECUTIONS_PATH = "/runtime/executions";
    private static final int PAGE_SIZE = 100;

    private final FlowableRestClient client;

    public CommunicationsService(FlowableRestClient client) {
        this.client = client;
    }

    /**
     * Solicitudes en curso, las más nuevas primero. Las instancias terminadas no están:
     * ya no aceptan comunicaciones.
     */
    public List<CourseRequest> findRequests() {
        JsonNode response = client.get(INSTANCES_PATH, new LinkedHashMap<>(Map.of(
                "processDefinitionKey", PROCESS_KEY,
                "includeProcessVariables", "true",
                "size", String.valueOf(PAGE_SIZE),
                "sort", "startTime",
                "order", "desc")));

        List<CourseRequest> requests = new ArrayList<>();
        for (JsonNode instance : response.path("data")) {
            Map<String, String> variables = readVariables(instance.path("variables"));
            requests.add(new CourseRequest(
                    instance.path("id").asText(""),
                    instance.path("businessKey").asText(""),
                    variables.getOrDefault("courseName", ""),
                    variables.getOrDefault("companyName", ""),
                    instance.path("startTime").asText("")));
        }
        return requests;
    }

    /**
     * Comunicaciones que la instancia puede recibir ahora.
     *
     * <p>No se deduce del estado con reglas propias: se le pregunta al motor por las
     * ejecuciones suscriptas a cada mensaje, que es la única fuente de verdad. Si la
     * instancia está esperando respuesta a la propuesta aparecen aceptación, rechazo y
     * modificaciones; si está en preparación, la nómina y la cancelación.
     */
    public List<EnabledCommunication> findEnabled(String processInstanceId) {
        List<EnabledCommunication> enabled = new ArrayList<>();

        for (Communication communication : Communication.values()) {
            JsonNode response = client.get(EXECUTIONS_PATH, new LinkedHashMap<>(Map.of(
                    "processInstanceId", processInstanceId,
                    "messageEventSubscriptionName", communication.messageName())));

            JsonNode execution = response.path("data").path(0);
            if (!execution.isMissingNode() && execution.hasNonNull("id")) {
                enabled.add(new EnabledCommunication(
                        communication,
                        execution.path("id").asText(),
                        execution.path("activityId").asText("")));
            }
        }
        return enabled;
    }

    /**
     * Envía la comunicación a la ejecución suscripta, con el dato que aporta.
     *
     * @param target comunicación habilitada, de {@link #findEnabled(String)}
     * @param input  dato que pide la comunicación, o null si no lleva ninguno
     */
    public void send(EnabledCommunication target, String input) {
        Communication communication = target.communication();

        ObjectNode body = client.objectMapper().createObjectNode();
        body.put("action", "messageEventReceived");
        body.put("messageName", communication.messageName());

        if (communication.needsInput()) {
            String value = normalize(communication, input);
            ArrayNode variables = body.putArray("variables");
            ObjectNode variable = variables.addObject();
            variable.put("name", communication.variableName());
            variable.put("type", "string");
            variable.put("value", value);
        }

        client.put(EXECUTIONS_PATH + "/" + target.executionId(), body.toString());
    }

    /**
     * Valida y normaliza el dato antes de mandarlo, así un error se ve acá y no como una
     * instancia trabada más adelante.
     */
    String normalize(Communication communication, String input) {
        String value = input == null ? "" : input.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Falta el dato: " + communication.prompt());
        }

        return switch (communication.inputKind()) {
            case DATE -> normalizeDate(value);
            case LIST -> normalizeList(value);
            default -> value;
        };
    }

    private String normalizeDate(String value) {
        try {
            OffsetDateTime.parse(value);
            return value;
        } catch (DateTimeParseException ignored) {
            // sigue con los formatos sin offset
        }
        try {
            LocalDateTime.parse(value);
            return value;
        } catch (DateTimeParseException ignored) {
            // sigue con la fecha sola
        }
        try {
            LocalDate.parse(value);
            return value;
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "'" + value + "' no es una fecha válida: usá AAAA-MM-DD o AAAA-MM-DDTHH:MM");
        }
    }

    private String normalizeList(String value) {
        List<String> items = Arrays.stream(value.split("[\\r\\n,;]+"))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .collect(Collectors.toList());
        if (items.isEmpty()) {
            throw new IllegalArgumentException("La nómina no puede estar vacía");
        }
        return String.join(", ", items);
    }

    private Map<String, String> readVariables(JsonNode variables) {
        Map<String, String> values = new LinkedHashMap<>();
        for (JsonNode variable : variables) {
            JsonNode value = variable.path("value");
            values.put(variable.path("name").asText(""), value.isNull() ? "" : value.asText(""));
        }
        return values;
    }
}
