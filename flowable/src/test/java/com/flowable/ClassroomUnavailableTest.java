package com.flowable;

import java.util.HashMap;
import java.util.Map;

import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.Execution;
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
    "spring.datasource.url=jdbc:h2:mem:classroom_unavailable;DB_CLOSE_DELAY=-1",
    "spring.datasource.driverClassName=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "flowable.database-schema-update=true",
    "flowable.async-executor-activate=false",
    // Una sola aula: el segundo curso se queda sin aula
    "aulas.disponibles=1"
})
class ClassroomUnavailableTest {

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private CourseProcessFixture fixture;

    @MockBean(name = "backendRestTemplate")
    private RestTemplate backendRestTemplate;

    @Test
    @DisplayName("7. No classrooms available: the error is caught and the reservation is retried")
    void testNoClassroomAvailable() {
        // El primer curso se queda con la única aula
        String firstCourse = fixture.startCourseInPreparation().getId();
        requestClassroom(firstCourse);
        assertEquals("AULA-1", runtimeService.getVariable(firstCourse, "aulaAsignada"));

        // El segundo curso no consigue aula: el error 'errSinAulas' lo atrapa el boundary 'sinaulas'
        String secondCourse = fixture.startCourseInPreparation().getId();
        requestClassroom(secondCourse);
        assertNull(runtimeService.getVariable(secondCourse, "aulaAsignada"), "No classroom should have been assigned");

        Task noClassroom = findTask(secondCourse, "gestionarfaltadeaulas");
        assertNotNull(noClassroom, "Task 'gestionarfaltadeaulas' should have been created");
        assertEquals(1, taskService.createTaskQuery().taskId(noClassroom.getId())
                .taskCandidateGroup("administracion").count(), "Group 'administracion' should be candidate");

        // Reintentar sin aulas libres vuelve a la misma tarea
        taskService.complete(noClassroom.getId());
        assertNotNull(findTask(secondCourse, "gestionarfaltadeaulas"), "Retry without classrooms should fail again");

        // Se cancela el primer curso: la compensación libera su aula
        cancel(firstCourse);
        assertNotNull(fixture.completeTask(firstCourse, "confirmarliberacionrecursos", new HashMap<>()),
                "Task 'confirmarliberacionrecursos' should have been created");
        assertNull(runtimeService.createProcessInstanceQuery().processInstanceId(firstCourse).singleResult(),
                "First course should have ended");

        // Ahora el reintento consigue el aula liberada
        assertNotNull(fixture.completeTask(secondCourse, "gestionarfaltadeaulas", new HashMap<>()),
                "Task 'gestionarfaltadeaulas' should still be pending");
        assertEquals("AULA-1", runtimeService.getVariable(secondCourse, "aulaAsignada"));
        assertNull(findTask(secondCourse, "gestionarfaltadeaulas"), "No more retries should be needed");
    }

    // Completa Evaluar recursos pidiendo solo aula física
    private void requestClassroom(String processInstanceId) {
        Map<String, Object> resources = new HashMap<>();
        resources.put("requiereAula", true);
        resources.put("requierePlataforma", false);
        resources.put("requiereEquipamiento", false);
        assertNotNull(fixture.completeTask(processInstanceId, "evaluarrecursos", resources),
                "Task 'evaluarrecursos' should have been created");
    }

    private void cancel(String processInstanceId) {
        Execution execution = runtimeService.createExecutionQuery()
                .processInstanceId(processInstanceId)
                .messageEventSubscriptionName("Cancelación")
                .singleResult();
        assertNotNull(execution, "Process should be waiting for 'Cancelación' message event");
        runtimeService.messageEventReceived("Cancelación", execution.getId());
    }

    private Task findTask(String processInstanceId, String taskKey) {
        return taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .taskDefinitionKey(taskKey)
                .singleResult();
    }
}
