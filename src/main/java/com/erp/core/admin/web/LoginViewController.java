package com.erp.core.admin.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Serves Thymeleaf pages that have no backing REST controller.
 * Registered as a bean via @Import on AdminModuleAutoConfiguration
 * (quickstart app only scans com.erp.example, not com.erp.core).
 */
@Controller
public class LoginViewController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/dashboard";
    }
}