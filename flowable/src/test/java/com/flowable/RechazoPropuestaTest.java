package com.flowable;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
class RechazoPropuestaTest {

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private HistoryService historyService;

    @MockBean(name = "backendRestTemplate")
    private RestTemplate backendRestTemplate;

    private Deployment deployment;

    @BeforeEach
    void setUp() throws Exception {
        // Leemos directamente Institución_de_capacitación.bpmn20.xml desde la raíz del proyecto
        File bpmnFile = new File("../Institución_de_capacitación.bpmn20.xml");
        if (!bpmnFile.exists()) {
            bpmnFile = new File("Institución_de_capacitación.bpmn20.xml");
        }

        try (InputStream bpmnStream = new FileInputStream(bpmnFile)) {
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
    @DisplayName("3. Rechazo de la propuesta: Solicitud -> Evaluar viabilidad (viable) -> Registrar y elaborar propuesta -> Enviar propuesta -> Rechazo -> Archivar solicitud -> Cerrada")
    void testCaminoRechazoPropuesta() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("viable", true);

        ProcessInstance processInstance = runtimeService.startProcessInstanceByMessage("Solicitud de curso", variables);
        assertNotNull(processInstance, "La instancia del proceso debe haberse iniciado");
        String processInstanceId = processInstance.getId();

        Task tareaPropuesta = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .taskDefinitionKey("registraryelaborarpropuesta")
                .singleResult();

        assertNotNull(tareaPropuesta, "Debe existir la tarea 'Registrar y Elaborar propuesta'");
        assertEquals("jefeadministrativo", tareaPropuesta.getAssignee());

        Map<String, Object> propuestaVars = new HashMap<>();
        propuestaVars.put("propuesta", "Propuesta Curso Flowable 2026");
        taskService.complete(tareaPropuesta.getId(), propuestaVars);

        // token esperando en rechazo, tambien lo hace en el timer y en los demas lugares
        Execution executionRechazo = runtimeService.createExecutionQuery()
                .processInstanceId(processInstanceId)
                .messageEventSubscriptionName("Rechazo")
                .singleResult();

        assertNotNull(executionRechazo, "El proceso debe estar esperando el mensaje de evento 'Rechazo'");

        // se mockea la respuesta y se cancelan los demas execution que estaban esperando
        runtimeService.messageEventReceived("Rechazo", executionRechazo.getId());

        Task tareaArchivar = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .taskDefinitionKey("archivarsolicitud")
                .singleResult();

        assertNotNull(tareaArchivar, "Debe haberse creado la tarea 'Archivar solicitud'");
        assertEquals("jefeadministrativo", tareaArchivar.getAssignee());

        Object resultadoCurso = runtimeService.getVariable(processInstanceId, "resultadoCurso");
        assertEquals("RECHAZADO", resultadoCurso, "El resultado del curso debe ser RECHAZADO");

        // se completa la tarea manual archivar solicitud
        taskService.complete(tareaArchivar.getId());

        // se verifica que finalizo el proceso
        ProcessInstance activeInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        assertNull(activeInstance, "La instancia del proceso debe haber finalizado");

        // se busca que en el historial haya quedado marcado con rechazado
        Object historicResultado = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId)
                .variableName("resultadoCurso")
                .singleResult()
                .getValue();
        assertEquals("RECHAZADO", historicResultado);
    }
}
