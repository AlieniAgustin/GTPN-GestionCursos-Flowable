package com.backend.proxy.security;

import java.util.List;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import com.backend.proxy.service.AuthenticationService;

/**
 * Autentica el formulario de login del sitio (enunciado 4.1).
 *
 * <p>Delega en {@link AuthenticationService#resolveRole} para no duplicar la decisión de
 * quién puede entrar: el personal de la institución se valida contra el IDM de Flowable y
 * las empresas contra la cuenta que les creó el proceso.
 */
@Component
public class SiteAuthenticationProvider implements AuthenticationProvider {

    private final AuthenticationService authenticationService;

    public SiteAuthenticationProvider(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        String password = authentication.getCredentials() != null
                ? authentication.getCredentials().toString()
                : "";

        String role = authenticationService.resolveRole(username, password)
                .orElseThrow(() -> new BadCredentialsException("Usuario o contraseña incorrectos"));

        return new UsernamePasswordAuthenticationToken(
                username,
                null,
                List.of(new SimpleGrantedAuthority(role)));
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
