package com.flowable.app.listeners;

import java.util.List;

import org.flowable.engine.IdentityService;
import org.flowable.engine.TaskService;
import org.flowable.engine.delegate.TaskListener;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.idm.api.User;
import org.flowable.task.service.delegate.DelegateTask;
import org.springframework.stereotype.Component;

@Component("assignLeastBusy")
public class AssignLeastBusyListener implements TaskListener {

    private static final long serialVersionUID = 1L;

    // ID del grupo tal como lo crea App.java
    public static final String ADMINISTRATION_GROUP = "administracion";

    @Override
    public void notify(DelegateTask task) {
        System.out.println("[LISTENER] assign least busy user");
        System.out.println("[INFO] Instancia: " + task.getProcessInstanceId()
                + " | Tarea: " + task.getName());

        // Si el modelo ya trae un usuario asignado, se respeta
        if (task.getAssignee() != null && !task.getAssignee().isBlank()) {
            System.out.println("[INFO] Instancia: " + task.getProcessInstanceId()
                    + " | Tarea ya estaba asignada a: " + task.getAssignee());
            return;
        }

        // Se reutiliza el motor que está ejecutando el listener
        ProcessEngineConfigurationImpl cfg = CommandContextUtil.getProcessEngineConfiguration();
        IdentityService identityService = cfg.getIdentityService();
        TaskService taskService = cfg.getTaskService();

        // Integrantes del grupo, ordenados por ID para desempatar
        List<User> members = identityService.createUserQuery()
                .memberOfGroup(ADMINISTRATION_GROUP)
                .orderByUserId().asc()
                .list();

        if (members.isEmpty()) {
            throw new IllegalStateException("No hay usuarios en el grupo " + ADMINISTRATION_GROUP);
        }

        String selectedUser = null;
        long lowestCount = Long.MAX_VALUE;

        for (User user : members) {
            // Solo cuenta tareas ASIGNADAS, no las candidatas sin reclamar
            long pendingTasks = taskService.createTaskQuery()
                    .taskAssignee(user.getId())
                    .count();
            System.out.println("[INFO] Usuario: " + user.getId() + " | Tareas asignadas: " + pendingTasks);

            // En caso de empate queda el primero por ID
            if (pendingTasks < lowestCount) {
                lowestCount = pendingTasks;
                selectedUser = user.getId();
            }
        }

        // Se usa taskService para que la tarea figure como asignada y no como "a reclamar"
        taskService.setAssignee(task.getId(), selectedUser);
        System.out.println("[INFO] Instancia: " + task.getProcessInstanceId()
                + " | Asignada a: " + selectedUser + " (" + lowestCount + " tareas previas)");
    }
}
