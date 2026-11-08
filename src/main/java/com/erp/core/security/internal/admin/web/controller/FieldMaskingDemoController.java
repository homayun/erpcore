package com.erp.core.security.internal.admin.web.controller;

import com.erp.core.security.api.actor.CurrentActor;
import com.erp.core.security.api.actor.CurrentActorProvider;
import com.erp.core.security.api.authorization.AuthorizationPort;
import com.erp.core.security.api.authorization.FieldAccessDecision;
import com.erp.core.security.api.authorization.FieldAccessRequest;
import com.erp.core.security.api.authorization.ResourceDescriptor;
import com.erp.core.security.internal.authorization.application.FieldMaskingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Demo endpoint proving field-level authorization (BOLA prevention) in action.
 *
 * Without a rule for (INVOICE, amount), all 3 fields are returned in plaintext.
 * After POST /admin/field-rules with (INVOICE, amount, VIEW_INVOICE_AMOUNT, REDACT),
 * the amount value is masked because the caller lacks VIEW_INVOICE_AMOUNT.
 */
@RestController
@RequestMapping("/admin/demo")
public class FieldMaskingDemoController {

    private final CurrentActorProvider actorProvider;
    private final AuthorizationPort authorizationPort;
    private final FieldMaskingService maskingService;

    public FieldMaskingDemoController(CurrentActorProvider actorProvider,
                                       AuthorizationPort authorizationPort,
                                       FieldMaskingService maskingService) {
        this.actorProvider = actorProvider;
        this.authorizationPort = authorizationPort;
        this.maskingService = maskingService;
    }

    @GetMapping("/invoice")
    public Map<String, Object> demoInvoice() {
        CurrentActor actor = actorProvider.getCurrentActor();
        Map<String, Object> response = new LinkedHashMap<>();
        if (actor == null || !actor.isAuthenticated()) {
            response.put("error", "unauthenticated");
            return response;
        }

        Map<String, Object> source = new LinkedHashMap<>();
        source.put("invoiceNo", "INV-2026-0001");
        source.put("amount", 50000);
        source.put("customerName", "Acme Corp");

        FieldAccessRequest req = new FieldAccessRequest(
                actor,
                new ResourceDescriptor("INVOICE", "INV-2026-0001", actor.tenantId()),
                Set.copyOf(source.keySet()),
                FieldAccessRequest.FieldAccessMode.READ);

        FieldAccessDecision decision = authorizationPort.authorizeFieldAccess(req);

        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String field = entry.getKey();
            if (!decision.grantedFields().contains(field)) continue;
            Object masked = maskingService.maskForApi(entry.getValue(), decision.maskings().get(field));
            if (masked == FieldMaskingService.HIDDEN) continue;
            response.put(field, masked);
        }
        return response;
    }
}