package com.hexagonal.workflowlab.infrastructure.adapter.jpa.repository;

import com.hexagonal.workflowlab.infrastructure.adapter.jpa.entity.ExperimentEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataExperimentRepository extends JpaRepository<ExperimentEntity, UUID> {
}
