package com.flowable.app.client;

import org.springframework.web.client.RestTemplate;

/**
 * Cliente HTTP base para llamadas salientes desde el motor de procesos.
 * Encapsula la infraestructura del {@link RestTemplate}.
 */
public abstract class BaseClient {

    protected final RestTemplate restTemplate;

    protected BaseClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
}
