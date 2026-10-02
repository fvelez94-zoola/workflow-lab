package com.hexagonal.workflowlab.domain.model.workflow;

public record IncubateNode(NodeId id, String name, int temperatureCelsius, int durationMinutes)
        implements WorkflowNode {

    public IncubateNode {
        NodeValidation.requireName(name);
        NodeValidation.requirePositive("temperatureCelsius", temperatureCelsius);
        NodeValidation.requirePositive("durationMinutes", durationMinutes);
    }
}
