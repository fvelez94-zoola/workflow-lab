package com.hexagonal.workflowlab.domain.model.exception;

/** A single, machine-readable reason why a workflow is not valid. */
public record ValidationFailure(String code, String message) {
}
