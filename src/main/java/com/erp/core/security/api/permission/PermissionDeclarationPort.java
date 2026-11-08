package com.erp.core.security.api.permission;

import java.util.List;

public interface PermissionDeclarationPort {
    void registerPermissions(List<PermissionDeclaration> declarations);
}