package com.backend.proxy.service;

import java.util.List;
import java.util.Optional;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;

import com.backend.proxy.dto.auth.LoginResponseDTO;
import com.backend.proxy.entity.Company;
import com.backend.proxy.repository.CompanyRepository;
import com.backend.proxy.security.Roles;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

/**
 * Abre y cierra la sesion del sitio (enunciado 4.1).
 *
 * <p>Acepta los dos tipos de usuario por el mismo endpoint: primero prueba contra el IDM
 * de Flowable, que es donde viven las cuentas del personal de la institucion, y si no
 * valida prueba contra las cuentas de empresa de la BD de la institucion. El rol que
 * queda en la sesion es lo que despues decide que cursos ve cada uno.
 */
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final FlowableAuthService flowableAuthService;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;

    public Optional<LoginResponseDTO> login(String username, String password, HttpServletRequest request) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return Optional.empty();
        }

        if (flowableAuthService.authenticates(username, password)) {
            startSession(username, Roles.ROLE_INSTITUCION, request);
            return Optional.of(new LoginResponseDTO(username, Roles.INSTITUCION, true));
        }

        if (authenticatesAsCompany(username, password)) {
            startSession(username, Roles.ROLE_EMPRESA, request);
            return Optional.of(new LoginResponseDTO(username, Roles.EMPRESA, true));
        }

        return Optional.empty();
    }

    public void logout(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    private boolean authenticatesAsCompany(String username, String password) {
        Optional<Company> company = companyRepository.findByAccessUsername(username);
        if (company.isEmpty()) {
            return false;
        }
        String hash = company.get().getAccessPasswordHash();
        return hash != null && passwordEncoder.matches(password, hash);
    }

    /**
     * Deja el SecurityContext en la HttpSession, que es de donde lo lee la cadena de
     * filtros en las llamadas siguientes: al cliente le alcanza con la cookie JSESSIONID.
     */
    private void startSession(String username, String role, HttpServletRequest request) {
        var authentication = new UsernamePasswordAuthenticationToken(
                username,
                null,
                List.of(new SimpleGrantedAuthority(role))
        );

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        HttpSession session = request.getSession(true);
        session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                securityContext
        );
    }
}
