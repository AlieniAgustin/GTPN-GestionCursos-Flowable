package com.flowable.app.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.flowable.app.dto.CompanyAccessResponseDTO;
import com.flowable.app.dto.CourseDetailsUpdateDTO;
import com.flowable.app.dto.CourseStatusUpdateDTO;

/**
 * Cliente para comunicarse con el Backend/BFF institucional.
 */
@Component
public class BackendClient extends BaseClient {

    private static final Logger log = LoggerFactory.getLogger(BackendClient.class);

    private static final String ACCESS_PATH = "/api/flowable/courses/{processId}/company-access";
    private static final String DETAILS_PATH = "/api/flowable/courses/{processId}/details";
    private static final String STATUS_PATH = "/api/flowable/courses/{processId}";

    public BackendClient(@Qualifier("backendRestTemplate") RestTemplate restTemplate) {
        super(restTemplate);
    }

    /**
     * Habilita o recupera la cuenta de la empresa dueña del curso (enunciado 4.1.2).
     * Atrapa los errores de red o HTTP del backend y devuelve null para no trabar el flujo BPMN.
     */
    public CompanyAccessResponseDTO enableCompanyAccess(String processInstanceId) {
        try {
            ResponseEntity<CompanyAccessResponseDTO> response = restTemplate.postForEntity(
                    ACCESS_PATH, null, CompanyAccessResponseDTO.class, processInstanceId);
            return response != null ? response.getBody() : null;
        } catch (Exception e) {
            log.error("Instancia: {} | No se pudo habilitar el acceso de la empresa en backend: {}",
                    processInstanceId, e.getMessage());
            return null;
        }
    }

    /**
     * Vuelca a la BD de la institución los datos del curso (enunciado 3.2).
     * Devuelve false si el backend no responde: la BD es un registro paralelo, no
     * una condición para que la instancia avance.
     */
    public boolean syncCourseDetails(String processInstanceId, CourseDetailsUpdateDTO details) {
        try {
            put(DETAILS_PATH, details, processInstanceId);
            return true;
        } catch (Exception e) {
            log.error("Instancia: {} | No se pudieron sincronizar los datos del curso: {}",
                    processInstanceId, e.getMessage());
            return false;
        }
    }

    /**
     * Registra en la BD de la institución el estado final del curso (enunciado 3.4).
     * Devuelve false si el backend no responde.
     */
    public boolean updateCourseStatus(String processInstanceId, String status) {
        try {
            put(STATUS_PATH, new CourseStatusUpdateDTO(status), processInstanceId);
            return true;
        } catch (Exception e) {
            log.error("Instancia: {} | Error al actualizar el estado del curso en backend: {}",
                    processInstanceId, e.getMessage());
            return false;
        }
    }

    private void put(String path, Object body, String processInstanceId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        restTemplate.exchange(path, HttpMethod.PUT, new HttpEntity<>(body, headers),
                Void.class, processInstanceId);
    }
}
