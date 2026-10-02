package com.hexagonal.workflowlab.infrastructure.adapter.protocoldocument;

import static org.assertj.core.api.Assertions.assertThat;

import com.hexagonal.workflowlab.domain.model.experiment.InstructionType;
import com.hexagonal.workflowlab.domain.model.protocol.ProtocolSheet;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProtocolFormattersTest {

    private static final ProtocolSheet SHEET = new ProtocolSheet("Absorbance assay", List.of(
            new ProtocolSheet.Step(1, "mix", InstructionType.MIX, "Mix at 300 rpm for 30 s"),
            new ProtocolSheet.Step(2, "incubate", InstructionType.SET_TEMPERATURE, "Set temperature, then \"hold\", 37 C")));

    @Test
    void shouldWriteMarkdownWithTitleAndTable() {
        String markdown = new MarkdownProtocolFormatter().format(SHEET);

        assertThat(markdown).startsWith("# Protocol: Absorbance assay\n\n| # | Node | Type | Description |");
        assertThat(markdown).contains("| 1 | mix | MIX | Mix at 300 rpm for 30 s |");
    }

    @Test
    void shouldWriteCsvWithHeaderAndEscapedDescriptions() {
        String csv = new CsvProtocolFormatter().format(SHEET);

        assertThat(csv).isEqualTo("""
                sequence,node,type,description
                1,mix,MIX,Mix at 300 rpm for 30 s
                2,incubate,SET_TEMPERATURE,"Set temperature, then ""hold"", 37 C"
                """);
    }

    @Test
    void shouldWriteOnlyTheHeaderWhenThereAreNoSteps() {
        assertThat(new CsvProtocolFormatter().format(new ProtocolSheet("Empty", List.of())))
                .isEqualTo("sequence,node,type,description\n");
    }
}
