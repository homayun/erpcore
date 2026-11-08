package com.erp.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@SpringBootApplication
@ComponentScan(
        basePackages = "com.erp.core",
        excludeFilters = {
                // Auto-config classes must only load via AutoConfiguration.imports
                @ComponentScan.Filter(
                        type = FilterType.ANNOTATION,
                        classes = AutoConfiguration.class
                ),
                // Other @SpringBootApplication classes (e.g. test bootstrap classes
                // on the test classpath) must NOT be scanned as configuration —
                // they carry @EnableAutoConfiguration and would trigger a second
                // auto-configuration pass, registering repositories twice.
                @ComponentScan.Filter(
                        type = FilterType.ANNOTATION,
                        classes = SpringBootApplication.class
                )
        }
)
public class ErpcoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(ErpcoreApplication.class, args);
    }
}