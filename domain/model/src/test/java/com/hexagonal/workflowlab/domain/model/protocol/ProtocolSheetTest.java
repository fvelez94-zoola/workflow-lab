package com.hexagonal.workflowlab.domain.model.protocol;

import static org.assertj.core.api.Assertions.assertThat;

import com.hexagonal.workflowlab.domain.model.experiment.Experiment;
import com.hexagonal.workflowlab.domain.model.experiment.InstructionType;
import com.hexagonal.workflowlab.domain.model.workflow.IncubateNode;
import com.hexagonal.workflowlab.domain.model.workflow.NodeId;
import com.hexagonal.workflowlab.domain.model.workflow.Workflow;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProtocolSheetTest {

    @Test
    void shouldNumberStepsFromOneAndKeepWorkflowNameAsTitle() {
        Workflow workflow = Workflow.draft("Heat assay", null,
                List.of(new IncubateNode(new NodeId("inc"), "Incubate", 37, 45)), List.of());
        workflow.publish();
        Experiment experiment = Experiment.fromWorkflow(workflow, Instant.parse("2026-10-02T10:00:00Z"));

        ProtocolSheet sheet = ProtocolSheet.from(workflow, experiment);

        assertThat(sheet.title()).isEqualTo("Heat assay");
        assertThat(sheet.steps()).extracting(ProtocolSheet.Step::sequence).containsExactly(1, 2);
        assertThat(sheet.steps()).extracting(ProtocolSheet.Step::type)
                .containsExactly(InstructionType.SET_TEMPERATURE, InstructionType.WAIT);
        assertThat(sheet.steps()).extracting(ProtocolSheet.Step::sourceNodeId).containsOnly("inc");
    }
}
