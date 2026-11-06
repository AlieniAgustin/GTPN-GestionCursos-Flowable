package com.backend.proxy.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

@Component
public class FlowableClientLoggingInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(FlowableClientLoggingInterceptor.class);

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        log.info("Outgoing request to Flowable: [{}] {}", request.getMethod(), request.getURI());
        if (body.length > 0 && log.isDebugEnabled()) {
            log.debug("Flowable request body: {}", new String(body, StandardCharsets.UTF_8));
        }

        try {
            ClientHttpResponse response = execution.execute(request, body);
            log.info("Response from Flowable: [{}] {} -> Status: {}",
                    request.getMethod(), request.getURI(), response.getStatusCode());
            return response;
        } catch (IOException ex) {
            log.error("Error communicating with Flowable: [{}] {} -> Exception: {}",
                    request.getMethod(), request.getURI(), ex.getMessage());
            throw ex;
        }
    }
}
