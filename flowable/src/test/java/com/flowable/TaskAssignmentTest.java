package com.flowable;

import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.api.FlowableException;
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
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = App.class)
@DirtiesContext
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:task_assignment;DB_CLOSE_DELAY=-1",
    "spring.datasource.driverClassName=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "flowable.database-schema-update=true",
    "flowable.async-executor-activate=false"
})
class TaskAssignmentTest {

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private CourseProcessFixture fixture;

    @MockBean(name = "backendRestTemplate")
    private RestTemplate backendRestTemplate;

    @Test
    @DisplayName("8. Task assignment: chiefs' tasks are assigned, group tasks are claimed, the final list goes to the least busy")
    void testTaskAssignmentAndClaim() {
        ProcessInstance processInstance = fixture.startProcessInstance(Map.of("viable", true));
        String processInstanceId = processInstance.getId();

        // Tarea exclusiva del jefe administrativo: ya viene asignada
        Task proposal = findTask(processInstanceId, "registraryelaborarpropuesta");
        assertNotNull(proposal, "Task 'registraryelaborarpropuesta' should have been created");
        assertEquals("jefeadministrativo", proposal.getAssignee());
        taskService.complete(proposal.getId());

        fixture.triggerAcceptance(processInstanceId);

        // Tareas de grupo: sin asignar, con 'administracion' como candidato
        Task evaluate = findTask(processInstanceId, "evaluarrecursos");
        assertNotNull(evaluate, "Task 'evaluarrecursos' should have been created");
        assertNull(evaluate.getAssignee(), "Group task should start unassigned");
        assertEquals(1, taskService.createTaskQuery().taskId(evaluate.getId())
                .taskCandidateGroup("administracion").count(), "Group 'administracion' should be candidate");
        assertEquals(1, taskService.createTaskQuery().taskId(evaluate.getId())
                .taskCandidateUser("administrativo1").count(), "A member of the group should see the task");

        // Un administrativo la reclama; otro ya no puede
        taskService.claim(evaluate.getId(), "administrativo1");
        assertEquals("administrativo1", findTask(processInstanceId, "evaluarrecursos").getAssignee());
        assertThrows(FlowableException.class, () -> taskService.claim(evaluate.getId(), "jefeadministrativo"),
                "An already claimed task cannot be claimed by someone else");

        Map<String, Object> resources = new HashMap<>();
        resources.put("requiereAula", true);
        resources.put("requierePlataforma", false);
        resources.put("requiereEquipamiento", false);
        taskService.complete(evaluate.getId(), resources);

        // administrativo1 reclama 'Preparar materiales' y la deja pendiente: queda con 1 tarea
        // asignada, mientras que jefeadministrativo queda con 0
        Task materials = findTask(processInstanceId, "prepararmateriales");
        assertNotNull(materials, "Task 'prepararmateriales' should have been created");
        taskService.claim(materials.getId(), "administrativo1");

        // 'Confeccionar lista definitiva' se asigna sola al administrativo con menos tareas
        fixture.triggerParticipantList(processInstanceId);
        Task finalList = findTask(processInstanceId, "confeccionarlistadefinitiva");
        assertNotNull(finalList, "Task 'confeccionarlistadefinitiva' should have been created");
        assertEquals("jefeadministrativo", finalList.getAssignee(),
                "Final list should go to the administrative with fewer assigned tasks");
    }

    private Task findTask(String processInstanceId, String taskKey) {
        return taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .taskDefinitionKey(taskKey)
                .singleResult();
    }
}
