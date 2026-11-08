package com.erp.core.security.internal.jwt;

import com.erp.core.security.internal.jwt.config.JwtConfiguration;
import com.erp.core.security.internal.platform.config.SecurityProperties;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtIdentityClaimValidationTest {

    private final SecurityProperties props = new SecurityProperties(true, null, null, null, null,
            new SecurityProperties.Jwt("erp-core", "test-secret-test-secret-test-secret-32",
                    Duration.ofMinutes(5), Duration.ofSeconds(30)),
            null, null, null, null);

    private String token(String actorId, String tenantId) {
        JwtEncoder encoder = JwtConfiguration.jwtEncoder(props);
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("erp-core").subject("alice")
                .issuedAt(now).expiresAt(now.plusSeconds(60))
                .claim("actor_id", actorId).claim("tenant_id", tenantId)
                .build();
        return encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    @Test
    void tokenWithUuidIdentityIsAccepted() {
        JwtDecoder decoder = JwtConfiguration.jwtDecoder(props);
        var jwt = decoder.decode(token(UUID.randomUUID().toString(), UUID.randomUUID().toString()));
        assertThat(jwt.getSubject()).isEqualTo("alice");
    }

    @Test
    void tokenWithNonUuidTenantIsRejected() {
        JwtDecoder decoder = JwtConfiguration.jwtDecoder(props);
        String bad = token(UUID.randomUUID().toString(), "not-a-uuid");
        assertThatThrownBy(() -> decoder.decode(bad)).isInstanceOf(JwtException.class);
    }
}