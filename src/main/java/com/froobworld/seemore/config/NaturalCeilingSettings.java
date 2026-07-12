package com.froobworld.seemore.config;

import org.bukkit.Material;

import java.util.Set;

public record NaturalCeilingSettings(boolean enabled, int searchDistance, int minimumThickness,
                                     Set<Material> additionalMaterials, Set<Material> excludedMaterials) {
    public NaturalCeilingSettings {
        additionalMaterials = Set.copyOf(additionalMaterials);
        excludedMaterials = Set.copyOf(excludedMaterials);
        for (Material material : additionalMaterials) {
            if (excludedMaterials.contains(material)) {
                throw new IllegalArgumentException(
                        "A natural-ceiling material cannot be both additional and excluded: " + material);
            }
        }
    }
}
