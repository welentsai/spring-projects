package com.example.demo.archunit;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.*;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ArchunitRuleTest {

    private static final JavaClasses ALL_CLASSES =
            new ClassFileImporter().importPackages("com.example");

    private static final Set<String> ITERATOR_MAP_METHODS =
            Set.of("map", "mapAsync", "thenMap", "thenMapAsync", "andMap", "andMapAsync");

    // ── Tests ──────────────────────────────────────────────────────────────────

    @Test
    public void classes_in_demo_folder_only_uses_in_demo() {
        ArchRule rule = noClasses()
                .that()
                .resideInAPackage("..demo..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("..demo2..", "..demo3..")
                .because("demo package should not access demo2 package classes");

        rule.check(ALL_CLASSES);
    }

    @Test
    public void multiphaseterator_only_used_by_framework_and_usecase() {
        ArchRule rule = noClasses()
                .that()
                .resideOutsideOfPackages("..framework..", "..usecase..", "..multiphaseterator..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..multiphaseterator..")
                .because(
                        "multiphaseterator utilities may only be used by the framework or usecase layers");

        rule.check(ALL_CLASSES);
    }

    @Test
    public void mcp_tool_adapters_should_have_tool_adapter_suffix() {
        ArchRule rule = classes()
                .that()
                .resideInAPackage("..adapter.in.mcp..")
                .and()
                .haveSimpleNameNotEndingWith("Test")
                .should()
                .haveSimpleNameEndingWith("ToolAdapter")
                .because(
                        "MCP inbound adapters must follow the {Domain}ToolAdapter naming convention");

        rule.check(ALL_CLASSES);
    }

    @Test
    public void mcp_tool_adapters_should_only_depend_on_inbound_ports() {
        ArchRule rule = noClasses()
                .that()
                .resideInAPackage("..adapter.in.mcp..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "..adapter.out..",
                        "..usecase.ports.out..",
                        "..usecase.ports.in.impl..",
                        "..framework..",
                        "..domain..")
                .because("MCP tool adapters may only depend on use case interfaces (ports/in),"
                        + " never on outbound ports, implementations, framework or domain classes");

        rule.check(ALL_CLASSES);
    }

    @Test
    public void controllers_and_mcp_tool_adapters_should_not_depend_on_each_other() {
        // Match only project classes as dependency targets — a bare simple-name match would
        // also hit framework annotation classes like org.springframework...RestController.
        DescribedPredicate<JavaClass> projectInboundAdapters =
                JavaClass.Predicates.resideInAPackage("com.example..")
                        .and(JavaClass.Predicates.simpleNameEndingWith("Controller")
                                .or(JavaClass.Predicates.simpleNameEndingWith("ToolAdapter")));

        ArchRule rule = noClasses()
                .that()
                .resideInAPackage("..adapter.in..")
                .and()
                .haveSimpleNameEndingWith("Controller")
                .or()
                .resideInAPackage("..adapter.in..")
                .and()
                .haveSimpleNameEndingWith("ToolAdapter")
                .should()
                .dependOnClassesThat(projectInboundAdapters)
                .because("inbound adapters must stay independent of each other");

        rule.check(ALL_CLASSES);
    }

    @Test
    public void methods_should_be_camel_case() {
        ArchRule rule = methods()
                .that()
                .doNotHaveModifier(JavaModifier.SYNTHETIC)
                .and()
                .doNotHaveModifier(JavaModifier.NATIVE)
                .and()
                .areNotAnnotatedWith(Test.class)
                .and()
                .areNotAnnotatedWith(ParameterizedTest.class)
                .and()
                .areNotDeclaredIn(Object.class)
                .should()
                .haveNameMatching("^[a-z][a-zA-Z0-9]*$")
                .because("Production methods should follow camelCase naming convention");

        rule.check(ALL_CLASSES);
    }

    // This test case will cause false alarm for the class that contains action gateway but not used
    // in PhaseIterator
    @Test
    public void multiphaseterator_should_not_be_used_with_gateways_in_usecase() {
        ArchRule rule = classes()
                .that()
                .resideInAPackage("..usecase..")
                .should(
                        new ArchCondition<JavaClass>(
                                "not use PhaseTaskIterator to call restricted Gateways") {
                            @Override
                            public void check(JavaClass javaClass, ConditionEvents events) {
                                javaClass.getCodeUnits().forEach(method -> {
                                    boolean callsIterator = method.getMethodCallsFromSelf().stream()
                                            .anyMatch(call -> call.getTarget()
                                                    .getOwner()
                                                    .getName()
                                                    .contains("PhaseTaskIterator"));

                                    boolean callsRestrictedGateway =
                                            method.getMethodCallsFromSelf().stream()
                                                    .anyMatch(
                                                            ArchunitRuleTest
                                                                    ::isRestrictedGatewayCall);

                                    if (callsIterator && callsRestrictedGateway) {
                                        events.add(SimpleConditionEvent.violated(
                                                javaClass,
                                                String.format(
                                                        "Method %s.%s uses PhaseTaskIterator and calls a restricted Gateway. "
                                                                + "Only Gateways starting with 'Inquire' or 'Get' are allowed"
                                                                + " within PhaseTaskIterator transformations in the usecase layer.",
                                                        javaClass.getName(), method.getName())));
                                    }
                                });
                            }
                        });

        rule.check(ALL_CLASSES);
    }

    @Test
    public void multiphaseterator_should_not_be_used_with_action_gateways_directly_in_usecase() {
        // ArchUnit 1.4+ attributes all lambda-body calls to the enclosing method;
        // isDeclaredInLambda() distinguishes them from direct calls in the same method body.
        // Known scope: flags any lambda in the method that directly calls a restricted gateway —
        // not only lambdas passed to PhaseTaskIterator. ArchUnit 1.4.x has no API to narrow a
        // isDeclaredInLambda() call back to a specific lambda site. If a method contains both a
        // PhaseTaskIterator chain and an unrelated lambda (e.g. Stream.map) that calls a restricted
        // gateway, this rule will still fire — and that is intentional: keep gateway side-effects
        // out of all lambdas in usecase methods that use PhaseTaskIterator.
        ArchRule rule = classes()
                .that()
                .resideInAPackage("..usecase..")
                .should(
                        new ArchCondition<JavaClass>(
                                "not call action gateways directly inside PhaseTaskIterator lambda arguments") {
                            @Override
                            public void check(JavaClass javaClass, ConditionEvents events) {
                                javaClass.getCodeUnits().stream()
                                        .filter(cu ->
                                                !cu.getModifiers().contains(JavaModifier.SYNTHETIC))
                                        .filter(ArchunitRuleTest::callsIteratorMapMethod)
                                        .forEach(codeUnit ->
                                                codeUnit.getMethodCallsFromSelf().stream()
                                                        .filter(JavaAccess::isDeclaredInLambda)
                                                        .filter(
                                                                ArchunitRuleTest
                                                                        ::isRestrictedGatewayCall)
                                                        .forEach(call -> reportDirectViolation(
                                                                javaClass, codeUnit, call,
                                                                events)));
                            }
                        });

        rule.check(ALL_CLASSES);
    }

    @Test
    public void
            multiphaseterator_should_not_indirectly_call_action_gateways_via_helpers_in_usecase() {
        ArchRule rule = classes()
                .that()
                .resideInAPackage("..usecase..")
                .should(
                        new ArchCondition<JavaClass>(
                                "not indirectly call action gateways via private helpers"
                                        + " inside PhaseTaskIterator lambda arguments") {
                            @Override
                            public void check(JavaClass javaClass, ConditionEvents events) {
                                javaClass.getCodeUnits().stream()
                                        .filter(cu ->
                                                !cu.getModifiers().contains(JavaModifier.SYNTHETIC))
                                        .filter(ArchunitRuleTest::callsIteratorMapMethod)
                                        .forEach(codeUnit ->
                                                codeUnit.getMethodCallsFromSelf().stream()
                                                        .filter(JavaAccess::isDeclaredInLambda)
                                                        .filter(call -> call.getTarget()
                                                                .getOwner()
                                                                .equals(javaClass))
                                                        .forEach(lambdaCall ->
                                                                reportIndirectViolations(
                                                                        javaClass,
                                                                        codeUnit,
                                                                        lambdaCall,
                                                                        events)));
                            }
                        });

        rule.check(ALL_CLASSES);
    }

    // ── Shared helpers ─────────────────────────────────────────────────────────

    private static boolean callsIteratorMapMethod(JavaCodeUnit codeUnit) {
        return codeUnit.getMethodCallsFromSelf().stream()
                .anyMatch(call -> call.getTarget()
                                .getOwner()
                                .getFullName()
                                .equals("com.example.demo.util.multiphaseterator.PhaseTaskIterator")
                        && ITERATOR_MAP_METHODS.contains(call.getTarget().getName()));
    }

    private static boolean isRestrictedGatewayCall(JavaMethodCall call) {
        JavaClass owner = call.getTarget().getOwner();
        String simpleName = owner.getSimpleName();
        return owner.getPackageName().contains("gateway")
                && call.getTarget().getName().equals("execute")
                && !simpleName.startsWith("Inquire")
                && !simpleName.startsWith("Get");
    }

    private static void reportDirectViolation(
            JavaClass javaClass,
            JavaCodeUnit codeUnit,
            JavaMethodCall call,
            ConditionEvents events) {
        events.add(SimpleConditionEvent.violated(
                javaClass,
                String.format(
                        "Method %s.%s passes a lambda to PhaseTaskIterator"
                                + " that calls restricted action gateway '%s' (at %s)."
                                + " Only gateways starting with 'Inquire' or 'Get'"
                                + " may be called inside PhaseTaskIterator lambda arguments.",
                        javaClass.getSimpleName(),
                        codeUnit.getName(),
                        call.getTarget().getOwner().getSimpleName(),
                        call.getSourceCodeLocation())));
    }

    private static void reportIndirectViolations(
            JavaClass javaClass,
            JavaCodeUnit callerUnit,
            JavaMethodCall lambdaCall,
            ConditionEvents events) {
        String helperName = lambdaCall.getTarget().getName();
        javaClass.getCodeUnits().stream()
                .filter(cu -> cu.getName().equals(helperName))
                .forEach(helperUnit -> collectRestrictedGatewayCalls(
                                javaClass, helperUnit, new HashSet<>())
                        .forEach(gatewayCall -> events.add(SimpleConditionEvent.violated(
                                javaClass,
                                String.format(
                                        "Method %s.%s passes a lambda to PhaseTaskIterator that indirectly"
                                                + " calls restricted action gateway '%s' via '%s' (at %s)."
                                                + " Only gateways starting with 'Inquire' or 'Get' may be"
                                                + " called inside PhaseTaskIterator lambda arguments.",
                                        javaClass.getSimpleName(),
                                        callerUnit.getName(),
                                        gatewayCall.getTarget().getOwner().getSimpleName(),
                                        helperName,
                                        gatewayCall.getSourceCodeLocation())))));
    }

    /**
     * Depth-first traversal from {@code codeUnit} within {@code ownerClass}, collecting all
     * restricted gateway calls reachable via same-class method calls. {@code visited} prevents
     * infinite loops on recursive or mutually-recursive methods.
     */
    private static List<JavaMethodCall> collectRestrictedGatewayCalls(
            JavaClass ownerClass, JavaCodeUnit codeUnit, Set<String> visited) {
        if (!visited.add(codeUnit.getName())) return List.of();

        List<JavaMethodCall> found = new ArrayList<>();
        codeUnit.getMethodCallsFromSelf().forEach(call -> {
            if (isRestrictedGatewayCall(call)) {
                found.add(call);
            } else if (call.getTarget().getOwner().equals(ownerClass)) {
                ownerClass.getCodeUnits().stream()
                        .filter(cu -> cu.getName().equals(call.getTarget().getName()))
                        .forEach(callee -> found.addAll(
                                collectRestrictedGatewayCalls(ownerClass, callee, visited)));
            }
        });
        return found;
    }
}
