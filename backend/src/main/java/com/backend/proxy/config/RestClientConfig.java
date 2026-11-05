package com.backend.proxy.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import com.backend.proxy.client.FlowableClientInterceptor;

@Configuration
public class RestClientConfig {

    // en la peticion a flowable, incorpora la cookie
    @Bean
    public RestClient flowableRestClient( @Value("${flowable.base-url:http://localhost:8080}") String flowableBaseUrl,
    FlowableClientInterceptor flowableClientInterceptor) {
        return RestClient.builder()
                .baseUrl(flowableBaseUrl)
                .requestInterceptor(flowableClientInterceptor)
                .build();
    }
}
