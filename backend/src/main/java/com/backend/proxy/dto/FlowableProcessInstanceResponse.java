package com.backend.proxy.dto;

public record FlowableProcessInstanceResponse(
        String id,
        String url,
        String businessKey,
        boolean suspended,
        boolean ended,
        String processDefinitionId
) {}
