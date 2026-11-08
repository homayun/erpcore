package com.erp.core.security.internal.audit.adapter.out.slf4j;

import com.erp.core.security.api.audit.SecurityAuditEvent;
import com.erp.core.security.api.audit.SecurityAuditPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default audit sink â€” structured SLF4J log. Replace with DB/JMS adapter
 * for production persistence.
 */
public class Slf4jSecurityAuditAdapter implements SecurityAuditPort {

    private static final Logger log = LoggerFactory.getLogger("SECURITY_AUDIT");

    @Override
    public void record(SecurityAuditEvent event) {
        log.info("eventId={} action={} actorId={} tenantId={} severity={} timestamp={}",
                event.eventId(), event.action(), event.actorId(),
                event.tenantId(), event.severity(), event.timestamp());
    }
}