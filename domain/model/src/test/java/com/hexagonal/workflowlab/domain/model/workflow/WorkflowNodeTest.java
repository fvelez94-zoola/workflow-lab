package com.hexagonal.workflowlab.domain.model.workflow;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hexagonal.workflowlab.domain.model.exception.InvalidWorkflowException;
import org.junit.jupiter.api.Test;

class WorkflowNodeTest {

    @Test
    void shouldRejectNonPositiveMixSpeed() {
        assertThatThrownBy(() -> new MixNode(new NodeId("a"), "Mix", 0, 30))
                .isInstanceOf(InvalidWorkflowException.class)
                .hasMessageContaining("speedRpm");
    }

    @Test
    void shouldRejectNonPositiveIncubationDuration() {
        assertThatThrownBy(() -> new IncubateNode(new NodeId("a"), "Incubate", 37, -5))
                .isInstanceOf(InvalidWorkflowException.class)
                .hasMessageContaining("durationMinutes");
    }

    @Test
    void shouldRejectBlankNodeName() {
        assertThatThrownBy(() -> new MeasureNode(new NodeId("a"), " ", MeasurementType.FLUORESCENCE))
                .isInstanceOf(InvalidWorkflowException.class);
    }

    @Test
    void shouldRejectBlankNodeId() {
        assertThatThrownBy(() -> new NodeId("  ")).isInstanceOf(InvalidWorkflowException.class);
    }
}
