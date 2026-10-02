# Infrastructure layer

[← Back to the project overview](../README.md) · Previous: [Domain layer](../domain/README.md) · Next: [Application layer](../applications/README.md)

## Definition

The infrastructure layer contains the **technology details**: everything that connects the domain to the
real world (HTTP, databases, files, external systems) and that could be swapped for something else without
touching a single business rule.

Its classes are **adapters**: they translate between an outside representation (a JSON body, a table row,
a CSV file) and the domain's own types. There are two kinds:

- **Driving adapters** (`entry-points`) receive a request from the outside and call an **input port**.
- **Driven adapters** (`driven-adapters`) implement an **output port** that the domain declared, using a
  concrete technology.

Dependencies point **inward only**: every module here depends on `domain/model` (and nothing else of this
project) plus the framework it adapts. Adapters never depend on each other, on `domain/usecase`, or on the
application layer.

| Module | Kind | Technology | Port it works with |
|---|---|---|---|
| [`entry-points/rest-api`](entry-points/rest-api) | Driving | Spring Web, OpenAPI Generator | Calls the **input ports** |
| [`driven-adapters/jpa-repository`](driven-adapters/jpa-repository) | Driven | Spring Data JPA, H2 | Implements `WorkflowRepository`, `ExperimentRepository` |
| [`driven-adapters/lab-runner-log`](driven-adapters/lab-runner-log) | Driven | SLF4J | Implements `RunSubmissionGateway` |
| [`driven-adapters/protocol-document`](driven-adapters/protocol-document) | Driven | Plain Java formatting | Implements `ProtocolDocumentWriter` |

## What belongs here

### Entry points (driving adapters)

- **The contract** of the interface exposed to the outside (here, the OpenAPI specification) and the code
  generated from it.
- **Controllers** that only translate and delegate: read the request, call an input port, build the
  response.
- **Mappers** between transport objects (DTOs) and domain objects, one per concern.
- **Validation of the shape of the input** (is a required field present for this node type?).
- **Translation of domain exceptions into protocol errors** (HTTP statuses and error bodies), in one place.
- **Presentation helpers**: how a value is shown or named for the client (human-readable durations,
  `Content-Type`, download file names).

### Driven adapters

- **A class implementing each output port** with a concrete technology.
- **The technology's own model**: JPA entities, SQL, file layouts, HTTP clients.
- **Mappers** between that model and the domain objects.
- **Technical helpers** that exist only because the technology exists (CSV escaping, Markdown tables,
  repository interfaces of the persistence framework).
- **Framework dependencies.** Spring Web, JPA, validation and similar libraries are declared in these
  modules, never in the domain.

## What does not belong here

- **Business rules or decisions.** Whether a workflow may be deleted or published is decided by the
  domain; an adapter never re-implements or bypasses it.
- **Calls to use case classes** (entry points). A controller depends on an input-port interface, so the
  implementation can be replaced or decorated without touching it.
- **Dependencies between adapters**, or from a driven adapter to an entry point. Each adapter must be
  replaceable on its own.
- **Leaking technology outward.** JPA entities and generated DTOs never leave their module; the rest of the
  system sees domain objects only.
- **Orchestration.** Sequencing several output ports is a use case's job.
- **Wiring and configuration of the whole application** (that is the
  [application layer](../applications/README.md)).

## Examples from this project

### `entry-points/rest-api`

| Concept | Example | Why it illustrates the rule |
|---|---|---|
| Contract first | [`openapi/`](entry-points/rest-api/src/main/resources/openapi) | The specification is split by concern (`paths/`, `schemas/`, ...). Interfaces and DTOs are generated at build time and never committed. |
| Thin controller | [`WorkflowsController`](entry-points/rest-api/src/main/java/com/hexagonal/workflowlab/infrastructure/entrypoint/rest/WorkflowsController.java) | Implements the generated API, receives **input-port interfaces**, maps, delegates. |
| One mapper per concern | [`mapper/`](entry-points/rest-api/src/main/java/com/hexagonal/workflowlab/infrastructure/entrypoint/rest/mapper) | `NodeRestMapper`, `WorkflowRestMapper`, `ExperimentRestMapper`, ... instead of one class that does everything. |
| HTTP-shape validation | [`RequiredParameters`](entry-points/rest-api/src/main/java/com/hexagonal/workflowlab/infrastructure/entrypoint/rest/mapper/RequiredParameters.java) | Checks that a parameter is present; whether its *value* makes sense is a domain rule. |
| Presentation helper | [`DurationFormatter`](entry-points/rest-api/src/main/java/com/hexagonal/workflowlab/infrastructure/entrypoint/rest/mapper/DurationFormatter.java) | The domain returns a `Duration`; "1h 15m 30s" is decided here and could be localized without touching a rule. |
| Protocol-specific knowledge | [`ProtocolMediaType`](entry-points/rest-api/src/main/java/com/hexagonal/workflowlab/infrastructure/entrypoint/rest/mapper/ProtocolMediaType.java) | Maps a domain format to a `Content-Type` and a file name. |
| Error translation | [`GlobalExceptionHandler`](entry-points/rest-api/src/main/java/com/hexagonal/workflowlab/infrastructure/entrypoint/rest/error/GlobalExceptionHandler.java) | The only place where a domain exception becomes an HTTP status. |

### `driven-adapters/jpa-repository`

| Concept | Example | Why it illustrates the rule |
|---|---|---|
| Technology's own model | [`entity/`](driven-adapters/jpa-repository/src/main/java/com/hexagonal/workflowlab/infrastructure/adapter/jpa/entity) | JPA entities are different classes from the domain objects and never leave this module. |
| One mapper per concern | [`NodeEmbeddableMapper`](driven-adapters/jpa-repository/src/main/java/com/hexagonal/workflowlab/infrastructure/adapter/jpa/mapper/NodeEmbeddableMapper.java), [`WorkflowEntityMapper`](driven-adapters/jpa-repository/src/main/java/com/hexagonal/workflowlab/infrastructure/adapter/jpa/mapper/WorkflowEntityMapper.java) | Flattening a sealed node into a row has its own reason to change. |
| Port implementation | [`JpaWorkflowRepositoryAdapter`](driven-adapters/jpa-repository/src/main/java/com/hexagonal/workflowlab/infrastructure/adapter/jpa/JpaWorkflowRepositoryAdapter.java) | Implements the domain's `WorkflowRepository` on top of a Spring Data repository the domain has never heard of. |

```java
@Repository
@RequiredArgsConstructor
public class JpaWorkflowRepositoryAdapter implements WorkflowRepository {   // output port from domain/model

    private final SpringDataWorkflowRepository repository;                   // framework detail, stays here

    @Override
    public Workflow save(Workflow workflow) {
        return WorkflowEntityMapper.toDomain(repository.save(WorkflowEntityMapper.toEntity(workflow)));
    }
    // ...
}
```

### `driven-adapters/lab-runner-log`

[`LoggingRunSubmissionGateway`](driven-adapters/lab-runner-log/src/main/java/com/hexagonal/workflowlab/infrastructure/adapter/labrunner/LoggingRunSubmissionGateway.java)
"submits" an experiment by logging it. Replacing it with an HTTP client for a real system means writing a
new adapter that implements `RunSubmissionGateway`; the domain and the use cases do not change.

### `driven-adapters/protocol-document`

| Concept | Example | Why it illustrates the rule |
|---|---|---|
| Strategy per format | [`ProtocolDocumentWriterAdapter`](driven-adapters/protocol-document/src/main/java/com/hexagonal/workflowlab/infrastructure/adapter/protocoldocument/ProtocolDocumentWriterAdapter.java) | Picks a formatter with an exhaustive `switch` over the domain's `DocumentFormat`. |
| Technical helpers | [`CsvEscaper`](driven-adapters/protocol-document/src/main/java/com/hexagonal/workflowlab/infrastructure/adapter/protocoldocument/CsvEscaper.java), [`MarkdownTable`](driven-adapters/protocol-document/src/main/java/com/hexagonal/workflowlab/infrastructure/adapter/protocoldocument/MarkdownTable.java) | They exist only because CSV and Markdown exist, so they are package-private here: not even other adapters can reach them. |

### Adding a new adapter

1. Declare the output port in `domain/model` (`port/out`), in domain types only.
2. Create a module under `driven-adapters/` that depends on `domain/model` and implements the port.
3. Add it to `settings.gradle` and as `runtimeOnly` in `applications/app-service`.

The ArchUnit rule that keeps adapters independent of each other applies to the new module automatically.

### How this layer is tested

| Module | Approach |
|---|---|
| `rest-api` | Standalone MockMvc with the **input ports mocked**: status codes, DTO mapping, error bodies. Pure JUnit for the helpers. |
| `jpa-repository` | `@DataJpaTest` against in-memory H2 to prove the domain-to-table round trip. Pure JUnit for the mappers. |
| `lab-runner-log` | Plain JUnit. |
| `protocol-document` | Plain JUnit for the formatters and helpers. |
