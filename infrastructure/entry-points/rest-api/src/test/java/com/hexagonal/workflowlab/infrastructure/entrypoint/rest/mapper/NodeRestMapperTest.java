package com.hexagonal.workflowlab.infrastructure.entrypoint.rest.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hexagonal.workflowlab.domain.model.exception.InvalidWorkflowException;
import com.hexagonal.workflowlab.domain.model.workflow.IncubateNode;
import com.hexagonal.workflowlab.domain.model.workflow.MeasureNode;
import com.hexagonal.workflowlab.domain.model.workflow.MeasurementType;
import com.hexagonal.workflowlab.domain.model.workflow.MixNode;
import com.hexagonal.workflowlab.domain.model.workflow.NodeId;
import com.hexagonal.workflowlab.infrastructure.entrypoint.rest.generated.model.MeasurementTypeDto;
import com.hexagonal.workflowlab.infrastructure.entrypoint.rest.generated.model.NodeDto;
import com.hexagonal.workflowlab.infrastructure.entrypoint.rest.generated.model.NodeTypeDto;
import org.junit.jupiter.api.Test;

class NodeRestMapperTest {

    @Test
    void shouldMapMixDtoToMixNode() {
        NodeDto dto = new NodeDto().id("m").name("Mix").type(NodeTypeDto.MIX).speedRpm(300).durationSeconds(30);

        assertThat(NodeRestMapper.toDomain(dto)).isEqualTo(new MixNode(new NodeId("m"), "Mix", 300, 30));
    }

    @Test
    void shouldMapIncubateDtoToIncubateNode() {
        NodeDto dto = new NodeDto().id("i").name("Inc").type(NodeTypeDto.INCUBATE)
                .temperatureCelsius(37).durationMinutes(45);

        assertThat(NodeRestMapper.toDomain(dto)).isEqualTo(new IncubateNode(new NodeId("i"), "Inc", 37, 45));
    }

    @Test
    void shouldMapMeasureDtoToMeasureNode() {
        NodeDto dto = new NodeDto().id("r").name("Read").type(NodeTypeDto.MEASURE)
                .measurementType(MeasurementTypeDto.FLUORESCENCE);

        assertThat(NodeRestMapper.toDomain(dto))
                .isEqualTo(new MeasureNode(new NodeId("r"), "Read", MeasurementType.FLUORESCENCE));
    }

    @Test
    void shouldRejectMissingParameterForTheNodeType() {
        NodeDto dto = new NodeDto().id("m").name("Mix").type(NodeTypeDto.MIX).speedRpm(300);

        assertThatThrownBy(() -> NodeRestMapper.toDomain(dto))
                .isInstanceOf(InvalidWorkflowException.class)
                .hasMessageContaining("durationSeconds");
    }

    @Test
    void shouldLeaveParametersOfOtherTypesEmptyWhenMappingToDto() {
        NodeDto dto = NodeRestMapper.toDto(new MixNode(new NodeId("m"), "Mix", 300, 30));

        assertThat(dto.getType()).isEqualTo(NodeTypeDto.MIX);
        assertThat(dto.getSpeedRpm()).isEqualTo(300);
        assertThat(dto.getTemperatureCelsius()).isNull();
        assertThat(dto.getMeasurementType()).isNull();
    }
}
