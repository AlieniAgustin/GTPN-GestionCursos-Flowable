package com.flowable.app.services;

import java.util.List;

import org.flowable.engine.IdentityService;
import org.flowable.engine.TaskService;
import org.flowable.idm.api.User;
import org.springframework.stereotype.Service;

/**
 * Asigna una tarea al integrante de un grupo con menos tareas asignadas.
 * Desempata por id de usuario (los miembros vienen ordenados por id).
 */
@Service
public class TaskAssignmentService {

    private final IdentityService identityService;
    private final TaskService taskService;

    public TaskAssignmentService(IdentityService identityService, TaskService taskService) {
        this.identityService = identityService;
        this.taskService = taskService;
    }

    public String assignToLeastBusy(String taskId, String candidateGroup) {
        List<User> members = identityService.createUserQuery()
                .memberOfGroup(candidateGroup)
                .orderByUserId().asc()
                .list();

        if (members.isEmpty()) {
            throw new IllegalStateException("No hay usuarios en el grupo " + candidateGroup);
        }

        String selectedUser = null;
        long lowestCount = Long.MAX_VALUE;

        for (User user : members) {
            // Solo cuenta tareas ASIGNADAS, no las candidatas sin reclamar
            long pendingTasks = taskService.createTaskQuery()
                    .taskAssignee(user.getId())
                    .count();
            System.out.println("[INFO] Usuario: " + user.getId() + " | Tareas asignadas: " + pendingTasks);

            if (pendingTasks < lowestCount) {
                lowestCount = pendingTasks;
                selectedUser = user.getId();
            }
        }

        // Se usa taskService para que la tarea figure como asignada y no como "a reclamar"
        taskService.setAssignee(taskId, selectedUser);
        return selectedUser;
    }
}
