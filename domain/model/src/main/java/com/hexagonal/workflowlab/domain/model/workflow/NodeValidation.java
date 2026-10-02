package com.hexagonal.workflowlab.domain.model.workflow;

import com.hexagonal.workflowlab.domain.model.exception.InvalidWorkflowException;

final class NodeValidation {

    private NodeValidation() {
    }

    static void requireName(String name) {
        if (name == null || name.isBlank()) {
            throw InvalidWorkflowException.of("INVALID_NODE_NAME", "Node name must not be blank");
        }
    }

    static void requirePositive(String parameter, int value) {
        if (value <= 0) {
            throw InvalidWorkflowException.of("INVALID_NODE_PARAMETER",
                    "Node parameter '%s' must be greater than zero".formatted(parameter));
        }
    }
}
