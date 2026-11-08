package com.erp.core.security.internal.authorization.adapter.out.jpa;

import com.erp.core.security.internal.authorization.domain.AttributeCondition;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "sec_attribute_condition")
public class AttributeConditionEntity {

    @Id
    private UUID id;

    @Column(name = "permission_key", nullable = false, length = 100)
    private String permissionKey;

    @Column(name = "attribute_name", nullable = false, length = 100)
    private String attributeName;

    @Column(name = "operator", nullable = false, length = 30)
    private String operator;

    @Column(name = "condition_value", nullable = false, length = 500)
    private String value;

    protected AttributeConditionEntity() {}

    public AttributeCondition toDomain() {
        return new AttributeCondition(
                attributeName,
                AttributeCondition.Operator.valueOf(operator),
                value);
    }

    public String getPermissionKey() { return permissionKey; }
}