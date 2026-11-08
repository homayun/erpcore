package com.erp.core.security.internal.admin.web.controller;

import com.erp.core.security.internal.admin.application.AdminUserService;
import com.erp.core.security.internal.admin.web.dto.CreateUserRequest;
import com.erp.core.security.internal.admin.web.dto.PageResponse;
import com.erp.core.security.internal.admin.web.dto.SetEnabledRequest;
import com.erp.core.security.internal.admin.web.dto.UserSummary;
import com.erp.core.security.internal.authentication.domain.PasswordPolicyViolation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/admin/users")
public class AdminUserController {

    private static final Logger log = LoggerFactory.getLogger(AdminUserController.class);

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
            return ResponseEntity.badRequest().body(Map.of(
                    "code", "SEC-4001",
                    "message", "Password does not satisfy policy",
                    "violations", ex.getViolations()));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(Map.of(
                    "code", "SEC-4000",
                    "message", ex.getMessage() == null ? "Bad request" : ex.getMessage()));
        } catch (RuntimeException ex) {
            log.error("Unexpected error creating user", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("code", "SEC-5000", "message", "Internal error"));
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
}