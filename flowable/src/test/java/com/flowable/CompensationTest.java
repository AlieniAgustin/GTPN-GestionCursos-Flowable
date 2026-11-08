package com.flowable;

import java.util.HashMap;
import java.util.Map;

import org.flowable.engine.ManagementService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.job.api.Job;
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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

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
    private ManagementService managementService;

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
        variables = new HashMap<>();
        taskService.complete(hireEquipament.getId(), variables);

        Execution execution = runtimeService.createExecutionQuery()
                                            .processInstanceId(processInstanceId)
                                            .messageEventSubscriptionName("Cancelación")
                                            .singleResult();
        assertNotNull(execution, "token in cancelation should have been created");
        runtimeService.messageEventReceived("Cancelación", execution.getId());

        System.out.println("DEBUG tasks after cancel: " + taskService.createTaskQuery().processInstanceId(processInstanceId).list());
        System.out.println("DEBUG executions after cancel: " + runtimeService.createExecutionQuery().processInstanceId(processInstanceId).list());
        System.out.println("DEBUG vars after cancel: " + runtimeService.getVariables(processInstanceId));

        Task freeEquipment = taskService.createTaskQuery().processInstanceId(processInstanceId)
                                        .taskDefinitionKey("liberarequipamiento")
                                        .singleResult();

        assertNotNull(freeEquipment, "Task 'liberarequipamiento' should have been created");

        taskService.complete(freeEquipment.getId());

        Job timer = managementService.createTimerJobQuery().processInstanceId(processInstanceId).singleResult();
        assertNotNull(timer, "Should be waiting on timer 'esperarliberacion'");
        managementService.executeJob(managementService.moveTimerToExecutableJob(timer.getId()).getId());
        
        assertNull(runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult());
    }
}
