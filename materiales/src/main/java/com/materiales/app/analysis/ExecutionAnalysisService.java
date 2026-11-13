package com.materiales.app.analysis;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.flowable.engine.HistoryService;
import org.flowable.engine.IdentityService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.idm.api.User;
import org.flowable.task.api.history.HistoricTaskInstance;

/**
 * Las tres consultas del enunciado 4.3 sobre la información histórica de Flowable, por la
 * API Java: resultados de las solicitudes, duración de las ramas de la preparación en
 * paralelo y distribución de tareas de usuario.
 *
 * <p>No guarda nada: cada llamada consulta el historial del motor en el momento.
 */
public class ExecutionAnalysisService {

    public static final String PROCESS_KEY = "Process_Curso";

    /** Variable que cada camino de cierre deja seteada (enunciado 3.4). */
    static final String VAR_RESULT = "resultadoCurso";

    /** Ids del BPMN que delimitan la preparación y sus tres ramas. */
    static final String ACT_PREPARATION = "prepararcurso";
    static final String ACT_PREPARATION_END = "finprepararcurso";
    static final String ACT_PARALLEL_FORK = "preparacionenparalelo";
    static final String ACT_CANCELLATION_RECEIVED = "recibircancelacion";
    static final String ACT_PARTICIPANTS_LIST = "confeccionarlistadefinitiva";
    static final String ACT_MATERIALS = "prepararmateriales";
    static final List<String> ACT_RESOURCES =
            List.of("reservaraula", "solicitaraulavirtual", "contratarequipamiento");

    static final String GROUP_ADMINISTRACION = "administracion";
    static final String GROUP_DOCENTES = "docentes";

    /** Los cinco resultados del enunciado 4.3.1, con el nombre que les da el enunciado. */
    private static final Map<String, String> RESULT_LABELS = new LinkedHashMap<>();

    static {
        RESULT_LABELS.put("FINALIZADO", "Curso finalizado");
        RESULT_LABELS.put("CANCELADO", "Cancelación");
        RESULT_LABELS.put("RECHAZADO", "Rechazo de la propuesta");
        RESULT_LABELS.put("NO_VIABLE", "Solicitud no viable");
        RESULT_LABELS.put("SIN_RESPUESTA", "Cierre por falta de respuesta");
    }

    private final HistoryService historyService;
    private final IdentityService identityService;

    public ExecutionAnalysisService(HistoryService historyService, IdentityService identityService) {
        this.historyService = historyService;
        this.identityService = identityService;
    }

    // ------------------------------------------------------------------ 4.3.1

    /**
     * Resultados de las solicitudes: cantidad y porcentaje de cada uno sobre el total de
     * instancias terminadas.
     */
    public ResultsReport analyzeResults() {
        List<HistoricProcessInstance> finished = historyService.createHistoricProcessInstanceQuery()
                .processDefinitionKey(PROCESS_KEY)
                .finished()
                .includeProcessVariables()
                .list();

        Map<String, Long> counts = new LinkedHashMap<>();
        RESULT_LABELS.keySet().forEach(result -> counts.put(result, 0L));
        long withoutResult = 0;

        for (HistoricProcessInstance instance : finished) {
            String result = resultOf(instance);
            if (result != null && counts.containsKey(result)) {
                counts.merge(result, 1L, Long::sum);
            } else {
                withoutResult++;
            }
        }

        long total = finished.size();
        List<ResultsReport.Tally> tallies = new ArrayList<>();
        counts.forEach((result, count) -> tallies.add(new ResultsReport.Tally(
                RESULT_LABELS.get(result), result, count, percentage(count, total))));

        // Solo se muestra si hay alguna: con el modelo actual debería ser siempre cero
        if (withoutResult > 0) {
            tallies.add(new ResultsReport.Tally(
                    "Sin resultado registrado", null, withoutResult, percentage(withoutResult, total)));
        }

        return new ResultsReport(total, tallies);
    }

    private String resultOf(HistoricProcessInstance instance) {
        Map<String, Object> variables = instance.getProcessVariables();
        if (variables == null) {
            return null;
        }
        Object value = variables.get(VAR_RESULT);
        return value == null ? null : value.toString().trim();
    }

    // ------------------------------------------------------------------ 4.3.2

    /**
     * Duración promedio de las ramas de inscripciones, materiales y recursos.
     *
     * <p>La activación es el momento en que el gateway paralelo abre las tres ramas, así
     * que es la misma para las tres. Cada rama termina con su última actividad: la lista
     * definitiva, los materiales y —para recursos— la última de las acciones
     * seleccionadas, que es lo que pide el enunciado ("cuando se hayan completado todas
     * las acciones seleccionadas").
     *
     * <p>Se miden solo las preparaciones que llegaron al fin del subproceso. Una
     * cancelación interrumpe el subproceso sin pasar por ahí, así que queda excluida sola.
     */
    public PreparationReport analyzePreparation() {
        List<HistoricProcessInstance> instances = historyService.createHistoricProcessInstanceQuery()
                .processDefinitionKey(PROCESS_KEY)
                .list();

        List<Long> inscriptions = new ArrayList<>();
        List<Long> materials = new ArrayList<>();
        List<Long> resources = new ArrayList<>();
        long cancelled = 0;
        long inProgress = 0;
        long withoutPreparation = 0;

        for (HistoricProcessInstance instance : instances) {
            List<HistoricActivityInstance> activities = historyService.createHistoricActivityInstanceQuery()
                    .processInstanceId(instance.getId())
                    .list();

            Date preparationEnd = lastEndTime(activities, ACT_PREPARATION_END);
            if (preparationEnd == null) {
                if (!startedAny(activities, ACT_PREPARATION)) {
                    withoutPreparation++;
                } else if (startedAny(activities, ACT_CANCELLATION_RECEIVED)) {
                    cancelled++;
                } else {
                    inProgress++;
                }
                continue;
            }

            Date activation = lastEndTime(activities, ACT_PARALLEL_FORK);
            if (activation == null) {
                // Sin el gateway en el historial no hay desde dónde medir
                continue;
            }

            addDuration(inscriptions, activation, lastEndTime(activities, ACT_PARTICIPANTS_LIST));
            addDuration(materials, activation, lastEndTime(activities, ACT_MATERIALS));
            addDuration(resources, activation, lastResourceEndTime(activities));
        }

        List<PreparationReport.Branch> branches = List.of(
                new PreparationReport.Branch("Inscripciones", inscriptions.size(), average(inscriptions)),
                new PreparationReport.Branch("Materiales", materials.size(), average(materials)),
                new PreparationReport.Branch("Recursos", resources.size(), average(resources)));

        long measured = Math.max(Math.max(inscriptions.size(), materials.size()), resources.size());
        return new PreparationReport(measured, cancelled, inProgress, withoutPreparation, branches);
    }

    /**
     * La rama de recursos termina con la última acción seleccionada. Las que no se
     * seleccionaron no están en el historial, así que no cuentan.
     */
    private Date lastResourceEndTime(List<HistoricActivityInstance> activities) {
        Date last = null;
        for (String activityId : ACT_RESOURCES) {
            Date end = lastEndTime(activities, activityId);
            if (end != null && (last == null || end.after(last))) {
                last = end;
            }
        }
        return last;
    }

    private void addDuration(List<Long> samples, Date from, Date to) {
        if (from == null || to == null) {
            return;
        }
        long millis = to.getTime() - from.getTime();
        samples.add(Math.max(millis, 0));
    }

    /**
     * Última finalización de esa actividad. Se toma la última porque alguna puede repetirse:
     * "Reservar aula" se reintenta después de gestionar la falta de aulas.
     */
    private Date lastEndTime(List<HistoricActivityInstance> activities, String activityId) {
        Date last = null;
        for (HistoricActivityInstance activity : activities) {
            if (activityId.equals(activity.getActivityId()) && activity.getEndTime() != null
                    && (last == null || activity.getEndTime().after(last))) {
                last = activity.getEndTime();
            }
        }
        return last;
    }

    private boolean startedAny(List<HistoricActivityInstance> activities, String activityId) {
        return activities.stream().anyMatch(activity -> activityId.equals(activity.getActivityId()));
    }

    private Duration average(List<Long> samples) {
        if (samples.isEmpty()) {
            return null;
        }
        long total = samples.stream().mapToLong(Long::longValue).sum();
        return Duration.ofMillis(Math.round((double) total / samples.size()));
    }

    // ------------------------------------------------------------------ 4.3.3

    /**
     * Tareas de usuario completadas por cada integrante.
     *
     * <p>{@code finished()} más {@code taskWithoutDeleteReason()} deja afuera las
     * pendientes y las que el motor borró al interrumpir la preparación por una
     * cancelación, que es lo que pide el enunciado.
     */
    public TaskDistributionReport analyzeTaskDistribution() {
        List<HistoricTaskInstance> completed = historyService.createHistoricTaskInstanceQuery()
                .processDefinitionKey(PROCESS_KEY)
                .finished()
                .taskWithoutDeleteReason()
                .list();

        Map<String, Long> countsByUser = new TreeMap<>();
        long total = 0;
        for (HistoricTaskInstance task : completed) {
            String assignee = task.getAssignee();
            if (assignee == null || assignee.isBlank()) {
                // Una tarea completada siempre tiene responsable; si no, no es de nadie
                continue;
            }
            countsByUser.merge(assignee, 1L, Long::sum);
            total++;
        }

        List<TaskDistributionReport.Tally> tallies = new ArrayList<>();
        addGroupTallies(tallies, GROUP_ADMINISTRACION, "Administración", countsByUser);
        addGroupTallies(tallies, GROUP_DOCENTES, "Docentes", countsByUser);

        // Cualquier otro responsable que aparezca (por ejemplo la cuenta técnica)
        countsByUser.forEach((userId, count) ->
                tallies.add(new TaskDistributionReport.Tally(userId, "(fuera de los grupos)", count)));

        return new TaskDistributionReport(total, tallies);
    }

    private void addGroupTallies(List<TaskDistributionReport.Tally> tallies, String groupId,
                                 String groupLabel, Map<String, Long> countsByUser) {
        List<User> members = identityService.createUserQuery()
                .memberOfGroup(groupId)
                .orderByUserId()
                .asc()
                .list();

        for (User member : members) {
            long count = Optional.ofNullable(countsByUser.remove(member.getId())).orElse(0L);
            tallies.add(new TaskDistributionReport.Tally(member.getId(), groupLabel, count));
        }
    }

    private double percentage(long count, long total) {
        return total == 0 ? 0.0 : (count * 100.0) / total;
    }
}
