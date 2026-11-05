package com.flowable.app.listeners;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.flowable.app.dto.CourseStatusUpdateDTO;

@Component("updateCourseStatus")
public class UpdateCourseStatusListener implements ExecutionListener {

    private static final long serialVersionUID = 1L;

    // Variable que cada camino de cierre marca antes de terminar el proceso
    public static final String RESULT_VARIABLE = "resultadoCurso";

    private final RestClient backendRestClient;

    public UpdateCourseStatusListener(@Qualifier("backendRestClient") RestClient backendRestClient) {
        this.backendRestClient = backendRestClient;
    }

    @Override
    public void notify(DelegateExecution execution) {
        System.out.println("***** EXECUTION LISTENER UpdateCourseStatus *****");

        // El business key vincula la instancia con el curso de la base de la institución
        String businessKey = execution.getProcessInstanceBusinessKey();
        String processInstanceId = execution.getProcessInstanceId();
        Object result = execution.getVariable(RESULT_VARIABLE);

        System.out.println("Instancia: " + processInstanceId
                + " | Curso (business key): " + businessKey);

        // Si ningún camino marcó el resultado, se avisa para revisar el modelo
        if (result == null) {
            System.out.println("ATENCION: la instancia terminó sin " + RESULT_VARIABLE);
            return;
        }

        System.out.println("Resultado del curso: " + result);

        try {
            backendRestClient.put()
                    .uri("/api/flowable/courses/{processId}", processInstanceId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new CourseStatusUpdateDTO(result.toString().trim()))
                    .retrieve()
                    .toBodilessEntity();

            System.out.println("Estado del curso actualizado con éxito en Backend");
        } catch (Exception e) {
            System.err.println("Error al actualizar el estado del curso en Backend: " + e.getMessage());
        }

        System.out.println("*************************************");
    }
}
