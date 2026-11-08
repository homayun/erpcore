package com.erp.core.security.api.event;

/**
 * Structured reason for an authentication failure.
 *
 * SECURITY NOTE:
 * Never surface these values verbatim to end-users in an HTTP response. The
 * HTTP layer MUST always respond with a generic "invalid credentials" message
 * to avoid leaking whether a username exists or whether an account is locked.
 * These reasons exist for internal audit, observability, and abuse-detection.
 */
public enum AuthenticationFailureReason {
    INVALID_CREDENTIALS,
    ACCOUNT_LOCKED,
    ACCOUNT_DISABLED
}