package com.hexagonal.workflowlab.infrastructure.adapter.labrunner;

import static org.assertj.core.api.Assertions.assertThat;

import com.hexagonal.workflowlab.domain.model.experiment.Experiment;
import com.hexagonal.workflowlab.domain.model.workflow.MixNode;
import com.hexagonal.workflowlab.domain.model.workflow.NodeId;
import com.hexagonal.workflowlab.domain.model.workflow.Workflow;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class LoggingRunSubmissionGatewayTest {

    private final LoggingRunSubmissionGateway gateway = new LoggingRunSubmissionGateway();

    @Test
    void shouldReturnRunReferenceDerivedFromExperimentId() {
        Workflow workflow = Workflow.draft("Single", null, List.of(new MixNode(new NodeId("a"), "Mix", 100, 10)),
                List.of());
        workflow.publish();
        Experiment experiment = Experiment.fromWorkflow(workflow, Instant.parse("2026-10-02T10:00:00Z"));

        String reference = gateway.submit(experiment);

        assertThat(reference).isEqualTo("LAB-RUN-" + experiment.getId().value());
    }
}
