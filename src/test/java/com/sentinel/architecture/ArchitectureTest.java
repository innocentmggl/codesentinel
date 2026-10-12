package com.sentinel.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Enforces the hexagonal dependency rules described in the README. */
@AnalyzeClasses(packages = "com.sentinel", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule domainIsFrameworkFree = noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "com.github.javaparser..", "..application..", "..adapter..",
                    "..config..", "jakarta..");

    @ArchTest
    static final ArchRule applicationDependsOnPortsNotAdapters = noClasses().that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..adapter..", "org.springframework.ai..", "org.springframework.web..",
                    "org.springframework.jdbc..", "com.github.javaparser..");

    @ArchTest
    static final ArchRule portsAreInterfaces = classes().that().resideInAPackage("..application.port..")
            .should().beInterfaces();

    @ArchTest
    static final ArchRule outboundAdaptersDoNotKnowEachOther =
            slices().matching("..adapter.outbound.(*)..").should().notDependOnEachOther();

    @ArchTest
    static final ArchRule inboundAdaptersDoNotUseOutboundAdapters = noClasses()
            .that().resideInAPackage("..adapter.inbound..")
            .should().dependOnClassesThat().resideInAPackage("..adapter.outbound..");

    @ArchTest
    static final ArchRule noCyclesBetweenLayers =
            slices().matching("com.sentinel.(*)..").should().beFreeOfCycles();

    @ArchTest
    static final ArchRule noCyclesBetweenUseCases =
            slices().matching("..application.(*)..").should().beFreeOfCycles();
}
