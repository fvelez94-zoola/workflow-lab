package com.hexagonal.workflowlab.infrastructure.adapter.jpa;

import com.hexagonal.workflowlab.domain.model.port.out.WorkflowRepository;
import com.hexagonal.workflowlab.domain.model.workflow.Workflow;
import com.hexagonal.workflowlab.domain.model.workflow.WorkflowId;
import com.hexagonal.workflowlab.infrastructure.adapter.jpa.mapper.WorkflowEntityMapper;
import com.hexagonal.workflowlab.infrastructure.adapter.jpa.repository.SpringDataWorkflowRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/** Driven adapter: fulfils the {@link WorkflowRepository} port using Spring Data JPA. */
@Repository
@RequiredArgsConstructor
public class JpaWorkflowRepositoryAdapter implements WorkflowRepository {

    private final SpringDataWorkflowRepository repository;

    @Override
    public Workflow save(Workflow workflow) {
        return WorkflowEntityMapper.toDomain(repository.save(WorkflowEntityMapper.toEntity(workflow)));
    }

    @Override
    public Optional<Workflow> findById(WorkflowId id) {
        return repository.findById(id.value()).map(WorkflowEntityMapper::toDomain);
    }

    @Override
    public List<Workflow> findAll() {
        return repository.findAll().stream().map(WorkflowEntityMapper::toDomain).toList();
    }

    @Override
    public void deleteById(WorkflowId id) {
        repository.deleteById(id.value());
    }
}
