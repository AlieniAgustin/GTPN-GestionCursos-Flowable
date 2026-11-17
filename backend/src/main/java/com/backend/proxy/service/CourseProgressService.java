package com.backend.proxy.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.backend.proxy.client.FlowableClient;
import com.backend.proxy.dto.course.ActivityDTO;
import com.backend.proxy.dto.course.ActivityListDTO;
import com.backend.proxy.types.CourseStage;
import com.backend.proxy.types.CourseStatus;

import lombok.RequiredArgsConstructor;

/**
 * Traduce lo que está pasando en el motor al estado del curso que ve la empresa
 * (enunciado 4.1.2).
 *
 * <p>La base de la institución solo guarda **cómo terminó** el trámite, y recién cuando
 * cerró. Mientras el curso está en marcha, el estado hay que deducirlo de las actividades
 * que el motor tiene abiertas.
 */
@Service
@RequiredArgsConstructor
public class CourseProgressService {

    /**
     * Una sola consulta trae las actividades activas de **todas** las instancias; después
     * se agrupan en memoria. Así listar N cursos no dispara N llamadas al motor.
     */
    private static final String ACTIVE_ACTIVITIES_URI =
            "/process-api/history/historic-activity-instances?finished=false&size=1000";

    // El orden importa: se evalúa de la etapa más avanzada a la más temprana
    private static final Set<String> INFORME = Set.of(
            "elaborarinformederesultados", "enviarinformeaempresayarchivarexpediente");
    private static final Set<String> DESARROLLO = Set.of("desarrollarcurso");
    private static final Set<String> CONFIRMADO = Set.of(
            "fechaprogramada", "definirdocentequedesarrollaelcurso", "enviarconfirmacion");
    private static final Set<String> PREPARACION = Set.of("prepararcurso");
    private static final Set<String> ESPERANDO_RESPUESTA = Set.of(
            "aceptacion", "rechazo", "modificaciones", "plazo5dias", "enviarrecordatorio");

    private final FlowableClient flowableClient;

    /**
     * Actividades abiertas, agrupadas por instancia. Si el motor no responde se devuelve
     * un mapa vacío: el sitio sigue mostrando lo que hay en la base.
     */
    public Map<String, List<ActivityDTO>> findActiveActivities() {
        try {
            ActivityListDTO activities = flowableClient.get(ACTIVE_ACTIVITIES_URI)
                    .retrieve()
                    .body(ActivityListDTO.class);

            if (activities == null || activities.data() == null) {
                return Map.of();
            }

            return activities.data().stream()
                    .filter(activity -> activity.processInstanceId() != null)
                    .collect(Collectors.groupingBy(ActivityDTO::processInstanceId));
        } catch (Exception e) {
            System.err.println("[WARN] No se pudieron leer las actividades activas: " + e.getMessage());
            return Map.of();
        }
    }

    public List<ActivityDTO> findActiveActivities(String processInstanceId) {
        return findActiveActivities().getOrDefault(processInstanceId, List.of());
    }

    /**
     * Estado del curso. Si la instancia ya cerró manda el resultado que quedó en la base;
     * si sigue viva, se deduce de las actividades abiertas.
     */
    public CourseStage resolveStage(CourseStatus status, List<ActivityDTO> activeActivities) {
        if (status != null) {
            return CourseStage.from(status);
        }

        Set<String> active = activeActivities.stream()
                .map(ActivityDTO::activityId)
                .collect(Collectors.toSet());

        if (anyIn(active, INFORME)) {
            return CourseStage.EN_ELABORACION_INFORME;
        }
        if (anyIn(active, DESARROLLO)) {
            return CourseStage.EN_DESARROLLO;
        }
        if (anyIn(active, CONFIRMADO)) {
            return CourseStage.CONFIRMADO_PENDIENTE_INICIO;
        }
        if (anyIn(active, PREPARACION)) {
            return CourseStage.EN_PREPARACION;
        }
        if (anyIn(active, ESPERANDO_RESPUESTA)) {
            return CourseStage.PROPUESTA_ENVIADA;
        }
        return CourseStage.EN_EVALUACION;
    }

    /**
     * Las actividades que tiene sentido mostrarle a la empresa: las tareas y los eventos
     * que el proceso está esperando, sin el subproceso contenedor ni las compuertas.
     *
     * <p>Durante la preparación son varias a la vez, una por cada rama paralela.
     */
    public List<ActivityDTO> visibleActivities(List<ActivityDTO> activeActivities) {
        return activeActivities.stream()
                .filter(ActivityDTO::isVisibleForCompany)
                .sorted((a, b) -> a.activityName().compareToIgnoreCase(b.activityName()))
                .toList();
    }

    private boolean anyIn(Set<String> active, Set<String> candidates) {
        return active.stream().anyMatch(candidates::contains);
    }
}
