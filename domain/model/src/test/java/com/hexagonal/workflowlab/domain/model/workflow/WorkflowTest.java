package com.hexagonal.workflowlab.domain.model.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hexagonal.workflowlab.domain.model.exception.InvalidWorkflowException;
import com.hexagonal.workflowlab.domain.model.exception.InvalidWorkflowStateException;
import com.hexagonal.workflowlab.domain.model.exception.ValidationFailure;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Pure domain tests: no Spring, no mocks, no database. They run in milliseconds. */
class WorkflowTest {

    private static MixNode mix(String id) {
        return new MixNode(new NodeId(id), "Mix " + id, 300, 30);
    }

    private static MeasureNode measure(String id) {
        return new MeasureNode(new NodeId(id), "Measure " + id, MeasurementType.ABSORBANCE);
    }

    private static Dependency dependency(String from, String to) {
        return new Dependency(new NodeId(from), new NodeId(to));
    }

    private static Workflow chain() {
        return Workflow.draft("Chain", null, List.of(mix("a"), measure("b")), List.of(dependency("a", "b")));
    }

    private static List<String> failureCodes(InvalidWorkflowException exception) {
        return exception.getFailures().stream().map(ValidationFailure::code).toList();
    }

    @Test
    void shouldCreateWorkflowAsDraft() {
        Workflow workflow = chain();

        assertThat(workflow.getState()).isEqualTo(WorkflowState.DRAFT);
        assertThat(workflow.getId()).isNotNull();
        assertThat(workflow.getNodes()).hasSize(2);
    }

    @Test
    void shouldRejectBlankName() {
        assertThatThrownBy(() -> Workflow.draft("  ", null, List.of(), List.of()))
                .isInstanceOfSatisfying(InvalidWorkflowException.class,
                        e -> assertThat(failureCodes(e)).containsExactly("INVALID_NAME"));
    }

    @Test
    void shouldRejectDuplicatedNodeIds() {
        assertThatThrownBy(() -> Workflow.draft("Dup", null, List.of(mix("a"), mix("a")), List.of()))
                .isInstanceOfSatisfying(InvalidWorkflowException.class,
                        e -> assertThat(failureCodes(e)).containsExactly("DUPLICATED_NODE_ID"));
    }

    @Test
    void shouldRejectDependencyToUnknownNode() {
        assertThatThrownBy(() -> Workflow.draft("Unknown", null, List.of(mix("a")), List.of(dependency("a", "zzz"))))
                .isInstanceOfSatisfying(InvalidWorkflowException.class,
                        e -> assertThat(failureCodes(e)).containsExactly("UNKNOWN_NODE_REFERENCE"));
    }

    @Test
    void shouldRejectSelfDependency() {
        assertThatThrownBy(() -> Workflow.draft("Self", null, List.of(mix("a")), List.of(dependency("a", "a"))))
                .isInstanceOfSatisfying(InvalidWorkflowException.class,
                        e -> assertThat(failureCodes(e)).containsExactly("SELF_DEPENDENCY"));
    }

    @Test
    void shouldAllowIncompleteDraftSuchAsEmptyOrCyclic() {
        Workflow empty = Workflow.draft("Empty", null, List.of(), List.of());
        Workflow cyclic = Workflow.draft("Cyclic", null, List.of(mix("a"), measure("b")),
                List.of(dependency("a", "b"), dependency("b", "a")));

        assertThat(empty.getState()).isEqualTo(WorkflowState.DRAFT);
        assertThat(cyclic.getState()).isEqualTo(WorkflowState.DRAFT);
    }

    @Test
    void shouldPublishValidWorkflow() {
        Workflow workflow = chain();

        workflow.publish();

        assertThat(workflow.getState()).isEqualTo(WorkflowState.PUBLISHED);
    }

    @Test
    void shouldRejectPublishWhenWorkflowIsEmpty() {
        Workflow workflow = Workflow.draft("Empty", null, List.of(), List.of());

        assertThatThrownBy(workflow::publish).isInstanceOfSatisfying(InvalidWorkflowException.class,
                e -> assertThat(failureCodes(e)).containsExactly("EMPTY_WORKFLOW"));
        assertThat(workflow.getState()).isEqualTo(WorkflowState.DRAFT);
    }

    @Test
    void shouldRejectPublishWhenGraphHasCycle() {
        Workflow workflow = Workflow.draft("Cycle", null, List.of(mix("a"), measure("b")),
                List.of(dependency("a", "b"), dependency("b", "a")));

        assertThatThrownBy(workflow::publish).isInstanceOfSatisfying(InvalidWorkflowException.class,
                e -> assertThat(failureCodes(e)).containsExactly("CYCLE_DETECTED"));
    }

    @Test
    void shouldRejectPublishWhenGraphIsDisconnected() {
        Workflow workflow = Workflow.draft("Islands", null, List.of(mix("a"), measure("b")), List.of());

        assertThatThrownBy(workflow::publish).isInstanceOfSatisfying(InvalidWorkflowException.class,
                e -> assertThat(failureCodes(e)).containsExactly("DISCONNECTED_GRAPH"));
    }

    @Test
    void shouldReportAllPublicationFailuresAtOnce() {
        Workflow workflow = Workflow.draft("Both", null, List.of(mix("a"), measure("b"), measure("c")),
                List.of(dependency("a", "b"), dependency("b", "a")));

        assertThatThrownBy(workflow::publish).isInstanceOfSatisfying(InvalidWorkflowException.class,
                e -> assertThat(failureCodes(e)).containsExactlyInAnyOrder("CYCLE_DETECTED", "DISCONNECTED_GRAPH"));
    }

    @Test
    void shouldRedefineDraft() {
        Workflow workflow = chain();

        workflow.redefine("Renamed", "new description", List.of(mix("x")), List.of());

        assertThat(workflow.getName()).isEqualTo("Renamed");
        assertThat(workflow.getNodes()).extracting(node -> node.id().value()).containsExactly("x");
    }

    @Test
    void shouldRejectRedefineWhenPublished() {
        Workflow workflow = chain();
        workflow.publish();

        assertThatThrownBy(() -> workflow.redefine("Renamed", null, List.of(mix("x")), List.of()))
                .isInstanceOf(InvalidWorkflowStateException.class);
    }

    @Test
    void shouldRejectDeleteWhenPublished() {
        Workflow workflow = chain();
        workflow.publish();

        assertThatThrownBy(workflow::requireDeletable).isInstanceOf(InvalidWorkflowStateException.class);
    }

    @Test
    void shouldReturnNodesInTopologicalOrderBreakingTiesByDeclarationOrder() {
        Workflow workflow = Workflow.draft("Order", null, List.of(mix("c"), mix("a"), measure("b")),
                List.of(dependency("a", "b"), dependency("c", "b")));

        assertThat(workflow.topologicalOrder()).extracting(node -> node.id().value()).containsExactly("c", "a", "b");
    }

    @Test
    void shouldFailTopologicalOrderWhenGraphHasCycle() {
        Workflow workflow = Workflow.draft("Cycle", null, List.of(mix("a"), measure("b")),
                List.of(dependency("a", "b"), dependency("b", "a")));

        assertThatThrownBy(workflow::topologicalOrder).isInstanceOf(InvalidWorkflowException.class);
    }
}
