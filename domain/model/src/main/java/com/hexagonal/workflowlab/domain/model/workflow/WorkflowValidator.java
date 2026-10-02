package com.hexagonal.workflowlab.domain.model.workflow;

import com.hexagonal.workflowlab.domain.model.exception.ValidationFailure;
import java.util.List;
import java.util.Optional;

/** Runs every publication rule and collects all failures (not just the first one). */
final class WorkflowValidator {

    private static final List<WorkflowValidationRule> RULES =
            List.of(new EmptyWorkflowRule(), new CycleRule(), new DisconnectedGraphRule());

    private WorkflowValidator() {
    }

    static List<ValidationFailure> validate(Workflow workflow) {
        return RULES.stream().map(rule -> rule.check(workflow)).flatMap(Optional::stream).toList();
    }
}
