package com.hexagonal.workflowlab.domain.model.workflow;

/** {@code from} must finish before {@code to} can start. */
public record Dependency(NodeId from, NodeId to) {
}
