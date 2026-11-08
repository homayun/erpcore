package com.erp.core.security.internal.data.application;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantContextHolderTest {

    @AfterEach
    void cleanup() {
        TenantContextHolder.clear();
    }

    @Test
    void bindStoresTenant() {
        UUID tenantId = UUID.randomUUID();
        TenantContextHolder.bind(tenantId);
        assertThat(TenantContextHolder.current()).contains(tenantId);
    }

    @Test
    void clearRemovesTenant() {
        TenantContextHolder.bind(UUID.randomUUID());
        TenantContextHolder.clear();
        assertThat(TenantContextHolder.current()).isEmpty();
    }

    @Test
    void bindRejectsNull() {
        assertThatThrownBy(() -> TenantContextHolder.bind(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tenantId");
    }

    @Test
    void currentIsEmptyByDefault() {
        assertThat(TenantContextHolder.current()).isEmpty();
    }
}