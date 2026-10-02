package com.hexagonal.workflowlab.domain.model.port.in;

import com.hexagonal.workflowlab.domain.model.analysis.WorkflowAnalysis;
import com.hexagonal.workflowlab.domain.model.workflow.WorkflowId;

/** Use case 4: estimate how long a workflow takes and which nodes decide it. */
public interface AnalyzeWorkflow {

    WorkflowAnalysis analyze(WorkflowId id);
}
