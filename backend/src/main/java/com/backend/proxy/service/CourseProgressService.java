package com.backend.proxy.service;

import java.util.Collections;
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

@Service
@RequiredArgsConstructor
public class CourseProgressService {
    private static final String ACTIVE_ACTIVITIES_URI =
            "/process-api/history/historic-activity-instances?finished=false&size=1000";

    private record StageRule(CourseStage stage, Set<String> activities) {
        boolean matches(Set<String> active) {
            return !Collections.disjoint(active, activities);
        }
    }

    private static final List<StageRule> STAGE_PRECEDENCE = List.of(
            new StageRule(CourseStage.EN_ELABORACION_INFORME, Set.of("elaborarinformederesultados", "enviarinformeaempresayarchivarexpediente")),
            new StageRule(CourseStage.EN_DESARROLLO, Set.of("desarrollarcurso")),
            new StageRule(CourseStage.CONFIRMADO_PENDIENTE_INICIO, Set.of("fechaprogramada", "definirdocentequedesarrollaelcurso", "enviarconfirmacion")),
            new StageRule(CourseStage.EN_PREPARACION, Set.of("prepararcurso")),
            new StageRule(CourseStage.PROPUESTA_ENVIADA, Set.of("aceptacion", "rechazo", "modificaciones", "plazo5dias", "enviarrecordatorio"))
    );

    private final FlowableClient flowableClient;

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

    public CourseStage resolveStage(CourseStatus status, List<ActivityDTO> activeActivities) {
        if (status != null) {
            return CourseStage.from(status);
        }

        Set<String> active = activeActivities.stream()
                .map(ActivityDTO::activityId)
                .collect(Collectors.toSet());

        return STAGE_PRECEDENCE.stream()
                .filter(rule -> rule.matches(active))
                .map(StageRule::stage)
                .findFirst()
                .orElse(CourseStage.EN_EVALUACION);
    }

    public List<ActivityDTO> visibleActivities(List<ActivityDTO> activeActivities) {
        return activeActivities.stream()
                .filter(ActivityDTO::isVisibleForCompany)
                .sorted((a, b) -> a.activityName().compareToIgnoreCase(b.activityName()))
                .toList();
    }
}
