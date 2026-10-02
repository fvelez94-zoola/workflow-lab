package com.hexagonal.workflowlab.domain.model.workflow;

import com.hexagonal.workflowlab.domain.model.exception.InvalidWorkflowException;
import com.hexagonal.workflowlab.domain.model.exception.InvalidWorkflowStateException;
import com.hexagonal.workflowlab.domain.model.exception.ValidationFailure;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;

/**
 * Aggregate root. A workflow is a graph of typed nodes connected by dependencies.
 *
 * <p>Two levels of rules exist on purpose:
 *
 * <ul>
 *   <li><b>Structural</b> (always enforced): a name, unique node ids, dependencies pointing at existing
 *       nodes. A draft can never be structurally broken.
 *   <li><b>Publication</b> (enforced by {@link #publish()}): not empty, no cycles, fully connected. A
 *       draft may violate them while the author is still working.
 * </ul>
 */
@Getter
public final class Workflow {

    private final WorkflowId id;
    private String name;
    private String description;
    private WorkflowState state;
    private List<WorkflowNode> nodes;
    private List<Dependency> dependencies;

    private Workflow(WorkflowId id, String name, String description, WorkflowState state,
            List<WorkflowNode> nodes, List<Dependency> dependencies) {
        requireStructurallyValid(name, nodes, dependencies);
        this.id = id;
        this.name = name.trim();
        this.description = description;
        this.state = state;
        this.nodes = List.copyOf(nodes);
        this.dependencies = List.copyOf(dependencies);
    }

    /** Creates a brand-new workflow, always in {@link WorkflowState#DRAFT}. */
    public static Workflow draft(String name, String description, List<WorkflowNode> nodes,
            List<Dependency> dependencies) {
        return new Workflow(WorkflowId.generate(), name, description, WorkflowState.DRAFT, nodes, dependencies);
    }

    /** Rebuilds an existing workflow (used by persistence adapters). */
    public static Workflow restore(WorkflowId id, String name, String description, WorkflowState state,
            List<WorkflowNode> nodes, List<Dependency> dependencies) {
        return new Workflow(id, name, description, state, nodes, dependencies);
    }

    public void redefine(String newName, String newDescription, List<WorkflowNode> newNodes,
            List<Dependency> newDependencies) {
        requireDraft("updated");
        requireStructurallyValid(newName, newNodes, newDependencies);
        this.name = newName.trim();
        this.description = newDescription;
        this.nodes = List.copyOf(newNodes);
        this.dependencies = List.copyOf(newDependencies);
    }

    public void publish() {
        requireDraft("published");
        List<ValidationFailure> failures = WorkflowValidator.validate(this);
        if (!failures.isEmpty()) {
            throw new InvalidWorkflowException(failures);
        }
        this.state = WorkflowState.PUBLISHED;
    }

    public void requireDeletable() {
        requireDraft("deleted");
    }

    /** Nodes in execution order. Ties are resolved by declaration order, so the result is deterministic. */
    public List<WorkflowNode> topologicalOrder() {
        List<NodeId> order = graph().topologicalOrder();
        if (order.size() < nodes.size()) {
            throw InvalidWorkflowException.of("CYCLE_DETECTED", "The workflow contains a dependency cycle");
        }
        return order.stream().map(this::nodeById).toList();
    }

    boolean hasCycle() {
        return graph().hasCycle();
    }

    boolean isConnected() {
        return graph().isWeaklyConnected();
    }

    private WorkflowNode nodeById(NodeId nodeId) {
        return nodes.stream().filter(node -> node.id().equals(nodeId)).findFirst().orElseThrow();
    }

    private WorkflowGraph graph() {
        return new WorkflowGraph(nodes, dependencies);
    }

    private void requireDraft(String action) {
        if (state != WorkflowState.DRAFT) {
            throw new InvalidWorkflowStateException(
                    "Workflow %s is %s and cannot be %s".formatted(id.value(), state, action));
        }
    }

    private static void requireStructurallyValid(String name, List<WorkflowNode> nodes,
            List<Dependency> dependencies) {
        List<ValidationFailure> failures = new ArrayList<>();
        if (name == null || name.isBlank()) {
            failures.add(new ValidationFailure("INVALID_NAME", "Workflow name must not be blank"));
        }
        Set<NodeId> knownIds = new HashSet<>();
        for (WorkflowNode node : nodes) {
            if (!knownIds.add(node.id())) {
                failures.add(new ValidationFailure("DUPLICATED_NODE_ID",
                        "Node id '%s' is used more than once".formatted(node.id().value())));
            }
        }
        for (Dependency dependency : dependencies) {
            if (dependency.from().equals(dependency.to())) {
                failures.add(new ValidationFailure("SELF_DEPENDENCY",
                        "Node '%s' cannot depend on itself".formatted(dependency.from().value())));
            } else if (!knownIds.contains(dependency.from()) || !knownIds.contains(dependency.to())) {
                failures.add(new ValidationFailure("UNKNOWN_NODE_REFERENCE",
                        "Dependency %s -> %s points to a node that does not exist"
                                .formatted(dependency.from().value(), dependency.to().value())));
            }
        }
        if (!failures.isEmpty()) {
            throw new InvalidWorkflowException(failures);
        }
    }
}
