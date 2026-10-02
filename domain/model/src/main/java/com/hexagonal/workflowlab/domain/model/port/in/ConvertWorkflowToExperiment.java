package com.hexagonal.workflowlab.domain.model.port.in;

import com.hexagonal.workflowlab.domain.model.experiment.Experiment;
import com.hexagonal.workflowlab.domain.model.workflow.WorkflowId;

/** Use case 3: turn a published workflow into an experiment and submit it for execution. */
public interface ConvertWorkflowToExperiment {

    Experiment convert(WorkflowId workflowId);
}
