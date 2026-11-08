package com.erp.core.security.api.audit;

public interface SecurityAuditPort {
    void record(SecurityAuditEvent event);
}