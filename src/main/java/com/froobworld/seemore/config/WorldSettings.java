package com.froobworld.seemore.config;

import java.util.LinkedHashMap;
import java.util.Map;

public record WorldSettings(int defaultMaximumViewDistance, Map<String, Integer> worldMaximums) {
    public WorldSettings {
        Map<String, Integer> normalized = new LinkedHashMap<>();
        worldMaximums.forEach((world, maximum) -> normalized.put(SeeMoreConfig.normalize(world), maximum));
        worldMaximums = Map.copyOf(normalized);
    }

    public int maximumViewDistance(String worldName) {
        return worldMaximums.getOrDefault(SeeMoreConfig.normalize(worldName), defaultMaximumViewDistance);
    }
}
