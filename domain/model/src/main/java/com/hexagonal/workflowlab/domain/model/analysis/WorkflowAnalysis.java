package com.hexagonal.workflowlab.domain.model.analysis;

import com.hexagonal.workflowlab.domain.model.workflow.WorkflowId;
import java.time.Duration;
import java.util.List;

public record WorkflowAnalysis(WorkflowId workflowId, int nodeCount, Duration estimatedDuration,
        List<CriticalPathStep> criticalPath) {

    public WorkflowAnalysis {
        criticalPath = List.copyOf(criticalPath);
    }
}
