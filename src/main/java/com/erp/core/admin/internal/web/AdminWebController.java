package com.erp.core.admin.internal.web;

import com.erp.core.admin.application.AdminRoleService;
import com.erp.core.admin.application.AdminUserService;
import com.erp.core.admin.web.dto.RoleSummary;
import com.erp.core.security.api.actor.CurrentActor;
import com.erp.core.security.api.actor.CurrentActorProvider;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminWebController {

    private final CurrentActorProvider actorProvider;
    private final AdminRoleService roleService;
    private final AdminUserService userService;

    public AdminWebController(CurrentActorProvider actorProvider,
                              AdminRoleService roleService,
                              AdminUserService userService) {
        this.actorProvider = actorProvider;
        this.roleService = roleService;
        this.userService = userService;
    }

    private CurrentActor getActor() {
        return actorProvider.getCurrentActor();
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("username", getActor().principal());
        return "admin/dashboard";
    }

    @GetMapping("/settings")
    public String settings(Model model) {
        model.addAttribute("username", getActor().principal());
        return "admin/settings";
    }

    @PreAuthorize("hasAuthority('ADMIN_ROLE_MANAGE')")
    @GetMapping("/settings/role-permission")
    public String rolePermissionPage(Model model) {
        CurrentActor actor = getActor();
        List<RoleSummary> roles = roleService.getRoles(actor.tenantId());
        model.addAttribute("username", actor.principal());
        model.addAttribute("roles", roles);
        model.addAttribute("allPermissions", roleService.getAllPermissions());
        return "admin/role-permission";
    }

    @PreAuthorize("hasAuthority('ADMIN_USER_MANAGE')")
    @GetMapping("/settings/user-role")
    public String userRolePage(Model model) {
        CurrentActor actor = getActor();
        model.addAttribute("username", actor.principal());
        model.addAttribute("users", userService.list(actor.tenantId(), 0, 100).items());
        model.addAttribute("roles", roleService.getRoles(actor.tenantId()));
        return "admin/user-role";
    }
}