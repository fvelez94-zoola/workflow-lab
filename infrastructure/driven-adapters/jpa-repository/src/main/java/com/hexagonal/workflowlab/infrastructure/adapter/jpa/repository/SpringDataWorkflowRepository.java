package com.hexagonal.workflowlab.infrastructure.adapter.jpa.repository;

import com.hexagonal.workflowlab.infrastructure.adapter.jpa.entity.WorkflowEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataWorkflowRepository extends JpaRepository<WorkflowEntity, UUID> {
}
