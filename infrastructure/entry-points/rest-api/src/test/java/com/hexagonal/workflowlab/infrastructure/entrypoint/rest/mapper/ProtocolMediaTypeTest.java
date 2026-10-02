package com.hexagonal.workflowlab.infrastructure.entrypoint.rest.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.hexagonal.workflowlab.domain.model.protocol.DocumentFormat;
import com.hexagonal.workflowlab.domain.model.protocol.ProtocolDocument;
import org.junit.jupiter.api.Test;

class ProtocolMediaTypeTest {

    @Test
    void shouldMapEachFormatToItsContentType() {
        assertThat(ProtocolMediaType.of(DocumentFormat.MARKDOWN).toString()).startsWith("text/markdown");
        assertThat(ProtocolMediaType.of(DocumentFormat.CSV).toString()).startsWith("text/csv");
    }

    @Test
    void shouldBuildFileNameFromTitleAndFormat() {
        assertThat(ProtocolMediaType.fileName(new ProtocolDocument("Absorbance assay", DocumentFormat.CSV, "")))
                .isEqualTo("absorbance-assay-protocol.csv");
        assertThat(ProtocolMediaType.fileName(new ProtocolDocument("Absorbance assay", DocumentFormat.MARKDOWN, "")))
                .isEqualTo("absorbance-assay-protocol.md");
    }

    @Test
    void shouldStripAccentsAndSymbolsFromTitle() {
        assertThat(ProtocolMediaType.slug("  Café assay #1 / v2 ")).isEqualTo("cafe-assay-1-v2");
    }

    @Test
    void shouldFallBackToGenericNameWhenTitleHasNoUsableCharacters() {
        assertThat(ProtocolMediaType.slug("???")).isEqualTo("workflow");
    }
}
