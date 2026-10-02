package com.hexagonal.workflowlab.domain.model.port.out;

import com.hexagonal.workflowlab.domain.model.experiment.Experiment;

/** Output port: sends an experiment to whatever executes it (a lab, a robot, a queue...). */
public interface RunSubmissionGateway {

    /** @return the reference under which the external system tracks the run */
    String submit(Experiment experiment);
}
