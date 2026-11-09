package com.erp.core.admin.web.controller;

import com.erp.core.admin.application.AdminUserService;
import com.erp.core.admin.web.dto.*;
import com.erp.core.security.internal.authentication.domain.PasswordPolicyViolation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/admin/users")
public class AdminUserController {

    private final AdminUserService service;

    public AdminUserController(AdminUserService service) {
        this.service = service;
    }

    @PreAuthorize("hasPermission('admin', 'ADMIN_USER', 'MANAGE')")
    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateUserRequest req) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
        } catch (PasswordPolicyViolation ex) {
            return ResponseEntity.badRequest().body(Map.of("code", "SEC-4001", "message", "Password policy violation"));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(Map.of("code", "SEC-4000", "message", ex.getMessage()));
        }
    }

    @PreAuthorize("hasPermission('admin', 'ADMIN_USER', 'MANAGE')")
    @GetMapping
    public PageResponse<UserSummary> list(@RequestParam UUID tenantId,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int size) {
        return service.list(tenantId, page, size);
    }

    @PreAuthorize("hasPermission('admin', 'ADMIN_USER', 'MANAGE')")
    @PatchMapping("/{actorId}/status")
    public ResponseEntity<Void> setEnabled(@PathVariable UUID actorId,
                                           @RequestParam UUID tenantId,
                                           @RequestBody SetEnabledRequest req) {
        service.setEnabled(actorId, tenantId, req.enabled());
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasPermission('admin', 'ADMIN_USER', 'MANAGE')")
    @PutMapping("/{actorId}")
    public ResponseEntity<Void> update(@PathVariable UUID actorId,
                                       @RequestParam UUID tenantId,
                                       @RequestBody UpdateUserRequest req) {
        try {
            service.update(actorId, tenantId, req);
            return ResponseEntity.ok().build();
        } catch (PasswordPolicyViolation | IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PreAuthorize("hasPermission('admin', 'ADMIN_USER', 'MANAGE')")
    @DeleteMapping("/{actorId}")
    public ResponseEntity<Void> delete(@PathVariable UUID actorId, @RequestParam UUID tenantId) {
        service.delete(actorId, tenantId);
        return ResponseEntity.noContent().build();
    }
}