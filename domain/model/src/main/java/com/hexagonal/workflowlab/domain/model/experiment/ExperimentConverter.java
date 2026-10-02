package com.hexagonal.workflowlab.domain.model.experiment;

import com.hexagonal.workflowlab.domain.model.workflow.IncubateNode;
import com.hexagonal.workflowlab.domain.model.workflow.MeasureNode;
import com.hexagonal.workflowlab.domain.model.workflow.MixNode;
import com.hexagonal.workflowlab.domain.model.workflow.Workflow;
import com.hexagonal.workflowlab.domain.model.workflow.WorkflowNode;
import java.util.List;

/**
 * Deterministic translation workflow -> instructions: same workflow, same instructions.
 *
 * <p>The {@code switch} is exhaustive over the sealed {@link WorkflowNode}: if someone adds a node type
 * and forgets to teach the converter about it, the project stops compiling.
 */
final class ExperimentConverter {

    private ExperimentConverter() {
    }

    static List<Instruction> convert(Workflow workflow) {
        return workflow.topologicalOrder().stream().flatMap(node -> toInstructions(node).stream()).toList();
    }

    private static List<Instruction> toInstructions(WorkflowNode node) {
        String source = node.id().value();
        return switch (node) {
            case MixNode mix -> List.of(new Instruction(source, InstructionType.MIX,
                    "Mix at %d rpm for %d s".formatted(mix.speedRpm(), mix.durationSeconds())));
            case IncubateNode incubate -> List.of(
                    new Instruction(source, InstructionType.SET_TEMPERATURE,
                            "Set temperature to %d C".formatted(incubate.temperatureCelsius())),
                    new Instruction(source, InstructionType.WAIT,
                            "Wait %d min".formatted(incubate.durationMinutes())));
            case MeasureNode measure -> List.of(new Instruction(source, InstructionType.MEASURE,
                    "Measure %s".formatted(measure.measurementType().name().toLowerCase())));
        };
    }
}
