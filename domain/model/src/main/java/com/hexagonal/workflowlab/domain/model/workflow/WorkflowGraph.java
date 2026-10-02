package com.hexagonal.workflowlab.domain.model.workflow;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Pure graph algorithms over the nodes and dependencies of a workflow. No framework involved. */
final class WorkflowGraph {

    private final List<NodeId> nodeIds;
    private final Map<NodeId, List<NodeId>> successors = new LinkedHashMap<>();
    private final Map<NodeId, List<NodeId>> neighbours = new LinkedHashMap<>();
    private final Map<NodeId, Integer> inDegree = new LinkedHashMap<>();

    WorkflowGraph(List<WorkflowNode> nodes, List<Dependency> dependencies) {
        this.nodeIds = nodes.stream().map(WorkflowNode::id).toList();
        nodeIds.forEach(id -> {
            successors.put(id, new ArrayList<>());
            neighbours.put(id, new ArrayList<>());
            inDegree.put(id, 0);
        });
        dependencies.forEach(dependency -> {
            successors.get(dependency.from()).add(dependency.to());
            neighbours.get(dependency.from()).add(dependency.to());
            neighbours.get(dependency.to()).add(dependency.from());
            inDegree.merge(dependency.to(), 1, Integer::sum);
        });
    }

    /**
     * Kahn's algorithm. Ties are broken by declaration order, so the result is deterministic. If the
     * graph has a cycle, the returned list is shorter than the number of nodes.
     */
    List<NodeId> topologicalOrder() {
        Map<NodeId, Integer> remainingInDegree = new HashMap<>(inDegree);
        List<NodeId> pending = new ArrayList<>(nodeIds);
        List<NodeId> ordered = new ArrayList<>();
        while (true) {
            NodeId next = pending.stream().filter(id -> remainingInDegree.get(id) == 0).findFirst().orElse(null);
            if (next == null) {
                return ordered;
            }
            pending.remove(next);
            ordered.add(next);
            successors.get(next).forEach(successor -> remainingInDegree.merge(successor, -1, Integer::sum));
        }
    }

    boolean hasCycle() {
        return topologicalOrder().size() < nodeIds.size();
    }

    /** True when every node can be reached from the first one ignoring the direction of dependencies. */
    boolean isWeaklyConnected() {
        if (nodeIds.isEmpty()) {
            return true;
        }
        Set<NodeId> visited = new HashSet<>();
        Deque<NodeId> toVisit = new ArrayDeque<>(List.of(nodeIds.getFirst()));
        while (!toVisit.isEmpty()) {
            NodeId current = toVisit.pop();
            if (visited.add(current)) {
                toVisit.addAll(neighbours.get(current));
            }
        }
        return visited.size() == nodeIds.size();
    }
}
