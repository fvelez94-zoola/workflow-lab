package com.hexagonal.workflowlab.infrastructure.entrypoint.rest.mapper;

import com.hexagonal.workflowlab.domain.model.port.in.WorkflowDraft;
import com.hexagonal.workflowlab.domain.model.workflow.Dependency;
import com.hexagonal.workflowlab.domain.model.workflow.NodeId;
import com.hexagonal.workflowlab.domain.model.workflow.Workflow;
import com.hexagonal.workflowlab.infrastructure.entrypoint.rest.generated.model.DependencyDto;
import com.hexagonal.workflowlab.infrastructure.entrypoint.rest.generated.model.WorkflowRequestDto;
import com.hexagonal.workflowlab.infrastructure.entrypoint.rest.generated.model.WorkflowResponseDto;
import com.hexagonal.workflowlab.infrastructure.entrypoint.rest.generated.model.WorkflowStatusDto;

/** Workflow-level mapping. Nodes are delegated to {@link NodeRestMapper}. */
public final class WorkflowRestMapper {

    private WorkflowRestMapper() {
    }

    public static WorkflowDraft toDraft(WorkflowRequestDto request) {
        return new WorkflowDraft(
                request.getName(),
                request.getDescription(),
                request.getNodes().stream().map(NodeRestMapper::toDomain).toList(),
                request.getDependencies().stream()
                        .map(dto -> new Dependency(new NodeId(dto.getFrom()), new NodeId(dto.getTo())))
                        .toList());
    }

    public static WorkflowResponseDto toResponse(Workflow workflow) {
        return new WorkflowResponseDto()
                .id(workflow.getId().value())
                .name(workflow.getName())
                .description(workflow.getDescription())
                .status(WorkflowStatusDto.valueOf(workflow.getState().name()))
                .nodes(workflow.getNodes().stream().map(NodeRestMapper::toDto).toList())
                .dependencies(workflow.getDependencies().stream()
                        .map(dependency -> new DependencyDto()
                                .from(dependency.from().value())
                                .to(dependency.to().value()))
                        .toList());
    }
}
