package com.erp.core.security.internal.authorization.adapter.out.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SpringDataRolePermissionRepository
        extends JpaRepository<RolePermissionEntity, RolePermissionEntity.Key> {

    @Query("SELECT rp.permissionKey FROM RolePermissionEntity rp " +
           "WHERE rp.roleId IN " +
           "(SELECT ra.roleId FROM RoleAssignmentEntity ra " +
           " WHERE ra.actorId = :actorId AND ra.tenantId = :tenantId)")
    List<String> findPermissionKeysByActorAndTenant(
            @Param("actorId") UUID actorId, @Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(rp) > 0 FROM RolePermissionEntity rp " +
           "WHERE rp.roleId = :roleId AND rp.permissionKey = :permissionKey")
    boolean existsByRoleIdAndPermissionKey(
            @Param("roleId") UUID roleId, @Param("permissionKey") String permissionKey);

    @Modifying
    @Query("DELETE FROM RolePermissionEntity rp " +
           "WHERE rp.roleId = :roleId AND rp.permissionKey = :permissionKey")
    int deleteByRoleIdAndPermissionKey(
            @Param("roleId") UUID roleId, @Param("permissionKey") String permissionKey);

    @Modifying
    @Query("DELETE FROM RolePermissionEntity rp WHERE rp.roleId = :roleId")
    void deleteByRoleId(@Param("roleId") UUID roleId);
}