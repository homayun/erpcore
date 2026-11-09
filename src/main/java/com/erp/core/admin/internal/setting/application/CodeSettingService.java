package com.erp.core.admin.internal.setting.application;
import com.erp.core.admin.internal.setting.adapter.out.jpa.*;
import com.erp.core.admin.internal.setting.domain.CodeSetting;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class CodeSettingService {
    private final CodeSettingRepository repo;
    public CodeSettingService(CodeSettingRepository repo) { this.repo = repo; }

    @Transactional(readOnly = true)
    public List<CodeSetting> getAll(UUID tenantId, String filter) {
        List<CodeSettingEntity> list = repo.findByTenantId(tenantId);
        if ("active".equals(filter)) return list.stream().filter(CodeSettingEntity::isActive).map(CodeSettingEntity::toDomain).toList();
        if ("deleted".equals(filter)) return list.stream().filter(e -> !e.isActive()).map(CodeSettingEntity::toDomain).toList();
        return list.stream().map(CodeSettingEntity::toDomain).toList();
    }
    @Transactional(readOnly = true)
    public CodeSetting getById(UUID id) { return repo.findById(id).map(CodeSettingEntity::toDomain).orElseThrow(() -> new IllegalArgumentException("Not found")); }

    @Transactional
    public CodeSetting create(UUID tenantId, String category, String code, String name) {
        CodeSetting c = new CodeSetting(UUID.randomUUID(), tenantId, category, code, name, true, Instant.now());
        repo.save(CodeSettingEntity.fromDomain(c)); return c;
    }
    @Transactional
    public CodeSetting update(UUID id, String category, String code, String name) {
        CodeSettingEntity e = repo.findById(id).orElseThrow(); e.update(category, code, name); repo.save(e); return e.toDomain();
    }
    @Transactional
    public void softDelete(UUID id) { repo.findById(id).ifPresent(e -> { e.setActive(false); repo.save(e); }); }
    @Transactional
    public void restore(UUID id) { repo.findById(id).ifPresent(e -> { e.setActive(true); repo.save(e); }); }
    @Transactional
    public void hardDelete(UUID id) { repo.deleteById(id); }
}