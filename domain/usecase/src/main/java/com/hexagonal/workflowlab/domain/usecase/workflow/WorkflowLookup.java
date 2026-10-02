package com.hexagonal.workflowlab.domain.usecase.workflow;

import com.hexagonal.workflowlab.domain.model.exception.WorkflowNotFoundException;
import com.hexagonal.workflowlab.domain.model.port.out.WorkflowRepository;
import com.hexagonal.workflowlab.domain.model.workflow.Workflow;
import com.hexagonal.workflowlab.domain.model.workflow.WorkflowId;

/**
 * Shared orchestration step: "load the workflow or fail with NotFound".
 *
 * <p>It is not business (the model knows nothing about it) and not technology (no framework), it is
 * application plumbing that every use case repeated. Extracting it keeps the use cases short.
 */
public final class WorkflowLookup {

    private WorkflowLookup() {
    }

    public static Workflow requireById(WorkflowRepository workflows, WorkflowId id) {
        return workflows.findById(id).orElseThrow(() -> new WorkflowNotFoundException(id));
    }
}
