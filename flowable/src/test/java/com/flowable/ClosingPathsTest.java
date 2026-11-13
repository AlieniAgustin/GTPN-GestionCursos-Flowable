package com.flowable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.EndEvent;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowableListener;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.ProcessDefinition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestTemplate;

import com.flowable.app.App;
import com.flowable.app.listeners.UpdateCourseStatusListener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Enunciado 3.4: "El resultado del proceso deberá establecerse en cada camino de cierre
 * para que el Execution Listener pueda registrar el estado correspondiente".
 *
 * <p>En vez de recorrer los cinco escenarios otra vez (ya los cubren `HappyPathTest`,
 * `ProposalRejectionTest`, `NoResponseTest`, `CourseLimitExceededTest` y
 * `CompensationTest`), este test revisa el modelo desplegado: para cada evento de fin del
 * proceso, camina hacia atrás por los flujos y verifica que **todos** los caminos que
 * llegan ahí pasan por un listener que setea `resultadoCurso`. Así, si alguien agrega un
 * cierre nuevo sin marcar el resultado, falla acá y no en la demo.
 */
@SpringBootTest(classes = App.class)
@DirtiesContext
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:closing_paths;DB_CLOSE_DELAY=-1",
    "spring.datasource.driverClassName=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "flowable.database-schema-update=true",
    "flowable.async-executor-activate=false"
})
class ClosingPathsTest {

    /** Los cinco resultados que pide el enunciado 3.4. */
    private static final Set<String> EXPECTED_RESULTS = Set.of(
            "FINALIZADO", "CANCELADO", "RECHAZADO", "SIN_RESPUESTA", "NO_VIABLE");

    private static final Pattern RESULT_ASSIGNMENT = Pattern.compile(
            UpdateCourseStatusListener.RESULT_VARIABLE + "'\\s*,\\s*'([A-Z_]+)'");

    @Autowired
    private RepositoryService repositoryService;

    @MockBean(name = "backendRestTemplate")
    private RestTemplate backendRestTemplate;

    @Test
    @DisplayName("3.4 Closing paths: every process end event is reached with 'resultadoCurso' set")
    void testEveryClosingPathSetsTheResult() {
        Process process = deployedProcess();
        Map<String, SequenceFlow> flows = topLevelFlows(process);

        List<EndEvent> endEvents = new ArrayList<>(process.findFlowElementsOfType(EndEvent.class, false));
        assertFalse(endEvents.isEmpty(), "The process should have end events");

        List<String> unmarked = endEvents.stream()
                .filter(endEvent -> !alwaysSetsResult(endEvent.getId(), flows, new HashSet<>()))
                .map(FlowElement::getId)
                .collect(Collectors.toList());

        assertTrue(unmarked.isEmpty(),
                "These end events can be reached without setting " + UpdateCourseStatusListener.RESULT_VARIABLE
                        + ": " + unmarked);
    }

    @Test
    @DisplayName("3.4 Closing paths: the five results of the assignment are the ones the model sets")
    void testModelSetsTheFiveExpectedResults() {
        Process process = deployedProcess();
        Set<String> results = new LinkedHashSet<>();

        for (SequenceFlow flow : topLevelFlows(process).values()) {
            resultOf(flow).ifPresent(results::add);
        }

        assertEquals(EXPECTED_RESULTS, results,
                "The model should set exactly the five results listed in the assignment");
    }

    /**
     * True when every path that reaches this node goes through a flow that sets the result.
     */
    private boolean alwaysSetsResult(String nodeId, Map<String, SequenceFlow> flows, Set<String> visited) {
        if (!visited.add(nodeId)) {
            // Ciclo (revisar propuesta, recordatorios): ya lo está mirando otra rama
            return true;
        }

        List<SequenceFlow> incoming = flows.values().stream()
                .filter(flow -> nodeId.equals(flow.getTargetRef()))
                .collect(Collectors.toList());

        if (incoming.isEmpty()) {
            // Se llegó al inicio (o al borde de un subproceso) sin pasar por ningún resultado
            return false;
        }

        for (SequenceFlow flow : incoming) {
            if (resultOf(flow).isPresent()) {
                continue;
            }
            if (!alwaysSetsResult(flow.getSourceRef(), flows, visited)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Result that this flow's take listeners assign to 'resultadoCurso', if any.
     */
    private Optional<String> resultOf(SequenceFlow flow) {
        List<FlowableListener> listeners = flow.getExecutionListeners();
        if (listeners == null) {
            return Optional.empty();
        }
        for (FlowableListener listener : listeners) {
            String implementation = listener.getImplementation();
            if (implementation == null) {
                continue;
            }
            Matcher matcher = RESULT_ASSIGNMENT.matcher(implementation);
            if (matcher.find()) {
                return Optional.of(matcher.group(1));
            }
        }
        return Optional.empty();
    }

    /**
     * Flows of the process itself; the ones inside "Preparar curso" belong to the
     * subprocess and never close the instance.
     */
    private Map<String, SequenceFlow> topLevelFlows(Process process) {
        Map<String, SequenceFlow> flows = new LinkedHashMap<>();
        for (FlowElement element : process.getFlowElements()) {
            if (element instanceof SequenceFlow flow) {
                flows.put(flow.getId(), flow);
            }
        }
        return flows;
    }

    private Process deployedProcess() {
        ProcessDefinition definition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey("Process_Curso")
                .latestVersion()
                .singleResult();
        assertNotNull(definition, "Process 'Process_Curso' should be deployed");

        BpmnModel model = repositoryService.getBpmnModel(definition.getId());
        assertNotNull(model, "The deployed BPMN model should be readable");
        return model.getMainProcess();
    }
}
