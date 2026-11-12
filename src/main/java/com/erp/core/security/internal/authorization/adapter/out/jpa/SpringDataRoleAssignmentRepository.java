package com.erp.core.security.internal.authorization.adapter.out.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SpringDataRoleAssignmentRepository extends JpaRepository<RoleAssignmentEntity, UUID> {

    boolean existsByActorIdAndRoleId(UUID actorId, UUID roleId);

    @Query("SELECT ra.roleId FROM RoleAssignmentEntity ra " +
           "WHERE ra.actorId = :actorId AND ra.tenantId = :tenantId")
    List<UUID> findRoleIdsByActorAndTenant(@Param("actorId") UUID actorId,
                                           @Param("tenantId") UUID tenantId);

    @Modifying
    @Query("DELETE FROM RoleAssignmentEntity r WHERE r.actorId = :actorId")
    void deleteByActorId(@Param("actorId") UUID actorId);

    @Modifying
    @Query("DELETE FROM RoleAssignmentEntity r WHERE r.roleId = :roleId")
    void deleteByRoleId(@Param("roleId") UUID roleId);
}