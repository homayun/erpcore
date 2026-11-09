package com.erp.core.admin.application;

import com.erp.core.security.internal.authorization.adapter.out.jpa.PermissionEntity;
import com.erp.core.security.internal.authorization.adapter.out.jpa.SpringDataPermissionRepository;
import com.erp.core.security.internal.authorization.domain.PermissionDefinition;
import com.erp.core.security.internal.authorization.domain.PermissionKey;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public class AdminPermissionService {

    private final SpringDataPermissionRepository repo;

    public AdminPermissionService(SpringDataPermissionRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<PermissionEntity> list(String filter) {
        List<PermissionEntity> all = repo.findAll();
        if ("active".equals(filter)) {
            return all.stream().filter(PermissionEntity::isActive).toList();
        }
        if ("deleted".equals(filter)) {
            return all.stream().filter(e -> !e.isActive()).toList();
        }
        return all;
    }

    @Transactional(readOnly = true)
    public PermissionEntity getById(String key) {
        return repo.findById(key)
                .orElseThrow(() -> new IllegalArgumentException("Permission not found: " + key));
    }

    @Transactional
    public PermissionEntity create(String key, String group, String description, boolean sensitive) {
        PermissionKey permissionKey = PermissionKey.of(key);
        if (repo.existsById(permissionKey.value())) {
            throw new IllegalStateException("Permission key already exists: " + key);
        }
        PermissionEntity e = PermissionEntity.fromDomain(
                new PermissionDefinition(permissionKey, group, description, sensitive));
        return repo.save(e);
    }

    @Transactional
    public PermissionEntity update(String key, String group, String description, boolean sensitive) {
        PermissionEntity e = repo.findById(key)
                .orElseThrow(() -> new IllegalArgumentException("Permission not found: " + key));
        e.setGroupKey(group);
        e.setDescription(description);
        e.setSensitive(sensitive);
        return repo.save(e);
    }

    @Transactional
    public void softDelete(String key) {
        PermissionEntity e = repo.findById(key)
                .orElseThrow(() -> new IllegalArgumentException("Permission not found: " + key));
        e.setActive(false);
        repo.save(e);
    }

    @Transactional
    public void restore(String key) {
        PermissionEntity e = repo.findById(key)
                .orElseThrow(() -> new IllegalArgumentException("Permission not found: " + key));
        e.setActive(true);
        repo.save(e);
    }

    @Transactional
    public void hardDelete(String key) {
        if (!repo.existsById(key)) {
            throw new IllegalArgumentException("Permission not found: " + key);
        }
        repo.deleteById(key);
    }
}