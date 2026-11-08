package com.erp.core.security.internal.authorization.adapter.out.jpa;

import com.erp.core.security.internal.authorization.domain.FieldRule;
import com.erp.core.security.internal.authorization.domain.PermissionKey;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "sec_field_rule")
public class FieldRuleEntity {

    @Id
    private UUID id;

    @Column(name = "resource_type", nullable = false, length = 100)
    private String resourceType;

    @Column(name = "field_name", nullable = false, length = 100)
    private String fieldName;

    @Column(name = "required_permission", nullable = false, length = 100)
    private String requiredPermission;

    @Column(name = "masking_when_denied", nullable = false, length = 30)
    private String maskingWhenDenied;

    protected FieldRuleEntity() {}

    public static FieldRuleEntity fromDomain(FieldRule rule) {
        FieldRuleEntity e = new FieldRuleEntity();
        e.id = UUID.randomUUID();
        e.resourceType = rule.resourceType();
        e.fieldName = rule.fieldName();
        e.requiredPermission = rule.requiredPermission().value();
        e.maskingWhenDenied = rule.maskingWhenDenied().name();
        return e;
    }

    public void applyUpdate(FieldRule rule) {
        this.requiredPermission = rule.requiredPermission().value();
        this.maskingWhenDenied = rule.maskingWhenDenied().name();
    }

    public FieldRule toDomain() {
        return new FieldRule(
                resourceType,
                fieldName,
                PermissionKey.of(requiredPermission),
                FieldRule.MaskingWhenDenied.valueOf(maskingWhenDenied));
    }

    public UUID getId() { return id; }
    public String getResourceType() { return resourceType; }
    public String getFieldName() { return fieldName; }
}