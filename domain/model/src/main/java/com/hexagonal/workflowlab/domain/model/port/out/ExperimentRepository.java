package com.hexagonal.workflowlab.domain.model.port.out;

import com.hexagonal.workflowlab.domain.model.experiment.Experiment;

public interface ExperimentRepository {

    Experiment save(Experiment experiment);
}
