package com.erp.core.security.internal.web.config;

import com.erp.core.security.internal.abuse.filter.LoginRateLimitFilter;
import com.erp.core.security.internal.authentication.adapter.in.springsecurity.ErpAuthenticationProvider;
import com.erp.core.security.internal.context.infrastructure.springsecurity.TenantContextRestorationFilter;
import com.erp.core.security.internal.platform.config.SecurityProperties;
import com.erp.core.security.internal.tenant.port.TenantMembershipPort;
import com.erp.core.security.internal.web.handler.JsonAccessDeniedHandler;
import com.erp.core.security.internal.web.handler.JsonAuthenticationEntryPoint;
import com.erp.core.security.internal.web.handler.JsonLogoutSuccessHandler;
import com.erp.core.security.internal.web.handler.SmartAuthenticationEntryPoint;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.csrf.CsrfFilter;

public class BrowserSecurityFilterChainConfiguration {

    public static SecurityFilterChain build(HttpSecurity http,
                                            SecurityProperties properties,
                                            ErpAuthenticationProvider provider,
                                            JsonAuthenticationEntryPoint jsonEntryPoint,
                                            JsonAccessDeniedHandler deniedHandler,
                                            JsonLogoutSuccessHandler logoutHandler,
                                            LoginRateLimitFilter rateLimitFilter,
                                            TenantMembershipPort membership) throws Exception {

        AuthenticationManager authManager = new ProviderManager(provider);
        AuthenticationEntryPoint entryPoint = new SmartAuthenticationEntryPoint(jsonEntryPoint);

        http
                .securityMatcher("/**")
                .authenticationManager(authManager)
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/auth/refresh"))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionFixation(fix -> fix.changeSessionId()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/login",
                                "/auth/refresh",
                                "/csrf",
                                "/css/**", "/js/**", "/images/**",
                                "/assets/**",
                                "/actuator/health", "/actuator/info"
                        ).permitAll()
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
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(deniedHandler))
                .addFilterBefore(rateLimitFilter, CsrfFilter.class)
                .addFilterAfter(new TenantContextRestorationFilter(membership),
                        SecurityContextHolderFilter.class);

        SecurityHeadersConfiguration.apply(http);
        return http.build();
    }
}