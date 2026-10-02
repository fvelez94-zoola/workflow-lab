package com.hexagonal.workflowlab.infrastructure.entrypoint.rest.mapper;

import com.hexagonal.workflowlab.domain.model.protocol.DocumentFormat;
import com.hexagonal.workflowlab.domain.model.protocol.ProtocolDocument;
import java.text.Normalizer;
import java.util.Locale;
import org.springframework.http.MediaType;

/**
 * HTTP-only knowledge about a protocol document: which Content-Type it has and what the downloaded file
 * is called. The domain only knows "MARKDOWN" or "CSV".
 */
public final class ProtocolMediaType {

    private ProtocolMediaType() {
    }

    public static MediaType of(DocumentFormat format) {
        return switch (format) {
            case MARKDOWN -> MediaType.parseMediaType("text/markdown;charset=UTF-8");
            case CSV -> MediaType.parseMediaType("text/csv;charset=UTF-8");
        };
    }

    public static String fileName(ProtocolDocument document) {
        String extension = switch (document.format()) {
            case MARKDOWN -> "md";
            case CSV -> "csv";
        };
        return slug(document.title()) + "-protocol." + extension;
    }

    static String slug(String title) {
        String ascii = Normalizer.normalize(title, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String slug = ascii.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
        return slug.isEmpty() ? "workflow" : slug;
    }
}
