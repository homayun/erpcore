package com.erp.core.admin.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class LoginViewController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    // পেজ লোড এবং HTMX রিকোয়েস্ট হ্যান্ডেল করার জন্য একটি কমন মেথড
    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard"; // আমরা dashboard.html কে base layout দিয়ে আপডেট করবো
    }

    // নতুন রাউট: /admin/companies (আপনার আগের তৈরি করা)
    @GetMapping("/admin/companies")
    public String companies() {
        return "admin/companies/index"; // এটি পুরো পেজ হবে
    }

    // নতুন রাউট: সেটিংস মেনুগুলোর জন্য প্লেসহোল্ডার
    @GetMapping("/admin/settings/{page}")
    public String settingsPages(@PathVariable String page) {
        // আমরা সরাসরি প্লেসহোল্ডার ফ্র্যাগমেন্ট রিটার্ন করছি
        return "admin/settings/placeholder :: content";
    }
}