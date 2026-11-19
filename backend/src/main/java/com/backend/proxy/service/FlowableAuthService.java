package com.backend.proxy.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;


@Service
public class FlowableAuthService {

    private final RestClient authRestClient;

    public FlowableAuthService(@Value("${flowable.base-url:http://localhost:8080}") String flowableBaseUrl) {
        this.authRestClient = RestClient.builder()
                .baseUrl(flowableBaseUrl)
                .build();
    }

    public boolean authenticates(String username, String password) {
        try {
            ResponseEntity<Void> response = authRestClient.get()
                    .uri("/me")
                    .headers(headers -> headers.setBasicAuth(username, password))
                    .retrieve()
                    .toBodilessEntity();

            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception ex) {
            return false;
        }
    }
}
