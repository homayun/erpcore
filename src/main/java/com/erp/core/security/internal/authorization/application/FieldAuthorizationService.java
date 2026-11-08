package com.erp.core.security.internal.authorization.application;

import com.erp.core.security.api.authorization.FieldAccessDecision;
import com.erp.core.security.api.authorization.FieldAccessRequest;
import com.erp.core.security.api.authorization.FieldAccessDecision.MaskingStrategy;
import com.erp.core.security.internal.authorization.domain.FieldRule;
import com.erp.core.security.internal.authorization.domain.PermissionKey;
import com.erp.core.security.internal.authorization.port.FieldRuleRepositoryPort;
import com.erp.core.security.internal.authorization.port.RoleRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class FieldAuthorizationService {

    private static final Logger log = LoggerFactory.getLogger(FieldAuthorizationService.class);

    private final FieldRuleRepositoryPort fieldRuleRepository;
    private final RoleRepositoryPort roleRepository;

    public FieldAuthorizationService(FieldRuleRepositoryPort fieldRuleRepository,
                                     RoleRepositoryPort roleRepository) {
        this.fieldRuleRepository = fieldRuleRepository;
        this.roleRepository = roleRepository;
    }

    public FieldAccessDecision decide(FieldAccessRequest request, Set<PermissionKey> effectivePermissions) {
        if (request == null || request.actor() == null) {
            return FieldAccessDecision.denyAll();
        }

        Map<String, FieldRule> rulesByField = new HashMap<>();
        for (FieldRule r : fieldRuleRepository.findByResourceType(request.resource().type())) {
            rulesByField.put(r.fieldName(), r);
        }

        Set<String> granted = new java.util.HashSet<>();
        Map<String, MaskingStrategy> maskings = new HashMap<>();
        boolean isWrite = request.mode() == FieldAccessRequest.FieldAccessMode.WRITE;

        for (String field : request.requestedFields()) {
            FieldRule rule = rulesByField.get(field);

            if (rule == null) {
                if (isWrite) {
                    PermissionKey updateKey = PermissionKey.of(request.resource().type() + "_UPDATE");
                    if (effectivePermissions.contains(updateKey)) {
                        granted.add(field);
                    }
                } else {
                    granted.add(field);
                }
                continue;
            }

            if (effectivePermissions.contains(rule.requiredPermission())) {
                granted.add(field);
                maskings.put(field, MaskingStrategy.FULL);
            } else {
                granted.add(field);
                maskings.put(field, toApiMasking(rule.maskingWhenDenied()));
                log.debug("Field {} on {} masked (missing {})",
                        field, request.resource().type(), rule.requiredPermission());
            }
        }

        return new FieldAccessDecision(granted, maskings);
    }

    private MaskingStrategy toApiMasking(FieldRule.MaskingWhenDenied m) {
        return switch (m) {
            case HIDDEN -> MaskingStrategy.REDACT;
            case REDACT -> MaskingStrategy.REDACT;
            case PARTIAL -> MaskingStrategy.PARTIAL;
            case HASH -> MaskingStrategy.HASH;
        };
    }
}