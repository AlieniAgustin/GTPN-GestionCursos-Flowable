package com.backend.proxy.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.backend.proxy.dto.auth.LoginRequestDTO;
import com.backend.proxy.dto.auth.LoginResponseDTO;
import com.backend.proxy.security.Roles;
import com.backend.proxy.service.AuthenticationService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/**
 * Sesion del sitio (enunciado 4.1). El mismo endpoint sirve al personal de la
 * institucion, que se valida contra el IDM de Flowable, y a las empresas, que usan la
 * cuenta creada por "Habilitar acceso de la empresa".
 *
 * <p>Al autenticarse queda el SecurityContext en la HttpSession, asi que las llamadas
 * siguientes solo necesitan la cookie JSESSIONID.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationService authenticationService;

    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(
            @Valid @RequestBody LoginRequestDTO credentials,
            HttpServletRequest request
    ) {
        return authenticationService.login(credentials.username(), credentials.password(), request)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        authenticationService.logout(request);
        return ResponseEntity.noContent().build();
    }

    /**
     * Permite al cliente comprobar si su sesion sigue viva, y con que rol, sin tener que
     * pegarle a un endpoint de negocio.
     */
    @GetMapping("/me")
    public ResponseEntity<LoginResponseDTO> me(Authentication authentication) {
        String role = authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring("ROLE_".length()))
                .findFirst()
                .orElse(Roles.INSTITUCION);

        return ResponseEntity.ok(new LoginResponseDTO(authentication.getName(), role, true));
    }
}
