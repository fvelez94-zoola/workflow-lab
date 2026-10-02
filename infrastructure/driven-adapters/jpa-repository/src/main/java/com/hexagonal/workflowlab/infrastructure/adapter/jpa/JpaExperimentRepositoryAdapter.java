package com.hexagonal.workflowlab.infrastructure.adapter.jpa;

import com.hexagonal.workflowlab.domain.model.experiment.Experiment;
import com.hexagonal.workflowlab.domain.model.port.out.ExperimentRepository;
import com.hexagonal.workflowlab.infrastructure.adapter.jpa.mapper.ExperimentEntityMapper;
import com.hexagonal.workflowlab.infrastructure.adapter.jpa.repository.SpringDataExperimentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JpaExperimentRepositoryAdapter implements ExperimentRepository {

    private final SpringDataExperimentRepository repository;

    @Override
    public Experiment save(Experiment experiment) {
        return ExperimentEntityMapper.toDomain(repository.save(ExperimentEntityMapper.toEntity(experiment)));
    }
}
