package com.erp.core.security.internal.jwt.config;

import com.erp.core.security.internal.platform.config.SecurityProperties;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.JwtTypeValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Builds JWT encoder/decoder for HS256 signing with symmetric key.
 *
 * Spring Security 7 note: NimbusJwtDecoder no longer validates the `typ` header
 * by itself - we add JwtTypeValidator.jwt() explicitly.
 *
 * Every accepted token must carry tenant_id AND actor_id as valid UUIDs.
 */
public final class JwtConfiguration {

    private JwtConfiguration() {}

    public static SecretKey secretKey(SecurityProperties props) {
        byte[] keyBytes = props.jwt().secret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 bytes for HS256");
        }
        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    public static JwtEncoder jwtEncoder(SecurityProperties props) {
        return NimbusJwtEncoder.withSecretKey(secretKey(props))
                .algorithm(MacAlgorithm.HS256)
                .build();
    }

    public static JwtDecoder jwtDecoder(SecurityProperties props) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withSecretKey(secretKey(props))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        OAuth2TokenValidator<Jwt> issuerValidator =
                new JwtIssuerValidator(props.jwt().issuer());
        OAuth2TokenValidator<Jwt> typeValidator = JwtTypeValidator.jwt();
        OAuth2TokenValidator<Jwt> timestampValidator =
                new JwtTimestampValidator(props.jwt().clockSkew());

        OAuth2TokenValidator<Jwt> identityValidator = jwt ->
                (isUuid(jwt.getClaimAsString("tenant_id")) && isUuid(jwt.getClaimAsString("actor_id")))
                        ? OAuth2TokenValidatorResult.success()
                        : OAuth2TokenValidatorResult.failure(new OAuth2Error(
                                OAuth2ErrorCodes.INVALID_TOKEN,
                                "tenant_id and actor_id must be UUIDs", null));

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                issuerValidator, typeValidator, timestampValidator, identityValidator));

        return decoder;
    }

    private static boolean isUuid(String value) {
        if (value == null) return false;
        try { UUID.fromString(value); return true; }
        catch (IllegalArgumentException e) { return false; }
    }
}