package com.backend.proxy.security;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class JsonAuthenticationFilterTest {

    private AuthenticationManager authenticationManager;
    private ObjectMapper objectMapper;
    private JsonAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        authenticationManager = Mockito.mock(AuthenticationManager.class);
        objectMapper = new ObjectMapper();
        filter = new JsonAuthenticationFilter("/api/auth/login", authenticationManager, objectMapper);
    }

    @Test
    @DisplayName("Successful authentication: returns 200 with LoginResponseDTO and sets session context")
    void testSuccessfulAuthentication() throws Exception {
        Authentication authSuccess = new UsernamePasswordAuthenticationToken(
                "docente1",
                null,
                List.of(new SimpleGrantedAuthority(Roles.ROLE_INSTITUCION))
        );
        when(authenticationManager.authenticate(any())).thenReturn(authSuccess);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setServletPath("/api/auth/login");
        request.setContentType("application/json");
        request.setContent("{\"username\":\"docente1\",\"password\":\"secret\"}".getBytes(StandardCharsets.UTF_8));

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        assertTrue(response.getContentType().contains("application/json"));
        assertTrue(response.getContentAsString().contains("\"username\":\"docente1\""));
        assertTrue(response.getContentAsString().contains("\"role\":\"INSTITUCION\""));
        assertTrue(response.getContentAsString().contains("\"authenticated\":true"));

        // Verify session holds Spring Security context
        assertNotNull(request.getSession(false));
        assertNotNull(request.getSession(false).getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY
        ));
    }

    @Test
    @DisplayName("Failed authentication: returns 401 with JSON error")
    void testFailedAuthentication() throws Exception {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setServletPath("/api/auth/login");
        request.setContentType("application/json");
        request.setContent("{\"username\":\"baduser\",\"password\":\"wrong\"}".getBytes(StandardCharsets.UTF_8));

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        assertTrue(response.getContentAsString().contains("Unauthorized"));
    }

    @Test
    @DisplayName("Company authentication: returns 200 with EMPRESA role")
    void testCompanyAuthenticationSuccess() throws Exception {
        Authentication authSuccess = new UsernamePasswordAuthenticationToken(
                "acme-corp",
                null,
                List.of(new SimpleGrantedAuthority(Roles.ROLE_EMPRESA))
        );
        when(authenticationManager.authenticate(any())).thenReturn(authSuccess);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setServletPath("/api/auth/login");
        request.setContentType("application/json");
        request.setContent("{\"username\":\"acme-corp\",\"password\":\"empresaPass\"}".getBytes(StandardCharsets.UTF_8));

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        assertTrue(response.getContentAsString().contains("\"role\":\"EMPRESA\""));
    }
}
