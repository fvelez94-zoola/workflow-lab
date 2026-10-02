package com.hexagonal.workflowlab.domain.model.workflow;

import com.hexagonal.workflowlab.domain.model.exception.ValidationFailure;
import java.util.Optional;

final class DisconnectedGraphRule implements WorkflowValidationRule {

    @Override
    public Optional<ValidationFailure> check(Workflow workflow) {
        return workflow.isConnected()
                ? Optional.empty()
                : Optional.of(new ValidationFailure("DISCONNECTED_GRAPH",
                        "All nodes must be connected to each other through dependencies"));
    }
}
