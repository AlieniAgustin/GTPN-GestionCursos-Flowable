package com.flowable;

import java.io.InputStream;

import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestTemplate;

import com.flowable.app.App;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest(classes = App.class)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:flowable;DB_CLOSE_DELAY=-1",
    "spring.datasource.driverClassName=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "flowable.database-schema-update=true",
    "flowable.async-executor-activate=false"
})
class LimiteCursosSuperadosTest {

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @MockBean(name = "backendRestTemplate")
    private RestTemplate backendRestTemplate;

    private Deployment deployment;

    @BeforeEach
    void setUp() throws Exception {
        try (InputStream bpmnStream = getClass().getClassLoader()
                .getResourceAsStream("processes/Institución_de_capacitación.bpmn20.xml")) {
            deployment = repositoryService.createDeployment()
                    .name("Despliegue Institucion Capacitacion - Test")
                    .addInputStream("Institución_de_capacitación.bpmn20.xml", bpmnStream)
                    .addClasspathResource("dmn/evaluarViabilidad.dmn")
                    .deploy();
        }
    }

    @AfterEach
    void tearDown() {
        if (deployment != null) {
            repositoryService.deleteDeployment(deployment.getId(), true);
        }
    }

    @Test
    @DisplayName("6. límite de cursos: con 5 cursos en preparación el sexto no es viable")
    void testCursoNoViablePropuesta() {
        // El DMN declara viable con menos de 5 cursos activos
        for (int i = 0; i < 5; i++) {
            iniciarCursoEnPreparacion();
        }

        ProcessInstance processInstance =
                runtimeService.startProcessInstanceByMessage("Solicitud de curso");
        assertNotNull(processInstance, "La instancia del proceso debe haberse iniciado");

        ProcessInstance activeInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstance.getId())
                .singleResult();
        assertNull(activeInstance, "La instancia del proceso debe haber finalizado");
    }

    // Lleva una solicitud hasta prepararcurso, que es donde pasa a contar como activa
    private void iniciarCursoEnPreparacion() {
        ProcessInstance processInstance =
                runtimeService.startProcessInstanceByMessage("Solicitud de curso");

        Task propuesta = taskService.createTaskQuery()
                .processInstanceId(processInstance.getId())
                .taskDefinitionKey("registraryelaborarpropuesta")
                .singleResult();
        taskService.complete(propuesta.getId());

        Execution aceptacion = runtimeService.createExecutionQuery()
                .processInstanceId(processInstance.getId())
                .messageEventSubscriptionName("Aceptación")
                .singleResult();
        runtimeService.messageEventReceived("Aceptación", aceptacion.getId());
    }

    
}
