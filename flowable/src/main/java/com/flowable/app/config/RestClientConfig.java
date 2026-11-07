package com.flowable.app.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestClientConfig {

    @Bean
    public RestTemplate backendRestTemplate(
            RestTemplateBuilder builder,
            @Value("${backend.base-url:http://localhost:8081}") String backendBaseUrl,
            @Value("${backend.api-key:flowable-secret-key}") String backendApiKey
    ) {
        return builder
                .rootUri(backendBaseUrl)
                .defaultHeader("X-API-KEY", backendApiKey)
                .build();
    }
}
