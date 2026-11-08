package com.erp.core.security.architecture;

import com.erp.core.security.SecurityModuleTestApplication;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

import static org.assertj.core.api.Assertions.assertThatCode;

class ApplicationModulesVerificationTest {

    @Test
    void verifiesModuleStructure() {
        ApplicationModules modules = ApplicationModules.of(SecurityModuleTestApplication.class);

        assertThatCode(modules::verify)
                .as("Spring Modulith module structure must be valid")
                .doesNotThrowAnyException();
    }
}