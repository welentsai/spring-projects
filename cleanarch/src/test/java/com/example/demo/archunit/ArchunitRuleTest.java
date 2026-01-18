package com.example.demo.archunit;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;

public class ArchunitRuleTest {

    @Test
    public void classes_in_demo_folder_only_uses_in_demo() {
        JavaClasses importedClasses = new ClassFileImporter().importPackages("com.example");

        ArchRule rule =
                noClasses()
                        .that()
                        .resideInAPackage("..demo..")
                        .should()
                        .dependOnClassesThat()
                        .resideInAnyPackage("..demo2..", "..demo3..")
                        .because("demo package should not access demo2 package classes");

        rule.check(importedClasses);
    }

    @Test
    public void methods_should_be_camel_case() {
        JavaClasses importedClasses = new ClassFileImporter().importPackages("com.example");

        ArchRule rule =
                methods()
                        // 1. Filter out Synthetic methods (Lambdas, switch map tables, etc.)
                        .that()
                        .doNotHaveModifier(JavaModifier.SYNTHETIC)
                        // 2. Filter out Native methods (JNI often requires underscores)
                        .and()
                        .doNotHaveModifier(JavaModifier.NATIVE)
                        // 3. Ignore methods in Test classes (Allow snake_case in tests)
                        .and()
                        .areNotAnnotatedWith(Test.class)
                        .and()
                        .areNotAnnotatedWith(ParameterizedTest.class)
                        // 4. Ignore standard Object overrides (toString, hashCode, equals)
                        .and()
                        .areNotDeclaredIn(Object.class)
                        // 5. The Check
                        .should()
                        .haveNameMatching("^[a-z][a-zA-Z0-9]*$")
                        .because("Production methods should follow camelCase naming convention");

        rule.check(importedClasses);
    }
}
