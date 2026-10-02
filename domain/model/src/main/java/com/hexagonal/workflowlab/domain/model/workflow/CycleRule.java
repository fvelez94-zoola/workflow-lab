package com.hexagonal.workflowlab.domain.model.workflow;

import com.hexagonal.workflowlab.domain.model.exception.ValidationFailure;
import java.util.Optional;

final class CycleRule implements WorkflowValidationRule {

    @Override
    public Optional<ValidationFailure> check(Workflow workflow) {
        return workflow.hasCycle()
                ? Optional.of(new ValidationFailure("CYCLE_DETECTED", "The workflow contains a dependency cycle"))
                : Optional.empty();
    }
}
