package com.erp.core.admin.web.dto;

/**
 * Options:
 *  - initialAdminUsername / initialAdminPassword are OPTIONAL. When both are
 *    provided, the newly created tenant gets a bootstrap admin user who can
 *    log in immediately. If left blank, only the creating super-admin is added
 *    as a member and they can create users after switching to the tenant.
 */
public record CreateTenantRequest(
        String code,
        String name,
        String subscriptionPlan,
        String initialAdminUsername,
        String initialAdminPassword
) {}