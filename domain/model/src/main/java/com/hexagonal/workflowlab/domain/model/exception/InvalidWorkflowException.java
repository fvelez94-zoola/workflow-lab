package com.hexagonal.workflowlab.domain.model.exception;

import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;

/** The workflow (or one of its nodes) breaks a business rule. */
@Getter
public class InvalidWorkflowException extends RuntimeException {

    private final List<ValidationFailure> failures;

    public InvalidWorkflowException(List<ValidationFailure> failures) {
        super(failures.stream().map(ValidationFailure::message).collect(Collectors.joining("; ")));
        this.failures = List.copyOf(failures);
    }

    public static InvalidWorkflowException of(String code, String message) {
        return new InvalidWorkflowException(List.of(new ValidationFailure(code, message)));
    }
}
