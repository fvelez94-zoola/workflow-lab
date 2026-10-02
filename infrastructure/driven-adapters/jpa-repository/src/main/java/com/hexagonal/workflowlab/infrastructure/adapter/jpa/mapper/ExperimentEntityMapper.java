package com.hexagonal.workflowlab.infrastructure.adapter.jpa.mapper;

import com.hexagonal.workflowlab.domain.model.experiment.Experiment;
import com.hexagonal.workflowlab.domain.model.experiment.ExperimentId;
import com.hexagonal.workflowlab.domain.model.experiment.ExperimentState;
import com.hexagonal.workflowlab.domain.model.experiment.Instruction;
import com.hexagonal.workflowlab.domain.model.experiment.InstructionType;
import com.hexagonal.workflowlab.domain.model.workflow.WorkflowId;
import com.hexagonal.workflowlab.infrastructure.adapter.jpa.entity.ExperimentEntity;
import com.hexagonal.workflowlab.infrastructure.adapter.jpa.entity.InstructionEmbeddable;
import java.util.ArrayList;

/** Domain <-> JPA translation for experiments. */
public final class ExperimentEntityMapper {

    private ExperimentEntityMapper() {
    }

    public static ExperimentEntity toEntity(Experiment experiment) {
        ExperimentEntity entity = new ExperimentEntity();
        entity.setId(experiment.getId().value());
        entity.setWorkflowId(experiment.getWorkflowId().value());
        entity.setState(experiment.getState().name());
        entity.setRunReference(experiment.getRunReference());
        entity.setCreatedAt(experiment.getCreatedAt());
        entity.setInstructions(new ArrayList<>(experiment.getInstructions().stream()
                .map(instruction -> new InstructionEmbeddable(instruction.sourceNodeId(),
                        instruction.type().name(), instruction.description()))
                .toList()));
        return entity;
    }

    public static Experiment toDomain(ExperimentEntity entity) {
        return Experiment.restore(
                new ExperimentId(entity.getId()),
                new WorkflowId(entity.getWorkflowId()),
                ExperimentState.valueOf(entity.getState()),
                entity.getRunReference(),
                entity.getCreatedAt(),
                entity.getInstructions().stream()
                        .map(row -> new Instruction(row.getSourceNodeKey(),
                                InstructionType.valueOf(row.getInstructionType()), row.getDescription()))
                        .toList());
    }
}
