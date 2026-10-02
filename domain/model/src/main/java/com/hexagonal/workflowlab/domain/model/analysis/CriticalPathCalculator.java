package com.hexagonal.workflowlab.domain.model.analysis;

import com.hexagonal.workflowlab.domain.model.workflow.NodeId;
import com.hexagonal.workflowlab.domain.model.workflow.Workflow;
import com.hexagonal.workflowlab.domain.model.workflow.WorkflowNode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Longest path over a DAG: walk the nodes in topological order and remember, for each node, the moment it
 * can finish and which predecessor made it wait the longest. Ties are resolved by declaration order, so the
 * result is deterministic.
 */
@Component
@RequiredArgsConstructor
public final class CriticalPathCalculator {

    private final NodeDurationPolicy durations;

    /** @throws com.hexagonal.workflowlab.domain.model.exception.InvalidWorkflowException if the graph has a cycle */
    public CriticalPath calculate(Workflow workflow) {
        List<WorkflowNode> ordered = workflow.topologicalOrder();
        if (ordered.isEmpty()) {
            return new CriticalPath(Duration.ZERO, List.of());
        }

        Map<NodeId, List<NodeId>> predecessors = new HashMap<>();
        workflow.getDependencies().forEach(dependency ->
                predecessors.computeIfAbsent(dependency.to(), key -> new ArrayList<>()).add(dependency.from()));

        Map<NodeId, WorkflowNode> byId = new HashMap<>();
        Map<NodeId, Duration> finishesAt = new HashMap<>();
        Map<NodeId, NodeId> slowestPredecessor = new HashMap<>();
        NodeId last = null;

        for (WorkflowNode node : ordered) {
            byId.put(node.id(), node);
            NodeId slowest = null;
            Duration startsAt = Duration.ZERO;
            for (NodeId predecessor : predecessors.getOrDefault(node.id(), List.of())) {
                Duration finish = finishesAt.get(predecessor);
                if (slowest == null || finish.compareTo(startsAt) > 0) {
                    slowest = predecessor;
                    startsAt = finish;
                }
            }
            finishesAt.put(node.id(), startsAt.plus(durations.durationOf(node)));
            if (slowest != null) {
                slowestPredecessor.put(node.id(), slowest);
            }
            if (last == null || finishesAt.get(node.id()).compareTo(finishesAt.get(last)) > 0) {
                last = node.id();
            }
        }

        List<CriticalPathStep> steps = new ArrayList<>();
        for (NodeId current = last; current != null; current = slowestPredecessor.get(current)) {
            WorkflowNode node = byId.get(current);
            steps.add(new CriticalPathStep(node.id(), node.name(), durations.durationOf(node)));
        }
        Collections.reverse(steps);
        return new CriticalPath(finishesAt.get(last), steps);
    }
}
