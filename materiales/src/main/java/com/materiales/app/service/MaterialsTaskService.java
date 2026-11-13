package com.materiales.app.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;

/**
 * Las tres operaciones que pide el enunciado 4.2.1, sobre la API Java del motor:
 * consultar las tareas <i>Preparar materiales</i> asignadas o reclamables, reclamar una
 * disponible para el grupo y completarla.
 */
public class MaterialsTaskService {

    /** Id de la tarea en el BPMN. */
    public static final String TASK_KEY = "prepararmateriales";

    /** Variables que deja la app al completar, para que quede registro de lo preparado. */
    public static final String VAR_PREPARED = "materialesPreparados";
    public static final String VAR_DETAIL = "detalleMateriales";

    private final TaskService taskService;

    public MaterialsTaskService(TaskService taskService) {
        this.taskService = taskService;
    }

    /**
     * Bandeja del usuario: las tareas que tiene asignadas más las que puede reclamar por
     * su grupo.
     *
     * <p>Son dos consultas en vez de {@code taskCandidateOrAssigned}: así los grupos
     * candidatos salen de los que ya resolvió el login y no de la resolución implícita del
     * motor. Las consultas por grupo candidato excluyen las tareas ya reclamadas, así que
     * las dos listas no se solapan.
     */
    public List<MaterialTask> findInbox(StaffUser user) {
        List<MaterialTask> inbox = new ArrayList<>();

        taskService.createTaskQuery()
                .taskDefinitionKey(TASK_KEY)
                .taskAssignee(user.id())
                .includeProcessVariables()
                .list()
                .forEach(task -> inbox.add(toMaterialTask(task)));

        if (!user.groups().isEmpty()) {
            taskService.createTaskQuery()
                    .taskDefinitionKey(TASK_KEY)
                    .taskCandidateGroupIn(user.groups())
                    .includeProcessVariables()
                    .list()
                    .forEach(task -> inbox.add(toMaterialTask(task)));
        }

        inbox.sort(Comparator.comparing(MaterialTask::createTime,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return inbox;
    }

    /**
     * Reclama una tarea disponible para el grupo. El motor rechaza el reclamo si otro
     * integrante se la llevó primero.
     */
    public void claim(String taskId, StaffUser user) {
        taskService.claim(taskId, user.id());
    }

    /**
     * Completa una tarea del usuario. El detalle es opcional y queda como variable de la
     * instancia.
     *
     * <p>Una tarea asignada solo la puede completar su responsable (enunciado 2), y el
     * motor no lo verifica: la validación es de la app.
     */
    public void complete(String taskId, StaffUser user, String detail) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new IllegalStateException("La tarea ya no existe: alguien la completó o la instancia terminó");
        }
        if (task.getAssignee() == null) {
            throw new IllegalStateException("Hay que reclamar la tarea antes de completarla");
        }
        if (!task.getAssignee().equals(user.id())) {
            throw new IllegalStateException("La tarea está asignada a '" + task.getAssignee()
                    + "': solo esa persona puede completarla");
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put(VAR_PREPARED, true);
        if (detail != null && !detail.isBlank()) {
            variables.put(VAR_DETAIL, detail.trim());
        }
        taskService.complete(taskId, variables);
    }

    private MaterialTask toMaterialTask(Task task) {
        Map<String, Object> variables = task.getProcessVariables();
        if (variables == null) {
            variables = Map.of();
        }
        return new MaterialTask(
                task.getId(),
                task.getName(),
                task.getAssignee(),
                task.getProcessInstanceId(),
                asText(variables.get("courseName")),
                asText(variables.get("companyName")),
                task.getCreateTime());
    }

    private String asText(Object value) {
        return value == null ? "" : value.toString();
    }
}
