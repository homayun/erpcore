package com.erp.core.security.internal.tenant.adapter.out.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SpringDataTenantMembershipRepository
        extends JpaRepository<TenantMembershipEntity, TenantMembershipEntity.Key> {

    @Query("SELECT m.tenantId FROM TenantMembershipEntity m WHERE m.actorId = :actorId")
    List<UUID> findTenantIdsByActor(@Param("actorId") UUID actorId);

    boolean existsByActorIdAndTenantId(UUID actorId, UUID tenantId);

    @Modifying
    @Query("DELETE FROM TenantMembershipEntity m " +
           "WHERE m.actorId = :actorId AND m.tenantId = :tenantId")
    void deleteByActorIdAndTenantId(@Param("actorId") UUID actorId,
                                    @Param("tenantId") UUID tenantId);
}