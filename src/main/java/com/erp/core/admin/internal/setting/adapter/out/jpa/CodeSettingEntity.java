package com.erp.core.admin.internal.setting.adapter.out.jpa;
import com.erp.core.admin.internal.setting.domain.CodeSetting;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "admin_code_setting")
public class CodeSettingEntity {
    @Id private UUID id;
    @Column(name = "tenant_id") private UUID tenantId;
    private String category;
    private String code;
    private String name;
    @Column(name = "is_active") private boolean isActive;
    @Column(name = "created_at") private Instant createdAt;

    public CodeSettingEntity() {}
    public static CodeSettingEntity fromDomain(CodeSetting d) {
        CodeSettingEntity e = new CodeSettingEntity();
        e.id = d.id(); e.tenantId = d.tenantId(); e.category = d.category(); e.code = d.code(); e.name = d.name(); e.isActive = d.isActive(); e.createdAt = d.createdAt();
        return e;
    }
    public void update(String category, String code, String name) { this.category = category; this.code = code; this.name = name; }
    public void setActive(boolean active) { this.isActive = active; }
    public boolean isActive() { return isActive; }
    public CodeSetting toDomain() { return new CodeSetting(id, tenantId, category, code, name, isActive, createdAt); }
}