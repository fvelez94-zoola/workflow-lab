package com.hexagonal.workflowlab.domain.model.workflow;

import com.hexagonal.workflowlab.domain.model.exception.ValidationFailure;
import java.util.Optional;

/** A publication rule. Rules are small, independent and registered in {@link WorkflowValidator}. */
interface WorkflowValidationRule {

    Optional<ValidationFailure> check(Workflow workflow);
}
