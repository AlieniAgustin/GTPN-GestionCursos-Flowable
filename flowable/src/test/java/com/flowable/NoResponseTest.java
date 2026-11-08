package com.flowable;

import java.util.HashMap;
import java.util.Map;

import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
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

@SpringBootTest(classes = App.class)
@DirtiesContext
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:no_response;DB_CLOSE_DELAY=-1",
    "spring.datasource.driverClassName=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "flowable.database-schema-update=true",
    "flowable.async-executor-activate=false"
})
class NoResponseTest {

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private CourseProcessFixture fixture;

    @MockBean(name = "backendRestTemplate")
    private RestTemplate backendRestTemplate;

    @Test
    @DisplayName("4. No response: request closed after two reminders")
    void testNoResponsePath() {
        Map<String, Object> proposalVars = new HashMap<>();
        proposalVars.put("propuesta", "Propuesta Curso Flowable 2026");

        ProcessInstance processInstance = fixture.advanceToWaitingForProposalResponse(proposalVars);
        assertNotNull(processInstance, "Process instance should have reached the response gateway");
        String processInstanceId = processInstance.getId();

        // Vencen los dos primeros plazos: se envían los recordatorios y se sigue esperando
        for (int reminder = 1; reminder <= 2; reminder++) {
            assertNotNull(fixture.fireTimer(processInstanceId), "Process should have been waiting on timer 'plazo5dias'");
            assertEquals(reminder, runtimeService.getVariable(processInstanceId, "recordatorios"));
            assertNull(taskService.createTaskQuery().processInstanceId(processInstanceId)
                    .taskDefinitionKey("archivarsolicitud").singleResult(),
                    "Request should not be archived after reminder " + reminder);
        }

        // Vence el tercer plazo sin respuesta: se archiva la solicitud
        assertNotNull(fixture.fireTimer(processInstanceId), "Process should have been waiting on timer 'plazo5dias'");

        Task archiveTask = fixture.completeTask(processInstanceId, "archivarsolicitud", new HashMap<>());
        assertNotNull(archiveTask, "Task 'archivarsolicitud' should have been created");
        assertEquals("jefeadministrativo", archiveTask.getAssignee());

        ProcessInstance activeInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        assertNull(activeInstance, "Process instance should have ended");

        Object historicResult = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId)
                .variableName("resultadoCurso")
                .singleResult()
                .getValue();
        assertEquals("SIN_RESPUESTA", historicResult);
    }
}
