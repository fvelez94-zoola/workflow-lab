package com.hexagonal.workflowlab.infrastructure.adapter.protocoldocument;

import com.hexagonal.workflowlab.domain.model.port.out.ProtocolDocumentWriter;
import com.hexagonal.workflowlab.domain.model.protocol.DocumentFormat;
import com.hexagonal.workflowlab.domain.model.protocol.ProtocolSheet;
import org.springframework.stereotype.Component;

/**
 * Driven adapter: fulfils the {@link ProtocolDocumentWriter} port by picking the formatter of the requested
 * format. The {@code switch} is exhaustive: adding a {@code DocumentFormat} stops compiling until handled here.
 */
@Component
public class ProtocolDocumentWriterAdapter implements ProtocolDocumentWriter {

    private final ProtocolFormatter markdown = new MarkdownProtocolFormatter();
    private final ProtocolFormatter csv = new CsvProtocolFormatter();

    @Override
    public String write(ProtocolSheet sheet, DocumentFormat format) {
        return switch (format) {
            case MARKDOWN -> markdown.format(sheet);
            case CSV -> csv.format(sheet);
        };
    }
}
