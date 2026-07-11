package com.froobworld.seemore.config;

import java.util.LinkedHashMap;
import java.util.Map;

public record WorldSettings(Integer defaultMaximumViewDistance, Map<String, Integer> worldMaximums) {
    public WorldSettings {
        Map<String, Integer> normalized = new LinkedHashMap<>();
        worldMaximums.forEach((world, maximum) -> normalized.put(SeeMoreConfig.normalize(world), maximum));
        worldMaximums = Map.copyOf(normalized);
    }

    public int maximumViewDistance(String worldName) {
        Integer worldMaximum = worldMaximumViewDistance(worldName);
        if (worldMaximum != null) {
            return worldMaximum;
        }
        if (defaultMaximumViewDistance == null) {
            throw new IllegalStateException("World settings do not define a default maximum view distance.");
        }
        return defaultMaximumViewDistance;
    }

    public Integer worldMaximumViewDistance(String worldName) {
        return worldMaximums.get(SeeMoreConfig.normalize(worldName));
    }
}
