package com.flowable.app.listeners;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.flowable.app.dto.CourseStatusUpdateDTO;

@Component("updateCourseStatus")
public class UpdateCourseStatusListener implements ExecutionListener {

    private static final long serialVersionUID = 1L;

    // Variable que cada camino de cierre marca antes de terminar el proceso
    public static final String RESULT_VARIABLE = "resultadoCurso";

    private final RestTemplate backendRestTemplate;

    public UpdateCourseStatusListener(@Qualifier("backendRestTemplate") RestTemplate backendRestTemplate) {
        this.backendRestTemplate = backendRestTemplate;
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

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            backendRestTemplate.exchange(
                    "/api/flowable/courses/{processId}",
                    HttpMethod.PUT,
                    new HttpEntity<>(new CourseStatusUpdateDTO(result.toString().trim()), headers),
                    Void.class,
                    processInstanceId);

            System.out.println("[INFO] Instancia: " + processInstanceId
                    + " | Estado del curso actualizado con éxito en Backend");
        } catch (Exception e) {
            System.err.println("[ERROR] Instancia: " + processInstanceId
                    + " | Error al actualizar el estado del curso en Backend: " + e.getMessage());
        }
    }
}
