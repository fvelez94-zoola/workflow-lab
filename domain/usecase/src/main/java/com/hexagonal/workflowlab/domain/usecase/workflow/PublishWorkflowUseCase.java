package com.hexagonal.workflowlab.domain.usecase.workflow;

import com.hexagonal.workflowlab.domain.model.port.in.PublishWorkflow;
import com.hexagonal.workflowlab.domain.model.port.out.WorkflowRepository;
import com.hexagonal.workflowlab.domain.model.workflow.Workflow;
import com.hexagonal.workflowlab.domain.model.workflow.WorkflowId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PublishWorkflowUseCase implements PublishWorkflow {

    private final WorkflowRepository workflows;

    @Override
    public Workflow publish(WorkflowId id) {
        Workflow workflow = WorkflowLookup.requireById(workflows, id);
        workflow.publish();
        return workflows.save(workflow);
    }
}
