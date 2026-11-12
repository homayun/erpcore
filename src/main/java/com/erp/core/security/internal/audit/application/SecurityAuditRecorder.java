package com.erp.core.security.internal.audit.application;

import com.erp.core.security.api.audit.SecurityAuditEvent;
import com.erp.core.security.api.audit.SecurityAuditPort;
import com.erp.core.security.api.audit.SecurityAuditSeverity;
import com.erp.core.security.internal.tenant.event.TenantSwitchedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.LogoutSuccessEvent;

import java.time.Clock;
import java.util.UUID;

public class SecurityAuditRecorder {

    private static final Logger log = LoggerFactory.getLogger(SecurityAuditRecorder.class);

    private final SecurityAuditPort auditPort;
    private final Clock clock;

    public SecurityAuditRecorder(SecurityAuditPort auditPort, Clock clock) {
        this.auditPort = auditPort;
        this.clock = clock;
    }

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        emit("AUTH_SUCCESS", null, null, SecurityAuditSeverity.INFO,
                event.getAuthentication().getName());
    }

    @EventListener
    public void onFailure(AbstractAuthenticationFailureEvent event) {
        emit("AUTH_FAILURE", null, null, SecurityAuditSeverity.WARN,
                event.getException().getClass().getSimpleName());
    }

    @EventListener
    public void onLogout(LogoutSuccessEvent event) {
        emit("AUTH_LOGOUT", null, null, SecurityAuditSeverity.INFO,
                event.getAuthentication() != null ? event.getAuthentication().getName() : "anonymous");
    }

    @EventListener
    public void onTenantSwitch(TenantSwitchedEvent event) {
        emit("TENANT_SWITCH", event.actorId(), event.toTenant(), SecurityAuditSeverity.INFO,
                event.fromTenant() + "->" + event.toTenant());
    }

    private void emit(String action, UUID actorId, UUID tenantId,
                      SecurityAuditSeverity severity, String detail) {
        SecurityAuditEvent auditEvent = new SecurityAuditEvent(
                UUID.randomUUID(), actorId, tenantId,
                action + ":" + detail, severity, clock.instant());
        auditPort.record(auditEvent);
        log.info("security.audit {} actor={} tenant={}", action, actorId, tenantId);
    }
}