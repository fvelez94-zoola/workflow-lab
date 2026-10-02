package com.hexagonal.workflowlab.domain.model.workflow;

public record MixNode(NodeId id, String name, int speedRpm, int durationSeconds) implements WorkflowNode {

    public MixNode {
        NodeValidation.requireName(name);
        NodeValidation.requirePositive("speedRpm", speedRpm);
        NodeValidation.requirePositive("durationSeconds", durationSeconds);
    }
}
