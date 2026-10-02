package com.hexagonal.workflowlab.infrastructure.entrypoint.rest.mapper;

import static com.hexagonal.workflowlab.infrastructure.entrypoint.rest.mapper.RequiredParameters.require;

import com.hexagonal.workflowlab.domain.model.workflow.IncubateNode;
import com.hexagonal.workflowlab.domain.model.workflow.MeasureNode;
import com.hexagonal.workflowlab.domain.model.workflow.MeasurementType;
import com.hexagonal.workflowlab.domain.model.workflow.MixNode;
import com.hexagonal.workflowlab.domain.model.workflow.NodeId;
import com.hexagonal.workflowlab.domain.model.workflow.WorkflowNode;
import com.hexagonal.workflowlab.infrastructure.entrypoint.rest.generated.model.MeasurementTypeDto;
import com.hexagonal.workflowlab.infrastructure.entrypoint.rest.generated.model.NodeDto;
import com.hexagonal.workflowlab.infrastructure.entrypoint.rest.generated.model.NodeTypeDto;

/** The flat {@code NodeDto} (type + optional parameters) to and from the sealed domain nodes. */
public final class NodeRestMapper {

    private NodeRestMapper() {
    }

    public static WorkflowNode toDomain(NodeDto dto) {
        NodeId id = new NodeId(dto.getId());
        return switch (dto.getType()) {
            case MIX -> new MixNode(id, dto.getName(),
                    require(dto.getSpeedRpm(), "speedRpm", dto.getType()),
                    require(dto.getDurationSeconds(), "durationSeconds", dto.getType()));
            case INCUBATE -> new IncubateNode(id, dto.getName(),
                    require(dto.getTemperatureCelsius(), "temperatureCelsius", dto.getType()),
                    require(dto.getDurationMinutes(), "durationMinutes", dto.getType()));
            case MEASURE -> new MeasureNode(id, dto.getName(), MeasurementType.valueOf(
                    require(dto.getMeasurementType(), "measurementType", dto.getType()).name()));
        };
    }

    public static NodeDto toDto(WorkflowNode node) {
        NodeDto dto = new NodeDto().id(node.id().value()).name(node.name());
        return switch (node) {
            case MixNode mix -> dto.type(NodeTypeDto.MIX)
                    .speedRpm(mix.speedRpm())
                    .durationSeconds(mix.durationSeconds());
            case IncubateNode incubate -> dto.type(NodeTypeDto.INCUBATE)
                    .temperatureCelsius(incubate.temperatureCelsius())
                    .durationMinutes(incubate.durationMinutes());
            case MeasureNode measure -> dto.type(NodeTypeDto.MEASURE)
                    .measurementType(MeasurementTypeDto.valueOf(measure.measurementType().name()));
        };
    }
}
