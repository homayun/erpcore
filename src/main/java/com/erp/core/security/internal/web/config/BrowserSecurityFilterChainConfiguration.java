package com.erp.core.security.internal.web.config;

import com.erp.core.security.internal.authentication.adapter.in.springsecurity.ErpAuthenticationProvider;
import com.erp.core.security.internal.abuse.filter.LoginRateLimitFilter;
import com.erp.core.security.internal.context.infrastructure.springsecurity.TenantContextRestorationFilter;
import com.erp.core.security.internal.platform.config.SecurityProperties;
import com.erp.core.security.internal.web.handler.JsonAccessDeniedHandler;
import com.erp.core.security.internal.web.handler.JsonAuthenticationEntryPoint;
import com.erp.core.security.internal.web.handler.JsonLogoutSuccessHandler;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.DelegatingAuthenticationEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.LinkedHashMap;

public class BrowserSecurityFilterChainConfiguration {

    public static SecurityFilterChain build(HttpSecurity http,
                                            SecurityProperties properties,
                                            ErpAuthenticationProvider provider,
                                            JsonAuthenticationEntryPoint jsonEntryPoint,
                                            JsonAccessDeniedHandler deniedHandler,
                                            JsonLogoutSuccessHandler logoutHandler,
                                            LoginRateLimitFilter rateLimitFilter) throws Exception {

        AuthenticationManager authManager = new ProviderManager(provider);

        // API-র জন্য JSON 401, ব্রাউজারের জন্য /login রিডাইরেক্ট
        LinkedHashMap<RequestMatcher, AuthenticationEntryPoint> entryPoints = new LinkedHashMap<>();
        entryPoints.put(new AntPathRequestMatcher("/api/**"), jsonEntryPoint);
        DelegatingAuthenticationEntryPoint entryPoint = new DelegatingAuthenticationEntryPoint(entryPoints);
        entryPoint.setDefaultEntryPoint(new LoginUrlAuthenticationEntryPoint("/login"));

        http
                .securityMatcher("/**")
                .authenticationManager(authManager)
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/login", "/auth/refresh"))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionFixation(fix -> fix.changeSessionId()))
                .authorizeHttpRequests(auth -> auth
                        // শুধু পাবলিক রিসোর্স এবং লগইন এন্ডপয়েন্ট permitAll
                        .requestMatchers(
                                "/login",
                                "/auth/refresh",
                                "/csrf",
                                "/css/**", "/js/**", "/images/**",
                                "/actuator/health", "/actuator/info"
                        ).permitAll()
                        // বাকি সব (ড্যাশবোর্ড, অ্যাডমিন, সেটিংস) এর জন্য লগইন বাধ্যতামূলক
                        .anyRequest().authenticated())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout
                        .logoutUrl(properties.logout().logoutUrl())
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessHandler(logoutHandler))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(entryPoint) // নতুন EntryPoint
                        .accessDeniedHandler(deniedHandler))
                .addFilterBefore(rateLimitFilter, CsrfFilter.class)
                .addFilterAfter(new TenantContextRestorationFilter(),
                        SecurityContextHolderFilter.class);

        SecurityHeadersConfiguration.apply(http);
        return http.build();
    }
}