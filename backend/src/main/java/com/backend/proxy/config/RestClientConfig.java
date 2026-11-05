package com.backend.proxy.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    // client http for comunicate with flowable motor using technical account
    @Bean
    public RestClient flowableRestClient(
            @Value("${flowable.base-url:http://localhost:8080}") String flowableBaseUrl,
            @Value("${flowable.username:admin}") String flowableUsername,
            @Value("${flowable.password:test}") String flowablePassword
    ) {
        return RestClient.builder()
                .baseUrl(flowableBaseUrl)
                .defaultHeaders(headers -> headers.setBasicAuth(flowableUsername, flowablePassword))
                .build();
    }
}
