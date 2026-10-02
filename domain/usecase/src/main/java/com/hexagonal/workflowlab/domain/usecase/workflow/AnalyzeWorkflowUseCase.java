package com.hexagonal.workflowlab.domain.usecase.workflow;

import com.hexagonal.workflowlab.domain.model.analysis.WorkflowAnalysis;
import com.hexagonal.workflowlab.domain.model.analysis.WorkflowAnalyzer;
import com.hexagonal.workflowlab.domain.model.port.in.AnalyzeWorkflow;
import com.hexagonal.workflowlab.domain.model.port.out.WorkflowRepository;
import com.hexagonal.workflowlab.domain.model.workflow.WorkflowId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AnalyzeWorkflowUseCase implements AnalyzeWorkflow {

    private final WorkflowRepository workflows;
    private final WorkflowAnalyzer analyzer;

    @Override
    public WorkflowAnalysis analyze(WorkflowId id) {
        return analyzer.analyze(WorkflowLookup.requireById(workflows, id));
    }
}
