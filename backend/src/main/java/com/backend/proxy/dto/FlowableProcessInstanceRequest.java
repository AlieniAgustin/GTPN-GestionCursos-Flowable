package com.backend.proxy.dto;

import java.util.List;

public record FlowableProcessInstanceRequest(
        String processDefinitionKey,
        String businessKey,
        List<FlowableVariable> variables
) {}
