package com.erp.core.security.api.authorization;

import com.erp.core.security.api.actor.CurrentActor;

import java.util.Set;

public record FieldAccessRequest(
        CurrentActor actor,
        ResourceDescriptor resource,
        Set<String> requestedFields,
        FieldAccessMode mode
) {
    public FieldAccessRequest {
        if (actor == null || resource == null) {
            throw new IllegalArgumentException("Actor and resource are required");
        }
        if (requestedFields == null || requestedFields.isEmpty()) {
            throw new IllegalArgumentException("At least one field must be requested");
        }
        requestedFields = Set.copyOf(requestedFields);
        if (mode == null) {
            throw new IllegalArgumentException("Access mode is required");
        }
    }

    public enum FieldAccessMode { READ, WRITE }
}