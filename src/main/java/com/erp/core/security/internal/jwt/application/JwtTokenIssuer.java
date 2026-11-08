package com.erp.core.security.internal.jwt.application;

import com.erp.core.security.internal.platform.config.SecurityProperties;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;

import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Issues signed JWT access tokens.
 *
 * The token is intentionally small: no business data, only identity claims.
 */
public class JwtTokenIssuer {

    private final JwtEncoder encoder;
    private final SecurityProperties props;
    private final Clock clock;

    public JwtTokenIssuer(JwtEncoder encoder, SecurityProperties props, Clock clock) {
        this.encoder = encoder;
        this.props = props;
        this.clock = clock;
    }

    public String issue(UUID actorId, UUID tenantId, String username, Set<String> authorities) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(props.jwt().issuer())
                .subject(username)
                .issuedAt(now)
                .expiresAt(now.plus(props.jwt().accessTtl()))
                .claim("actor_id", actorId.toString())
                .claim("tenant_id", tenantId.toString())
                .claim("authorities", authorities)
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}