package com.backend.proxy.client;

import java.io.IOException;

import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import com.backend.proxy.context.FlowableSessionContext;
import com.backend.proxy.context.FlowableSessionHolder;
import com.backend.proxy.exception.FlowableSessionExpiredException;

/**
 * Interceptor HTTP saliente para RestTemplate o RestClient.
 * Inyecta automáticamente la cookie 'JSESSIONID=<flowableJSessionId>'
 * en todas las peticiones dirigidas a la API REST de Flowable.
 * Además, intercepta respuestas 401 de Flowable para lanzar una excepción
 * descriptiva de sesión vencida.
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

        ClientHttpResponse response = execution.execute(request, body);

        if (response.getStatusCode().isSameCodeAs(HttpStatus.UNAUTHORIZED)) {
            FlowableSessionContext context = FlowableSessionHolder.get();
            String username = context != null ? context.username() : "desconocido";
            throw new FlowableSessionExpiredException(
                "La sesión en Flowable ha expirado o no es válida para el usuario: " + username,
                username
            );
        }

        return response;
    }
}
