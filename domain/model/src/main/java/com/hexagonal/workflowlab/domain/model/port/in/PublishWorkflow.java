package com.hexagonal.workflowlab.domain.model.port.in;

import com.hexagonal.workflowlab.domain.model.workflow.Workflow;
import com.hexagonal.workflowlab.domain.model.workflow.WorkflowId;

/** Use case 2: validate a draft and freeze it as PUBLISHED. */
public interface PublishWorkflow {

    Workflow publish(WorkflowId id);
}
