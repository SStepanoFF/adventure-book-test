package com.example.adventurebook.arch;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Tag;

/**
 * Keeps the layering honest. The classes directly in {@code api} and {@code ui} are the generic core (they know
 * nothing about the Adventure Book); {@code api.dto/payload/service} and {@code ui.pages/components/steps/asserts}
 * are the application layers built on top of them. A page object only holds locators.
 */
@Tag("arch")
@AnalyzeClasses(packages = "com.example.adventurebook", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String ROOT = "com.example.adventurebook";

    @ArchTest
    static final ArchRule apiCoreDoesNotKnowTheApplicationLayers = noClasses()
            .that().resideInAPackage(ROOT + ".api")
            .should().dependOnClassesThat().resideInAnyPackage(
                    ROOT + ".api.dto..", ROOT + ".api.service..", ROOT + ".api.payload..");

    @ArchTest
    static final ArchRule uiCoreDoesNotKnowTheApplicationLayers = noClasses()
            .that().resideInAPackage(ROOT + ".ui")
            .should().dependOnClassesThat().resideInAnyPackage(
                    ROOT + ".ui.pages..", ROOT + ".ui.components..", ROOT + ".ui.steps..", ROOT + ".ui.asserts..");

    @ArchTest
    static final ArchRule apiLayerHasNoUiDependency = noClasses()
            .that().resideInAPackage(ROOT + ".api..")
            .should().dependOnClassesThat().resideInAPackage(ROOT + ".ui..");

    @ArchTest
    static final ArchRule uiDoesNotCallTheApiServices = noClasses()
            .that().resideInAPackage(ROOT + ".ui..")
            .should().dependOnClassesThat().resideInAPackage(ROOT + ".api.service..")
            .because("UI classes describe/drive the screen; API calls belong to tests (preconditions) and services");

    @ArchTest
    static final ArchRule pagesOnlyHoldLocators = noClasses()
            .that().resideInAnyPackage(ROOT + ".ui.pages..", ROOT + ".ui.components..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    ROOT + ".ui.steps..", ROOT + ".ui.asserts..", ROOT + ".logging..",
                    "com.microsoft.playwright.assertions..")
            .because("pages describe elements; actions are in <Page>Steps, checks in <Page>Asserts");
}
