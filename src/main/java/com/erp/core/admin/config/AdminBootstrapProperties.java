package com.erp.core.admin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.UUID;

@ConfigurationProperties(prefix = "erp.security.admin.bootstrap")
public record AdminBootstrapProperties(
        boolean enabled,
        UUID tenantId,
        String username,
        String password
) {}