package com.hexagonal.workflowlab.infrastructure.adapter.protocoldocument;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** Technical helper: renders a GitHub-flavoured Markdown table. */
final class MarkdownTable {

    private final List<String> headers;
    private final List<List<String>> rows = new ArrayList<>();

    MarkdownTable(String... headers) {
        this.headers = List.of(headers);
    }

    MarkdownTable row(String... cells) {
        rows.add(List.of(cells));
        return this;
    }

    String render() {
        StringBuilder table = new StringBuilder();
        table.append(line(headers));
        table.append(line(headers.stream().map(header -> "---").toList()));
        rows.forEach(row -> table.append(line(row)));
        return table.toString();
    }

    private static String line(List<String> cells) {
        return cells.stream().map(MarkdownTable::escape).collect(Collectors.joining(" | ", "| ", " |\n"));
    }

    private static String escape(String cell) {
        return cell.replace("|", "\\|").replace("\r", " ").replace("\n", " ");
    }
}
