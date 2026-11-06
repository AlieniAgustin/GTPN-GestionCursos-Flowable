package com.backend.proxy.client;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.backend.proxy.dto.FlowableProcessInstanceRequest;
import com.backend.proxy.dto.FlowableProcessInstanceResponse;
import com.backend.proxy.dto.FlowableVariable;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class FlowableClient {

    private static final String PROCESS_INSTANCES_URI = "/process-api/runtime/process-instances";

    private final RestClient restClient;

    @Retryable(
            retryFor = RestClientException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2.0)
    )
    public FlowableProcessInstanceResponse startProcessInstance(FlowableProcessInstanceRequest request) {
        return restClient.post()
                .uri(PROCESS_INSTANCES_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(FlowableProcessInstanceResponse.class);
    }

    public FlowableProcessInstanceResponse startProcessInstance(
            String processDefinitionKey,
            String businessKey,
            List<FlowableVariable> variables
    ) {
        return startProcessInstance(new FlowableProcessInstanceRequest(processDefinitionKey, businessKey, variables));
    }

    public RestClient.RequestBodyUriSpec post() {
        return restClient.post();
    }

    public RestClient.RequestBodySpec post(String uri, Object body) {
        return restClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    public RestClient.RequestHeadersUriSpec<?> get() {
        return restClient.get();
    }

    public RestClient.RequestHeadersSpec<?> get(String uri) {
        return restClient.get().uri(uri);
    }

    public RestClient.RequestBodyUriSpec put() {
        return restClient.put();
    }

    public RestClient.RequestBodySpec put(String uri, Object body) {
        return restClient.put()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    public RestClient.RequestHeadersUriSpec<?> delete() {
        return restClient.delete();
    }

    public RestClient.RequestHeadersSpec<?> delete(String uri) {
        return restClient.delete().uri(uri);
    }
}
