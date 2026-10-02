package com.hexagonal.workflowlab.domain.model.port.in;

import com.hexagonal.workflowlab.domain.model.workflow.Workflow;
import com.hexagonal.workflowlab.domain.model.workflow.WorkflowId;
import java.util.List;

/** Use case 1: create, read, update and delete workflows. */
public interface WorkflowCrud {

    Workflow create(WorkflowDraft draft);

    Workflow get(WorkflowId id);

    List<Workflow> list();

    Workflow update(WorkflowId id, WorkflowDraft draft);

    void delete(WorkflowId id);
}
