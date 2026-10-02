package com.hexagonal.workflowlab.domain.model.experiment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hexagonal.workflowlab.domain.model.exception.InvalidWorkflowStateException;
import com.hexagonal.workflowlab.domain.model.workflow.Dependency;
import com.hexagonal.workflowlab.domain.model.workflow.IncubateNode;
import com.hexagonal.workflowlab.domain.model.workflow.MeasureNode;
import com.hexagonal.workflowlab.domain.model.workflow.MeasurementType;
import com.hexagonal.workflowlab.domain.model.workflow.MixNode;
import com.hexagonal.workflowlab.domain.model.workflow.NodeId;
import com.hexagonal.workflowlab.domain.model.workflow.Workflow;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExperimentTest {

    private static final Instant NOW = Instant.parse("2026-10-02T10:00:00Z");

    private static Workflow assay() {
        // Declared out of order on purpose: the converter must follow the dependencies, not the declaration.
        return Workflow.draft("Assay", null,
                List.of(
                        new MeasureNode(new NodeId("measure"), "Read", MeasurementType.ABSORBANCE),
                        new IncubateNode(new NodeId("incubate"), "Incubate", 37, 45),
                        new MixNode(new NodeId("mix"), "Mix", 300, 30)),
                List.of(
                        new Dependency(new NodeId("mix"), new NodeId("incubate")),
                        new Dependency(new NodeId("incubate"), new NodeId("measure"))));
    }

    @Test
    void shouldRejectConversionWhenWorkflowIsDraft() {
        assertThatThrownBy(() -> Experiment.fromWorkflow(assay(), NOW))
                .isInstanceOf(InvalidWorkflowStateException.class);
    }

    @Test
    void shouldConvertNodesIntoInstructionsFollowingDependencies() {
        Workflow workflow = assay();
        workflow.publish();

        Experiment experiment = Experiment.fromWorkflow(workflow, NOW);

        assertThat(experiment.getInstructions()).extracting(Instruction::sourceNodeId)
                .containsExactly("mix", "incubate", "incubate", "measure");
    }

    @Test
    void shouldProduceTwoInstructionsForAnIncubateNode() {
        Workflow workflow = assay();
        workflow.publish();

        Experiment experiment = Experiment.fromWorkflow(workflow, NOW);

        assertThat(experiment.getInstructions()).extracting(Instruction::type).containsExactly(
                InstructionType.MIX, InstructionType.SET_TEMPERATURE, InstructionType.WAIT, InstructionType.MEASURE);
    }

    @Test
    void shouldStartAsCreatedWithoutRunReference() {
        Workflow workflow = assay();
        workflow.publish();

        Experiment experiment = Experiment.fromWorkflow(workflow, NOW);

        assertThat(experiment.getState()).isEqualTo(ExperimentState.CREATED);
        assertThat(experiment.getRunReference()).isNull();
        assertThat(experiment.getWorkflowId()).isEqualTo(workflow.getId());
        assertThat(experiment.getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void shouldMarkExperimentAsSubmitted() {
        Workflow workflow = assay();
        workflow.publish();
        Experiment experiment = Experiment.fromWorkflow(workflow, NOW);

        experiment.markSubmitted("RUN-1");

        assertThat(experiment.getState()).isEqualTo(ExperimentState.SUBMITTED);
        assertThat(experiment.getRunReference()).isEqualTo("RUN-1");
    }

    @Test
    void shouldRejectSubmittingTwice() {
        Workflow workflow = assay();
        workflow.publish();
        Experiment experiment = Experiment.fromWorkflow(workflow, NOW);
        experiment.markSubmitted("RUN-1");

        assertThatThrownBy(() -> experiment.markSubmitted("RUN-2")).isInstanceOf(InvalidWorkflowStateException.class);
    }
}
