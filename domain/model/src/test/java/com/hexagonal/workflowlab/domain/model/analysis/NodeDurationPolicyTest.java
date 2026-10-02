package com.hexagonal.workflowlab.domain.model.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import com.hexagonal.workflowlab.domain.model.workflow.IncubateNode;
import com.hexagonal.workflowlab.domain.model.workflow.MeasureNode;
import com.hexagonal.workflowlab.domain.model.workflow.MeasurementType;
import com.hexagonal.workflowlab.domain.model.workflow.MixNode;
import com.hexagonal.workflowlab.domain.model.workflow.NodeId;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class NodeDurationPolicyTest {

    private final NodeDurationPolicy policy = new NodeDurationPolicy();

    @Test
    void shouldUseSecondsForMixNodes() {
        assertThat(policy.durationOf(new MixNode(new NodeId("a"), "Mix", 300, 45))).isEqualTo(Duration.ofSeconds(45));
    }

    @Test
    void shouldUseMinutesForIncubateNodes() {
        assertThat(policy.durationOf(new IncubateNode(new NodeId("a"), "Incubate", 37, 45)))
                .isEqualTo(Duration.ofMinutes(45));
    }

    @Test
    void shouldUseFixedTimePerMeasurementType() {
        assertThat(policy.durationOf(new MeasureNode(new NodeId("a"), "R", MeasurementType.ABSORBANCE)))
                .isEqualTo(Duration.ofSeconds(30));
        assertThat(policy.durationOf(new MeasureNode(new NodeId("a"), "R", MeasurementType.FLUORESCENCE)))
                .isEqualTo(Duration.ofSeconds(45));
        assertThat(policy.durationOf(new MeasureNode(new NodeId("a"), "R", MeasurementType.LUMINESCENCE)))
                .isEqualTo(Duration.ofSeconds(60));
    }
}
