package com.erp.core.admin.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class LoginViewController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpServletRequest request) {
        if (request.getHeader("HX-Request") != null) return "dashboard :: content";
        return "dashboard";
    }

    @GetMapping("/admin/settings/{page}")
    public String settingsPages(@PathVariable String page, HttpServletRequest request) {
        if (request.getHeader("HX-Request") != null) return "admin/settings/placeholder :: content";
        return "admin/settings/placeholder";
    }
}