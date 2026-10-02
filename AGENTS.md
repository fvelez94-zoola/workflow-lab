# AGENTS.md: source of truth for this repository

This file is the **single source of truth** for how to work in this repository. Humans and AI agents must read it
before reading, writing, reviewing or planning any change.

- If any other document, comment or habit conflicts with this file, **this file wins**.
- If this file is wrong or outdated, say so and **fix it in the same change** that makes it wrong. Never diverge
  silently.
- The layer READMEs ([domain](domain/README.md), [infrastructure](infrastructure/README.md),
  [applications](applications/README.md)) and the [main README](README.md) explain the architecture for readers.
  This file is the operational rulebook: what to do, where, and how.

## Table of contents

1. [Project facts](#1-project-facts)
2. [Architecture: layers and where things go](#2-architecture-layers-and-where-things-go)
3. [Keeping classes small: helpers and utilities](#3-keeping-classes-small-helpers-and-utilities)
4. [Code standards](#4-code-standards)
5. [Naming conventions](#5-naming-conventions)
6. [OpenAPI standards](#6-openapi-standards)
7. [Persistence standards](#7-persistence-standards)
8. [Testing standards, layer by layer](#8-testing-standards-layer-by-layer)
9. [Build and Gradle conventions](#9-build-and-gradle-conventions)
10. [Documentation standards](#10-documentation-standards)
11. [Checklists](#11-checklists)
12. [Decision log](#12-decision-log)
13. [Working agreement and definition of done](#13-working-agreement-and-definition-of-done)

---

## 1. Project facts

**Purpose.** A Spring Boot showcase of Clean Architecture (ports and adapters). The sample domain (lab
workflows) is deliberately small: the architecture is the product, the business logic is only a vehicle.

| Item | Value |
|---|---|
| Language / runtime | Java 25 (Gradle toolchain) |
| Framework | Spring Boot 3.5.16 (kept on 3.x on purpose, see the [decision log](#12-decision-log)) |
| Build | Gradle multi-module, wrapper 9.7.1. Versions live in [`gradle.properties`](gradle.properties) |
| Persistence | Spring Data JPA, in-memory H2 |
| HTTP contract | OpenAPI 3.0.3, spec-first, OpenAPI Generator 7.25.0 (`spring` generator, interfaces and DTOs only) |
| Boilerplate | Lombok (restricted, see [section 4](#4-code-standards)) |
| Tests | JUnit 5, AssertJ, Mockito, Spring Boot Test, ArchUnit 1.5.1 |
| Root package | `com.hexagonal.workflowlab` |
| Gradle root project | `workflow-lab` |

### Commands

```bash
./gradlew clean build                                  # compile every module and run every test (the gate)
./gradlew :applications:app-service:bootRun            # run the app on http://localhost:8080
./gradlew :<module-path>:test                          # one module, e.g. :domain:model:test
./gradlew :infrastructure:entry-points:rest-api:openApiGenerate   # regenerate the HTTP interfaces and DTOs
```

H2 console: `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:mem:workflowlab`).

IntelliJ IDEA: the shared run configuration in `.idea/runConfigurations` gives a ready **Workflow Lab** (Play).
It is the only part of `.idea/` that is committed.

### Module map

| Module (Gradle path) | Layer | Role |
|---|---|---|
| `:domain:model` | Domain | Entities, value objects, rules, domain services, **all ports** (`port/in`, `port/out`) |
| `:domain:usecase` | Domain | One `@Service` class per use case; orchestration only |
| `:infrastructure:entry-points:rest-api` | Infrastructure (driving) | OpenAPI contract, controllers, DTO mappers, presentation helpers, error handler |
| `:infrastructure:driven-adapters:jpa-repository` | Infrastructure (driven) | JPA entities, Spring Data repositories, repository adapters |
| `:infrastructure:driven-adapters:lab-runner-log` | Infrastructure (driven) | `RunSubmissionGateway` adapter (logs) |
| `:infrastructure:driven-adapters:protocol-document` | Infrastructure (driven) | `ProtocolDocumentWriter` adapter (Markdown, CSV) |
| `:applications:app-service` | Application | `main()`, wiring, sample data, system and architecture tests |

### Dependency graph (compile time)

```
rest-api, jpa-repository, lab-runner-log, protocol-document  ──►  model
usecase                                                      ──►  model
app-service                                                  ──►  usecase, model     (adapters: runtimeOnly)
```

Picture: [`docs/images/architecture-modules.svg`](docs/images/architecture-modules.svg).

---

## 2. Architecture: layers and where things go

### 2.1 The dependency rule

Source code dependencies point **inward**, toward `domain/model`. The domain depends on no other layer.
Adapters know the domain's ports, **never each other**, and never `domain/usecase`.

### 2.2 The decision test

Before creating or moving a class, ask:

> Would this rule still exist if REST became a command line, or JPA became plain files?
> **Yes** → it is business: it belongs in the **domain**.
> **No** → it is a technology detail: it belongs in **infrastructure**.

Then use this table:

| If the class is... | It goes in | Example |
|---|---|---|
| A business rule, invariant or state transition | `domain/model`, as a method on the aggregate | `Workflow.publish()` |
| A pure algorithm over domain data | `domain/model`, helper (package-private if only one aggregate uses it) | `CriticalPathCalculator`, `WorkflowGraph` |
| A business assumption or policy | `domain/model`, `*Policy` | `NodeDurationPolicy` |
| A domain service (logic spanning objects) | `domain/model`, stateless collaborator | `WorkflowAnalyzer` |
| A contract the application **offers** | `domain/model/.../port/in` | `PublishWorkflow` |
| A contract the application **needs** | `domain/model/.../port/out` | `WorkflowRepository` |
| Format-free data to be exported | `domain/model` | `ProtocolSheet` |
| A business exception | `domain/model/.../exception` | `WorkflowNotFoundException` |
| A "load, act, save" sequence | `domain/usecase` | `PublishWorkflowUseCase` |
| Plumbing repeated across use cases (neither business nor technology) | `domain/usecase`, helper | `WorkflowLookup` |
| DTO to domain mapping, HTTP status, display format, `Content-Type` | `rest-api` | `NodeRestMapper`, `DurationFormatter`, `ProtocolMediaType` |
| Shape validation of the HTTP input | `rest-api` | `RequiredParameters` |
| Tables, entities, queries, row mapping | `jpa-repository` | `WorkflowEntity`, `NodeEmbeddableMapper` |
| A file format, escaping or serialization rule | the adapter that writes it | `CsvEscaper` in `protocol-document` |
| Another external system (HTTP client, queue, ...) | a **new** driven-adapter module | see [11.4](#114-add-a-driven-adapter) |
| `main()`, beans for JDK or library classes, profiles, seeding | `app-service` | `WorkflowLabApplication`, `SampleDataConfig` |

### 2.3 Layer by layer

#### Domain layer

The core: business rules and use cases. Everything else depends on it; it depends on nothing outside
itself except Spring stereotypes (see 2.4).

**`domain/model`: belongs**
- Aggregates and entities **with behavior**: invariants and transitions are methods on the owner of the data.
- Value objects and enums, as immutable records where possible.
- Business rules, validators, graph algorithms, policies, domain services.
- Domain exceptions that name what went wrong in business terms.
- **Input ports** (`port/in`): one interface per use case. **Output ports** (`port/out`): one interface per
  need from the outside world. Signatures use **domain types only**.
- Command objects used in port signatures (for example `WorkflowDraft`).

**`domain/model`: does not belong**
- JPA, Jackson, validation or web types and annotations. DTOs. JPA entities.
- Presentation or format logic (human-readable text, CSV, `Content-Type`, HTTP status).
- Any I/O (database, HTTP, files, messaging); that is what output ports are for.
- Spring beyond `@Component` on the stateless collaborators in `model.analysis`.
- Any dependency on `domain/usecase` or `infrastructure`.

**`domain/usecase`: belongs**
- One class per use case: `@Service`, `implements` exactly one input port.
- Orchestration only: load through output ports, ask the domain to act, persist or send through output ports.
- Shared application-level helpers (`WorkflowLookup`).

**`domain/usecase`: does not belong**
- Business rules (they go on the aggregate or a domain service).
- SQL, HTTP, JSON, file formats, or any technology knowledge.
- References to adapter or infrastructure classes. Only output-port interfaces.
- Technical or presentation helpers.
- Spring beyond `@Service`.

#### Infrastructure layer

The technology details. Adapters translate between an outside representation and domain types.

**Entry points (driving adapters): belongs**
- The contract (OpenAPI) and the code generated from it.
- Controllers that only translate and delegate to an **input port**.
- DTO to domain mappers (one per concern), shape validation of the input, presentation helpers.
- The single place that turns domain exceptions into protocol errors (`GlobalExceptionHandler`).

**Driven adapters: belongs**
- One class per output port, implemented with a concrete technology.
- The technology's own model (JPA entities, SQL, file layouts) and the mappers to the domain.
- Technical helpers that exist only because the technology exists (`CsvEscaper`, `MarkdownTable`).
- Framework dependencies (Spring Web, JPA, validation, ...).

**Infrastructure: does not belong**
- Business rules or decisions of any kind.
- Calls to use case **classes** (entry points depend on input-port interfaces).
- Dependencies between adapters, or from a driven adapter to an entry point.
- Technology types leaking outward: JPA entities and generated DTOs never leave their module.
- Orchestration of several output ports (that is a use case).
- Whole-application wiring (that is the application layer).

#### Application layer

The composition root. The only module that knows every other module and the only one with `main()`.

**Belongs**: the `@SpringBootApplication` class; beans for classes that cannot be annotated (`Clock`);
`application.yaml` and profiles; start-up tasks that go **through input ports**; whole-system tests and the
architecture tests; the list of `runtimeOnly` adapters.

**Does not belong**: business logic; controllers, entities, mappers or any adapter code; compile-time
references to adapter classes (adapters are `runtimeOnly`); shared utilities for other layers; per-feature code.

### 2.4 Spring inside the domain (the only exception)

This is a Spring Boot showcase, so a minimal, enforced contact with Spring is allowed:

- `@Service` on `domain/usecase` classes (`*UseCase` only).
- `@Component` on the three stateless collaborators in `domain/model/.../analysis`
  (`NodeDurationPolicy`, `CriticalPathCalculator`, `WorkflowAnalyzer`).
- `domain/model` and `domain/usecase` have **only `spring-context`** on their classpath. Web, data, JPA and
  Jackson are not available, so importing them does not compile.
- Everything else in `domain/model` stays plain Java. **Do not add Spring annotations anywhere else in the
  domain.** A class that needs a bean but cannot carry an annotation (a JDK class) gets a `@Bean` method in
  `WorkflowLabApplication`.
- Component scanning starts at `WorkflowLabApplication` (`com.hexagonal.workflowlab`) and reaches use cases,
  collaborators and adapters. **There is no configuration class that registers use cases.**

### 2.5 Architecture rules enforced as code

[`ArchitectureTest`](applications/app-service/src/test/java/com/hexagonal/workflowlab/ArchitectureTest.java)
fails the build when:

1. `domain.model` depends on `jakarta..`, Hibernate, Jackson, SLF4J, `domain.usecase` or `infrastructure`.
2. `domain.model` (outside `model.analysis`) depends on any `org.springframework..` class.
3. `domain.model` or `domain.usecase` depends on Spring beyond `org.springframework.stereotype`.
4. `domain.usecase` depends on `jakarta..`, Hibernate or `infrastructure`.
5. `@Service` is used outside `domain.usecase`, or on a class not named `*UseCase`.
6. An entry point depends on `domain.usecase` or `infrastructure.adapter`.
7. A driven adapter depends on `infrastructure.entrypoint` or `domain.usecase`.
8. Two driven adapters depend on each other (one slice per `infrastructure.adapter.(*)`).
9. A class named `*Formatter`, `*Escaper`, `*MediaType`, `*RestMapper` or `MarkdownTable` sits outside
   `infrastructure`.
10. JPA entities are used outside `infrastructure.adapter.jpa`.
11. An input port is implemented outside `domain.usecase`.

**Never weaken, delete or skip a rule to make the build pass.** A failing rule means the change is in the wrong
place. Change a rule only as an explicit, recorded design decision (update this section and the
[decision log](#12-decision-log)).

---

## 3. Keeping classes small: helpers and utilities

A use case, a mapper or a controller that does several jobs becomes a "monster": hard to read, hard to test,
and a magnet for unrelated changes. Extract **early**, and put the extraction in the **right layer**.

### 3.1 Warning signs

- A use case contains `if` statements about the business: move that logic onto the aggregate or into a domain
  service. A use case should read like a short script (load, act, save).
- A class has more than one reason to change (a mapper that maps workflows, nodes, experiments **and**
  validates input).
- The same private method or the same three lines appear in several classes (`findById(...).orElseThrow(...)`
  was repeated in every use case, which became `WorkflowLookup`).
- A class is long enough that you must scroll to find the method you want. As a guideline, review any class
  past roughly 150 lines for a second responsibility; this is a prompt to look, not a hard limit.
- A method mixes levels: business decision, technology call and formatting in the same body.
- Someone proposes a `Utils`, `Helper`, `Manager` or `Common` class. Do not create generic dumping grounds.

### 3.2 Where each kind of helper goes

| Kind of helper | Layer and module | Example | Notes |
|---|---|---|---|
| Business policy or assumption | `domain/model` | `NodeDurationPolicy` | Returns domain values (`Duration`), never text |
| Algorithm over domain data | `domain/model` | `CriticalPathCalculator`, `WorkflowGraph` | Pure Java; package-private when one aggregate owns it |
| Domain service composing helpers | `domain/model` | `WorkflowAnalyzer` | One entry point for the use case |
| Format-free content for an adapter | `domain/model` | `ProtocolSheet` | The data, not its rendering |
| Application plumbing shared by use cases | `domain/usecase` | `WorkflowLookup` | Neither business nor technology |
| Presentation (how a value is shown or named) | `rest-api` | `DurationFormatter`, `ProtocolMediaType` | Could change without touching a rule |
| Shape validation of the transport input | `rest-api` | `RequiredParameters` | Values that make no business sense stay a domain rule |
| DTO <-> domain mapping | `rest-api` | `NodeRestMapper`, `WorkflowRestMapper`, `ExperimentRestMapper`, `AnalysisRestMapper` | One mapper per concern |
| Domain <-> persistence mapping | `jpa-repository` | `NodeEmbeddableMapper`, `WorkflowEntityMapper`, `ExperimentEntityMapper` | One mapper per concern |
| Format or escaping rule | the adapter that writes the format | `CsvEscaper`, `MarkdownTable` | Package-private: nothing else may reach it |

### 3.3 Rules for helpers

1. **Name by responsibility**, not by role: `CsvEscaper`, not `StringUtils`.
2. **Narrowest visibility that works.** Helpers are package-private by default (`WorkflowGraph`, `CsvEscaper`,
   `MarkdownTable`). Make a helper public only when another package genuinely needs it.
3. **Static or bean?**
   - Stateless pure functions that need no collaborators: `final` class, private constructor, `static`
     methods (all mappers, `DurationFormatter`, `CsvEscaper`, `WorkflowLookup`).
   - Collaborators that use cases receive or that you may want to replace or decorate: instance classes, with
     `@Component` only if they are one of the allowed domain collaborators (section 2.4), otherwise built by
     the adapter that owns them.
4. **A helper lives in the layer whose concern it serves.** If it knows about a technology or a display format,
   it is infrastructure and the domain must not see it (rule 9 of `ArchitectureTest`).
5. **Do not push logic outward to shrink a class.** Splitting a use case by moving business rules into an
   infrastructure "service" makes the domain anemic and untestable. Move them **down** into the model instead.
6. **Pure logic does not get a port.** A port exists to isolate I/O or a volatile technology. A graph algorithm
   behind an interface is needless indirection.
7. **Every new helper gets its own focused test** in its own layer (section 8).

### 3.4 Refactors that already happened (precedent)

- `WorkflowRestMapper` did four jobs → split into `NodeRestMapper`, `WorkflowRestMapper`, `ExperimentRestMapper`,
  `AnalysisRestMapper`, plus `RequiredParameters`.
- `WorkflowEntityMapper` also flattened nodes → `NodeEmbeddableMapper` extracted.
- Repeated "find or throw not-found" in three use cases → `WorkflowLookup`.

---

## 4. Code standards

### 4.1 Language and style

- **Java 25.** Use modern features where they make intent clearer: records, sealed interfaces, pattern-matching
  `switch`, text blocks, `List.copyOf`, `Stream.toList()`.
- **Immutability by default.** Value objects are records; collections are copied defensively (`List.copyOf`)
  in constructors and never exposed mutable.
- **Sealed hierarchies with exhaustive `switch`** (no `default`) for closed sets such as `WorkflowNode` and
  `DocumentFormat`. Adding a case must stop the build everywhere it is not handled.
- **Aggregates**: private constructor, static factories (`draft(...)`, `fromWorkflow(...)`, `restore(...)`).
  `restore(...)` is for persistence adapters; creation factories enforce invariants.
- **Invariants live in the type.** Compact record constructors and aggregate methods reject invalid state.
- **Exceptions**: unchecked, specific, named in business terms, in `domain.model.exception`. The domain never
  decides an HTTP status.
- **Time**: use `java.time`. Inject `Clock` into use cases (`clock.instant()`); domain methods receive an
  `Instant`. No `Instant.now()` or `LocalDate.now()` in domain or use case code.
- **Optionals** only as return values of lookups (`findById`). Never as parameters or fields.
- **No `null` in domain APIs** unless a field is genuinely optional and documented.
- **Dependency injection**: constructor injection only, via Lombok `@RequiredArgsConstructor`, on `final`
  fields. No field or setter injection.
- **Imports**: no wildcards; static imports first, then the rest, alphabetically.
- **Formatting**: 4 spaces, lines up to about 120 characters, one top-level type per file.
- Compile with `-parameters` (already configured in the root `build.gradle`).

### 4.2 Lombok policy

| Annotation | Allowed on |
|---|---|
| `@RequiredArgsConstructor` | Use cases, adapters, controllers, domain collaborators (constructor injection) |
| `@Getter` | Aggregates and other classes that expose state; JPA entities |
| `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor` | **JPA entities and embeddables only** |
| `@Slf4j` | Adapters and configuration that log |

`@Data`, `@Value`, `@Builder`, `@With`, `@ToString`, `@EqualsAndHashCode` are **not used**. Immutable values are
records; everything else gets only what it needs.

### 4.3 Comments and Javadoc

- Explain **why**, not what. A class-level Javadoc says what the class is for and, when non-obvious, why it lives
  in this layer.
- **Every production package has a `package-info.java`** with the three statements:
  `Owns: ...`, `May depend on: ...`, `Invariant: ...`. Create one for every new package and keep it accurate.
- Do not leave commented-out code, `TODO` without an owner, or Javadoc that restates the method name.
- In domain Javadoc prefer `{@code ...}` over `{@link ...}` across packages that would force new imports.

### 4.4 Controllers, use cases and adapters at a glance

Use case template:

```java
@Service
@RequiredArgsConstructor
public class PublishWorkflowUseCase implements PublishWorkflow {

    private final WorkflowRepository workflows;           // output port

    @Override
    public Workflow publish(WorkflowId id) {
        Workflow workflow = WorkflowLookup.requireById(workflows, id);
        workflow.publish();                               // the rules live in the aggregate
        return workflows.save(workflow);
    }
}
```

Controller template: implements the generated `*Api`, depends on **input-port interfaces**, maps with a
`*RestMapper`, delegates, returns `ResponseEntity`. No `if` about business, no try/catch for domain exceptions
(`GlobalExceptionHandler` does that).

Driven adapter template: `@Repository` or `@Component`, `implements` one output port, depends on the
technology's own classes, maps with a mapper, **returns domain types only**.

---

## 5. Naming conventions

### 5.1 Packages and modules

| Thing | Convention | Example |
|---|---|---|
| Gradle modules | kebab-case, named after the role | `rest-api`, `jpa-repository`, `protocol-document` |
| Domain packages | `com.hexagonal.workflowlab.domain.model.<concept>` | `...model.workflow`, `...model.analysis` |
| Ports | `...domain.model.port.in` and `...port.out` | |
| Use case packages | `com.hexagonal.workflowlab.domain.usecase.<concept>` | `...usecase.workflow` |
| Entry point | `com.hexagonal.workflowlab.infrastructure.entrypoint.<technology>` | `...entrypoint.rest` |
| Driven adapters | `com.hexagonal.workflowlab.infrastructure.adapter.<name>` (no hyphens) | `...adapter.jpa`, `...adapter.labrunner`, `...adapter.protocoldocument` |
| Generated code | `...entrypoint.rest.generated.api` and `...generated.model` | |

### 5.2 Classes

| Kind | Convention | Examples |
|---|---|---|
| Aggregate / entity / value object | Plain domain noun | `Workflow`, `Experiment`, `NodeId`, `Dependency` |
| Identifier value object | `<Concept>Id` record | `WorkflowId`, `ExperimentId` |
| Sealed implementation | `<Variant><Base-or-Kind>` | `MixNode`, `IncubateNode` |
| Business rule | `<What>Rule` (package-private) | `CycleRule`, `EmptyWorkflowRule` |
| Policy / calculator / analyzer | `*Policy`, `*Calculator`, `*Analyzer` | `NodeDurationPolicy` |
| Domain exception | `<Problem>Exception` | `WorkflowNotFoundException` |
| **Input port** | Verb phrase, **no suffix** | `PublishWorkflow`, `ExportProtocol`, `WorkflowCrud` |
| **Use case (implementation)** | `<InputPort>UseCase`, `@Service` | `PublishWorkflowUseCase` |
| Command object for a port | `<Concept>Draft` / `<Concept>Command` record | `WorkflowDraft` |
| **Output port** | Named by the role it plays | `WorkflowRepository`, `RunSubmissionGateway`, `ProtocolDocumentWriter` |
| Controller | `<Tag>Controller`, implements the generated `<Tag>Api` | `WorkflowsController` |
| Generated DTO | `<SchemaName>Dto` (automatic) | `WorkflowResponseDto` |
| REST mapper | `<Concept>RestMapper` | `NodeRestMapper` |
| Error handler | `GlobalExceptionHandler` | |
| Presentation helper | `<What>Formatter`, `*MediaType` | `DurationFormatter`, `ProtocolMediaType` |
| JPA entity | `<Concept>Entity`; embedded rows `<Concept>Embeddable` | `WorkflowEntity`, `NodeEmbeddable` |
| Spring Data repository | `SpringData<Concept>Repository` | `SpringDataWorkflowRepository` |
| Persistence mapper | `<Concept>EntityMapper` / `<Concept>EmbeddableMapper` | `NodeEmbeddableMapper` |
| Adapter for a port | `<Technology><Port>Adapter` or `<Style><Port>` | `JpaWorkflowRepositoryAdapter`, `LoggingRunSubmissionGateway`, `ProtocolDocumentWriterAdapter` |
| Format helper | `<Format><What>` | `CsvEscaper`, `MarkdownTable`, `CsvProtocolFormatter` |
| Application class | `<App>Application` | `WorkflowLabApplication` |

Two naming rules matter most: input ports are **named for what the application can do** and the class that
does it ends in **`UseCase`**; output ports are named for **the role they play for the domain**, never for the
technology (`WorkflowRepository`, not `JpaWorkflowRepository`).

### 5.3 Methods, fields, tests, files

- Methods are verbs: `publish`, `findById`, `toDomain`, `toEntity`, `toResponse`, `requireById`.
- Mapper methods: `toDomain` / `toDto` / `toResponse` / `toEntity` / `toDraft`.
- **Test classes**: `<ClassUnderTest>Test`. **Test methods: `shouldXYZ`**, starting with `should` and stating the
  expected behavior: `shouldReturn404WhenWorkflowIsMissing`, `shouldRejectPublishWhenGraphHasCycle`. This applies
  to every new or rewritten test, in every layer.
- OpenAPI: files and schemas in `PascalCase` (`WorkflowResponse.yaml`); path files in `kebab-case`
  (`workflow-publish.yaml`); `operationId` in `camelCase` verbs (`publishWorkflow`); enum values in
  `UPPER_SNAKE_CASE`.
- Constants: `UPPER_SNAKE_CASE`. Spring properties for this project: prefix `workflowlab.`.

---

## 6. OpenAPI standards

### 6.1 Principles

- **Spec-first.** The YAML is the source of truth. Generated code is never edited and never committed (it is
  produced into `build/generated/openapi` by `openApiGenerate` before every compile).
- **Interfaces and DTOs only.** The generator produces `*Api` interfaces and `*Dto` classes; controllers are
  handwritten and implement the interfaces.
- **DTOs are not domain objects and not entities.** They never cross into `domain` or `jpa-repository`.

### 6.2 Layout

All files live in `infrastructure/entry-points/rest-api/src/main/resources/openapi/`:

```
workflow-lab-api.yaml     root: openapi version, info, servers, tags, and the list of paths ($ref only)
paths/                    one file per path item: workflows.yaml, workflow.yaml, workflow-publish.yaml,
                          workflow-experiments.yaml, workflow-analysis.yaml, workflow-protocol.yaml
schemas/<context>/        one schema per file, grouped by business context (workflow/, experiment/,
                          analysis/, protocol/, common/)
parameters/               reusable parameters (WorkflowId.yaml)
responses/                reusable error responses (BadRequest, NotFound, Conflict, Unprocessable)
```

Rules:

- The root file contains **no schemas or operations**, only `$ref`s to `paths/`.
- **The file name is the model name**: `schemas/workflow/Node.yaml` generates `NodeDto`. Rename the file to
  rename the class.
- References are **relative file paths** (`$ref: ../schemas/workflow/WorkflowResponse.yaml`,
  `$ref: NodeType.yaml` between siblings), never `#/components/...`.
- Every file starts with a one-line comment saying what it is.
- One path file per path item; group operations by resource, not by HTTP verb.

### 6.3 Operations

- Every operation has `tags` (exactly one), `operationId` (unique, camelCase verb phrase), `summary`, and every
  response it can return.
- **Tags decide the generated interface and the controller**: tag `Workflows` → `WorkflowsApi` →
  `WorkflowsController`. Current tags: `Workflows`, `Experiments`, `Analysis`, `Protocols`. A new tag means a
  new controller; keep controllers small by tagging per use-case area.
- Path parameters reuse `parameters/WorkflowId.yaml` (UUID). Optional query parameters declare a `default`.
- Success codes: `200` read/update, `201` create, `204` delete.

### 6.4 Errors

| Status | When | Domain exception |
|---|---|---|
| `400` | Malformed request, missing required field, wrong type or enum value | (Spring and bean validation) |
| `404` | Resource does not exist | `WorkflowNotFoundException` |
| `409` | Operation not allowed in the current state | `InvalidWorkflowStateException` |
| `422` | Well-formed request that breaks a business rule | `InvalidWorkflowException` (carries a list of `ValidationFailure`) |

- All errors use `ErrorResponse` (`code`, `message`, optional `failures[]` with `code` and `message`), defined in
  `schemas/common/`.
- **Errors are always JSON**, even when the client asked for `text/csv` or `text/markdown`.
  `GlobalExceptionHandler` forces `Content-Type: application/json`.
- `GlobalExceptionHandler` is the **only** place that maps exceptions to statuses.

### 6.5 Schemas

- Closed sets are enums with `UPPER_SNAKE_CASE` values that **equal the Java enum names** (the generated enum
  converters and `valueOf` mappings rely on it).
- Polymorphism is expressed with a **flat DTO** plus a `type` discriminator and optional parameters
  (`NodeDto`), not `oneOf`. The mapper checks which parameters each type needs (`RequiredParameters`).
  Business validity of the values stays in the domain.
- Mark `required` precisely; optional fields are nullable in Java.
- Use `format: uuid`, `format: date-time` (generated as `OffsetDateTime`) and `format: int64` for long counts.
- Binary-ish or document responses declare their media types (`text/markdown`, `text/csv`) with `type: string`.

### 6.6 Generator configuration

Set in [`rest-api/build.gradle`](infrastructure/entry-points/rest-api/build.gradle) (do not change casually):
`generatorName=spring`, `interfaceOnly`, `useSpringBoot3`, `useTags`, `skipDefaultInterface`,
`openApiNullable=false`, `documentationProvider=none`, `annotationLibrary=none`, `useBeanValidation`,
`dateLibrary=java8`, `hideGenerationTimestamp`, `modelNameSuffix=Dto`, API package `...generated.api`, model
package `...generated.model`.

- **`openApiGenerate` declares `inputs.dir` on the whole `openapi/` folder.** The plugin tracks only the root
  file by default; without that line, editing a referenced file would **not** regenerate the code. Keep it.
- The generator also emits an unused `org.openapitools.configuration.EnumConverterConfiguration`. It is harmless;
  do not depend on it.

### 6.7 Adding an endpoint

See [11.2](#112-add-an-endpoint-to-the-http-contract).

---

## 7. Persistence standards

- **Entities are not domain objects.** `*Entity` and `*Embeddable` classes live in
  `jpa-repository/.../adapter/jpa/entity`, never leave that module, and are rebuilt into domain objects by
  mappers (`Workflow.restore(...)`, `Experiment.restore(...)`).
- **Identifiers are assigned by the domain** (`UUID`), not generated by the database.
- Enumerated values (state, node type, instruction type) are stored as **strings**; the entity does not import
  domain enums.
- Collections of value rows use `@ElementCollection(fetch = EAGER)` with `@OrderColumn`, so order is preserved
  and there are no multiple-bag fetch problems.
- A sealed hierarchy is flattened into one row type with nullable columns for the variants (`NodeEmbeddable`);
  the exhaustive `switch` lives in a dedicated mapper (`NodeEmbeddableMapper`).
- **Avoid reserved or ambiguous column names** (`from`, `to`, `position`, `type`): use descriptive names
  (`source_node_key`, `node_order`, `node_type`).
- Adapters implement **only the port** the domain declared: no Spring Data types in the signature.
- Schema is created by Hibernate (`ddl-auto: create-drop`) on in-memory H2; `open-in-view` is `false`.
- Repository adapters return domain objects and `Optional` for lookups; they never return entities.

---

## 8. Testing standards, layer by layer

### 8.1 Rules for every test

- **Naming**: class `<ClassUnderTest>Test`, methods `shouldXYZ` stating the expected behavior (section 5.3).
- **Test the behavior, not the implementation.** Assert outcomes and interactions that matter, not internals.
- **Use real domain objects.** Never mock aggregates, value objects, policies or other pure domain classes.
  Mock only **ports** (and only in the layer where the port is a collaborator).
- **Fixtures are local**: small `private static` factory methods inside the test class. No shared test-fixture
  module.
- **Determinism**: fixed `Clock` (`Clock.fixed(...)`) or fixed `Instant`; no sleeps, no randomness in assertions.
- Prefer AssertJ (`assertThat`, `assertThatThrownBy`). Use Mockito strict stubs (`MockitoExtension`).
- Every new class gets a test in its own layer. Every new helper gets a focused test.
- **Test the failure paths**, not only the happy path: not found, wrong state, invalid input, ports not called.

### 8.2 Approach per layer

| Module | What is tested | Tooling | Do not |
|---|---|---|---|
| `domain/model` | Aggregates, rules, algorithms, policies, conversions, value objects | Plain JUnit + AssertJ | Use Spring, Mockito or a database |
| `domain/usecase` | Orchestration: which ports are called, in what order, with what; failure paths | JUnit + Mockito on **output ports only** | Mock domain objects; re-test business rules already covered in `model` |
| `rest-api` | Status codes, request/response mapping, error bodies, presentation helpers, mappers | Standalone `MockMvc` with the **input ports mocked**; plain JUnit for helpers and mappers | Start a Spring context; use a real use case |
| `jpa-repository` | Domain ↔ table round trips, ordering, updates, deletes | `@DataJpaTest` on in-memory H2 with the adapter `@Import`ed; plain JUnit for mappers | Mock the Spring Data repository; test business rules |
| `lab-runner-log` | The adapter fulfils the port contract | Plain JUnit | Start Spring |
| `protocol-document` | Formatters and helpers produce exact output | Plain JUnit | Start Spring |
| `app-service` | End-to-end flows through the real context; architecture rules | `@SpringBootTest` + `MockMvc`; ArchUnit | Duplicate unit-level assertions |

### 8.3 Templates

**Use case test (Mockito on ports, real domain):**

```java
@ExtendWith(MockitoExtension.class)
class PublishWorkflowUseCaseTest {

    @Mock private WorkflowRepository workflows;
    private PublishWorkflowUseCase useCase;

    @BeforeEach void setUp() { useCase = new PublishWorkflowUseCase(workflows); }

    @Test
    void shouldNotSaveWhenValidationFails() {
        Workflow empty = Workflow.draft("Empty", null, List.of(), List.of());   // real domain object
        when(workflows.findById(empty.getId())).thenReturn(Optional.of(empty));

        assertThatThrownBy(() -> useCase.publish(empty.getId())).isInstanceOf(InvalidWorkflowException.class);
        verify(workflows, never()).save(any());
    }
}
```

**Controller test (standalone MockMvc, input ports mocked, real error handler):**

```java
mvc = MockMvcBuilders.standaloneSetup(new WorkflowsController(workflowCrud, publishWorkflow))
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
```

**Adapter test (JPA slice):** `@DataJpaTest` + `@Import(JpaWorkflowRepositoryAdapter.class)`; the module has a
test-only `JpaTestApplication` (`@SpringBootApplication`) because it has no `main()`. After saving, call
`entityManager.flush()` and `entityManager.clear()` so the read really hits the database.

**System test:** `@SpringBootTest(properties = "workflowlab.sample-data.enabled=false") @AutoConfigureMockMvc`
so the seed data does not interfere.

### 8.4 Architecture tests

- Rules live in `ArchitectureTest` and nowhere else.
- **Every new rule must be proven to bite.** Introduce a deliberate violation (a temporary class), confirm the
  rule fails, then remove the violation. This was done for the existing rules; repeat it for new ones.
- ArchUnit's `failOnEmptyShould` is on: a rule that matches no class fails, which protects against vacuous rules.

### 8.5 The gate

`./gradlew clean build` must pass before any change is considered done. Do not report a change as working
without having run it.

---

## 9. Build and Gradle conventions

- **Versions** (Spring Boot, OpenAPI Generator, ArchUnit) live in `gradle.properties`. Plugin versions are
  resolved in `settings.gradle` (`pluginManagement`).
- The **root `build.gradle`** configures every leaf module the same way: `java-library`,
  `io.spring.dependency-management` with the Spring Boot BOM (alignment only), Java 25 toolchain, Lombok,
  JUnit, AssertJ, `-parameters`.
- **Each module declares only what its layer may have.** The Spring Boot BOM does not add Spring to a classpath.
  Adding a dependency to a module is an architectural decision: check the layer rules first.
- Allowed non-test dependencies by module:

  | Module | Allowed |
  |---|---|
  | `model` | `spring-context` (stereotype only) |
  | `usecase` | `model`, `spring-context` (stereotype only) |
  | `rest-api` | `model`, Spring Web, validation, OpenAPI Generator plugin |
  | `jpa-repository` | `model`, Spring Data JPA |
  | `lab-runner-log` | `model`, `spring-context`, SLF4J |
  | `protocol-document` | `model`, `spring-context` |
  | `app-service` | `model`, `usecase`, `spring-boot-starter`; adapters as `runtimeOnly`; H2 `runtimeOnly` |

- Adapters are **`runtimeOnly`** in `app-service`. Application code must not import them.
- Do not commit `build/`, `.gradle/` or generated sources.
- **Git hygiene is defined by [`.gitignore`](.gitignore)**, organized in commented sections. Rules to keep:
  - Inside `.idea/` only `runConfigurations/` is shared. Do not commit `gradle.xml`, `misc.xml`, `workspace.xml`
    or similar: IntelliJ rewrites them on every sync and they contain machine-specific paths.
  - The `out/` rule is neutralized for source code (`!**/src/main/**/out/`) because `domain.model.port.out` is a
    real package. Never remove those exceptions.
  - Secrets and local overrides (`.env*`, `application-local.*`, keys, `.claude/settings.local.json`) are
    ignored. Share a template (for example `.env.example`) instead of the real file.
- **JDK**: the build needs Java 25 through a Gradle toolchain. `settings.gradle` applies the `foojay` resolver so
  Gradle downloads it when it is missing. Do not hard-code a JDK path in the build or in committed IDE files.
- **Run configurations**: when a module path or the main task changes, update the file in
  `.idea/runConfigurations` in the same change. Keep **exactly one** shared run configuration (the one that starts
  the app): IntelliJ stores which configuration is selected in the local `workspace.xml`, which cannot be
  shared, so with a single configuration there is nothing to choose and Play works on a fresh open.

---

## 10. Documentation standards

- **All documentation is in English**: READMEs, this file, Javadoc, `package-info.java`, code comments, OpenAPI
  descriptions, commit messages. (A non-ASCII character is acceptable only as deliberate test data.)
- Each **layer README** follows the same outline: **Definition**, **What belongs here**, **What does not belong
  here**, **Examples from this project** (last), with a short "how it is tested" note.
- The **main README** has the documentation map, the module-dependency diagram, the folder tree, the
  request-flow diagram, the rules, the decision table, run and test instructions.
- Documentation focuses on **the architecture, not the sample business logic**.
- **Diagrams are SVG files in `docs/images/`** (renderable everywhere, editable as text, opaque white
  background). Do not embed Mermaid or other code that needs a renderer.
- Links are **relative** and must resolve. Check them after moving or renaming files.
- **Update documentation in the same change** that alters architecture, modules, rules, naming or commands:
  this file, the affected layer README, the main README, and the diagrams if module dependencies changed.
- A `package-info.java` accompanies every production package (Owns / May depend on / Invariant).

---

## 11. Checklists

### 11.1 Add a use case

1. Add the **input port** in `domain/model/.../port/in` (verb-phrase name, domain types only; a `*Draft` or
   `*Command` record if it needs input data).
2. Put business rules **on the aggregate or in a domain service**, with plain JUnit tests in `model`.
3. If the use case needs something from the outside, add an **output port** in `port/out` (see 11.4).
4. Create `<Port>UseCase` in `domain/usecase/<concept>`: `@Service`, `@RequiredArgsConstructor`, `implements`
   the port, orchestration only. Reuse `WorkflowLookup` when it fits.
5. Test it with Mockito on the output ports.
6. Expose it over HTTP if needed (11.2).
7. Add a system test in `WorkflowLabFlowTest` if it adds a user-visible flow.
8. If you added a rule that deserves it, extend `ArchitectureTest` (and prove it bites).
9. Update the READMEs and this file if conventions or inventory changed.

### 11.2 Add an endpoint to the HTTP contract

1. Add or edit a file in `paths/`; reference it from the root `workflow-lab-api.yaml`. Pick the tag, add an
   `operationId`, list every response (including `404`, `409`, `422`, `400` where they can occur).
2. Add schemas as one file each under `schemas/<context>/`; reuse `parameters/` and `responses/`.
3. Run `./gradlew :infrastructure:entry-points:rest-api:openApiGenerate`.
4. Implement the generated `*Api` in a `*Controller` (new tag → new controller). Depend on **input ports** only.
5. Map with `*RestMapper` (new concern → new mapper). Presentation goes in a helper, never in the domain.
6. Map any new domain exception in `GlobalExceptionHandler`.
7. Test: standalone MockMvc for the controller, plain JUnit for new mappers and helpers, a system test for the flow.

### 11.3 Add a node type (the sealed hierarchy)

The compiler forces every `switch`, so this is a checklist the build will enforce:

1. Domain: new record implementing `WorkflowNode` (add it to `permits`), validated in its compact constructor.
2. `ExperimentConverter` (instructions), `NodeDurationPolicy` (duration), plus tests.
3. REST: `NodeRestMapper` (`toDomain`, `toDto`) and `NodeType.yaml` / `Node.yaml` in the contract (new optional
   parameters), then regenerate.
4. JPA: `NodeEmbeddable` columns if new parameters, and `NodeEmbeddableMapper` (both directions).
5. Tests in each layer; update the docs if the node list is mentioned.

### 11.4 Add a driven adapter

1. Declare the **output port** in `domain/model/.../port/out`, in domain types only. Pure logic does not need a
   port (section 3.3).
2. Create a module under `infrastructure/driven-adapters/<name>`; add it to `settings.gradle`.
3. Its `build.gradle` depends on `:domain:model` plus only the technology it adapts.
4. Package: `com.hexagonal.workflowlab.infrastructure.adapter.<name>` (no hyphens). Add `package-info.java`.
5. Implement the port in a class annotated `@Component` (or `@Repository`). Keep technical helpers
   package-private.
6. Add it as `runtimeOnly` in `applications/app-service/build.gradle`.
7. Test it in isolation (plain JUnit or a slice test).
8. `ArchitectureTest` already keeps adapters independent through its slice rule; run it to confirm.
9. Update the module map and diagram in the READMEs, the diagram in `docs/images`, and this file.

### 11.5 Add a helper or utility

1. Apply the decision test (2.2) to pick the layer. Use the table in 3.2 for the kind of helper.
2. Name it by responsibility; narrowest visibility (3.3).
3. Add a focused test in its layer.
4. If it is a presentation or format helper, make sure its name follows the patterns covered by rule 9 of
   `ArchitectureTest`.

### 11.6 Add a module

Only when a new layer role needs it. Update `settings.gradle`, add the module `build.gradle` with only the
allowed dependencies, a `package-info.java` for each package, tests, `app-service` wiring if it is an adapter,
the READMEs, this file's module map, and the dependency diagram.

---

## 12. Decision log

Decisions already taken. Do not reverse them without an explicit decision, and record any change here.

| # | Decision | Why | Consequence |
|---|---|---|---|
| 1 | **Multi-module by layer** (not by business context) | Gradle enforces the dependency rule at compile time (classpath isolation), keeps tests honest and limits recompilation; clearest way to teach layers. Measured benefits and costs: README, "Why multi-module?" | Business contexts (`workflow`, `experiment`, ...) are sub-packages inside each layer module |
| 2 | **Input ports are interfaces; use cases are classes named `*UseCase`** | The controller depends on a contract, so a use case can be replaced or decorated without touching it; the class that does the work is plainly named a use case | Two files per use case; ports named for the capability, classes end in `UseCase` |
| 3 | **Use cases are orchestrators; business logic lives in the domain model** | An infrastructure "service" with heavy rules makes the domain anemic and untestable without infrastructure | Heavy technical logic goes behind an output port in an adapter; business logic goes on aggregates and domain services |
| 4 | **`@Service` on use cases, `@Component` on three analysis collaborators; no registration config class** | A Spring Boot showcase should feel idiomatic; fewer moving parts | `model` and `usecase` see `spring-context`; ArchUnit restricts Spring in the domain to stereotypes and the `analysis` package (section 2.4); `Clock` is a `@Bean` in the main class |
| 5 | **Spring Boot 3.5.x, not 4.x** | Avoids generator compatibility risk with Spring 7 and Jackson 3 | Starters use the 3.x names (`spring-boot-starter-web`) |
| 6 | **OpenAPI split into many files; flat `NodeDto`; `Dto` suffix** | Readable, reviewable contract; generated code identical to a single file; `oneOf` adds generator complexity | `inputs.dir` on the spec folder; file name = model name |
| 7 | **Separate `protocol-document` adapter module** | Keeps CSV and Markdown helpers out of the domain and unreachable from other modules | Seventh module; adapters are independent (ArchUnit slice rule) |
| 8 | **Domain returns values, adapters format them** (`Duration`, `ProtocolSheet`) | Presentation can change without touching a rule | `DurationFormatter` and `ProtocolMediaType` live in `rest-api` |
| 9 | **Sample data is seeded through an input port** (`SampleDataConfig`) | Seed data obeys the same rules as real traffic | No SQL seed files |
| 10 | **Errors are always JSON** | Clients that request `text/csv` must still get a parseable error | `GlobalExceptionHandler` sets the content type |
| 11 | **Documentation in English; diagrams as SVG in `docs/images`** | Audience and tooling (previews do not render Mermaid) | See section 10 |
| 12 | **`shouldXYZ` test naming** | Tests read as specifications | Applies to every new or rewritten test |
| 13 | **ArchUnit on top of the module structure** | Gradle only controls dependencies between whole modules; it cannot express rules about annotations, naming, packages inside a module, or "stereotypes only" (README, "Why ArchUnit?") | Rules live only in `ArchitectureTest`, are never weakened to get a green build, and each new rule must be proven to bite |
| 14 | **Share one IntelliJ run configuration (start the app); Gradle resolves the JDK** | "Open and press Play" without committing files the IDE rewrites constantly; no machine-specific JDK paths in the repo | `.gitignore` allows only `.idea/runConfigurations/`; `settings.gradle` applies the `foojay` toolchain resolver |

---

## 13. Working agreement and definition of done

### Before changing anything

1. Read this file. Read the README of the layer you will touch.
2. **Verify the real state** of the code instead of assuming. Check that classes, modules and paths exist before
   relying on them.
3. For a non-trivial change (new module, new rule, new dependency, change of a decision), state the plan and
   the open decisions **before** editing, and record the outcome in the decision log.

### Commits and pull requests

- Write commit messages in English, in the imperative mood, with a short subject and a body that explains **why**.
- **Never add a `Co-Authored-By` trailer or any other AI attribution** to a commit message. The author of
  every commit is the repository owner, nobody else.
- Do not rewrite or force-push published history unless the owner explicitly asks for it.

### While changing

- Put each class in the right layer using section 2. When a class grows a second responsibility, extract it
  using section 3.
- Never take a shortcut that crosses a layer boundary "just this once". If the architecture seems to block a
  change, the change is probably in the wrong place; if the architecture itself must change, make that an
  explicit decision.
- Do not weaken `ArchitectureTest`, delete tests, or relax a standard to get a green build.

### Definition of done

- [ ] `./gradlew clean build` passes (compiles every module, runs every test and `ArchitectureTest`).
- [ ] New or changed behavior has tests in the right layer, named `shouldXYZ`, with failure paths covered.
- [ ] New classes follow the naming conventions and sit in the layer the decision test selects.
- [ ] New packages have a `package-info.java`; Javadoc explains why, in English.
- [ ] If the HTTP contract changed: files split correctly, code regenerated, errors mapped, controller thin.
- [ ] Documentation updated in the same change: this file, the layer README, the main README, the diagrams.
- [ ] The result is **reported faithfully**: what was verified, what was not, and any trade-off introduced.
