package com.flowable.app;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;

import org.flowable.idm.api.Group;

import java.util.List;
import org.flowable.engine.IdentityService;

@RestController
public class Health {
    private final IdentityService identityService;

    Health(IdentityService identityService) {
        this.identityService = identityService;
    }

    @GetMapping("/health")
    public String health() {
        return "UP";
    }

    @GetMapping("groups")
    public HashMap<String,Object> getGroups() {
        HashMap<String, Object> response = new LinkedHashMap<>();

        List<Group> groups = identityService.createGroupQuery().list();
        List<String> groupsNames = new LinkedList<>();
        // get groups
        groups.forEach(group -> groupsNames.add(group.getName()));
        // extract names and quantity for groups
        response.put("groups", groupsNames);
        response.put("quantityGroups", groupsNames.size());

        return response;
    }
}