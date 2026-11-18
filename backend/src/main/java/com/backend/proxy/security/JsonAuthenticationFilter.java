package com.backend.proxy.security;

import java.io.IOException;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import com.backend.proxy.dto.auth.LoginRequestDTO;
import com.backend.proxy.dto.auth.LoginResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filtro de autenticación para endpoints JSON (REST API).
 *
 * <p>Intercepta las solicitudes de login por HTTP POST, deserializa el body JSON con las
 * credenciales y delega la autenticación en el {@link AuthenticationManager}.
 * En caso de éxito, persiste el SecurityContext en la HttpSession y emite la respuesta JSON.
 */
public class JsonAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    private final ObjectMapper objectMapper;

    public JsonAuthenticationFilter(
            String loginProcessingUrl,
            AuthenticationManager authenticationManager,
            ObjectMapper objectMapper
    ) {
        super(new AntPathRequestMatcher(loginProcessingUrl, HttpMethod.POST.name()), authenticationManager);
        this.objectMapper = objectMapper;
        setSecurityContextRepository(new HttpSessionSecurityContextRepository());

        setAuthenticationSuccessHandler((request, response, authentication) -> {
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            String role = Roles.isCompany(authentication) ? Roles.EMPRESA : Roles.INSTITUCION;
            LoginResponseDTO responseDto = new LoginResponseDTO(authentication.getName(), role, true);
            objectMapper.writeValue(response.getOutputStream(), responseDto);
        });

        setAuthenticationFailureHandler((request, response, exception) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"error\": \"Unauthorized\", \"message\": \"" + exception.getMessage() + "\"}");
        });
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException, IOException {
        if (!HttpMethod.POST.matches(request.getMethod())) {
            throw new AuthenticationServiceException("Authentication method not supported: " + request.getMethod());
        }

        LoginRequestDTO credentials;
        try {
            credentials = objectMapper.readValue(request.getInputStream(), LoginRequestDTO.class);
        } catch (Exception e) {
            throw new AuthenticationServiceException("Failed to parse authentication request body", e);
        }

        if (credentials == null || credentials.username() == null || credentials.password() == null) {
            throw new AuthenticationServiceException("Username and password must be provided");
        }

        var authRequest = new UsernamePasswordAuthenticationToken(
                credentials.username().trim(),
                credentials.password()
        );

        setDetails(request, authRequest);

        return this.getAuthenticationManager().authenticate(authRequest);
    }

    protected void setDetails(HttpServletRequest request, UsernamePasswordAuthenticationToken authRequest) {
        authRequest.setDetails(this.authenticationDetailsSource.buildDetails(request));
    }
}
