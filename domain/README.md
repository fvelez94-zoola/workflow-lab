# Domain layer

[← Back to the project overview](../README.md) · Next: [Infrastructure layer](../infrastructure/README.md) · [Application layer](../applications/README.md)

## Definition

The domain layer is the **core of the system**: the business rules and the use cases that orchestrate
them. It is the part that must survive a change of framework, database, transport protocol or file format,
so it knows nothing about any of them.

It is the innermost layer, which means that **every other layer depends on it and it depends on none of
them**. This is the dependency rule of Clean Architecture, and it is enforced twice: by Gradle (the
module dependencies below) and by ArchUnit (see
[ArchitectureTest](../applications/app-service/src/test/java/com/hexagonal/workflowlab/ArchitectureTest.java)).

The layer is split in two Gradle modules:

| Module | Role | Depends on |
|---|---|---|
| [`domain/model`](model) | The business itself: entities, value objects, rules, domain services, **and the ports** (interfaces) through which the outside world talks to the application | `spring-context`, only for the `@Component` stereotype on three stateless collaborators |
| [`domain/usecase`](usecase) | The application's use cases: one class per use case that implements an input port | `domain/model`, and `spring-context` only for `@Service` |

Neither module has web, persistence, serialization or logging frameworks on its classpath, so importing
them does not even compile.

> **A pragmatic decision.** This showcase targets Spring Boot, so use cases carry `@Service` and a few
> stateless domain collaborators carry `@Component`. That is the only contact with Spring allowed in this
> layer, and ArchUnit keeps it that way. A purist variant would register these beans from the application
> layer instead and keep the domain completely framework-free.

## What belongs here

### `domain/model`

- **Entities and aggregates with behavior.** Invariants and state transitions are methods on the object
  that owns the data, not logic scattered across services.
- **Value objects and enums** (identifiers, states, types), preferably immutable records.
- **Business rules and validators**, including the algorithms that implement them (graph traversal,
  scheduling, pricing, ...) as plain Java classes.
- **Domain services**: stateless collaborators for logic that does not fit one entity.
- **Domain exceptions** that name what went wrong in business terms.
- **Input ports** (`port/in`): one interface per use case. They describe what the application can do, as
  seen by whoever calls it.
- **Output ports** (`port/out`): interfaces for everything the application needs from the outside world
  (storage, external systems, document writers). Signatures use **domain types only**.
- **Command/query objects** used in port signatures.

### `domain/usecase`

- **One class per use case**, implementing exactly one input port and annotated with `@Service`.
- **Orchestration only**: load aggregates through output ports, ask the domain to act, persist or send
  the result through output ports.
- **Application-level helpers shared by use cases**: plumbing that is neither business nor technology.

## What does not belong here

### `domain/model`

- Persistence, serialization or web annotations and types (`@Entity`, `@JsonProperty`, `@RestController`,
  `ResponseEntity`, `HttpStatus`, ...).
- DTOs and JPA entities. A domain object is never serialized or persisted directly.
- Presentation or format concerns: turning a duration into "1h 15m", escaping CSV, choosing a
  `Content-Type`. The domain exposes **values** (`Duration`, a format-free `ProtocolSheet`); how they are
  displayed or written is decided elsewhere.
- Any I/O: database, HTTP calls, files, messaging. That is what output ports are for.
- Spring beyond the `@Component` stereotype on the stateless collaborators of `model.analysis`.
- Any dependency on `domain/usecase` or on `infrastructure`.

### `domain/usecase`

- **Business rules.** If a use case grows `if` statements about the business, that logic belongs in an
  aggregate or a domain service. A use case should read like a short script.
- **Technology knowledge**: SQL, HTTP status codes, JSON, file formats.
- **References to adapter classes.** A use case sees output-port interfaces and nothing else.
- **Technical or presentation helpers.** If a use case seems to need one, the helper belongs behind an
  output port in an adapter.
- Spring beyond the `@Service` stereotype.

## Examples from this project

### In `domain/model`

| Concept | Example | Why it illustrates the rule |
|---|---|---|
| Aggregate with behavior | [`Workflow`](model/src/main/java/com/hexagonal/workflowlab/domain/model/workflow/Workflow.java) | `publish()` runs the rules and changes state; `redefine()` refuses to run unless the workflow is a draft. The use cases never re-implement this. |
| Sealed hierarchy | [`WorkflowNode`](model/src/main/java/com/hexagonal/workflowlab/domain/model/workflow/WorkflowNode.java) | Every `switch` over it, in this layer and in the adapters, is exhaustive: adding a node type stops the build until it is handled everywhere. |
| Pure business helper | [`WorkflowGraph`](model/src/main/java/com/hexagonal/workflowlab/domain/model/workflow/WorkflowGraph.java) | Package-private graph algorithms. No framework, so they are tested with plain JUnit. |
| Business policy | [`NodeDurationPolicy`](model/src/main/java/com/hexagonal/workflowlab/domain/model/analysis/NodeDurationPolicy.java) | Returns a `Duration`, never text. Formatting it for humans is an entry-point concern. |
| Domain service | [`WorkflowAnalyzer`](model/src/main/java/com/hexagonal/workflowlab/domain/model/analysis/WorkflowAnalyzer.java) | Composes the policy and the calculator behind a single entry point. |
| Format-free content | [`ProtocolSheet`](model/src/main/java/com/hexagonal/workflowlab/domain/model/protocol/ProtocolSheet.java) | The *data* of an exported document. Writing it as Markdown or CSV is an adapter's job. |
| Input port | [`PublishWorkflow`](model/src/main/java/com/hexagonal/workflowlab/domain/model/port/in/PublishWorkflow.java) | What the application offers, with no hint of who calls it. |
| Output port | [`WorkflowRepository`](model/src/main/java/com/hexagonal/workflowlab/domain/model/port/out/WorkflowRepository.java) | What the application needs, in domain types. No `Entity`, no `Optional<JpaThing>`. |
| Business exceptions | [`exception/`](model/src/main/java/com/hexagonal/workflowlab/domain/model/exception) | Say what went wrong; the entry point decides the HTTP status. |

### In `domain/usecase`

[`PublishWorkflowUseCase`](usecase/src/main/java/com/hexagonal/workflowlab/domain/usecase/workflow/PublishWorkflowUseCase.java)
is the whole shape of a use case:

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

| Concept | Example | Why it illustrates the rule |
|---|---|---|
| Orchestration only | The five `*UseCase` classes | None of them contains a business rule or a technology detail. |
| Application-level helper | [`WorkflowLookup`](usecase/src/main/java/com/hexagonal/workflowlab/domain/usecase/workflow/WorkflowLookup.java) | "Load it or fail with not-found" was repeated in every use case. It is neither business nor technology, so it lives next to the use cases. |
| A sequence of ports | [`ConvertWorkflowToExperimentUseCase`](usecase/src/main/java/com/hexagonal/workflowlab/domain/usecase/experiment/ConvertWorkflowToExperimentUseCase.java) | Saves, submits through an output port, marks the result, saves again. The conversion itself is a domain concern and is not here. |

### How this layer is tested

| Module | Approach |
|---|---|
| `domain/model` | Plain JUnit and AssertJ: no Spring, no mocks, no database. Tests run in milliseconds. |
| `domain/usecase` | JUnit with Mockito on the **output ports** only. Pure domain collaborators are used for real. |
