package com.materiales.app.analysis;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.impl.runtime.Clock;
import org.flowable.engine.IdentityService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.Execution;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.User;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.materiales.app.engine.EngineConfig;
import com.materiales.app.engine.EngineFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Las tres consultas del enunciado 4.3 contra el historial de un motor embebido.
 *
 * <p>El reloj del motor se mueve a mano, así las duraciones de las ramas son exactas y no
 * dependen de cuánto tarde la máquina en correr el test.
 *
 * <p>Se montan tres instancias, una por cada caso que hay que distinguir: una preparación
 * completada normalmente, una interrumpida por cancelación (que el enunciado manda excluir)
 * y una solicitud que nunca entró en preparación.
 */
class ExecutionAnalysisServiceTest {

    private static final Instant T0 = Instant.parse("2026-03-02T09:00:00Z");

    private static ProcessEngine engine;
    private static Clock clock;
    private static RuntimeService runtimeService;
    private static TaskService taskService;
    private static ExecutionAnalysisService analysis;

    @BeforeAll
    static void runScenarios() {
        engine = EngineFactory.create(EngineConfig.inMemory("materiales_analisis"));
        clock = engine.getProcessEngineConfiguration().getClock();
        runtimeService = engine.getRuntimeService();
        taskService = engine.getTaskService();
        analysis = new ExecutionAnalysisService(
                engine.getHistoryService(), engine.getIdentityService());

        engine.getRepositoryService().createDeployment()
                .addClasspathResource("analisis-test.bpmn20.xml")
                .deploy();

        seedUsers(engine.getIdentityService());

        normalPreparation();
        cancelledPreparation();
        requestWithoutPreparation();

        clock.reset();
    }

    @AfterAll
    static void stopEngine() {
        engine.close();
    }

    /**
     * Instancia 1: preparación completa. Materiales a los 5 minutos de abrirse las ramas,
     * inscripciones a los 10 y recursos a los 20 (solo aula física).
     */
    private static void normalPreparation() {
        at(0);
        String instance = runtimeService.startProcessInstanceByKey("Process_Curso", Map.of(
                "viable", true,
                "resultadoCurso", "FINALIZADO")).getId();

        at(5);
        completeAs(instance, "prepararmateriales", "administrativo1", Map.of());

        at(8);
        completeAs(instance, "evaluarrecursos", "administrativo1", Map.of(
                "requiereAula", true,
                "requierePlataforma", false,
                "requiereEquipamiento", false));

        at(10);
        Execution waiting = runtimeService.createExecutionQuery()
                .processInstanceId(instance)
                .messageEventSubscriptionName("Nómina de participantes")
                .singleResult();
        assertNotNull(waiting, "La rama de inscripciones debería estar esperando la nómina");
        runtimeService.messageEventReceived("Nómina de participantes", waiting.getId());
        completeAs(instance, "confeccionarlistadefinitiva", "jefeadministrativo", Map.of());

        at(20);
        completeAs(instance, "reservaraula", "administrativo1", Map.of());

        assertTrue(runtimeService.createProcessInstanceQuery().processInstanceId(instance).count() == 0,
                "La instancia debería haber terminado");
    }

    /**
     * Instancia 2: cancelada durante la preparación. Deja una tarea completada y otra
     * pendiente, que el motor borra al interrumpir: esa no se cuenta.
     */
    private static void cancelledPreparation() {
        at(60);
        String instance = runtimeService.startProcessInstanceByKey("Process_Curso", Map.of(
                "viable", true,
                "resultadoCurso", "CANCELADO")).getId();

        at(65);
        completeAs(instance, "evaluarrecursos", "administrativo1", Map.of(
                "requiereAula", false,
                "requierePlataforma", false,
                "requiereEquipamiento", true));

        // Reclamada y sin completar: queda pendiente hasta la cancelación
        Task pending = findTask(instance, "prepararmateriales");
        taskService.claim(pending.getId(), "jefeadministrativo");

        at(70);
        Execution waiting = runtimeService.createExecutionQuery()
                .processInstanceId(instance)
                .messageEventSubscriptionName("Cancelación")
                .singleResult();
        assertNotNull(waiting, "El subproceso de cancelación debería estar escuchando");
        runtimeService.messageEventReceived("Cancelación", waiting.getId());
    }

    /** Instancia 3: no viable, nunca entra en preparación. */
    private static void requestWithoutPreparation() {
        at(120);
        runtimeService.startProcessInstanceByKey("Process_Curso", Map.of(
                "viable", false,
                "resultadoCurso", "RECHAZADO"));
    }

    @Test
    @DisplayName("4.3.1 Results: counts and percentages over the finished instances")
    void testResultsReport() {
        ResultsReport report = analysis.analyzeResults();

        assertEquals(3, report.finishedInstances());
        assertEquals(5, report.tallies().size(), "Las cinco filas del enunciado, incluso en cero");

        assertEquals(List.of("FINALIZADO", "CANCELADO", "RECHAZADO", "NO_VIABLE", "SIN_RESPUESTA"),
                report.tallies().stream().map(ResultsReport.Tally::result).toList(),
                "En el orden del enunciado");

        assertEquals(1, tally(report, "FINALIZADO").count());
        assertEquals(1, tally(report, "CANCELADO").count());
        assertEquals(1, tally(report, "RECHAZADO").count());
        assertEquals(0, tally(report, "NO_VIABLE").count());
        assertEquals(0, tally(report, "SIN_RESPUESTA").count());

        assertEquals(33.3, tally(report, "FINALIZADO").percentage(), 0.1);
        assertEquals(0.0, tally(report, "NO_VIABLE").percentage(), 0.001);
    }

    @Test
    @DisplayName("4.3.2 Preparation: average per branch, cancelled ones excluded")
    void testPreparationReport() {
        PreparationReport report = analysis.analyzePreparation();

        assertEquals(1, report.measuredPreparations(), "Solo la preparación que terminó normalmente");
        assertEquals(1, report.cancelledPreparations(), "La cancelada se excluye");
        assertEquals(0, report.preparationsInProgress());
        assertEquals(1, report.instancesWithoutPreparation());

        assertEquals(Duration.ofMinutes(10), branch(report, "Inscripciones").average());
        assertEquals(Duration.ofMinutes(5), branch(report, "Materiales").average());
        assertEquals(Duration.ofMinutes(20), branch(report, "Recursos").average(),
                "La rama de recursos termina con la última acción seleccionada");

        report.branches().forEach(branch -> assertEquals(1, branch.samples(), branch.label()));
    }

    @Test
    @DisplayName("4.3.3 Task distribution: completed per user, interrupted ones excluded")
    void testTaskDistributionReport() {
        TaskDistributionReport report = analysis.analyzeTaskDistribution();

        // administrativo1: materiales, evaluar recursos y reservar aula de la instancia 1,
        // más evaluar recursos de la cancelada. jefeadministrativo: solo la lista definitiva
        // (la que tenía reclamada quedó interrumpida).
        assertEquals(4, count(report, "administrativo1"));
        assertEquals(1, count(report, "jefeadministrativo"));
        assertEquals(5, report.completedTasks());

        assertEquals(0, count(report, "docente1"), "Los docentes aparecen aunque sea en cero");
        assertEquals(0, count(report, "jefedocente"));

        assertEquals(List.of("Administración", "Administración", "Docentes", "Docentes", "Docentes"),
                report.tallies().stream().map(TaskDistributionReport.Tally::group).toList(),
                "Primero Administración, después Docentes, sin nadie de más");
    }

    @Test
    @DisplayName("4.3 Report: the three tables render with the real history")
    void testFormattedReport() {
        String report = AnalysisFormatter.format(
                analysis.analyzeResults(),
                analysis.analyzePreparation(),
                analysis.analyzeTaskDistribution());

        // Se imprime a propósito: es la salida que pide entregar el enunciado
        System.out.println(report);

        assertTrue(report.contains("Curso finalizado"), report);
        assertTrue(report.contains("Inscripciones"), report);
        assertTrue(report.contains("10 min 0 s"), report);
        assertTrue(report.contains("administrativo1"), report);
    }

    // ------------------------------------------------------------------ helpers

    private static void seedUsers(IdentityService identityService) {
        createGroup(identityService, "administracion", "Administracion");
        createGroup(identityService, "docentes", "Docentes");
        createUser(identityService, "jefeadministrativo", "administracion");
        createUser(identityService, "administrativo1", "administracion");
        createUser(identityService, "jefedocente", "docentes");
        createUser(identityService, "docente1", "docentes");
        createUser(identityService, "docente2", "docentes");
    }

    private static void createGroup(IdentityService identityService, String groupId, String name) {
        Group group = identityService.newGroup(groupId);
        group.setName(name);
        identityService.saveGroup(group);
    }

    private static void createUser(IdentityService identityService, String userId, String groupId) {
        User user = identityService.newUser(userId);
        user.setPassword("password");
        identityService.saveUser(user);
        identityService.createMembership(userId, groupId);
    }

    /** Moves the engine clock to T0 + the given minutes. */
    private static void at(int minutes) {
        clock.setCurrentTime(Date.from(T0.plus(Duration.ofMinutes(minutes))));
    }

    private static void completeAs(String instanceId, String taskKey, String assignee,
                                   Map<String, Object> variables) {
        Task task = findTask(instanceId, taskKey);
        taskService.claim(task.getId(), assignee);
        taskService.complete(task.getId(), variables);
    }

    private static Task findTask(String instanceId, String taskKey) {
        Task task = taskService.createTaskQuery()
                .processInstanceId(instanceId)
                .taskDefinitionKey(taskKey)
                .singleResult();
        assertNotNull(task, "Debería existir la tarea '" + taskKey + "'");
        return task;
    }

    private ResultsReport.Tally tally(ResultsReport report, String result) {
        return report.tallies().stream()
                .filter(tally -> result.equals(tally.result()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Falta la fila " + result));
    }

    private PreparationReport.Branch branch(PreparationReport report, String label) {
        return report.branches().stream()
                .filter(branch -> branch.label().equals(label))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Falta la rama " + label));
    }

    private long count(TaskDistributionReport report, String userId) {
        return report.tallies().stream()
                .filter(tally -> tally.userId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Falta el usuario " + userId))
                .completedTasks();
    }
}
