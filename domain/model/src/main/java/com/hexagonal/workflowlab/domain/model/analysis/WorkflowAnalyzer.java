package com.hexagonal.workflowlab.domain.model.analysis;

import com.hexagonal.workflowlab.domain.model.workflow.Workflow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Domain service: the single entry point to "analyze a workflow". It composes the policy and the
 * calculator so that the use case only has to call one collaborator.
 */
@Component
@RequiredArgsConstructor
public final class WorkflowAnalyzer {

    private final CriticalPathCalculator criticalPathCalculator;

    public WorkflowAnalysis analyze(Workflow workflow) {
        CriticalPath criticalPath = criticalPathCalculator.calculate(workflow);
        return new WorkflowAnalysis(workflow.getId(), workflow.getNodes().size(), criticalPath.totalDuration(),
                criticalPath.steps());
    }
}
