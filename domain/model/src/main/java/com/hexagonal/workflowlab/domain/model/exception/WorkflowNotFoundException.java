package com.hexagonal.workflowlab.domain.model.exception;

import com.hexagonal.workflowlab.domain.model.workflow.WorkflowId;

public class WorkflowNotFoundException extends RuntimeException {

    public WorkflowNotFoundException(WorkflowId id) {
        super("Workflow %s was not found".formatted(id.value()));
    }
}
