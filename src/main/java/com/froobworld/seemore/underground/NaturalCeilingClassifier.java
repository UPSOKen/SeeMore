package com.froobworld.seemore.underground;

import com.froobworld.seemore.config.NaturalCeilingSettings;
import org.bukkit.Material;
import org.bukkit.Tag;

import java.util.Set;
import java.util.function.Predicate;

public final class NaturalCeilingClassifier {
    private static final Set<Material> SUPPLEMENTAL_NATURAL_MATERIALS = Set.of(
            Material.CALCITE,
            Material.DRIPSTONE_BLOCK,
            Material.POINTED_DRIPSTONE,
            Material.BUDDING_AMETHYST,
            Material.GRAVEL,
            Material.NETHER_QUARTZ_ORE,
            Material.NETHER_GOLD_ORE,
            Material.ANCIENT_DEBRIS,
            Material.SOUL_SAND,
            Material.SOUL_SOIL,
            Material.END_STONE
    );

    private final Predicate<Material> vanillaTagMatcher;

    public NaturalCeilingClassifier() {
        this(NaturalCeilingClassifier::matchesVanillaTag);
    }

    NaturalCeilingClassifier(Predicate<Material> vanillaTagMatcher) {
        this.vanillaTagMatcher = vanillaTagMatcher;
    }

    public boolean isNatural(Material material, NaturalCeilingSettings settings) {
        if (settings.excludedMaterials().contains(material)) {
            return false;
        }
        return settings.additionalMaterials().contains(material)
                || SUPPLEMENTAL_NATURAL_MATERIALS.contains(material)
                || vanillaTagMatcher.test(material);
    }

    private static boolean matchesVanillaTag(Material material) {
        return Tag.BASE_STONE_OVERWORLD.isTagged(material)
                || Tag.BASE_STONE_NETHER.isTagged(material)
                || Tag.COAL_ORES.isTagged(material)
                || Tag.COPPER_ORES.isTagged(material)
                || Tag.DIAMOND_ORES.isTagged(material)
                || Tag.EMERALD_ORES.isTagged(material)
                || Tag.GOLD_ORES.isTagged(material)
                || Tag.IRON_ORES.isTagged(material)
                || Tag.LAPIS_ORES.isTagged(material)
                || Tag.REDSTONE_ORES.isTagged(material);
    }
}
