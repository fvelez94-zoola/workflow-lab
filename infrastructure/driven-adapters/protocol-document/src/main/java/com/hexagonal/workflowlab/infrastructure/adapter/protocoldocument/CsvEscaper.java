package com.hexagonal.workflowlab.infrastructure.adapter.protocoldocument;

/**
 * Technical helper: makes a value safe to place in a CSV cell (RFC 4180). It exists only because CSV
 * exists, which is exactly why it lives in this adapter and not in the domain.
 */
final class CsvEscaper {

    private CsvEscaper() {
    }

    static String escape(String value) {
        if (value == null) {
            return "";
        }
        boolean needsQuotes = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r");
        return needsQuotes ? "\"" + value.replace("\"", "\"\"") + "\"" : value;
    }
}
