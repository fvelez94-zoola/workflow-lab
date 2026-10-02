package com.hexagonal.workflowlab;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideOutsideOfPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.stereotype.Service;

/**
 * Executable version of the dependency rule. This module is the only one that sees every other module
 * on its classpath, so it is the natural place to check the whole architecture.
 */
@AnalyzeClasses(packages = "com.hexagonal.workflowlab", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule shouldKeepTheDomainModelFreeOfOuterLayersAndTechnologies = noClasses()
            .that().resideInAPackage("..domain.model..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "jakarta..", "org.hibernate..", "com.fasterxml..", "org.slf4j..",
                    "..domain.usecase..", "..infrastructure..")
            .because("the domain must not know persistence, serialization, HTTP or any outer layer");

    @ArchTest
    static final ArchRule shouldKeepSpringOutOfTheDomainModelExceptForTheAnalysisCollaborators = noClasses()
            .that().resideInAPackage("..domain.model..")
            .and().resideOutsideOfPackage("..domain.model.analysis..")
            .should().dependOnClassesThat().resideInAPackage("org.springframework..")
            .because("entities, rules, ports and value objects stay plain Java; only the three stateless "
                    + "analysis collaborators carry a stereotype");

    @ArchTest
    static final ArchRule shouldAllowOnlyStereotypesAsSpringDependencyInTheDomain = noClasses()
            .that().resideInAnyPackage("..domain.model..", "..domain.usecase..")
            .should().dependOnClassesThat(resideInAPackage("org.springframework..")
                    .and(resideOutsideOfPackage("org.springframework.stereotype..")))
            .because("the domain may use @Service/@Component and nothing else from Spring: no web, no data, no context");

    @ArchTest
    static final ArchRule shouldKeepUseCasesFreeOfPersistenceAndInfrastructure = noClasses()
            .that().resideInAPackage("..domain.usecase..")
            .should().dependOnClassesThat().resideInAnyPackage("jakarta..", "org.hibernate..", "..infrastructure..")
            .because("use cases orchestrate ports; they never touch a technology directly");

    @ArchTest
    static final ArchRule shouldAnnotateUseCasesWithServiceAndNothingElseInTheDomainWithIt = classes()
            .that().areAnnotatedWith(Service.class)
            .should().resideInAPackage("..domain.usecase..")
            .andShould().haveSimpleNameEndingWith("UseCase")
            .because("@Service marks use cases only, and every use case follows the naming convention");

    @ArchTest
    static final ArchRule shouldKeepEntryPointsDependingOnPortsAndNotOnImplementations = noClasses()
            .that().resideInAPackage("..infrastructure.entrypoint..")
            .should().dependOnClassesThat().resideInAnyPackage("..domain.usecase..", "..infrastructure.adapter..")
            .because("controllers talk to input ports (interfaces), never to use case classes or adapters");

    @ArchTest
    static final ArchRule shouldKeepDrivenAdaptersAwayFromEntryPointsAndUseCases = noClasses()
            .that().resideInAPackage("..infrastructure.adapter..")
            .should().dependOnClassesThat().resideInAnyPackage("..infrastructure.entrypoint..", "..domain.usecase..")
            .because("adapters only implement output ports");

    @ArchTest
    static final ArchRule shouldKeepEveryDrivenAdapterIndependentFromTheOthers = slices()
            .matching("com.hexagonal.workflowlab.infrastructure.adapter.(*)..")
            .should().notDependOnEachOther()
            .because("adapters must be replaceable one by one");

    @ArchTest
    static final ArchRule shouldKeepPresentationAndFormattingHelpersInInfrastructure = classes()
            .that().haveSimpleNameEndingWith("Formatter")
            .or().haveSimpleNameEndingWith("Escaper")
            .or().haveSimpleNameEndingWith("MediaType")
            .or().haveSimpleNameEndingWith("RestMapper")
            .or().haveSimpleName("MarkdownTable")
            .should().resideInAPackage("..infrastructure..")
            .because("how data is displayed, escaped or serialized is a technology detail, never a domain concern");

    @ArchTest
    static final ArchRule shouldKeepJpaEntitiesInsideThePersistenceAdapter = noClasses()
            .that().resideOutsideOfPackage("..infrastructure.adapter.jpa..")
            .should().dependOnClassesThat().resideInAPackage("..infrastructure.adapter.jpa.entity..")
            .because("JPA entities are a persistence detail and must never leak");

    @ArchTest
    static final ArchRule shouldImplementInputPortsOnlyInTheUseCaseModule = classes()
            .that().implement(com.hexagonal.workflowlab.domain.model.port.in.WorkflowCrud.class)
            .or().implement(com.hexagonal.workflowlab.domain.model.port.in.PublishWorkflow.class)
            .or().implement(com.hexagonal.workflowlab.domain.model.port.in.ConvertWorkflowToExperiment.class)
            .or().implement(com.hexagonal.workflowlab.domain.model.port.in.AnalyzeWorkflow.class)
            .or().implement(com.hexagonal.workflowlab.domain.model.port.in.ExportProtocol.class)
            .should().resideInAPackage("..domain.usecase..")
            .because("business flow lives in one place");
}
