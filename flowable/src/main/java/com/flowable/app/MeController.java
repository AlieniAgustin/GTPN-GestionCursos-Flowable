package com.flowable.app;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.flowable.engine.IdentityService;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@RestController
public class MeController {

    private final IdentityService identityService;

    public MeController(IdentityService identityService) {
        this.identityService = identityService;
    }

    @GetMapping("/me")
    public Map<String, Object> getMe(Authentication authentication, HttpServletRequest request) {
        Map<String, Object> response = new LinkedHashMap<>();

        if (authentication == null) {
            response.put("authenticated", false);
            return response;
        }

        String username = authentication.getName();
        response.put("authenticated", authentication.isAuthenticated());
        response.put("username", username);

        List<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        response.put("authorities", authorities);

        List<Group> userGroups = identityService.createGroupQuery().groupMember(username).list();
        List<String> groups = userGroups.stream()
                .map(Group::getId)
                .collect(Collectors.toList());
        response.put("groups", groups);

        User user = identityService.createUserQuery().userId(username).singleResult();
        if (user != null) {
            Map<String, Object> userDetails = new LinkedHashMap<>();
            userDetails.put("id", user.getId());
            userDetails.put("firstName", user.getFirstName());
            userDetails.put("lastName", user.getLastName());
            userDetails.put("email", user.getEmail());
            response.put("user", userDetails);
        }

        HttpSession session = request.getSession(false);
        if (session != null) {
            Map<String, Object> sessionInfo = new LinkedHashMap<>();
            sessionInfo.put("id", session.getId());
            sessionInfo.put("creationTime", session.getCreationTime());
            sessionInfo.put("lastAccessedTime", session.getLastAccessedTime());
            sessionInfo.put("maxInactiveInterval", session.getMaxInactiveInterval());
            response.put("session", sessionInfo);
        }

        return response;
    }
}
