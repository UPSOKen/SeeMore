package com.froobworld.seemore.config;

import java.time.Duration;

public record AfkSettings(boolean enabled, Duration checkInterval, Duration timeout, int maximumViewDistance,
                          double minimumLookChange, int requiredLookEvents, Duration lookEventWindow) {
}
