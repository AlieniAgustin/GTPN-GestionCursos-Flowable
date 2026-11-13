package com.materiales.app.service;

import java.util.Map;

import org.flowable.engine.IdentityService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RuntimeService;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.User;

import com.materiales.app.engine.EngineConfig;
import com.materiales.app.engine.EngineFactory;

/**
 * Motor embebido sobre H2 para los tests: los mismos grupos y usuarios que siembra la app
 * del motor, y un proceso con la tarea <i>Preparar materiales</i>.
 */
final class MaterialsAppTestEngine {

    static final String PROCESS_KEY = "TestMateriales";
    static final String PASSWORD = "password";

    private MaterialsAppTestEngine() {
    }

    static ProcessEngine start(String databaseName) {
        ProcessEngine engine = EngineFactory.create(EngineConfig.inMemory(databaseName));

        engine.getRepositoryService()
                .createDeployment()
                .addClasspathResource("materiales-test.bpmn20.xml")
                .deploy();

        IdentityService identityService = engine.getIdentityService();
        createGroup(identityService, "administracion", "Administracion");
        createGroup(identityService, "docentes", "Docentes");
        createUser(identityService, "jefeadministrativo", "administracion");
        createUser(identityService, "administrativo1", "administracion");
        createUser(identityService, "docente1", "docentes");

        return engine;
    }

    /** Starts an instance carrying the variables the BFF sets when a course is requested. */
    static String startCourse(RuntimeService runtimeService, String courseName, String companyName) {
        return runtimeService.startProcessInstanceByKey(PROCESS_KEY,
                Map.of("courseName", courseName, "companyName", companyName)).getId();
    }

    private static void createGroup(IdentityService identityService, String groupId, String name) {
        Group group = identityService.newGroup(groupId);
        group.setName(name);
        identityService.saveGroup(group);
    }

    private static void createUser(IdentityService identityService, String userId, String groupId) {
        User user = identityService.newUser(userId);
        user.setPassword(PASSWORD);
        identityService.saveUser(user);
        identityService.createMembership(userId, groupId);
    }
}
