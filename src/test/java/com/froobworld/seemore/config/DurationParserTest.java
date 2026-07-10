package com.froobworld.seemore.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class DurationParserTest {
    @Test
    void parsesSupportedHumanDurations() {
        assertEquals(Duration.ofSeconds(10), DurationParser.parse("10s", "test"));
        assertEquals(Duration.ofMinutes(10), DurationParser.parse("10m", "test"));
        assertEquals(Duration.ofHours(2), DurationParser.parse("2h", "test"));
        assertEquals(Duration.ofMillis(250), DurationParser.parse("250ms", "test"));
        assertEquals(Duration.ofMillis(100), DurationParser.parse("2t", "test"));
    }

    @Test
    void convertsDurationsToWholeTicksWithoutRoundingDown() {
        assertEquals(1, DurationParser.toTicks(Duration.ofMillis(1)));
        assertEquals(2, DurationParser.toTicks(Duration.ofMillis(51)));
    }

    @Test
    void rejectsMissingUnitsAndNonPositiveValues() {
        assertThrows(IllegalArgumentException.class, () -> DurationParser.parse("30", "test"));
        assertThrows(IllegalArgumentException.class, () -> DurationParser.parse("0s", "test"));
    }
}
