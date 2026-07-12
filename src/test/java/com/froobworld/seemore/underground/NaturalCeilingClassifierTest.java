package com.froobworld.seemore.underground;

import com.froobworld.seemore.config.NaturalCeilingSettings;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class NaturalCeilingClassifierTest {
    @Test
    void acceptsVanillaTagsAndConservativeSupplementalMaterials() {
        NaturalCeilingClassifier classifier = new NaturalCeilingClassifier(material -> material == Material.STONE);
        NaturalCeilingSettings settings = settings(Set.of(), Set.of());

        assertTrue(classifier.isNatural(Material.STONE, settings));
        assertTrue(classifier.isNatural(Material.CALCITE, settings));
        assertFalse(classifier.isNatural(Material.OAK_PLANKS, settings));
    }

    @Test
    void appliesConfiguredAdditionsAndExclusionsAroundTheDefaults() {
        NaturalCeilingClassifier classifier = new NaturalCeilingClassifier(material -> material == Material.STONE);
        NaturalCeilingSettings settings = settings(Set.of(Material.SCULK), Set.of(Material.STONE));

        assertTrue(classifier.isNatural(Material.SCULK, settings));
        assertFalse(classifier.isNatural(Material.STONE, settings));
    }

    private static NaturalCeilingSettings settings(Set<Material> additions, Set<Material> exclusions) {
        return new NaturalCeilingSettings(true, 32, 2, additions, exclusions);
    }
}
