package com.erp.core.admin.web.controller;

import com.erp.core.admin.application.AdminFieldRuleService;
import com.erp.core.admin.web.dto.CreateFieldRuleRequest;
import com.erp.core.admin.web.dto.FieldRuleSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/field-rules")
public class AdminFieldRuleController {

    private static final Logger log = LoggerFactory.getLogger(AdminFieldRuleController.class);

    private final AdminFieldRuleService service;

    public AdminFieldRuleController(AdminFieldRuleService service) {
        this.service = service;
    }

    @PreAuthorize("hasPermission('admin', 'ADMIN_FIELD_RULE', 'MANAGE')")
    @PostMapping
    public ResponseEntity<?> createOrUpdate(@RequestBody CreateFieldRuleRequest req) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(service.createOrUpdate(req));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of(
                    "code", "SEC-4000",
                    "message", ex.getMessage() == null ? "Bad request" : ex.getMessage()));
        } catch (RuntimeException ex) {
            log.error("Error creating field rule", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("code", "SEC-5000", "message", "Internal error"));
        }
    }

    @PreAuthorize("hasPermission('admin', 'ADMIN_FIELD_RULE', 'MANAGE')")
    @GetMapping
    public List<FieldRuleSummary> list(@RequestParam(required = false) String resourceType) {
        return resourceType == null ? service.list() : service.listByResource(resourceType);
    }

    @PreAuthorize("hasPermission('admin', 'ADMIN_FIELD_RULE', 'MANAGE')")
    @DeleteMapping("/{resourceType}/{fieldName}")
    public ResponseEntity<Void> delete(@PathVariable String resourceType, @PathVariable String fieldName) {
        boolean deleted = service.delete(resourceType, fieldName);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}