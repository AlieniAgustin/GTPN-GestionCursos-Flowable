package com.flowable.app.security;

import org.flowable.engine.IdentityService;
import org.flowable.idm.api.Group;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class FlowableAuthenticationProvider implements AuthenticationProvider {

    private final IdentityService identityService;

    public FlowableAuthenticationProvider(IdentityService identityService) {
        this.identityService = identityService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        String password = authentication.getCredentials() != null ? authentication.getCredentials().toString() : "";
        if (!identityService.checkPassword(username, password)) {
            throw new BadCredentialsException("Invalid credentials for user: " + username);
        } else {
            System.out.println("good");
        }

        List<Group> groups = identityService.createGroupQuery().groupMember(username).list();
        List<SimpleGrantedAuthority> authorities = groups.stream()
                .map(group -> new SimpleGrantedAuthority("ROLE_" + group.getId().toUpperCase()))
                .collect(Collectors.toList());

        return new UsernamePasswordAuthenticationToken(username, null, authorities);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
