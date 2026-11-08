package com.erp.core.security.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * Architecture enforcement for the Security module.
 *
 * Test classes are excluded from the import because integration tests
 * legitimately need to seed data via internal ports.
 * External-module access to `internal..` is enforced by Spring Modulith.
 */
class ModuleBoundaryTest {

    private static final String API_PACKAGE = "com.erp.core.security.api..";
    private static final String INTERNAL_PACKAGE = "com.erp.core.security.internal..";

    private static JavaClasses productionClasses;

    @BeforeAll
    static void importProductionClasses() {
        productionClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.erp.core.security");
    }

    @Test
    void apiMustNotDependOnInternal() {
        ArchRule rule = classes()
                .that().resideInAPackage(API_PACKAGE)
                .should().onlyDependOnClassesThat()
                .resideOutsideOfPackage(INTERNAL_PACKAGE)
                .because("public API must never reference internal implementation");

        rule.check(productionClasses);
    }

    @Test
    void layeredArchitectureIsRespected() {
        ArchRule rule = layeredArchitecture()
                .consideringAllDependencies()
                .optionalLayer("API").definedBy(API_PACKAGE)
                .optionalLayer("Internal").definedBy(INTERNAL_PACKAGE)
                .whereLayer("Internal").mayNotBeAccessedByAnyLayer()
                .whereLayer("API").mayOnlyBeAccessedByLayers("Internal")
                .withOptionalLayers(true);

        rule.check(productionClasses);
    }
}