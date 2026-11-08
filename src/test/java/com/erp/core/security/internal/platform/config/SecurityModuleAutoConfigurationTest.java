package com.erp.core.security.internal.platform.config;

import com.erp.core.security.api.actor.CurrentActorProvider;
import com.erp.core.security.internal.authentication.adapter.in.springsecurity.ErpAuthenticationProvider;
import com.erp.core.security.internal.authentication.application.AuthenticationOrchestrator;
import com.erp.core.security.internal.authentication.application.LocalAuthenticationService;
import com.erp.core.security.internal.authentication.port.CredentialRepositoryPort;
import com.erp.core.security.internal.authentication.port.PasswordHashingPort;
import com.erp.core.security.internal.context.application.capture.SecurityContextCaptureService;
import com.erp.core.security.internal.context.application.capture.SecurityContextSnapshotFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test for SecurityModuleAutoConfiguration's non-web beans.
 *
 * Web infrastructure (HttpSecurity, SecurityFilterChain) requires a servlet
 * context and is tested end-to-end in SecurityFilterChainIntegrationTest.
 * Here we disable web-dependent beans via erp.security.enabled=false so the
 * core beans can be verified in isolation.
 */
class SecurityModuleAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    JacksonAutoConfiguration.class,
                    SecurityModuleAutoConfiguration.class
            ))
            .withPropertyValues("erp.security.enabled=false", "erp.security.persistence.type=memory",
                    "erp.security.jwt.allow-insecure-default-secret=true");

    @Test
    void providesCoreBeans() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(CurrentActorProvider.class);
            assertThat(context).hasSingleBean(SecurityContextSnapshotFactory.class);
            assertThat(context).hasSingleBean(SecurityContextCaptureService.class);
            assertThat(context).hasSingleBean(SecurityProperties.class);
            assertThat(context).hasSingleBean(PasswordHashingPort.class);
            assertThat(context).hasSingleBean(CredentialRepositoryPort.class);
            assertThat(context).hasSingleBean(LocalAuthenticationService.class);
            assertThat(context).hasSingleBean(AuthenticationOrchestrator.class);
            assertThat(context).hasSingleBean(ErpAuthenticationProvider.class);
        });
    }

    @Test
    void propertiesHaveDefaults() {
        runner.run(context -> {
            SecurityProperties props = context.getBean(SecurityProperties.class);
            assertThat(props.defaultTenantHeader()).isEqualTo("X-Tenant-Id");
            assertThat(props.login().processingUrl()).isEqualTo("/login");
            assertThat(props.logout().logoutUrl()).isEqualTo("/logout");
        });
    }
}