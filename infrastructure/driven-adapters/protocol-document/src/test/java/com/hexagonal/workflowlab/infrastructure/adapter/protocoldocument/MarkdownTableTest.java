package com.hexagonal.workflowlab.infrastructure.adapter.protocoldocument;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MarkdownTableTest {

    @Test
    void shouldRenderHeaderSeparatorAndRows() {
        String table = new MarkdownTable("A", "B").row("1", "2").row("3", "4").render();

        assertThat(table).isEqualTo("""
                | A | B |
                | --- | --- |
                | 1 | 2 |
                | 3 | 4 |
                """);
    }

    @Test
    void shouldEscapePipesAndFlattenLineBreaksInsideCells() {
        String table = new MarkdownTable("A").row("x|y\nz").render();

        assertThat(table).contains("| x\\|y z |");
    }
}
