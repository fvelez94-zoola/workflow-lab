package com.hexagonal.workflowlab.domain.model.experiment;

/** One executable step. {@code sourceNodeId} tells which workflow node produced it. */
public record Instruction(String sourceNodeId, InstructionType type, String description) {
}
