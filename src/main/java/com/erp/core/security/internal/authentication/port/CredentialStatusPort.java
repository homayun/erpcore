package com.erp.core.security.internal.authentication.port;

import java.util.Optional;
import java.util.UUID;

/**
 * Read-only view of an account's current state. Used on login and on token
 * refresh so a disabled / deleted account cannot keep minting tokens.
 */
public interface CredentialStatusPort {

    Optional<CredentialStatus> find(UUID actorId, UUID tenantId);

    record CredentialStatus(String username, boolean enabled) {}
}