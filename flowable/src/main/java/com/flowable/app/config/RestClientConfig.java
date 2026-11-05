package com.flowable.app.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient backendRestClient(
            @Value("${backend.base-url:http://localhost:8081}") String backendBaseUrl,
            @Value("${backend.api-key:flowable-secret-key}") String backendApiKey
    ) {
        return RestClient.builder()
                .baseUrl(backendBaseUrl)
                .defaultHeader("X-API-KEY", backendApiKey)
                .build();
    }
}
