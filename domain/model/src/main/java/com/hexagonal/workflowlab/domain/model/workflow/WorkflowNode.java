package com.hexagonal.workflowlab.domain.model.workflow;

/**
 * One authored step of a workflow.
 *
 * <p>The hierarchy is sealed on purpose: every {@code switch} over a node (conversion, persistence
 * mapping, REST mapping) is checked exhaustively by the compiler, so adding a node type can never be
 * forgotten somewhere.
 */
public sealed interface WorkflowNode permits MixNode, IncubateNode, MeasureNode {

    NodeId id();

    String name();
}
