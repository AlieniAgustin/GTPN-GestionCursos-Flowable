package com.flowable;

import java.util.HashMap;
import java.util.Map;

import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.flowable.app.App;
import com.flowable.app.dto.CompanyAccessResponseDTO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Enunciado 4.1.2: "Habilitar acceso de la empresa" corre después de la aceptación y
 * antes de la preparación. Crea la cuenta de la empresa si todavía no tiene una, o
 * reutiliza la que quedó de una contratación anterior, y el curso nuevo queda vinculado
 * a esa cuenta.
 */
@SpringBootTest(classes = App.class)
@DirtiesContext
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:company_access;DB_CLOSE_DELAY=-1",
    "spring.datasource.driverClassName=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "flowable.database-schema-update=true",
    "flowable.async-executor-activate=false"
})
class CompanyAccessTest {

    private static final String ACCESS_PATH = "/api/flowable/courses/{processId}/company-access";

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private CourseProcessFixture fixture;

    @MockBean(name = "backendRestTemplate")
    private RestTemplate backendRestTemplate;

    @Test
    @DisplayName("4.1.2 Company access: a new company gets an account and its credentials")
    void testAccountIsCreatedForANewCompany() {
        stubAccess(new CompanyAccessResponseDTO(7L, "ACME S.A.", "acme-sa", "Abc123xyz789", true));

        String processInstanceId = acceptProposal();

        verify(backendRestTemplate).postForEntity(
                eq(ACCESS_PATH), isNull(), eq(CompanyAccessResponseDTO.class), eq(processInstanceId));

        assertEquals(true, runtimeService.getVariable(processInstanceId, "empresaAccesoHabilitado"));
        assertEquals(true, runtimeService.getVariable(processInstanceId, "empresaAccesoCreado"));
        assertEquals("acme-sa", runtimeService.getVariable(processInstanceId, "empresaUsuario"));
        assertEquals(7L, runtimeService.getVariable(processInstanceId, "empresaId"));
        // Comunicación simulada de las credenciales (enunciado 4.1.2)
        assertEquals("Abc123xyz789", runtimeService.getVariable(processInstanceId, "empresaPasswordTemporal"));

        assertInPreparation(processInstanceId);
    }

    @Test
    @DisplayName("4.1.2 Company access: a returning company reuses its account, no new password")
    void testAccountIsReusedForAReturningCompany() {
        stubAccess(new CompanyAccessResponseDTO(7L, "ACME S.A.", "acme-sa", null, false));

        String processInstanceId = acceptProposal();

        assertEquals(true, runtimeService.getVariable(processInstanceId, "empresaAccesoHabilitado"));
        assertEquals(false, runtimeService.getVariable(processInstanceId, "empresaAccesoCreado"));
        assertEquals("acme-sa", runtimeService.getVariable(processInstanceId, "empresaUsuario"));
        assertNull(runtimeService.getVariable(processInstanceId, "empresaPasswordTemporal"),
                "A reused account should not hand out a new password");

        assertInPreparation(processInstanceId);
    }

    @Test
    @DisplayName("4.1.2 Company access: if the backend is down the course still goes into preparation")
    void testPreparationStartsEvenIfTheBackendIsDown() {
        when(backendRestTemplate.postForEntity(
                eq(ACCESS_PATH), isNull(), eq(CompanyAccessResponseDTO.class), anyString()))
                .thenThrow(new RestClientException("backend unreachable"));

        String processInstanceId = acceptProposal();

        assertEquals(false, runtimeService.getVariable(processInstanceId, "empresaAccesoHabilitado"),
                "The failure should be recorded in the instance");
        assertNull(runtimeService.getVariable(processInstanceId, "empresaUsuario"));

        assertInPreparation(processInstanceId);
    }

    private void stubAccess(CompanyAccessResponseDTO access) {
        when(backendRestTemplate.postForEntity(
                eq(ACCESS_PATH), isNull(), eq(CompanyAccessResponseDTO.class), anyString()))
                .thenReturn(ResponseEntity.ok(access));
    }

    private String acceptProposal() {
        Map<String, Object> initialVars = new HashMap<>();
        initialVars.put("viable", true);
        Map<String, Object> proposalVars = new HashMap<>();
        proposalVars.put("propuesta", "Propuesta Curso Flowable");

        ProcessInstance instance = fixture.advanceToWaitingForProposalResponse(initialVars, proposalVars);
        assertNotNull(fixture.triggerAcceptance(instance.getId()),
                "Process should have been waiting for the 'Aceptación' message event");
        return instance.getId();
    }

    private void assertInPreparation(String processInstanceId) {
        assertEquals(true, runtimeService.getVariable(processInstanceId, "cursoActivo"),
                "After enabling the access the course should be in preparation");
        assertNotNull(taskService.createTaskQuery()
                        .processInstanceId(processInstanceId)
                        .taskDefinitionKey("evaluarrecursos")
                        .singleResult(),
                "Preparation should have created the 'Evaluar Recursos' task");
    }
}
