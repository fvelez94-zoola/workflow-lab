package com.hexagonal.workflowlab.infrastructure.entrypoint.rest.mapper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Presentation helper: "4530 seconds" becomes "1h 15m 30s". It lives here and not in the domain on
 * purpose: the domain deals in {@link Duration}; how a number is shown to a person is a UI decision (it
 * could be localized, abbreviated or dropped without touching a single business rule).
 */
public final class DurationFormatter {

    private DurationFormatter() {
    }

    public static String format(Duration duration) {
        long totalSeconds = duration.getSeconds();
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        List<String> parts = new ArrayList<>();
        if (hours > 0) {
            parts.add(hours + "h");
        }
        if (minutes > 0) {
            parts.add(minutes + "m");
        }
        if (seconds > 0 || parts.isEmpty()) {
            parts.add(seconds + "s");
        }
        return String.join(" ", parts);
    }
}
