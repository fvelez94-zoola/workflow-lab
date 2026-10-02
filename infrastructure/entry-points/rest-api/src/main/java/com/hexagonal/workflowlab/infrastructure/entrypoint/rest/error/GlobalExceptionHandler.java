package com.hexagonal.workflowlab.infrastructure.entrypoint.rest.error;

import com.hexagonal.workflowlab.domain.model.exception.InvalidWorkflowException;
import com.hexagonal.workflowlab.domain.model.exception.InvalidWorkflowStateException;
import com.hexagonal.workflowlab.domain.model.exception.WorkflowNotFoundException;
import com.hexagonal.workflowlab.infrastructure.entrypoint.rest.generated.model.ErrorFailureDto;
import com.hexagonal.workflowlab.infrastructure.entrypoint.rest.generated.model.ErrorResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** The only place that decides which domain exception becomes which HTTP status. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(WorkflowNotFoundException.class)
    ResponseEntity<ErrorResponseDto> handleNotFound(WorkflowNotFoundException exception) {
        return respond(HttpStatus.NOT_FOUND, "WORKFLOW_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(InvalidWorkflowStateException.class)
    ResponseEntity<ErrorResponseDto> handleInvalidState(InvalidWorkflowStateException exception) {
        return respond(HttpStatus.CONFLICT, "INVALID_WORKFLOW_STATE", exception.getMessage());
    }

    @ExceptionHandler(InvalidWorkflowException.class)
    ResponseEntity<ErrorResponseDto> handleInvalidWorkflow(InvalidWorkflowException exception) {
        ErrorResponseDto body = new ErrorResponseDto()
                .code("INVALID_WORKFLOW")
                .message(exception.getMessage())
                .failures(exception.getFailures().stream()
                        .map(failure -> new ErrorFailureDto().code(failure.code()).message(failure.message()))
                        .toList());
        return json(HttpStatus.UNPROCESSABLE_ENTITY, body);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class})
    ResponseEntity<ErrorResponseDto> handleMalformedRequest(Exception exception) {
        return respond(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "The request is malformed or incomplete");
    }

    private static ResponseEntity<ErrorResponseDto> respond(HttpStatus status, String code, String message) {
        return json(status, new ErrorResponseDto().code(code).message(message));
    }

    /** Errors are always JSON, even when the client asked for text/csv or text/markdown. */
    private static ResponseEntity<ErrorResponseDto> json(HttpStatus status, ErrorResponseDto body) {
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(body);
    }
}
