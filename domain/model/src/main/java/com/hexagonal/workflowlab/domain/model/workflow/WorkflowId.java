package com.hexagonal.workflowlab.domain.model.workflow;

import java.util.Objects;
import java.util.UUID;

public record WorkflowId(UUID value) {

    public WorkflowId {
        Objects.requireNonNull(value, "value");
    }

    public static WorkflowId generate() {
        return new WorkflowId(UUID.randomUUID());
    }
}
