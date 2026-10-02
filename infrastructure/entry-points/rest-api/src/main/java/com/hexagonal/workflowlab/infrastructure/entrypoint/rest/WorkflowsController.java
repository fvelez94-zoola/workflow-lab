package com.hexagonal.workflowlab.infrastructure.entrypoint.rest;

import com.hexagonal.workflowlab.domain.model.port.in.PublishWorkflow;
import com.hexagonal.workflowlab.domain.model.port.in.WorkflowCrud;
import com.hexagonal.workflowlab.domain.model.workflow.WorkflowId;
import com.hexagonal.workflowlab.infrastructure.entrypoint.rest.generated.api.WorkflowsApi;
import com.hexagonal.workflowlab.infrastructure.entrypoint.rest.generated.model.WorkflowRequestDto;
import com.hexagonal.workflowlab.infrastructure.entrypoint.rest.generated.model.WorkflowResponseDto;
import com.hexagonal.workflowlab.infrastructure.entrypoint.rest.mapper.WorkflowRestMapper;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** Driving adapter: turns HTTP into calls on the input ports. No business logic here. */
@RestController
@RequiredArgsConstructor
public class WorkflowsController implements WorkflowsApi {

    private final WorkflowCrud workflowCrud;
    private final PublishWorkflow publishWorkflow;

    @Override
    public ResponseEntity<WorkflowResponseDto> createWorkflow(WorkflowRequestDto request) {
        var created = workflowCrud.create(WorkflowRestMapper.toDraft(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(WorkflowRestMapper.toResponse(created));
    }

    @Override
    public ResponseEntity<List<WorkflowResponseDto>> listWorkflows() {
        return ResponseEntity.ok(workflowCrud.list().stream().map(WorkflowRestMapper::toResponse).toList());
    }

    @Override
    public ResponseEntity<WorkflowResponseDto> getWorkflow(UUID workflowId) {
        return ResponseEntity.ok(WorkflowRestMapper.toResponse(workflowCrud.get(new WorkflowId(workflowId))));
    }

    @Override
    public ResponseEntity<WorkflowResponseDto> updateWorkflow(UUID workflowId, WorkflowRequestDto request) {
        var updated = workflowCrud.update(new WorkflowId(workflowId), WorkflowRestMapper.toDraft(request));
        return ResponseEntity.ok(WorkflowRestMapper.toResponse(updated));
    }

    @Override
    public ResponseEntity<Void> deleteWorkflow(UUID workflowId) {
        workflowCrud.delete(new WorkflowId(workflowId));
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<WorkflowResponseDto> publishWorkflow(UUID workflowId) {
        return ResponseEntity.ok(WorkflowRestMapper.toResponse(publishWorkflow.publish(new WorkflowId(workflowId))));
    }
}
