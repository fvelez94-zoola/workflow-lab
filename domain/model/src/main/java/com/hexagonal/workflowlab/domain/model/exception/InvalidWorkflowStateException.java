package com.hexagonal.workflowlab.domain.model.exception;

/** The requested operation is not allowed in the current state of the workflow. */
public class InvalidWorkflowStateException extends RuntimeException {

    public InvalidWorkflowStateException(String message) {
        super(message);
    }
}
