package com.flowable.app.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.flowable.app.dto.CompanyAccessResponseDTO;

/**
 * Cliente para comunicarse con el Backend/BFF institucional.
 */
@Component
public class BackendClient extends BaseClient {

    private static final Logger log = LoggerFactory.getLogger(BackendClient.class);
    private static final String ACCESS_PATH = "/api/flowable/courses/{processId}/company-access";

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
        } catch (RestClientException e) {
            log.error("Instancia: {} | No se pudo habilitar el acceso de la empresa en backend: {}",
                    processInstanceId, e.getMessage());
            return null;
        }
    }
}
