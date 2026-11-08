package com.erp.core.security.internal.web.config;

import com.erp.core.security.internal.context.infrastructure.springsecurity.TenantContextRestorationFilter;
import com.erp.core.security.internal.web.handler.JsonAccessDeniedHandler;
import com.erp.core.security.internal.web.handler.JsonAuthenticationEntryPoint;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Stateless JWT chain covering /api/** (business data) and /admin/** (admin ops).
 * Both require JWT authentication; CSRF is disabled (no session cookie).
 */
public class ApiSecurityFilterChainConfiguration {

    public static SecurityFilterChain build(HttpSecurity http,
                                            JsonAuthenticationEntryPoint entryPoint,
                                            JsonAccessDeniedHandler deniedHandler,
                                            JwtDecoder jwtDecoder) throws Exception {

        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("authorities");
        authorities.setAuthorityPrefix("");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);

        http
                .securityMatcher("/api/**", "/admin/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().authenticated())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder)
                                .jwtAuthenticationConverter(converter))
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(deniedHandler))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(deniedHandler))
                .addFilterAfter(new TenantContextRestorationFilter(),
                        BearerTokenAuthenticationFilter.class);

        SecurityHeadersConfiguration.apply(http);
        return http.build();
    }
}