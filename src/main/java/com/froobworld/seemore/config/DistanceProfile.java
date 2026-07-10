package com.froobworld.seemore.config;

import java.util.Objects;

public record DistanceProfile(String name, String permission, WorldSettings worldSettings) {
    public DistanceProfile {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(permission, "permission");
        Objects.requireNonNull(worldSettings, "worldSettings");
    }
}
