package com.hexagonal.workflowlab.domain.model.workflow;

public enum WorkflowState {
    /** Editable, may be incomplete. */
    DRAFT,
    /** Validated and frozen; can be converted to an experiment. */
    PUBLISHED
}
