package com.froobworld.seemore.config;

import java.util.List;
import java.util.function.Predicate;

public final class PermissionProfileResolver {
    private PermissionProfileResolver() {
    }

    public static ResolvedProfile resolve(List<DistanceProfile> profiles, Predicate<String> hasPermission,
                                          WorldSettings defaults, String worldName) {
        for (DistanceProfile profile : profiles) {
            if (hasPermission.test(profile.permission())) {
                Integer groupWorldOverride = profile.worldSettings().worldMaximumViewDistance(worldName);
                if (groupWorldOverride != null) {
                    return new ResolvedProfile(profile.name(), groupWorldOverride);
                }

                Integer globalWorldSetting = defaults.worldMaximumViewDistance(worldName);
                if (globalWorldSetting != null) {
                    return new ResolvedProfile(profile.name(), globalWorldSetting);
                }

                Integer groupDefaultOverride = profile.worldSettings().defaultMaximumViewDistance();
                if (groupDefaultOverride != null) {
                    return new ResolvedProfile(profile.name(), groupDefaultOverride);
                }

                return new ResolvedProfile(profile.name(), defaults.maximumViewDistance(worldName));
            }
        }
        return new ResolvedProfile("default", defaults.maximumViewDistance(worldName));
    }
}
