package com.hexagonal.workflowlab.infrastructure.adapter.protocoldocument;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CsvEscaperTest {

    @Test
    void shouldLeaveSimpleValuesUntouched() {
        assertThat(CsvEscaper.escape("Mix at 300 rpm")).isEqualTo("Mix at 300 rpm");
    }

    @Test
    void shouldQuoteValuesContainingCommas() {
        assertThat(CsvEscaper.escape("a,b")).isEqualTo("\"a,b\"");
    }

    @Test
    void shouldDoubleQuotesInsideQuotedValues() {
        assertThat(CsvEscaper.escape("say \"hi\"")).isEqualTo("\"say \"\"hi\"\"\"");
    }

    @Test
    void shouldQuoteValuesContainingLineBreaks() {
        assertThat(CsvEscaper.escape("line1\nline2")).isEqualTo("\"line1\nline2\"");
    }

    @Test
    void shouldTurnNullIntoEmptyCell() {
        assertThat(CsvEscaper.escape(null)).isEmpty();
    }
}
