package com.hexagonal.workflowlab.domain.model.protocol;

import com.hexagonal.workflowlab.domain.model.experiment.Experiment;
import com.hexagonal.workflowlab.domain.model.experiment.Instruction;
import com.hexagonal.workflowlab.domain.model.experiment.InstructionType;
import com.hexagonal.workflowlab.domain.model.workflow.Workflow;
import java.util.List;
import java.util.stream.IntStream;

/**
 * The content of a protocol document, with no format at all: a title and numbered steps. Writing it as
 * Markdown, CSV or anything else is the job of an adapter.
 */
public record ProtocolSheet(String title, List<Step> steps) {

    public ProtocolSheet {
        steps = List.copyOf(steps);
    }

    public static ProtocolSheet from(Workflow workflow, Experiment experiment) {
        List<Instruction> instructions = experiment.getInstructions();
        return new ProtocolSheet(workflow.getName(), IntStream.range(0, instructions.size())
                .mapToObj(index -> new Step(index + 1, instructions.get(index).sourceNodeId(),
                        instructions.get(index).type(), instructions.get(index).description()))
                .toList());
    }

    public record Step(int sequence, String sourceNodeId, InstructionType type, String description) {
    }
}
