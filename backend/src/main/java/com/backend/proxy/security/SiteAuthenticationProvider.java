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
