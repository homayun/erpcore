package com.erp.core.security.internal.jwt;

import com.erp.core.security.internal.jwt.config.JwtSecretGuard;
import com.erp.core.security.internal.platform.config.SecurityProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtSecretGuardTest {

    private SecurityProperties withDefaults() {
        return new SecurityProperties(true, null, null, null, null, null, null, null, null, null);
    }

    @Test
    void placeholderSecretStopsStartup() {
        var guard = new JwtSecretGuard(withDefaults(), new MockEnvironment());
        assertThatThrownBy(guard::afterPropertiesSet).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void placeholderIsToleratedOnlyWithExplicitDevFlag() {
        MockEnvironment env = new MockEnvironment().withProperty(JwtSecretGuard.ALLOW_PROPERTY, "true");
        assertThatCode(() -> new JwtSecretGuard(withDefaults(), env).afterPropertiesSet()).doesNotThrowAnyException();
    }

    @Test
    void realSecretPasses() {
        var props = new SecurityProperties(true, null, null, null, null,
                new SecurityProperties.Jwt(null, "k8s-generated-0123456789-abcdefghijklmnop", null, null),
                null, null, null, null);
        assertThatCode(() -> new JwtSecretGuard(props, new MockEnvironment()).afterPropertiesSet())
                .doesNotThrowAnyException();
    }
}