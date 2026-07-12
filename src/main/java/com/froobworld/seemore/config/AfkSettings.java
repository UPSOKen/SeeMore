package com.froobworld.seemore.config;

import java.time.Duration;
import java.util.Objects;

public record AfkSettings(boolean enabled, Duration checkInterval, Duration timeout, int maximumViewDistance,
                          int minimumReduction, double minimumLookChange, int requiredLookEvents,
                          Duration lookEventWindow, String reducedMessage, String restoringMessage) {
    public AfkSettings {
        reducedMessage = Objects.requireNonNullElse(reducedMessage, "");
        restoringMessage = Objects.requireNonNullElse(restoringMessage, "");
    }
}
