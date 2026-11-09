package com.erp.core.security.internal.web.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

import java.io.IOException;

public class SmartAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final JsonAuthenticationEntryPoint jsonEntryPoint;

    public SmartAuthenticationEntryPoint(JsonAuthenticationEntryPoint jsonEntryPoint) {
        this.jsonEntryPoint = jsonEntryPoint;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        // ১. HTMX-এর জন্য: ব্রাউজারকে সম্পূর্ণ রিলোড দিয়ে /login-এ পাঠাবে
        if ("true".equals(request.getHeader("HX-Request"))) {
            response.setHeader("HX-Redirect", "/login");
            response.setStatus(200);
            return;
        }
        
        // ২. API বা JSON রিকোয়েস্টের জন্য
        String accept = request.getHeader("Accept");
        if (request.getRequestURI().startsWith("/api/") || (accept != null && accept.contains("application/json"))) {
            jsonEntryPoint.commence(request, response, authException);
            return;
        }

        // ৩. সাধারণ ব্রাউজার রিকোয়েস্টের জন্য /login-এ 302 রিডাইরেক্ট
        new LoginUrlAuthenticationEntryPoint("/login").commence(request, response, authException);
    }
}