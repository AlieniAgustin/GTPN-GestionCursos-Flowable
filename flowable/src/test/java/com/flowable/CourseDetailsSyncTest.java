package com.flowable;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.flowable.app.App;
import com.flowable.app.dto.CourseDetailsUpdateDTO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * Enunciado 3.2: la BD de la institución guarda la información del curso —programa,
 * presupuesto, fechas e informe de resultados—, y cada curso queda vinculado con su
 * instancia de proceso.
 *
 * <p>El proceso produce esos datos en etapas distintas, así que el listener
 * {@code syncCourseDetails} los empuja al BFF a medida que aparecen. Estos tests
 * verifican qué se manda y cuándo.
 */
@SpringBootTest(classes = App.class)
@DirtiesContext
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:course_details_sync;DB_CLOSE_DELAY=-1",
    "spring.datasource.driverClassName=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "flowable.database-schema-update=true",
    "flowable.async-executor-activate=false"
})
class CourseDetailsSyncTest {

    private static final String DETAILS_PATH = "/api/flowable/courses/{processId}/details";

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private CourseProcessFixture fixture;

    @MockBean(name = "backendRestTemplate")
    private RestTemplate backendRestTemplate;

    @Test
    @DisplayName("3.2 Details sync: the proposal pushes program and budget to the institution DB")
    void testProposalSyncsProgramAndBudget() {
        Map<String, Object> proposal = new HashMap<>();
        proposal.put("propuesta", "Propuesta Inicial");
        proposal.put("programa", "Modulo 1: BPMN. Modulo 2: Flowable.");
        proposal.put("presupuesto", new BigDecimal("150000.50"));

        ProcessInstance instance = fixture.advanceToWaitingForProposalResponse(proposal);

        CourseDetailsUpdateDTO sent = lastDetailsSentFor(instance.getId());
        assertEquals("Modulo 1: BPMN. Modulo 2: Flowable.", sent.program());
        assertEquals(new BigDecimal("150000.50"), sent.budget());
        assertNull(sent.report(), "The report does not exist yet at proposal time");
    }

    @Test
    @DisplayName("3.2 Details sync: the accepted date reaches the institution DB as a plain date")
    void testAcceptanceSyncsAgreedDate() {
        ProcessInstance instance = fixture.advanceToWaitingForProposalResponse(
                Map.of("propuesta", "Propuesta Inicial"));
        fixture.triggerAcceptance(instance.getId(), Map.of("fechaAcordada", "2026-03-01"));

        CourseDetailsUpdateDTO sent = lastDetailsSentFor(instance.getId());
        assertEquals("2026-03-01", sent.agreedDate(),
                "ResolveCourseDateListener normalises to ISO offset; only the date goes to the DB");
    }

    @Test
    @DisplayName("3.2 Details sync: a budget that is not a number is skipped instead of breaking the instance")
    void testUnparseableBudgetIsSkipped() {
        Map<String, Object> proposal = new HashMap<>();
        proposal.put("propuesta", "Propuesta Inicial");
        proposal.put("programa", "Programa valido");
        proposal.put("presupuesto", "a convenir");

        ProcessInstance instance = fixture.advanceToWaitingForProposalResponse(proposal);

        CourseDetailsUpdateDTO sent = lastDetailsSentFor(instance.getId());
        assertNull(sent.budget(), "An unparseable budget is dropped");
        assertEquals("Programa valido", sent.program(), "The rest of the details still travel");
        assertNotNull(runtimeService.createProcessInstanceQuery()
                .processInstanceId(instance.getId()).singleResult(), "The instance keeps running");
    }

    @Test
    @DisplayName("3.2 Details sync: if the BFF is down the instance carries on")
    void testBackendFailureDoesNotBlockTheInstance() {
        doThrow(new RestClientException("backend caido"))
                .when(backendRestTemplate)
                .exchange(eq(DETAILS_PATH), eq(HttpMethod.PUT), any(), eq(Void.class), any(Object[].class));

        ProcessInstance instance = fixture.advanceToWaitingForProposalResponse(
                Map.of("propuesta", "Propuesta Inicial", "programa", "Programa"));

        assertNotNull(runtimeService.createProcessInstanceQuery()
                .processInstanceId(instance.getId()).singleResult(),
                "The institution DB is a parallel record, not a condition to advance");
        assertTrue(runtimeService.createExecutionQuery()
                .processInstanceId(instance.getId())
                .messageEventSubscriptionName(CourseProcessFixture.MESSAGE_ACCEPTANCE)
                .count() > 0, "The instance reached the event gateway as usual");
    }

    /**
     * Devuelve el último payload que el listener mandó al BFF para esa instancia.
     */
    private CourseDetailsUpdateDTO lastDetailsSentFor(String processInstanceId) {
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<HttpEntity> payload = ArgumentCaptor.forClass(HttpEntity.class);
        verify(backendRestTemplate, atLeastOnce()).exchange(
                eq(DETAILS_PATH),
                eq(HttpMethod.PUT),
                payload.capture(),
                eq(Void.class),
                eq(processInstanceId));

        List<HttpEntity> sent = payload.getAllValues();
        return (CourseDetailsUpdateDTO) sent.get(sent.size() - 1).getBody();
    }
}
