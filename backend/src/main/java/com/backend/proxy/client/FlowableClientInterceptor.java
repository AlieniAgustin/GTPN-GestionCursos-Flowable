package com.backend.proxy.client;

import java.io.IOException;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import com.backend.proxy.context.FlowableSessionHolder;

/**
 * Interceptor HTTP saliente para RestTemplate o RestClient.
 * Inyecta automáticamente la cookie 'JSESSIONID=<flowableJSessionId>'
 * en todas las peticiones dirigidas a la API REST de Flowable.
 */
@Component
public class FlowableClientInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution
    ) throws IOException {

        String jsessionId = FlowableSessionHolder.getFlowableJSessionId();

        if (jsessionId != null && !jsessionId.isBlank()) {
            request.getHeaders().add("Cookie", "JSESSIONID=" + jsessionId);
        }

        return execution.execute(request, body);
    }
}
