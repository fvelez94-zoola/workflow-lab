package com.hexagonal.workflowlab.infrastructure.entrypoint.rest;

import com.hexagonal.workflowlab.domain.model.port.in.ExportProtocol;
import com.hexagonal.workflowlab.domain.model.protocol.DocumentFormat;
import com.hexagonal.workflowlab.domain.model.protocol.ProtocolDocument;
import com.hexagonal.workflowlab.domain.model.workflow.WorkflowId;
import com.hexagonal.workflowlab.infrastructure.entrypoint.rest.generated.api.ProtocolsApi;
import com.hexagonal.workflowlab.infrastructure.entrypoint.rest.generated.model.ProtocolFormatDto;
import com.hexagonal.workflowlab.infrastructure.entrypoint.rest.mapper.ProtocolMediaType;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ProtocolsController implements ProtocolsApi {

    private final ExportProtocol exportProtocol;

    @Override
    public ResponseEntity<String> exportProtocol(UUID workflowId, ProtocolFormatDto format) {
        ProtocolDocument document = exportProtocol.export(new WorkflowId(workflowId),
                DocumentFormat.valueOf(format.name()));
        return ResponseEntity.ok()
                .contentType(ProtocolMediaType.of(document.format()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(ProtocolMediaType.fileName(document)).build().toString())
                .body(document.content());
    }
}
