package com.backend.proxy.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.backend.proxy.dto.auth.LoginRequestDTO;
import com.backend.proxy.dto.auth.LoginResponseDTO;
import com.backend.proxy.service.FlowableAuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/**
 * Session endpoints for institutional staff. Credentials are validated against the
 * Flowable IDM; on success the Spring Security context is stored in the HttpSession,
 * so subsequent calls only need the JSESSIONID cookie.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final FlowableAuthService authService;

    public AuthController(FlowableAuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(
            @Valid @RequestBody LoginRequestDTO credentials,
            HttpServletRequest request
    ) {
        boolean authenticated = authService.login(credentials.username(), credentials.password(), request);

        if (!authenticated) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(new LoginResponseDTO(credentials.username(), true));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }

    /**
     * Lets a client check whether its session is still valid without hitting a business endpoint.
     */
    @GetMapping("/me")
    public ResponseEntity<LoginResponseDTO> me(@AuthenticationPrincipal String username) {
        return ResponseEntity.ok(new LoginResponseDTO(username, true));
    }
}
