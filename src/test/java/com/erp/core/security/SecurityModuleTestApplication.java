package com.erp.core.security;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.erp.core")
@EnableJpaRepositories(basePackages = "com.erp.core")
@EntityScan(basePackages = "com.erp.core")
public class SecurityModuleTestApplication {
}