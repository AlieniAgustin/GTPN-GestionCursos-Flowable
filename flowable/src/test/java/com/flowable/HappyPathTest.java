package com.flowable;

import java.util.HashMap;
import java.util.Map;

import org.flowable.engine.HistoryService;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest(classes = App.class)
@DirtiesContext
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:happy_path;DB_CLOSE_DELAY=-1",
    "spring.datasource.driverClassName=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "flowable.database-schema-update=true",
    "flowable.async-executor-activate=false"
})
class HappyPathTest {

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
    @DisplayName("1. Happy path: proposal accepted, course delivered and resources released")
    void testHappyPath() {
        // fechaCurso en el pasado: el timer 'Fecha programada' queda listo para dispararse
        Map<String, Object> initialVars = new HashMap<>();
        initialVars.put("viable", true);
        initialVars.put("fechaCurso", "2026-01-01T10:00:00");
        Map<String, Object> proposalVars = new HashMap<>();
        proposalVars.put("propuesta", "Propuesta Curso Flowable 2026");

        ProcessInstance processInstance = fixture.advanceToWaitingForProposalResponse(initialVars, proposalVars);
        assertNotNull(processInstance, "Process instance should have reached the response gateway");
        String processInstanceId = processInstance.getId();

        Execution acceptance = fixture.triggerAcceptance(processInstanceId);
        assertNotNull(acceptance, "Process should have been waiting for 'Aceptación' message event");
        assertEquals(true, runtimeService.getVariable(processInstanceId, "cursoActivo"));

        // Preparación: solo aula física
        Map<String, Object> resources = new HashMap<>();
        resources.put("requiereAula", true);
        resources.put("requierePlataforma", false);
        resources.put("requiereEquipamiento", false);
        assertNotNull(fixture.completeTask(processInstanceId, "evaluarrecursos", resources),
                "Task 'evaluarrecursos' should have been created");
        assertEquals("AULA-1", runtimeService.getVariable(processInstanceId, "aulaAsignada"));

        assertNotNull(fixture.completeTask(processInstanceId, "prepararmateriales", new HashMap<>()),
                "Task 'prepararmateriales' should have been created");

        assertNotNull(fixture.triggerParticipantList(processInstanceId),
                "Process should have been waiting for 'Nómina de participantes' message event");

        assertNotNull(fixture.completeTask(processInstanceId, "confeccionarlistadefinitiva", new HashMap<>()),
                "Task 'confeccionarlistadefinitiva' should have been created");

        // Preparación completa: confirmación enviada y el jefe docente define el docente
        Map<String, Object> teacher = new HashMap<>();
        teacher.put("docenteSeleccionado", "docente1");
        Task defineTeacher = fixture.completeTask(processInstanceId, "definirdocentequedesarrollaelcurso", teacher);
        assertNotNull(defineTeacher, "Task 'definirdocentequedesarrollaelcurso' should have been created");
        assertEquals("jefedocente", defineTeacher.getAssignee());

        // Llega la fecha programada
        Job scheduledDate = fixture.fireTimer(processInstanceId);
        assertNotNull(scheduledDate, "Process should have been waiting on timer 'fechaprogramada'");

        Task deliverCourse = fixture.completeTask(processInstanceId, "desarrollarcurso", new HashMap<>());
        assertNotNull(deliverCourse, "Task 'desarrollarcurso' should have been created");
        assertEquals("docente1", deliverCourse.getAssignee());

        Task report = fixture.completeTask(processInstanceId, "elaborarinformederesultados", new HashMap<>());
        assertNotNull(report, "Task 'elaborarinformederesultados' should have been created");
        assertEquals("jefedocente", report.getAssignee());

        Task sendReport = fixture.completeTask(processInstanceId, "enviarinformeaempresayarchivarexpediente", new HashMap<>());
        assertNotNull(sendReport, "Task 'enviarinformeaempresayarchivarexpediente' should have been created");
        assertEquals("jefeadministrativo", sendReport.getAssignee());

        // Al finalizar se compensa la preparación: se libera el aula y termina FINALIZADO
        ProcessInstance activeInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        assertNull(activeInstance, "Process instance should have ended");

        assertEquals("FINALIZADO", historicValue(processInstanceId, "resultadoCurso"));
        assertEquals(true, historicValue(processInstanceId, "aulaLiberada"), "Classroom should have been released");
    }

    private Object historicValue(String processInstanceId, String name) {
        return historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId)
                .variableName(name)
                .singleResult()
                .getValue();
    }
}
