package com.hexagonal.workflowlab.infrastructure.adapter.jpa.mapper;

import com.hexagonal.workflowlab.domain.model.workflow.IncubateNode;
import com.hexagonal.workflowlab.domain.model.workflow.MeasureNode;
import com.hexagonal.workflowlab.domain.model.workflow.MeasurementType;
import com.hexagonal.workflowlab.domain.model.workflow.MixNode;
import com.hexagonal.workflowlab.domain.model.workflow.NodeId;
import com.hexagonal.workflowlab.domain.model.workflow.WorkflowNode;
import com.hexagonal.workflowlab.infrastructure.adapter.jpa.entity.NodeEmbeddable;

/**
 * The only place that knows how a sealed domain node is flattened into one table row and back. Kept apart
 * from {@link WorkflowEntityMapper} so each mapper has a single reason to change.
 */
public final class NodeEmbeddableMapper {

    private static final String MIX = "MIX";
    private static final String INCUBATE = "INCUBATE";
    private static final String MEASURE = "MEASURE";

    private NodeEmbeddableMapper() {
    }

    public static NodeEmbeddable toEmbeddable(WorkflowNode node) {
        NodeEmbeddable row = new NodeEmbeddable();
        row.setNodeKey(node.id().value());
        row.setName(node.name());
        switch (node) {
            case MixNode mix -> {
                row.setNodeType(MIX);
                row.setSpeedRpm(mix.speedRpm());
                row.setDurationSeconds(mix.durationSeconds());
            }
            case IncubateNode incubate -> {
                row.setNodeType(INCUBATE);
                row.setTemperatureCelsius(incubate.temperatureCelsius());
                row.setDurationMinutes(incubate.durationMinutes());
            }
            case MeasureNode measure -> {
                row.setNodeType(MEASURE);
                row.setMeasurementType(measure.measurementType().name());
            }
        }
        return row;
    }

    public static WorkflowNode toDomain(NodeEmbeddable row) {
        NodeId id = new NodeId(row.getNodeKey());
        return switch (row.getNodeType()) {
            case MIX -> new MixNode(id, row.getName(), row.getSpeedRpm(), row.getDurationSeconds());
            case INCUBATE -> new IncubateNode(id, row.getName(), row.getTemperatureCelsius(), row.getDurationMinutes());
            case MEASURE -> new MeasureNode(id, row.getName(), MeasurementType.valueOf(row.getMeasurementType()));
            default -> throw new IllegalStateException("Unknown node type stored in database: " + row.getNodeType());
        };
    }
}
