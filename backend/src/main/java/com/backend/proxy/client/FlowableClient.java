package com.backend.proxy.client;

import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.backend.proxy.dto.CourseProcessStartRequest;
import com.backend.proxy.dto.CourseProcessStartResponse;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class FlowableClient {

    private static final String COURSE_REQUESTS_URI = "/api/course-requests";

    private final RestClient restClient;

    /**
     * Arranca el proceso de una solicitud de curso. El motor decide qué proceso es: acá
     * solo viaja el contexto del curso.
     */
    @Retryable(
            retryFor = RestClientException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2.0)
    )
    public CourseProcessStartResponse startCourseProcess(CourseProcessStartRequest request) {
        return restClient.post()
                .uri(COURSE_REQUESTS_URI)
                .body(request)
                .retrieve()
                .body(CourseProcessStartResponse.class);
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
