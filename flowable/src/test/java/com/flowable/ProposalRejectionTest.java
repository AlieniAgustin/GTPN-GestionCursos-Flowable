package com.flowable;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestTemplate;

import com.flowable.app.App;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest(classes = App.class)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:flowable;DB_CLOSE_DELAY=-1",
    "spring.datasource.driverClassName=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "flowable.database-schema-update=true",
    "flowable.async-executor-activate=false"
})
class ProposalRejectionTest {

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private HistoryService historyService;

    @MockBean(name = "backendRestTemplate")
    private RestTemplate backendRestTemplate;

    private Deployment deployment;

    @BeforeEach
    void setUp() throws Exception {
        try (InputStream bpmnStream = getClass().getClassLoader()
                .getResourceAsStream("processes/Institución_de_capacitación.bpmn20.xml")) {
            deployment = repositoryService.createDeployment()
                    .name("Training Institution Deployment - Test")
                    .addInputStream("Institución_de_capacitación.bpmn20.xml", bpmnStream)
                    .addClasspathResource("dmn/evaluarViabilidad.dmn")
                    .deploy();
        }
    }

    @AfterEach
    void tearDown() {
        if (deployment != null) {
            repositoryService.deleteDeployment(deployment.getId(), true);
        }
    }

    @Test
    @DisplayName("5. Proposal rejection: path when proposal is rejected")
    void testProposalRejectionPath() {
        // Replace hardcoded viable=true with business logic execution
        Map<String, Object> variables = new HashMap<>();
        variables.put("viable", true);

        ProcessInstance processInstance = runtimeService.startProcessInstanceByMessage("Solicitud de curso", variables);
        assertNotNull(processInstance, "Process instance should have started");
        String processInstanceId = processInstance.getId();

        Task proposalTask = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .taskDefinitionKey("registraryelaborarpropuesta")
                .singleResult();

        assertNotNull(proposalTask, "Task 'registraryelaborarpropuesta' should exist");
        assertEquals("jefeadministrativo", proposalTask.getAssignee());

        Map<String, Object> proposalVars = new HashMap<>();
        proposalVars.put("propuesta", "Propuesta Curso Flowable 2026");
        taskService.complete(proposalTask.getId(), proposalVars);

        // Execution waiting on rejection; timers and parallel executions also wait
        Execution rejectionExecution = runtimeService.createExecutionQuery()
                .processInstanceId(processInstanceId)
                .messageEventSubscriptionName("Rechazo")
                .singleResult();

        assertNotNull(rejectionExecution, "Process should be waiting for 'Rechazo' message event");

        // Simulate received message and cancel other waiting executions
        runtimeService.messageEventReceived("Rechazo", rejectionExecution.getId());

        Task archiveTask = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .taskDefinitionKey("archivarsolicitud")
                .singleResult();

        assertNotNull(archiveTask, "Task 'archivarsolicitud' should have been created");
        assertEquals("jefeadministrativo", archiveTask.getAssignee());

        Object courseResult = runtimeService.getVariable(processInstanceId, "resultadoCurso");
        assertEquals("RECHAZADO", courseResult, "Course result variable should be RECHAZADO");

        // Complete manual task to archive the request
        taskService.complete(archiveTask.getId());

        // Verify the process instance has ended
        ProcessInstance activeInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        assertNull(activeInstance, "Process instance should have ended");

        // Verify historic variable records RECHAZADO
        Object historicResult = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId)
                .variableName("resultadoCurso")
                .singleResult()
                .getValue();

        assertEquals("RECHAZADO", historicResult);
    }
}
