package com.hexagonal.workflowlab.domain.usecase.experiment;

import com.hexagonal.workflowlab.domain.model.experiment.Experiment;
import com.hexagonal.workflowlab.domain.model.port.in.ConvertWorkflowToExperiment;
import com.hexagonal.workflowlab.domain.model.port.out.ExperimentRepository;
import com.hexagonal.workflowlab.domain.model.port.out.RunSubmissionGateway;
import com.hexagonal.workflowlab.domain.model.port.out.WorkflowRepository;
import com.hexagonal.workflowlab.domain.model.workflow.Workflow;
import com.hexagonal.workflowlab.domain.model.workflow.WorkflowId;
import com.hexagonal.workflowlab.domain.usecase.workflow.WorkflowLookup;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConvertWorkflowToExperimentUseCase implements ConvertWorkflowToExperiment {

    private final WorkflowRepository workflows;
    private final ExperimentRepository experiments;
    private final RunSubmissionGateway runSubmission;
    private final Clock clock;

    @Override
    public Experiment convert(WorkflowId workflowId) {
        Workflow workflow = WorkflowLookup.requireById(workflows, workflowId);

        Experiment experiment = Experiment.fromWorkflow(workflow, clock.instant());
        experiments.save(experiment);

        String runReference = runSubmission.submit(experiment);
        experiment.markSubmitted(runReference);
        return experiments.save(experiment);
    }
}
