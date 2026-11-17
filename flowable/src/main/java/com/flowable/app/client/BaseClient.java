package com.flowable.app.client;

import org.springframework.web.client.RestTemplate;

public abstract class BaseClient {

    protected final RestTemplate restTemplate;

    protected BaseClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
}
