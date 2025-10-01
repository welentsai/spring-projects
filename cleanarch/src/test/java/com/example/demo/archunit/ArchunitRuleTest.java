package com.example.demo.archunit;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

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
}
