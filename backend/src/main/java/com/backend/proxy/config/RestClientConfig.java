package com.backend.proxy.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    // Technical client for communicating with Flowable engine
    @Bean
    public RestClient flowableRestClient(
            @Value("${flowable.base-url:http://localhost:8080}") String flowableBaseUrl,
            @Value("${flowable.username:admin}") String flowableUsername,
            @Value("${flowable.password:test}") String flowablePassword,
            FlowableClientLoggingInterceptor loggingInterceptor
    ) {
        return RestClient.builder()
                .baseUrl(flowableBaseUrl)
                .defaultHeaders(headers -> {
                    headers.setBasicAuth(flowableUsername, flowablePassword);
                    headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
                    headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
                })
                .requestInterceptor(loggingInterceptor)
                .build();
    }
}
