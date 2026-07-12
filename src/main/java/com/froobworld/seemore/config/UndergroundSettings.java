package com.froobworld.seemore.config;

import java.time.Duration;
import java.util.Set;
import java.util.stream.Collectors;

public record UndergroundSettings(boolean enabled, boolean bypassPermissionEnabled,
                                  WorldListMode worldListMode, Set<String> worlds,
                                  Duration checkInterval, Duration enterAfter, Duration exitAfter,
                                  int minimumDepth, int exitDepth, int maximumViewDistance,
                                  NaturalCeilingSettings naturalCeiling) {
    public static final String BYPASS_PERMISSION = "seemore.underground.bypass";

    public UndergroundSettings {
        worlds = worlds.stream()
                .map(SeeMoreConfig::normalize)
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean isWorldEnabled(String worldName) {
        boolean listed = worlds.contains(SeeMoreConfig.normalize(worldName));
        return worldListMode == WorldListMode.WHITELIST ? listed : !listed;
    }
}
