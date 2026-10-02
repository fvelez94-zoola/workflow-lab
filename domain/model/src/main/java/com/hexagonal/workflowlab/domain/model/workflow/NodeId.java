package com.hexagonal.workflowlab.domain.model.workflow;

import com.hexagonal.workflowlab.domain.model.exception.InvalidWorkflowException;

/** Identifies a node inside ONE workflow. Chosen by the author (for example {@code mix-1}). */
public record NodeId(String value) {

    public NodeId {
        if (value == null || value.isBlank()) {
            throw InvalidWorkflowException.of("INVALID_NODE_ID", "Node id must not be blank");
        }
        value = value.trim();
    }
}
