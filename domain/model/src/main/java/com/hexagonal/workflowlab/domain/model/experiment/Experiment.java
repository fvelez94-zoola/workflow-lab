package com.hexagonal.workflowlab.domain.model.experiment;

import com.hexagonal.workflowlab.domain.model.exception.InvalidWorkflowStateException;
import com.hexagonal.workflowlab.domain.model.workflow.Workflow;
import com.hexagonal.workflowlab.domain.model.workflow.WorkflowId;
import com.hexagonal.workflowlab.domain.model.workflow.WorkflowState;
import java.time.Instant;
import java.util.List;
import lombok.Getter;

/** The result of converting a published workflow: an ordered list of instructions ready to run. */
@Getter
public final class Experiment {

    private final ExperimentId id;
    private final WorkflowId workflowId;
    private final Instant createdAt;
    private final List<Instruction> instructions;
    private ExperimentState state;
    private String runReference;

    private Experiment(ExperimentId id, WorkflowId workflowId, ExperimentState state, String runReference,
            Instant createdAt, List<Instruction> instructions) {
        this.id = id;
        this.workflowId = workflowId;
        this.state = state;
        this.runReference = runReference;
        this.createdAt = createdAt;
        this.instructions = List.copyOf(instructions);
    }

    public static Experiment fromWorkflow(Workflow workflow, Instant now) {
        if (workflow.getState() != WorkflowState.PUBLISHED) {
            throw new InvalidWorkflowStateException(
                    "Workflow %s is %s: only published workflows can be converted to an experiment"
                            .formatted(workflow.getId().value(), workflow.getState()));
        }
        return new Experiment(ExperimentId.generate(), workflow.getId(), ExperimentState.CREATED, null, now,
                ExperimentConverter.convert(workflow));
    }

    /** Rebuilds an existing experiment (used by persistence adapters). */
    public static Experiment restore(ExperimentId id, WorkflowId workflowId, ExperimentState state,
            String runReference, Instant createdAt, List<Instruction> instructions) {
        return new Experiment(id, workflowId, state, runReference, createdAt, instructions);
    }

    public void markSubmitted(String reference) {
        if (state != ExperimentState.CREATED) {
            throw new InvalidWorkflowStateException("Experiment %s was already submitted".formatted(id.value()));
        }
        this.runReference = reference;
        this.state = ExperimentState.SUBMITTED;
    }
}
