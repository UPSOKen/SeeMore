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
                ResolvedDistance distance = resolveDistance(profile, defaults, worldName);
                return new ResolvedProfile(profile.name(), distance.maximumViewDistance());
            }
        }
        ResolvedDistance distance = resolveDefaultDistance(defaults, worldName);
        return new ResolvedProfile("default", distance.maximumViewDistance());
    }

    public static ResolvedDistance resolveDistance(DistanceProfile profile, WorldSettings defaults,
                                                   String worldName) {
        Integer groupWorldOverride = profile.worldSettings().worldMaximumViewDistance(worldName);
        if (groupWorldOverride != null) {
            return new ResolvedDistance(groupWorldOverride, DistanceResolutionSource.GROUP_WORLD_OVERRIDE);
        }

        Integer globalWorldSetting = defaults.worldMaximumViewDistance(worldName);
        if (globalWorldSetting != null) {
            return new ResolvedDistance(globalWorldSetting, DistanceResolutionSource.WORLD_SETTING);
        }

        Integer groupDefaultOverride = profile.worldSettings().defaultMaximumViewDistance();
        if (groupDefaultOverride != null) {
            return new ResolvedDistance(groupDefaultOverride, DistanceResolutionSource.GROUP_DEFAULT_OVERRIDE);
        }

        return new ResolvedDistance(defaults.maximumViewDistance(worldName),
                DistanceResolutionSource.DEFAULT_SETTING);
    }

    public static ResolvedDistance resolveDefaultDistance(WorldSettings defaults, String worldName) {
        Integer worldSetting = defaults.worldMaximumViewDistance(worldName);
        if (worldSetting != null) {
            return new ResolvedDistance(worldSetting, DistanceResolutionSource.WORLD_SETTING);
        }
        return new ResolvedDistance(defaults.maximumViewDistance(worldName),
                DistanceResolutionSource.DEFAULT_SETTING);
    }
}
