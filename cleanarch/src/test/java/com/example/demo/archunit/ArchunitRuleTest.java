package com.example.demo.archunit;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;

public class ArchunitRuleTest {

    @Test
    public void classes_in_demo_folder_only_uses_in_demo() {
        JavaClasses importedClasses = new ClassFileImporter().importPackages("com.example");

        ArchRule rule = noClasses()
                .that()
                .resideInAPackage("..demo..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("..demo2..", "..demo3..")
                .because("demo package should not access demo2 package classes");

        rule.check(importedClasses);
    }

    @Test
    public void multiphaseterator_only_used_by_framework_and_usecase() {
        JavaClasses importedClasses = new ClassFileImporter().importPackages("com.example");

        ArchRule rule = noClasses()
                .that()
                .resideOutsideOfPackages(
                        "..framework..",
                        "..usecase..",
                        "..multiphaseterator..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..multiphaseterator..")
                .because("multiphaseterator utilities may only be used by the framework or usecase layers");

        rule.check(importedClasses);
    }

    @Test
    public void methods_should_be_camel_case() {
        JavaClasses importedClasses = new ClassFileImporter().importPackages("com.example");

        ArchRule rule = methods()
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

    @Test
    public void multiphaseterator_should_not_be_used_with_gateways_in_usecase() {
        JavaClasses importedClasses = new ClassFileImporter().importPackages("com.example");

        ArchRule rule = classes()
                .that()
                .resideInAPackage("..usecase..")
                .should(new ArchCondition<JavaClass>("not use PhaseTaskIterator to call Gateways") {
                    @Override
                    public void check(JavaClass javaClass, ConditionEvents events) {
                        javaClass.getCodeUnits().forEach(method -> {
                            boolean callsIterator = method.getMethodCallsFromSelf().stream()
                                    .anyMatch(call -> call.getTarget().getOwner().getName().contains("PhaseTaskIterator"));
                            
                            boolean callsGateway = method.getMethodCallsFromSelf().stream()
                                    .anyMatch(call -> call.getTarget().getOwner().getPackageName().contains("gateway")
                                                   && call.getTarget().getName().equals("execute"));

                            if (callsIterator && callsGateway) {
                                events.add(SimpleConditionEvent.violated(javaClass,
                                        String.format("Method %s.%s uses PhaseTaskIterator and calls a Gateway. " +
                                                "Gateways should not be used within PhaseTaskIterator transformations in the usecase layer.",
                                                javaClass.getName(), method.getName())));
                            }
                        });
                    }
                });

        rule.check(importedClasses);
    }
}
