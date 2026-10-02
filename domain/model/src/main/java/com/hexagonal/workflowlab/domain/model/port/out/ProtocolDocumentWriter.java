package com.hexagonal.workflowlab.domain.model.port.out;

import com.hexagonal.workflowlab.domain.model.protocol.DocumentFormat;
import com.hexagonal.workflowlab.domain.model.protocol.ProtocolSheet;

/** Output port: turns a format-free protocol into the text of a document. */
public interface ProtocolDocumentWriter {

    String write(ProtocolSheet sheet, DocumentFormat format);
}
