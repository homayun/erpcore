package com.erp.core.security.internal.authorization.adapter.out.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SpringDataUserPermissionRepository
        extends JpaRepository<UserPermissionEntity, UserPermissionEntity.Key> {

    @Query("SELECT up.permissionKey FROM UserPermissionEntity up " +
           "WHERE up.actorId = :actorId AND up.tenantId = :tenantId")
    List<String> findPermissionKeysByActor(@Param("actorId") UUID actorId,
                                           @Param("tenantId") UUID tenantId);

    @Modifying
    @Query("DELETE FROM UserPermissionEntity up WHERE up.actorId = :actorId")
    void deleteByActorId(@Param("actorId") UUID actorId);
}