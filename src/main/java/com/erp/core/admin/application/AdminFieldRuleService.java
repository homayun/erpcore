package com.erp.core.admin.application;

import com.erp.core.admin.web.dto.CreateFieldRuleRequest;
import com.erp.core.admin.web.dto.FieldRuleSummary;
import com.erp.core.security.internal.authorization.domain.FieldRule;
import com.erp.core.security.internal.authorization.domain.PermissionDefinition;
import com.erp.core.security.internal.authorization.domain.PermissionKey;
import com.erp.core.security.internal.authorization.port.FieldRuleRepositoryPort;
import com.erp.core.security.internal.authorization.port.PermissionRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Admin field-rule CRUD.
 *
 * IMPORTANT: sec_field_rule.required_permission has an FK to sec_permission.
 * This service registers the permission on the fly if it doesn't exist yet,
 * because admins legitimately want to add a field rule for a permission
 * that a business module hasn't declared yet. This is idempotent â€” if the
 * permission already exists (declared by a module at startup), it's left
 * untouched.
 *
 * The FK remains as a guard: if the permission can't be registered for some
 * reason (validation error, DB issue), the insert fails, not silently accepted.
 */
public class AdminFieldRuleService {

    private static final Logger log = LoggerFactory.getLogger(AdminFieldRuleService.class);

    private final FieldRuleRepositoryPort fieldRuleRepository;
    private final PermissionRepositoryPort permissionRepository;

    public AdminFieldRuleService(FieldRuleRepositoryPort fieldRuleRepository,
                                 PermissionRepositoryPort permissionRepository) {
        this.fieldRuleRepository = fieldRuleRepository;
        this.permissionRepository = permissionRepository;
    }

    public FieldRuleSummary createOrUpdate(CreateFieldRuleRequest req) {
        if (req == null) throw new IllegalArgumentException("request is required");
        if (isBlank(req.resourceType())) throw new IllegalArgumentException("resourceType is required");
        if (isBlank(req.fieldName())) throw new IllegalArgumentException("fieldName is required");
        if (isBlank(req.requiredPermission())) throw new IllegalArgumentException("requiredPermission is required");
        if (isBlank(req.maskingWhenDenied())) throw new IllegalArgumentException("maskingWhenDenied is required");

        FieldRule.MaskingWhenDenied masking;
        try {
            masking = FieldRule.MaskingWhenDenied.valueOf(req.maskingWhenDenied().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "maskingWhenDenied must be one of HIDDEN, REDACT, PARTIAL, HASH");
        }

        String resourceType = req.resourceType().toUpperCase();
        PermissionKey permissionKey = PermissionKey.of(req.requiredPermission().toUpperCase());

        ensurePermissionExists(permissionKey, resourceType, req.fieldName());

        FieldRule rule = new FieldRule(resourceType, req.fieldName(), permissionKey, masking);
        return toSummary(fieldRuleRepository.upsert(rule));
    }

    public List<FieldRuleSummary> list() {
        return fieldRuleRepository.findAll().stream().map(this::toSummary).toList();
    }

    public List<FieldRuleSummary> listByResource(String resourceType) {
        return fieldRuleRepository.findByResourceType(resourceType.toUpperCase()).stream()
                .map(this::toSummary).toList();
    }

    public boolean delete(String resourceType, String fieldName) {
        return fieldRuleRepository.delete(resourceType.toUpperCase(), fieldName);
    }

    private void ensurePermissionExists(PermissionKey key, String resourceType, String fieldName) {
        if (permissionRepository.exists(key)) return;
        log.info("Auto-registering permission {} (referenced by field rule {}.{})",
                key, resourceType, fieldName);
        permissionRepository.upsertAll(List.of(new PermissionDefinition(
                key,
                "ADMIN",
                "Auto-registered: required by field rule for " + resourceType + "." + fieldName,
                true)));
    }

    private FieldRuleSummary toSummary(FieldRule rule) {
        return new FieldRuleSummary(
                rule.resourceType(),
                rule.fieldName(),
                rule.requiredPermission().value(),
                rule.maskingWhenDenied().name());
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
}