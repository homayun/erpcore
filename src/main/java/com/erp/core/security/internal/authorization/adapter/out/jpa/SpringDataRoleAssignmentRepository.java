package com.erp.core.security.internal.authorization.adapter.out.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface SpringDataRoleAssignmentRepository extends JpaRepository<RoleAssignmentEntity, UUID> {
    boolean existsByActorIdAndRoleId(UUID actorId, UUID roleId);

    @Modifying
    @Query("DELETE FROM RoleAssignmentEntity r WHERE r.actorId = :actorId")
    void deleteByActorId(@Param("actorId") UUID actorId);

    @Modifying
    @Query("DELETE FROM RoleAssignmentEntity r WHERE r.roleId = :roleId")
    void deleteByRoleId(@Param("roleId") UUID roleId);
}