package com.hexagonal.workflowlab.infrastructure.entrypoint.rest;

import com.hexagonal.workflowlab.domain.model.port.in.ConvertWorkflowToExperiment;
import com.hexagonal.workflowlab.domain.model.workflow.WorkflowId;
import com.hexagonal.workflowlab.infrastructure.entrypoint.rest.generated.api.ExperimentsApi;
import com.hexagonal.workflowlab.infrastructure.entrypoint.rest.generated.model.ExperimentResponseDto;
import com.hexagonal.workflowlab.infrastructure.entrypoint.rest.mapper.ExperimentRestMapper;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ExperimentsController implements ExperimentsApi {

    private final ConvertWorkflowToExperiment convertWorkflowToExperiment;

    @Override
    public ResponseEntity<ExperimentResponseDto> convertWorkflowToExperiment(UUID workflowId) {
        var experiment = convertWorkflowToExperiment.convert(new WorkflowId(workflowId));
        return ResponseEntity.status(HttpStatus.CREATED).body(ExperimentRestMapper.toResponse(experiment));
    }
}
