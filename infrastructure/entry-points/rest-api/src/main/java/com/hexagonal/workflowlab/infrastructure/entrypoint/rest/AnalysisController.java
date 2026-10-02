package com.hexagonal.workflowlab.infrastructure.entrypoint.rest;

import com.hexagonal.workflowlab.domain.model.port.in.AnalyzeWorkflow;
import com.hexagonal.workflowlab.domain.model.workflow.WorkflowId;
import com.hexagonal.workflowlab.infrastructure.entrypoint.rest.generated.api.AnalysisApi;
import com.hexagonal.workflowlab.infrastructure.entrypoint.rest.generated.model.WorkflowAnalysisResponseDto;
import com.hexagonal.workflowlab.infrastructure.entrypoint.rest.mapper.AnalysisRestMapper;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AnalysisController implements AnalysisApi {

    private final AnalyzeWorkflow analyzeWorkflow;

    @Override
    public ResponseEntity<WorkflowAnalysisResponseDto> getWorkflowAnalysis(UUID workflowId) {
        return ResponseEntity.ok(AnalysisRestMapper.toResponse(analyzeWorkflow.analyze(new WorkflowId(workflowId))));
    }
}
