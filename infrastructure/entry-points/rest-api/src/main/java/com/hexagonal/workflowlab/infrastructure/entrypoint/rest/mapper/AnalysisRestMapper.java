package com.hexagonal.workflowlab.infrastructure.entrypoint.rest.mapper;

import com.hexagonal.workflowlab.domain.model.analysis.WorkflowAnalysis;
import com.hexagonal.workflowlab.infrastructure.entrypoint.rest.generated.model.CriticalPathStepDto;
import com.hexagonal.workflowlab.infrastructure.entrypoint.rest.generated.model.WorkflowAnalysisResponseDto;

public final class AnalysisRestMapper {

    private AnalysisRestMapper() {
    }

    public static WorkflowAnalysisResponseDto toResponse(WorkflowAnalysis analysis) {
        return new WorkflowAnalysisResponseDto()
                .workflowId(analysis.workflowId().value())
                .nodeCount(analysis.nodeCount())
                .estimatedDurationSeconds(analysis.estimatedDuration().getSeconds())
                .estimatedDuration(DurationFormatter.format(analysis.estimatedDuration()))
                .criticalPath(analysis.criticalPath().stream()
                        .map(step -> new CriticalPathStepDto()
                                .nodeId(step.nodeId().value())
                                .name(step.nodeName())
                                .durationSeconds(step.duration().getSeconds()))
                        .toList());
    }
}
