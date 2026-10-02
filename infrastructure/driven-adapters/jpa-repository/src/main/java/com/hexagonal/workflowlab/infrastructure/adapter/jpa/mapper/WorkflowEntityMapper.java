package com.hexagonal.workflowlab.infrastructure.adapter.jpa.mapper;

import com.hexagonal.workflowlab.domain.model.workflow.Dependency;
import com.hexagonal.workflowlab.domain.model.workflow.NodeId;
import com.hexagonal.workflowlab.domain.model.workflow.Workflow;
import com.hexagonal.workflowlab.domain.model.workflow.WorkflowId;
import com.hexagonal.workflowlab.domain.model.workflow.WorkflowState;
import com.hexagonal.workflowlab.infrastructure.adapter.jpa.entity.DependencyEmbeddable;
import com.hexagonal.workflowlab.infrastructure.adapter.jpa.entity.WorkflowEntity;
import java.util.ArrayList;

/** Domain <-> JPA translation for workflows. */
public final class WorkflowEntityMapper {

    private WorkflowEntityMapper() {
    }

    public static WorkflowEntity toEntity(Workflow workflow) {
        WorkflowEntity entity = new WorkflowEntity();
        entity.setId(workflow.getId().value());
        entity.setName(workflow.getName());
        entity.setDescription(workflow.getDescription());
        entity.setState(workflow.getState().name());
        entity.setNodes(new ArrayList<>(workflow.getNodes().stream().map(NodeEmbeddableMapper::toEmbeddable).toList()));
        entity.setDependencies(new ArrayList<>(workflow.getDependencies().stream()
                .map(dependency -> new DependencyEmbeddable(dependency.from().value(), dependency.to().value()))
                .toList()));
        return entity;
    }

    public static Workflow toDomain(WorkflowEntity entity) {
        return Workflow.restore(
                new WorkflowId(entity.getId()),
                entity.getName(),
                entity.getDescription(),
                WorkflowState.valueOf(entity.getState()),
                entity.getNodes().stream().map(NodeEmbeddableMapper::toDomain).toList(),
                entity.getDependencies().stream()
                        .map(row -> new Dependency(new NodeId(row.getSourceNodeKey()), new NodeId(row.getTargetNodeKey())))
                        .toList());
    }
}
