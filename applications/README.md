# Application layer

[← Back to the project overview](../README.md) · Previous: [Domain layer](../domain/README.md) · [Infrastructure layer](../infrastructure/README.md)

## Definition

The application layer is the **composition root**: the outermost layer, where all the pieces are brought
together into a running program. It is the only module that knows every other module, and the only one
with a `main()`.

Its job is to answer one question: *which implementation does the running system use for each port?* It
does not add behavior of its own. It also hosts the tests that need the whole system, because it is the only
place where everything is on the classpath.

| Module | Role | Depends on |
|---|---|---|
| [`applications/app-service`](app-service) | Starts the application, wires it, seeds it and verifies it end to end | `domain/model` and `domain/usecase` at compile time; every adapter at **runtime only** |

Adapters are declared `runtimeOnly`, so the code of this module cannot import an adapter class even by
accident: Spring finds the adapters by component scanning and injects them through the output-port
interfaces.

## What belongs here

- **The `main()` class** and the `@SpringBootApplication` that starts component scanning.
- **Beans that cannot be discovered by annotation**, such as classes from the JDK or from a library.
- **Environment configuration**: `application.yaml`, profiles, connection settings.
- **Start-up tasks** such as seeding sample data, executed **through the input ports** so the data obeys
  the same rules as real traffic.
- **Whole-system tests**: end-to-end flows against the real Spring context, and the architecture tests
  that look at every module at once.
- **The list of adapters that make up the running system** (the `runtimeOnly` dependencies).

## What does not belong here

- **Business logic.** If it decides something about the domain, it belongs in `domain/`.
- **Controllers, entities, mappers or any adapter code.** Those belong in `infrastructure/`.
- **Compile-time references to adapter classes.** Depending on adapters at compile time would let
  application code couple itself to a specific technology.
- **Shared utilities for other layers.** Helpers live in the layer whose concern they serve.
- **Per-feature code.** Adding a feature should not require touching this module, except to register a new
  adapter module.

## Examples from this project

| Concept | Example | Why it illustrates the rule |
|---|---|---|
| Runtime-only adapters | [`build.gradle`](app-service/build.gradle) | The three adapters and the REST entry point are `runtimeOnly`; the application code cannot reference their classes. |
| Entry point and unannotatable bean | [`WorkflowLabApplication`](app-service/src/main/java/com/hexagonal/workflowlab/WorkflowLabApplication.java) | `@SpringBootApplication` discovers use cases and adapters. The only factory method is for `Clock`, a JDK class that cannot be annotated. |
| Start-up task through a port | [`SampleDataConfig`](app-service/src/main/java/com/hexagonal/workflowlab/config/SampleDataConfig.java) | Seeds data by calling the `WorkflowCrud` input port, not a repository. |
| Environment configuration | [`application.yaml`](app-service/src/main/resources/application.yaml) | Datasource and JPA settings live here, not in the adapter. |
| Architecture as tests | [`ArchitectureTest`](app-service/src/test/java/com/hexagonal/workflowlab/ArchitectureTest.java) | Turns the dependency rule into executable checks (see below). |
| End-to-end test | [`WorkflowLabFlowTest`](app-service/src/test/java/com/hexagonal/workflowlab/WorkflowLabFlowTest.java) | Drives every use case through HTTP, use case, domain, JPA and the database. |

### The architecture rules, as code

`ArchitectureTest` is what stops the structure from eroding. Among others, it fails the build when:

- the domain model depends on JPA, Jackson, Hibernate, any outer layer, or on Spring beyond the
  `@Component` stereotype in `model.analysis`;
- a use case depends on persistence types, on infrastructure, or on any Spring type other than the
  `@Service` stereotype;
- `@Service` is used outside the use cases;
- an entry point depends on a use case class or an adapter;
- a driven adapter depends on an entry point, a use case, or another adapter;
- JPA entities are used outside the persistence adapter;
- a formatter, escaper, media-type or mapper class sits outside `infrastructure`.

### How this layer is tested

| Test | Approach |
|---|---|
| `ArchitectureTest` | ArchUnit over the classes of all modules. |
| `WorkflowLabFlowTest` | `@SpringBootTest` with MockMvc: the real context, the real adapters, an in-memory database. |
