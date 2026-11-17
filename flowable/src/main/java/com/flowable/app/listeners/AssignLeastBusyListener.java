package com.flowable.app.listeners;

import org.flowable.engine.delegate.TaskListener;
import org.flowable.task.service.delegate.DelegateTask;
import org.springframework.stereotype.Component;

import com.flowable.app.config.IdentitySeeder;
import com.flowable.app.services.TaskAssignmentService;

@Component("assignLeastBusy")
public class AssignLeastBusyListener implements TaskListener {

    private static final long serialVersionUID = 1L;

    private final TaskAssignmentService taskAssignmentService;

    public AssignLeastBusyListener(TaskAssignmentService taskAssignmentService) {
        this.taskAssignmentService = taskAssignmentService;
    }

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

        String selectedUser = taskAssignmentService.assignToLeastBusy(
                task.getId(), IdentitySeeder.GROUP_ADMINISTRACION);

        System.out.println("[INFO] Instancia: " + task.getProcessInstanceId()
                + " | Asignada a: " + selectedUser);
    }
}
