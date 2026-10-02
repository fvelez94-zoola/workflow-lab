package com.hexagonal.workflowlab.domain.model.workflow;

import java.util.Objects;

public record MeasureNode(NodeId id, String name, MeasurementType measurementType) implements WorkflowNode {

    public MeasureNode {
        NodeValidation.requireName(name);
        Objects.requireNonNull(measurementType, "measurementType");
    }
}
