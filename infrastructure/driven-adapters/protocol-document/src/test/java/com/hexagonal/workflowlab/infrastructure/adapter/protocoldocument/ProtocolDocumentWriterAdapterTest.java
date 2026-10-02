package com.hexagonal.workflowlab.infrastructure.adapter.protocoldocument;

import static org.assertj.core.api.Assertions.assertThat;

import com.hexagonal.workflowlab.domain.model.experiment.InstructionType;
import com.hexagonal.workflowlab.domain.model.protocol.DocumentFormat;
import com.hexagonal.workflowlab.domain.model.protocol.ProtocolSheet;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProtocolDocumentWriterAdapterTest {

    private final ProtocolDocumentWriterAdapter adapter = new ProtocolDocumentWriterAdapter();
    private final ProtocolSheet sheet = new ProtocolSheet("Assay",
            List.of(new ProtocolSheet.Step(1, "mix", InstructionType.MIX, "Mix")));

    @Test
    void shouldUseMarkdownFormatterForMarkdown() {
        assertThat(adapter.write(sheet, DocumentFormat.MARKDOWN)).startsWith("# Protocol: Assay");
    }

    @Test
    void shouldUseCsvFormatterForCsv() {
        assertThat(adapter.write(sheet, DocumentFormat.CSV)).startsWith("sequence,node,type,description");
    }
}
