package com.flowable;

import java.util.HashMap;
import java.util.Map;

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

@SpringBootTest(classes = App.class)
@DirtiesContext
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:proposal_modifications;DB_CLOSE_DELAY=-1",
    "spring.datasource.driverClassName=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "flowable.database-schema-update=true",
    "flowable.async-executor-activate=false"
})
class ProposalModificationsTest {

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private CourseProcessFixture fixture;

    @MockBean(name = "backendRestTemplate")
    private RestTemplate backendRestTemplate;

    @Test
    @DisplayName("2. Proposal modifications: company asks for changes and a new proposal is sent")
    void testProposalModificationsPath() {
        Map<String, Object> proposalVars = new HashMap<>();
        proposalVars.put("propuesta", "Propuesta Curso Flowable 2026");

        ProcessInstance processInstance = fixture.advanceToWaitingForProposalResponse(proposalVars);
        assertNotNull(processInstance, "Process instance should have reached the response gateway");
        String processInstanceId = processInstance.getId();

        // Un recordatorio antes de la respuesta, para verificar que después se reinicia el contador
        assertNotNull(fixture.fireTimer(processInstanceId), "Process should have been waiting on timer 'plazo5dias'");
        assertEquals(1, runtimeService.getVariable(processInstanceId, "recordatorios"));

        Execution modifications = fixture.triggerModifications(processInstanceId);
        assertNotNull(modifications, "Process should have been waiting for 'Solicitud de modificaciones' message event");

        Map<String, Object> newProposal = new HashMap<>();
        newProposal.put("propuesta", "Propuesta Curso Flowable 2026 - v2");
        Task review = fixture.completeTask(processInstanceId, "revisarpropuesta", newProposal);
        assertNotNull(review, "Task 'revisarpropuesta' should have been created");
        assertEquals("jefeadministrativo", review.getAssignee());

        // Nueva propuesta enviada: recordatorios en 0 y otra vez esperando la respuesta
        assertEquals(0, runtimeService.getVariable(processInstanceId, "recordatorios"),
                "Reminders counter should restart with the new proposal");
        assertEquals("Propuesta Curso Flowable 2026 - v2", runtimeService.getVariable(processInstanceId, "propuesta"));

        Execution waitingAnswer = runtimeService.createExecutionQuery()
                .processInstanceId(processInstanceId)
                .messageEventSubscriptionName(CourseProcessFixture.MESSAGE_ACCEPTANCE)
                .singleResult();
        assertNotNull(waitingAnswer, "Process should be waiting again for the company's answer");
    }
}
