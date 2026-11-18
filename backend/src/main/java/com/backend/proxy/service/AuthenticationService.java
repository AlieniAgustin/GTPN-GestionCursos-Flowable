package com.backend.proxy.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.backend.proxy.dto.auth.LoginResponseDTO;
import com.backend.proxy.security.Roles;

import lombok.RequiredArgsConstructor;

/**
 * Valida credenciales y resuelve roles para el personal de la institución y empresas.
 *
 * <p>Capa de servicio pura: agnóstica de Servlets y sesiones HTTP. La persistencia
 * de sesión y manejo de cookies es responsabilidad exclusiva de la cadena de filtros de seguridad.
 */
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final FlowableAuthService flowableAuthService;
    private final AccessService accessService;

    public Optional<LoginResponseDTO> authenticate(String username, String password) {
        return resolveRole(username, password)
                .map(role -> new LoginResponseDTO(
                        username,
                        role.startsWith("ROLE_") ? role.substring("ROLE_".length()) : role,
                        true
                ));
    }

    public Optional<String> resolveRole(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return Optional.empty();
        }

        if (flowableAuthService.authenticates(username, password)) {
            return Optional.of(Roles.ROLE_INSTITUCION);
        }

        if (authenticatesAsCompany(username, password)) {
            return Optional.of(Roles.ROLE_EMPRESA);
        }

        return Optional.empty();
    }

    private boolean authenticatesAsCompany(String username, String password) {
        return accessService.validateCredentials(username, password);
    }
}
