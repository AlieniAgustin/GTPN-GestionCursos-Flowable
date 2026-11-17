package com.flowable.app.listeners;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.springframework.stereotype.Component;

import com.flowable.app.client.BackendClient;

@Component("updateCourseStatus")
public class UpdateCourseStatusListener implements ExecutionListener {

    private static final long serialVersionUID = 1L;

    // Variable que cada camino de cierre marca antes de terminar el proceso
    public static final String RESULT_VARIABLE = "resultadoCurso";

    private final BackendClient backendClient;

    public UpdateCourseStatusListener(BackendClient backendClient) {
        this.backendClient = backendClient;
    }

    @Override
    public void notify(DelegateExecution execution) {
        System.out.println("[LISTENER] update course status");

        // El business key vincula la instancia con el curso de la base de la institución
        String businessKey = execution.getProcessInstanceBusinessKey();
        String processInstanceId = execution.getProcessInstanceId();
        Object result = execution.getVariable(RESULT_VARIABLE);

        System.out.println("[INFO] Instancia: " + processInstanceId
                + " | Curso (business key): " + businessKey);

        // Si ningún camino marcó el resultado, se avisa para revisar el modelo
        if (result == null) {
            System.out.println("[WARN] Instancia: " + processInstanceId
                    + " | ATENCION: la instancia terminó sin " + RESULT_VARIABLE);
            return;
        }

        System.out.println("[INFO] Instancia: " + processInstanceId
                + " | Resultado del curso: " + result);

        if (backendClient.updateCourseStatus(processInstanceId, result.toString().trim())) {
            System.out.println("[INFO] Instancia: " + processInstanceId
                    + " | Estado del curso actualizado con éxito en Backend");
        }
    }
}
