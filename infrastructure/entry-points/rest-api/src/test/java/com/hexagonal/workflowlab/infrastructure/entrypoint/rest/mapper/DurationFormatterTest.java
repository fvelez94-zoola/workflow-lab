package com.hexagonal.workflowlab.infrastructure.entrypoint.rest.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class DurationFormatterTest {

    @Test
    void shouldFormatHoursMinutesAndSeconds() {
        assertThat(DurationFormatter.format(Duration.ofSeconds(4530))).isEqualTo("1h 15m 30s");
    }

    @Test
    void shouldOmitZeroUnits() {
        assertThat(DurationFormatter.format(Duration.ofHours(2))).isEqualTo("2h");
        assertThat(DurationFormatter.format(Duration.ofSeconds(3605))).isEqualTo("1h 5s");
    }

    @Test
    void shouldFormatSecondsOnly() {
        assertThat(DurationFormatter.format(Duration.ofSeconds(45))).isEqualTo("45s");
    }

    @Test
    void shouldFormatZeroAsZeroSeconds() {
        assertThat(DurationFormatter.format(Duration.ZERO)).isEqualTo("0s");
    }
}
