package com.hexagonal.workflowlab.domain.model.protocol;

/** A rendered protocol. {@code title} is the workflow name; naming the file is up to the caller. */
public record ProtocolDocument(String title, DocumentFormat format, String content) {
}
