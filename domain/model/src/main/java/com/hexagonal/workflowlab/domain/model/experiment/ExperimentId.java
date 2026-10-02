package com.hexagonal.workflowlab.domain.model.experiment;

import java.util.Objects;
import java.util.UUID;

public record ExperimentId(UUID value) {

    public ExperimentId {
        Objects.requireNonNull(value, "value");
    }

    public static ExperimentId generate() {
        return new ExperimentId(UUID.randomUUID());
    }
}
