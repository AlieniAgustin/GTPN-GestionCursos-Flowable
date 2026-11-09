package com.flowable;

import java.util.HashMap;
import java.util.Map;

import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestTemplate;

import com.flowable.app.App;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = App.class)
@DirtiesContext
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:compensation;DB_CLOSE_DELAY=-1",
    "spring.datasource.driverClassName=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "flowable.database-schema-update=true",
    "flowable.async-executor-activate=false"
})
class CompensationTest {
    @Autowired
    private TaskService taskService;

    @Autowired
    private CourseProcessFixture fixture;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private HistoryService historyService;

    @MockBean(name = "backendRestTemplate")
    private RestTemplate backendRestTemplate;

    @Test
    @DisplayName("5. compensation during the course of administrative tasks")
    void testCompensation() {
        Map<String, Object> proposalVars = new HashMap<>();
        proposalVars.put("propuesta", "Propuesta Curso Flowable 2026");

        ProcessInstance processInstance = fixture.advanceToWaitingForProposalResponse(proposalVars);
        assertNotNull(processInstance, "Process instance should have reached the response gateway");
        String processInstanceId = processInstance.getId();

        fixture.triggerAcceptance(processInstanceId);

        // reservaraula is service task, it is executed without intervention user

        Task prepareMaterials = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .taskDefinitionKey("prepararmateriales")
                .singleResult();
        assertNotNull(prepareMaterials, "Task 'prepararmateriales' should have been created");

        Task evaluateResources = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .taskDefinitionKey("evaluarrecursos")
                .singleResult();
        assertNotNull(evaluateResources, "Task 'evaluarrecursos' should have been created");
        Map<String, Object> variables = new HashMap<>();
        variables.put("requiereAula", false);
        variables.put("requierePlataforma", true);
        variables.put("requiereEquipamiento", true);
        taskService.complete(evaluateResources.getId(), variables);

        Task reserveVirtualRoom = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .taskDefinitionKey("solicitaraulavirtual")
                .singleResult();
        assertNotNull(reserveVirtualRoom, "Task 'solicitaraulavirtual' should have been created");
        
        Task hireEquipament = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .taskDefinitionKey("contratarequipamiento")
                .singleResult();
        taskService.complete(hireEquipament.getId());

        Execution execution = runtimeService.createExecutionQuery()
                                            .processInstanceId(processInstanceId)
                                            .messageEventSubscriptionName("Cancelación")
                                            .singleResult();
        assertNotNull(execution, "token in cancelation should have been created");
        runtimeService.messageEventReceived("Cancelación", execution.getId());

        // después de cancelar, el proceso espera que se libere el equipamiento
        assertNotNull(runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult(),
                "Process should wait for the release tasks");

        assertNotNull(fixture.completeTask(processInstanceId, "liberarequipamiento", new HashMap<>()),
                "Task 'liberarequipamiento' should have been created");
        assertNotNull(fixture.completeTask(processInstanceId, "confirmarliberacionrecursos", new HashMap<>()),
                "Task 'confirmarliberacionrecursos' should have been created");

        assertNull(runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult(),
                "Process instance should have completed after cancellation and compensation");

        // La compensación de 'contratarequipamiento' liberó el equipamiento y el curso quedó cancelado
        assertEquals(true, historicValue(processInstanceId, "equipoLiberado"), "Equipment should have been released");
        assertEquals("CANCELADO", historicValue(processInstanceId, "resultadoCurso"));
    }

    @Test
    @DisplayName("5b. compensation: 'Confirmar liberacion' cannot be completed before releasing the equipment")
    void testConfirmBeforeReleaseIsRejected() {
        String processInstanceId = startAndCancel(true);

        // Confirmar es el punto de pausa: mientras Liberar esté pendiente, el motor rechaza completarla
        Task confirm = findTask(processInstanceId, "confirmarliberacionrecursos");
        assertNotNull(confirm, "Task 'confirmarliberacionrecursos' should have been created");
        assertThrows(RuntimeException.class, () -> taskService.complete(confirm.getId()),
                "Confirm should be rejected while the equipment is not released");

        // El error deshace todo: el proceso sigue vivo y las dos tareas siguen pendientes
        assertNotNull(runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult(),
                "Process should still be active after the rejected confirmation");
        assertNotNull(findTask(processInstanceId, "confirmarliberacionrecursos"), "Confirm should still be pending");

        // Liberando primero, el cierre funciona
        assertNotNull(fixture.completeTask(processInstanceId, "liberarequipamiento", new HashMap<>()),
                "Task 'liberarequipamiento' should have been created");
        assertNotNull(fixture.completeTask(processInstanceId, "confirmarliberacionrecursos", new HashMap<>()),
                "Task 'confirmarliberacionrecursos' should have been created");

        assertNull(runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult(),
                "Process instance should have completed after cancellation and compensation");
        assertEquals(true, historicValue(processInstanceId, "equipoLiberado"), "Equipment should have been released");
        assertEquals("CANCELADO", historicValue(processInstanceId, "resultadoCurso"));
    }

    @Test
    @DisplayName("5c. compensation: cancellation without hired equipment only needs the confirmation")
    void testCancellationWithoutEquipment() {
        String processInstanceId = startAndCancel(false);

        // No se contrató equipamiento: no hay nada que liberar a mano
        assertNull(findTask(processInstanceId, "liberarequipamiento"), "Task 'liberarequipamiento' should not exist");

        assertNotNull(fixture.completeTask(processInstanceId, "confirmarliberacionrecursos", new HashMap<>()),
                "Task 'confirmarliberacionrecursos' should have been created");

        assertNull(runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult(),
                "Process instance should have completed after cancellation");
        // El aula reservada se libera sola por compensación
        assertEquals(true, historicValue(processInstanceId, "aulaLiberada"), "Classroom should have been released");
        assertEquals("CANCELADO", historicValue(processInstanceId, "resultadoCurso"));
    }

    /**
     * Lleva un curso a preparación, evalúa recursos (aula física y, si se pide, equipamiento contratado)
     * y manda la cancelación. Devuelve el id de la instancia.
     */
    private String startAndCancel(boolean withEquipment) {
        String processInstanceId = fixture.startCourseInPreparation().getId();

        Map<String, Object> resources = new HashMap<>();
        resources.put("requiereAula", true);
        resources.put("requierePlataforma", false);
        resources.put("requiereEquipamiento", withEquipment);
        assertNotNull(fixture.completeTask(processInstanceId, "evaluarrecursos", resources),
                "Task 'evaluarrecursos' should have been created");

        if (withEquipment) {
            assertNotNull(fixture.completeTask(processInstanceId, "contratarequipamiento", new HashMap<>()),
                    "Task 'contratarequipamiento' should have been created");
        }

        Execution execution = runtimeService.createExecutionQuery()
                .processInstanceId(processInstanceId)
                .messageEventSubscriptionName("Cancelación")
                .singleResult();
        assertNotNull(execution, "Process should be waiting for 'Cancelación' message event");
        runtimeService.messageEventReceived("Cancelación", execution.getId());
        return processInstanceId;
    }

    private Task findTask(String processInstanceId, String taskKey) {
        return taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .taskDefinitionKey(taskKey)
                .singleResult();
    }

    private Object historicValue(String processInstanceId, String name) {
        return historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId)
                .variableName(name)
                .singleResult()
                .getValue();
    }
}
