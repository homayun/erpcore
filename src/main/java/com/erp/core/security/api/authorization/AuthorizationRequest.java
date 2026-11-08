package com.erp.core.security.api.authorization;

import com.erp.core.security.api.actor.CurrentActor;

import java.util.Map;

public record AuthorizationRequest(
        CurrentActor actor,
        ActionDescriptor action,
        ResourceDescriptor resource,
        Map<String, Object> attributes
) {
    public AuthorizationRequest {
        if (actor == null) {
            throw new IllegalArgumentException("Actor is required");
        }
        if (action == null) {
            throw new IllegalArgumentException("Action is required");
        }
        if (resource == null) {
            throw new IllegalArgumentException("Resource is required");
        }
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}