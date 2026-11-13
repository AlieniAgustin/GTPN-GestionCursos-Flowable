package com.flowable;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

import org.flowable.engine.HistoryService;
import org.flowable.engine.ManagementService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.job.api.Job;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Enunciado 3.5: la fecha de inicio del curso se acuerda durante la ejecución, al aceptar
 * la propuesta, y el timer "Fecha programada" la toma de la variable `fechaCurso` sin
 * fecha fija en el modelo. Antes de esto, completar "Definir docente" sin haber inyectado
 * la variable al iniciar la instancia reventaba con
 * `Unknown property used in expression: ${fechaCurso}`.
 */
@SpringBootTest(classes = App.class)
@DirtiesContext
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:course_date;DB_CLOSE_DELAY=-1",
    "spring.datasource.driverClassName=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "flowable.database-schema-update=true",
    "flowable.async-executor-activate=false",
    // Plazos cortos, como se haría para la demo
    "proceso.plazo-respuesta=PT1S",
    "proceso.fecha-curso-offset=PT1S"
})
class CourseDateTest {

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private ManagementService managementService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private CourseProcessFixture fixture;

    @MockBean(name = "backendRestTemplate")
    private RestTemplate backendRestTemplate;

    @Test
    @DisplayName("3.5 Course date: the acceptance brings the agreed date and the timer uses it")
    void testAgreedDateFromAcceptanceDrivesTheTimer() {
        String processInstanceId = startAndAccept(Map.of("fechaCurso", "2026-03-01T09:00"));

        String courseDate = (String) runtimeService.getVariable(processInstanceId, "fechaCurso");
        assertNotNull(courseDate, "The acceptance should have left 'fechaCurso' set");
        assertTrue(courseDate.startsWith("2026-03-01T09:00"),
                "The agreed date should be kept, normalized to ISO-8601: " + courseDate);

        advanceToScheduledDate(processInstanceId);

        Job timer = managementService.createTimerJobQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        assertNotNull(timer, "The instance should be waiting on the 'fechaprogramada' timer");
        assertEquals(
                LocalDateTime.parse("2026-03-01T09:00").atZone(ZoneId.systemDefault()).toInstant(),
                timer.getDuedate().toInstant(),
                "The timer should be due on the agreed date");
    }

    @Test
    @DisplayName("3.5 Course date: 'fechaAcordada' from the communications app is accepted too")
    void testAgreedDateAliasIsAccepted() {
        String processInstanceId = startAndAccept(Map.of("fechaAcordada", "2026-04-15"));

        String courseDate = (String) runtimeService.getVariable(processInstanceId, "fechaCurso");
        assertNotNull(courseDate, "'fechaAcordada' should have been copied into 'fechaCurso'");
        assertTrue(courseDate.startsWith("2026-04-15T00:00"),
                "A date without time should be taken as the start of that day: " + courseDate);
    }

    @Test
    @DisplayName("3.5 Course date: without an agreed date the process still runs to the end")
    void testHappyPathWithoutAnyCourseDate() {
        // Ninguna variable de fecha, ni al iniciar ni en la aceptación: es lo que pasa
        // cuando la solicitud entra por el sitio web y la empresa acepta sin fecha
        String processInstanceId = startAndAccept(Map.of());

        String courseDate = (String) runtimeService.getVariable(processInstanceId, "fechaCurso");
        assertNotNull(courseDate, "The engine should have derived a course date");

        advanceToScheduledDate(processInstanceId);

        assertNotNull(fixture.fireTimer(processInstanceId),
                "The instance should be waiting on the 'fechaprogramada' timer");
        assertNotNull(fixture.completeTask(processInstanceId, "desarrollarcurso", new HashMap<>()),
                "Task 'desarrollarcurso' should have been created");
        assertNotNull(fixture.completeTask(processInstanceId, "elaborarinformederesultados", new HashMap<>()),
                "Task 'elaborarinformederesultados' should have been created");
        assertNotNull(fixture.completeTask(processInstanceId, "enviarinformeaempresayarchivarexpediente", new HashMap<>()),
                "Task 'enviarinformeaempresayarchivarexpediente' should have been created");

        assertNull(runtimeService.createProcessInstanceQuery()
                        .processInstanceId(processInstanceId)
                        .singleResult(),
                "Process instance should have ended");
        assertEquals("FINALIZADO", historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId)
                .variableName("resultadoCurso")
                .singleResult()
                .getValue());
    }

    /**
     * Starts a request, sends the proposal and accepts it with the given data.
     */
    private String startAndAccept(Map<String, Object> acceptanceVariables) {
        Map<String, Object> initialVars = new HashMap<>();
        initialVars.put("viable", true);
        Map<String, Object> proposalVars = new HashMap<>();
        proposalVars.put("propuesta", "Propuesta Curso Flowable");

        ProcessInstance instance = fixture.advanceToWaitingForProposalResponse(initialVars, proposalVars);
        assertNotNull(fixture.triggerAcceptance(instance.getId(), new HashMap<>(acceptanceVariables)),
                "Process should have been waiting for the 'Aceptación' message event");
        return instance.getId();
    }

    /**
     * Runs the preparation (physical classroom only) and the teacher assignment, leaving
     * the instance waiting on the 'fechaprogramada' timer.
     */
    private void advanceToScheduledDate(String processInstanceId) {
        Map<String, Object> resources = new HashMap<>();
        resources.put("requiereAula", true);
        resources.put("requierePlataforma", false);
        resources.put("requiereEquipamiento", false);
        assertNotNull(fixture.completeTask(processInstanceId, "evaluarrecursos", resources),
                "Task 'evaluarrecursos' should have been created");
        assertNotNull(fixture.completeTask(processInstanceId, "prepararmateriales", new HashMap<>()),
                "Task 'prepararmateriales' should have been created");
        assertNotNull(fixture.triggerParticipantList(processInstanceId),
                "Process should have been waiting for the participants list");
        assertNotNull(fixture.completeTask(processInstanceId, "confeccionarlistadefinitiva", new HashMap<>()),
                "Task 'confeccionarlistadefinitiva' should have been created");

        Map<String, Object> teacher = new HashMap<>();
        teacher.put("docenteSeleccionado", "docente1");
        assertNotNull(fixture.completeTask(processInstanceId, "definirdocentequedesarrollaelcurso", teacher),
                "Task 'definirdocentequedesarrollaelcurso' should have been created");
    }
}
