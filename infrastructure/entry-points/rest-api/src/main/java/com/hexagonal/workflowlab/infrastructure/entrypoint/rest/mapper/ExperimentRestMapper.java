package com.hexagonal.workflowlab.infrastructure.entrypoint.rest.mapper;

import com.hexagonal.workflowlab.domain.model.experiment.Experiment;
import com.hexagonal.workflowlab.domain.model.experiment.Instruction;
import com.hexagonal.workflowlab.infrastructure.entrypoint.rest.generated.model.ExperimentResponseDto;
import com.hexagonal.workflowlab.infrastructure.entrypoint.rest.generated.model.ExperimentStatusDto;
import com.hexagonal.workflowlab.infrastructure.entrypoint.rest.generated.model.InstructionDto;
import com.hexagonal.workflowlab.infrastructure.entrypoint.rest.generated.model.InstructionTypeDto;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.IntStream;

public final class ExperimentRestMapper {

    private ExperimentRestMapper() {
    }

    public static ExperimentResponseDto toResponse(Experiment experiment) {
        List<Instruction> instructions = experiment.getInstructions();
        return new ExperimentResponseDto()
                .id(experiment.getId().value())
                .workflowId(experiment.getWorkflowId().value())
                .status(ExperimentStatusDto.valueOf(experiment.getState().name()))
                .runReference(experiment.getRunReference())
                .createdAt(experiment.getCreatedAt().atOffset(ZoneOffset.UTC))
                .instructions(IntStream.range(0, instructions.size())
                        .mapToObj(index -> toDto(index + 1, instructions.get(index)))
                        .toList());
    }

    private static InstructionDto toDto(int sequence, Instruction instruction) {
        return new InstructionDto()
                .sequence(sequence)
                .sourceNodeId(instruction.sourceNodeId())
                .type(InstructionTypeDto.valueOf(instruction.type().name()))
                .description(instruction.description());
    }
}
