package com.hexagonal.workflowlab.infrastructure.adapter.jpa.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hexagonal.workflowlab.domain.model.workflow.IncubateNode;
import com.hexagonal.workflowlab.domain.model.workflow.MeasureNode;
import com.hexagonal.workflowlab.domain.model.workflow.MeasurementType;
import com.hexagonal.workflowlab.domain.model.workflow.MixNode;
import com.hexagonal.workflowlab.domain.model.workflow.NodeId;
import com.hexagonal.workflowlab.domain.model.workflow.WorkflowNode;
import com.hexagonal.workflowlab.infrastructure.adapter.jpa.entity.NodeEmbeddable;
import java.util.List;
import org.junit.jupiter.api.Test;

/** No Spring and no database: a mapper is just a function, so it is tested as one. */
class NodeEmbeddableMapperTest {

    @Test
    void shouldRoundTripEveryNodeType() {
        List<WorkflowNode> nodes = List.of(
                new MixNode(new NodeId("m"), "Mix", 300, 30),
                new IncubateNode(new NodeId("i"), "Incubate", 37, 45),
                new MeasureNode(new NodeId("r"), "Read", MeasurementType.LUMINESCENCE));

        List<WorkflowNode> roundTripped = nodes.stream()
                .map(NodeEmbeddableMapper::toEmbeddable)
                .map(NodeEmbeddableMapper::toDomain)
                .toList();

        assertThat(roundTripped).containsExactlyElementsOf(nodes);
    }

    @Test
    void shouldOnlyFillTheColumnsOfTheNodeType() {
        NodeEmbeddable row = NodeEmbeddableMapper.toEmbeddable(new MixNode(new NodeId("m"), "Mix", 300, 30));

        assertThat(row.getNodeType()).isEqualTo("MIX");
        assertThat(row.getSpeedRpm()).isEqualTo(300);
        assertThat(row.getTemperatureCelsius()).isNull();
        assertThat(row.getMeasurementType()).isNull();
    }

    @Test
    void shouldFailLoudlyWhenDatabaseContainsUnknownNodeType() {
        NodeEmbeddable row = new NodeEmbeddable("x", "Unknown", "TELEPORT", null, null, null, null, null);

        assertThatThrownBy(() -> NodeEmbeddableMapper.toDomain(row)).isInstanceOf(IllegalStateException.class);
    }
}
