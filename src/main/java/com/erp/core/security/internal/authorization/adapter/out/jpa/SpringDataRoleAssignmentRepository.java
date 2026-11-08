package com.erp.core.security.internal.authorization.adapter.out.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataRoleAssignmentRepository
        extends JpaRepository<RoleAssignmentEntity, UUID> {

    boolean existsByActorIdAndRoleId(UUID actorId, UUID roleId);
}