package com.erp.core.security.internal.jwt;

import com.erp.core.security.internal.jwt.application.JwtTokenIssuer;
import com.erp.core.security.internal.jwt.config.JwtConfiguration;
import com.erp.core.security.internal.platform.config.SecurityProperties;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;

import java.time.Clock;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtRoundTripTest {

    private final SecurityProperties props = new SecurityProperties(
            true,
            "X-Tenant-Id",
            null,   // persistence
            null,   // login
            null,   // logout
            new SecurityProperties.Jwt(
                    "erp-core",
                    "test-secret-test-secret-test-secret-32",
                    Duration.ofMinutes(15),
                    Duration.ofSeconds(30)),
            null,   // refreshToken
            null,   // lockout
            null,   // cleanup
            null    // redis
    );

    @Test
    void issueAndDecode() {
        Clock clock = Clock.systemUTC();
        JwtEncoder encoder = JwtConfiguration.jwtEncoder(props);
        JwtDecoder decoder = JwtConfiguration.jwtDecoder(props);
        JwtTokenIssuer issuer = new JwtTokenIssuer(encoder, props, clock);

        UUID actorId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        String token = issuer.issue(actorId, tenantId, "alice", Set.of("ROLE_USER"));

        Jwt decoded = decoder.decode(token);
        assertThat(decoded.getSubject()).isEqualTo("alice");
        assertThat(decoded.getClaimAsString("actor_id")).isEqualTo(actorId.toString());
        assertThat(decoded.getClaimAsString("tenant_id")).isEqualTo(tenantId.toString());
    }

    @Test
    void wrongSecretFails() {
        JwtEncoder encoder = JwtConfiguration.jwtEncoder(props);
        SecurityProperties other = new SecurityProperties(
                true,
                "X-Tenant-Id",
                null,
                null,
                null,
                new SecurityProperties.Jwt(
                        "erp-core",
                        "different-secret-different-secret-32",
                        Duration.ofMinutes(15),
                        Duration.ofSeconds(30)),
                null,
                null,
                null,
                null
        );
        JwtDecoder otherDecoder = JwtConfiguration.jwtDecoder(other);
        String token = new JwtTokenIssuer(encoder, props, Clock.systemUTC())
                .issue(UUID.randomUUID(), UUID.randomUUID(), "alice", Set.of());

        assertThatThrownBy(() -> otherDecoder.decode(token))
                .isInstanceOf(org.springframework.security.oauth2.jwt.JwtException.class);
    }
}