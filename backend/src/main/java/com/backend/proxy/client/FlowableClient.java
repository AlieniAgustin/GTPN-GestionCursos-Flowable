package com.backend.proxy.client;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class FlowableClient {

    private final RestClient restClient;

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
