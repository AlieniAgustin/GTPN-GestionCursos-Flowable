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

@SpringBootTest(classes = App.class)
@DirtiesContext
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
    @DisplayName("5. Proposal rejection: path when proposal is rejected")
    void testProposalRejectionPath() {
        Map<String, Object> proposalVars = new HashMap<>();
        proposalVars.put("propuesta", "Propuesta Curso Flowable 2026");

        // Advance to event gateway ('esperarrespuesta') with custom proposal variables
        ProcessInstance processInstance = fixture.advanceToWaitingForProposalResponse(proposalVars);
        assertNotNull(processInstance, "Process instance should have reached the response gateway");
        String processInstanceId = processInstance.getId();

        // Execution waiting on rejection; trigger rejection message
        Execution rejectionExecution = fixture.triggerRejection(processInstanceId);
        assertNotNull(rejectionExecution, "Process should have been waiting for 'Rechazo' message event");

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
