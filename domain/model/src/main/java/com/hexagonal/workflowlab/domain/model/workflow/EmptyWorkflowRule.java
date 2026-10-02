package com.hexagonal.workflowlab.domain.model.workflow;

import com.hexagonal.workflowlab.domain.model.exception.ValidationFailure;
import java.util.Optional;

final class EmptyWorkflowRule implements WorkflowValidationRule {

    @Override
    public Optional<ValidationFailure> check(Workflow workflow) {
        return workflow.getNodes().isEmpty()
                ? Optional.of(new ValidationFailure("EMPTY_WORKFLOW", "A workflow needs at least one node"))
                : Optional.empty();
    }
}
