package com.materiales.app.service;

import java.util.List;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.HistoryService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Las tres funciones del enunciado 4.2.1 sobre la API Java: consultar las tareas
 * asignadas o reclamables, reclamar y completar.
 */
class MaterialsTaskServiceTest {

    private static ProcessEngine engine;
    private static RuntimeService runtimeService;
    private static TaskService taskService;
    private static HistoryService historyService;
    private static MaterialsTaskService materialsTaskService;

    private static StaffUser administrativo1;
    private static StaffUser jefeadministrativo;

    @BeforeAll
    static void startEngine() throws Exception {
        engine = MaterialsAppTestEngine.start("materiales_tasks");
        runtimeService = engine.getRuntimeService();
        taskService = engine.getTaskService();
        historyService = engine.getHistoryService();
        materialsTaskService = new MaterialsTaskService(taskService);

        StaffAuthenticator authenticator = new StaffAuthenticator(engine.getIdentityService());
        administrativo1 = authenticator.login("administrativo1", MaterialsAppTestEngine.PASSWORD);
        jefeadministrativo = authenticator.login("jefeadministrativo", MaterialsAppTestEngine.PASSWORD);
    }

    @AfterAll
    static void stopEngine() {
        engine.close();
    }

    @BeforeEach
    void clearRunningInstances() {
        runtimeService.createProcessInstanceQuery().list()
                .forEach(instance -> runtimeService.deleteProcessInstance(instance.getId(), "test"));
    }

    @Test
    @DisplayName("4.2.1 Inbox: an unclaimed task shows up for every member of the group")
    void testUnclaimedTaskIsVisibleToTheWholeGroup() {
        MaterialsAppTestEngine.startCourse(runtimeService, "Flowable Avanzado", "ACME S.A.");

        List<MaterialTask> inbox = materialsTaskService.findInbox(administrativo1);
        assertEquals(1, inbox.size(), "The task should be claimable by the group");

        MaterialTask task = inbox.get(0);
        assertEquals("Preparar materiales", task.name());
        assertEquals("Flowable Avanzado", task.courseName(), "The course should come from the instance variables");
        assertEquals("ACME S.A.", task.companyName());
        assertTrue(task.claimable(), "Nobody claimed it yet");
        assertNotNull(task.createTime());

        assertEquals(1, materialsTaskService.findInbox(jefeadministrativo).size(),
                "The other administrative of the group should see it too");
    }

    @Test
    @DisplayName("4.2.1 Claim: once claimed it is only in the claimer's inbox")
    void testClaimedTaskLeavesTheGroupInbox() {
        MaterialsAppTestEngine.startCourse(runtimeService, "Flowable Avanzado", "ACME S.A.");
        MaterialTask available = materialsTaskService.findInbox(administrativo1).get(0);

        materialsTaskService.claim(available.id(), administrativo1);

        List<MaterialTask> mine = materialsTaskService.findInbox(administrativo1);
        assertEquals(1, mine.size());
        assertTrue(mine.get(0).assignedTo("administrativo1"));
        assertFalse(mine.get(0).claimable());

        assertTrue(materialsTaskService.findInbox(jefeadministrativo).isEmpty(),
                "A claimed task should not show up for the rest of the group");
    }

    @Test
    @DisplayName("4.2.1 Claim: a task already claimed cannot be claimed by someone else")
    void testAlreadyClaimedTaskCannotBeStolen() {
        MaterialsAppTestEngine.startCourse(runtimeService, "Flowable Avanzado", "ACME S.A.");
        MaterialTask available = materialsTaskService.findInbox(administrativo1).get(0);
        materialsTaskService.claim(available.id(), administrativo1);

        assertThrows(FlowableException.class,
                () -> materialsTaskService.claim(available.id(), jefeadministrativo));
    }

    @Test
    @DisplayName("4.2.1 Complete: the claimed task is completed and the detail is recorded")
    void testCompleteClaimedTask() {
        String processInstanceId =
                MaterialsAppTestEngine.startCourse(runtimeService, "Flowable Avanzado", "ACME S.A.");
        MaterialTask available = materialsTaskService.findInbox(administrativo1).get(0);
        materialsTaskService.claim(available.id(), administrativo1);

        materialsTaskService.complete(available.id(), administrativo1, "  Apuntes y VM de laboratorio  ");

        assertTrue(materialsTaskService.findInbox(administrativo1).isEmpty(), "The task should be gone");
        assertEquals(true, historicValue(processInstanceId, MaterialsTaskService.VAR_PREPARED));
        assertEquals("Apuntes y VM de laboratorio",
                historicValue(processInstanceId, MaterialsTaskService.VAR_DETAIL),
                "The detail should be trimmed");
    }

    @Test
    @DisplayName("4.2.1 Complete: without a detail no empty variable is left behind")
    void testCompleteWithoutDetail() {
        String processInstanceId =
                MaterialsAppTestEngine.startCourse(runtimeService, "Flowable Avanzado", "ACME S.A.");
        MaterialTask available = materialsTaskService.findInbox(administrativo1).get(0);
        materialsTaskService.claim(available.id(), administrativo1);

        materialsTaskService.complete(available.id(), administrativo1, "   ");

        assertEquals(true, historicValue(processInstanceId, MaterialsTaskService.VAR_PREPARED));
        assertNull(historicValue(processInstanceId, MaterialsTaskService.VAR_DETAIL));
    }

    @Test
    @DisplayName("4.2.1 Complete: an unclaimed task has to be claimed first")
    void testCompleteRequiresClaiming() {
        MaterialsAppTestEngine.startCourse(runtimeService, "Flowable Avanzado", "ACME S.A.");
        MaterialTask available = materialsTaskService.findInbox(administrativo1).get(0);

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> materialsTaskService.complete(available.id(), administrativo1, null));
        assertTrue(error.getMessage().contains("reclamar"), error.getMessage());
    }

    @Test
    @DisplayName("4.2.1 Complete: someone else's task cannot be completed")
    void testCompleteSomeoneElsesTaskIsRejected() {
        MaterialsAppTestEngine.startCourse(runtimeService, "Flowable Avanzado", "ACME S.A.");
        MaterialTask available = materialsTaskService.findInbox(administrativo1).get(0);
        materialsTaskService.claim(available.id(), administrativo1);

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> materialsTaskService.complete(available.id(), jefeadministrativo, null));
        assertTrue(error.getMessage().contains("administrativo1"), error.getMessage());
    }

    @Test
    @DisplayName("4.2.1 Complete: a task that is already gone reports it instead of failing raw")
    void testCompleteMissingTask() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> materialsTaskService.complete("no-existe", administrativo1, null));
        assertTrue(error.getMessage().contains("ya no existe"), error.getMessage());
    }

    private Object historicValue(String processInstanceId, String variableName) {
        var variable = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId)
                .variableName(variableName)
                .singleResult();
        return variable == null ? null : variable.getValue();
    }
}
