package com.hexagonal.workflowlab.domain.usecase.experiment;

import com.hexagonal.workflowlab.domain.model.experiment.Experiment;
import com.hexagonal.workflowlab.domain.model.port.in.ExportProtocol;
import com.hexagonal.workflowlab.domain.model.port.out.ProtocolDocumentWriter;
import com.hexagonal.workflowlab.domain.model.port.out.WorkflowRepository;
import com.hexagonal.workflowlab.domain.model.protocol.DocumentFormat;
import com.hexagonal.workflowlab.domain.model.protocol.ProtocolDocument;
import com.hexagonal.workflowlab.domain.model.protocol.ProtocolSheet;
import com.hexagonal.workflowlab.domain.model.workflow.Workflow;
import com.hexagonal.workflowlab.domain.model.workflow.WorkflowId;
import com.hexagonal.workflowlab.domain.usecase.workflow.WorkflowLookup;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExportProtocolUseCase implements ExportProtocol {

    private final WorkflowRepository workflows;
    private final ProtocolDocumentWriter writer;
    private final Clock clock;

    @Override
    public ProtocolDocument export(WorkflowId id, DocumentFormat format) {
        Workflow workflow = WorkflowLookup.requireById(workflows, id);
        // Converted only to read its instructions: it is never saved nor submitted.
        Experiment preview = Experiment.fromWorkflow(workflow, clock.instant());
        String content = writer.write(ProtocolSheet.from(workflow, preview), format);
        return new ProtocolDocument(workflow.getName(), format, content);
    }
}
