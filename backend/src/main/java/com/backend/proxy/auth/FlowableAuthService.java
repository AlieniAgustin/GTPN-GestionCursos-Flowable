package com.backend.proxy.auth;

import java.net.HttpCookie;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.backend.proxy.context.FlowableSessionContext;
import com.backend.proxy.filter.FlowableSessionFilter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Servicio en el Backend Proxy para autenticar empleados institucionales contra Flowable.
 *
 * Flujo:
 * 1. Envía credenciales con Basic Auth a Flowable.
 * 2. Captura la cabecera 'Set-Cookie: JSESSIONID=...'.
 * 3. Almacena el JSESSIONID en la sesión del usuario del Backend.
 */
@Service
public class FlowableAuthService {

    private final RestClient restClient;

    public FlowableAuthService(@Value("${flowable.base-url:http://localhost:8080}") String flowableBaseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(flowableBaseUrl)
                .build();
    }

    public boolean login(String username, String password, HttpServletRequest request) {
        String basicAuthHeader = "Basic " + Base64.getEncoder().encodeToString(
                (username + ":" + password).getBytes(StandardCharsets.UTF_8)
        );

        ResponseEntity<Void> response = restClient.get()
                .uri("/groups")
                .header(HttpHeaders.AUTHORIZATION, basicAuthHeader)
                .retrieve()
                .toBodilessEntity();

        if (response.getStatusCode().is2xxSuccessful()) {
            List<String> setCookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
            String flowableJSessionId = extractJSessionId(setCookies);

            if (flowableJSessionId != null) {
                HttpSession session = request.getSession(true);
                FlowableSessionContext context = new FlowableSessionContext(username, flowableJSessionId);
                session.setAttribute(FlowableSessionFilter.FLOWABLE_SESSION_KEY, context);
                return true;
            }
        }

        return false;
    }

    public void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    private String extractJSessionId(List<String> setCookies) {
        if (setCookies == null) {
            return null;
        }

        for (String header : setCookies) {
            List<HttpCookie> cookies = HttpCookie.parse(header);
            for (HttpCookie cookie : cookies) {
                if ("JSESSIONID".equalsIgnoreCase(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
