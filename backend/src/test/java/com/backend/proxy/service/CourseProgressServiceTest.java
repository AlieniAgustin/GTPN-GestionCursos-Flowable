package com.backend.proxy.service;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.backend.proxy.client.FlowableClient;
import com.backend.proxy.dto.course.ActivityDTO;
import com.backend.proxy.dto.course.ActivityListDTO;
import com.backend.proxy.types.CourseStage;
import com.backend.proxy.types.CourseStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class CourseProgressServiceTest {

    private FlowableClient flowableClient;
    private CourseProgressService progressService;

    @BeforeEach
    void setUp() {
        flowableClient = Mockito.mock(FlowableClient.class, Mockito.RETURNS_DEEP_STUBS);
        progressService = new CourseProgressService(flowableClient);
    }

    @Test
    @DisplayName("4.1.2 Estado: un curso cerrado toma el resultado que quedó en la base")
    void testClosedCourseUsesStoredStatus() {
        assertEquals(CourseStage.FINALIZADO,
                progressService.resolveStage(CourseStatus.FINALIZADO, List.of()));
        assertEquals(CourseStage.CANCELADO,
                progressService.resolveStage(CourseStatus.CANCELADO, List.of()));
        assertTrue(CourseStage.RECHAZADO.isClosed());
    }

    @Test
    @DisplayName("4.1.2 Estado: el subproceso abierto significa que está en preparación")
    void testPreparationStage() {
        assertEquals(CourseStage.EN_PREPARACION, progressService.resolveStage(null, List.of(
                activity("prepararcurso", "subProcess"),
                activity("prepararmateriales", "userTask"),
                activity("evaluarrecursos", "userTask"))));
    }

    @Test
    @DisplayName("4.1.2 Estado: esperando el temporizador es confirmado y pendiente de inicio")
    void testWaitingForTheStartDate() {
        assertEquals(CourseStage.CONFIRMADO_PENDIENTE_INICIO,
                progressService.resolveStage(null, List.of(
                        activity("fechaprogramada", "intermediateCatchEvent"))));
    }

    @Test
    @DisplayName("4.1.2 Estado: cada etapa posterior le gana a la anterior")
    void testLaterStagesWin() {
        assertEquals(CourseStage.EN_DESARROLLO, progressService.resolveStage(null, List.of(
                activity("desarrollarcurso", "userTask"))));

        assertEquals(CourseStage.EN_ELABORACION_INFORME, progressService.resolveStage(null, List.of(
                activity("elaborarinformederesultados", "userTask"))));

        assertEquals(CourseStage.EN_ELABORACION_INFORME, progressService.resolveStage(null, List.of(
                activity("enviarinformeaempresayarchivarexpediente", "userTask"))));
    }

    @Test
    @DisplayName("4.1.2 Estado: esperando la respuesta de la empresa es propuesta enviada")
    void testWaitingForCompanyAnswer() {
        assertEquals(CourseStage.PROPUESTA_ENVIADA, progressService.resolveStage(null, List.of(
                activity("aceptacion", "intermediateCatchEvent"),
                activity("rechazo", "intermediateCatchEvent"),
                activity("plazo5dias", "intermediateCatchEvent"))));
    }

    @Test
    @DisplayName("4.1.2 Estado: recién iniciado, sin nada reconocible, es en evaluación")
    void testFreshRequestIsUnderReview() {
        assertEquals(CourseStage.EN_EVALUACION, progressService.resolveStage(null, List.of(
                activity("registraryelaborarpropuesta", "userTask"))));
        assertEquals(CourseStage.EN_EVALUACION, progressService.resolveStage(null, List.of()));
    }

    @Test
    @DisplayName("4.1.2 Actividades: se muestran las paralelas y se ocultan subproceso y compuertas")
    void testVisibleActivitiesKeepParallelBranches() {
        List<ActivityDTO> visibles = progressService.visibleActivities(List.of(
                activity("prepararcurso", "subProcess"),
                activity("gateway1", "parallelGateway"),
                activity("prepararmateriales", "userTask"),
                activity("evaluarrecursos", "userTask"),
                activity("recibirnomina", "intermediateCatchEvent")));

        assertEquals(3, visibles.size(), "Las tres ramas paralelas quedan visibles");
        assertEquals(
                List.of("evaluarrecursos", "prepararmateriales", "recibirnomina"),
                visibles.stream().map(ActivityDTO::activityId).toList(),
                "Ordenadas por nombre para que la lista sea estable");
    }

    @Test
    @DisplayName("4.1.2 Actividades: una sola consulta al motor sirve para todos los cursos")
    void testActivitiesAreFetchedOnceAndGrouped() {
        when(flowableClient.get(anyString()).retrieve().body(ActivityListDTO.class))
                .thenReturn(new ActivityListDTO(List.of(
                        new ActivityDTO("proc-1", "prepararcurso", "Preparar curso", "subProcess"),
                        new ActivityDTO("proc-1", "prepararmateriales", "Preparar materiales", "userTask"),
                        new ActivityDTO("proc-2", "desarrollarcurso", "Desarrollar curso", "userTask")), 3));

        Map<String, List<ActivityDTO>> porInstancia = progressService.findActiveActivities();

        assertEquals(2, porInstancia.size());
        assertEquals(2, porInstancia.get("proc-1").size());
        assertEquals(1, porInstancia.get("proc-2").size());
    }

    @Test
    @DisplayName("4.1.2 Actividades: si el motor no responde, el sitio sigue mostrando la base")
    void testEngineFailureDegradesGracefully() {
        when(flowableClient.get(anyString()).retrieve().body(ActivityListDTO.class))
                .thenThrow(new RestClientException("motor caido"));

        assertTrue(progressService.findActiveActivities().isEmpty());
        assertEquals(CourseStage.EN_EVALUACION, progressService.resolveStage(null, List.of()));
    }

    private ActivityDTO activity(String id, String type) {
        return new ActivityDTO("proc-1", id, id, type);
    }
}
