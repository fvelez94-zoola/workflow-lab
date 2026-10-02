package com.hexagonal.workflowlab.domain.model.port.in;

import com.hexagonal.workflowlab.domain.model.protocol.DocumentFormat;
import com.hexagonal.workflowlab.domain.model.protocol.ProtocolDocument;
import com.hexagonal.workflowlab.domain.model.workflow.WorkflowId;

/** Use case 5: export the instructions of a published workflow as a document. */
public interface ExportProtocol {

    ProtocolDocument export(WorkflowId id, DocumentFormat format);
}
