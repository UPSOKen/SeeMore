package com.froobworld.seemore.underground;

import com.froobworld.seemore.config.NaturalCeilingSettings;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class NaturalCeilingDetectorTest {
    private final NaturalCeilingClassifier classifier = new NaturalCeilingClassifier(
            material -> material == Material.STONE || material == Material.DEEPSLATE);

    @Test
    void ignoresPassableBlocksAndAcceptsAThickNaturalCeiling() {
        List<CeilingBlock> column = List.of(
                passable(Material.AIR),
                passable(Material.WATER),
                solid(Material.STONE),
                solid(Material.DEEPSLATE));

        assertTrue(NaturalCeilingDetector.hasNaturalCeiling(
                offset -> sample(column, offset), settings(32, 2), classifier));
    }

    @Test
    void rejectsAConstructedCeilingEvenWhenNaturalBlocksAreAboveIt() {
        List<CeilingBlock> column = List.of(
                passable(Material.AIR),
                solid(Material.GLASS),
                solid(Material.STONE),
                solid(Material.STONE));

        assertFalse(NaturalCeilingDetector.hasNaturalCeiling(
                offset -> sample(column, offset), settings(32, 2), classifier));
    }

    @Test
    void rejectsAThinNaturalRoof() {
        List<CeilingBlock> column = List.of(
                passable(Material.AIR),
                solid(Material.STONE),
                passable(Material.AIR));

        assertFalse(NaturalCeilingDetector.hasNaturalCeiling(
                offset -> sample(column, offset), settings(32, 2), classifier));
    }

    @Test
    void doesNotSearchPastTheConfiguredDistance() {
        List<CeilingBlock> column = java.util.stream.IntStream.range(0, 33)
                .mapToObj(index -> index < 32 ? passable(Material.AIR) : solid(Material.STONE))
                .toList();

        assertFalse(NaturalCeilingDetector.hasNaturalCeiling(
                offset -> sample(column, offset), settings(32, 1), classifier));
    }

    private static CeilingBlock sample(List<CeilingBlock> column, int offset) {
        int index = offset - 1;
        return index < column.size() ? column.get(index) : null;
    }

    private static CeilingBlock passable(Material material) {
        return new CeilingBlock(material, true);
    }

    private static CeilingBlock solid(Material material) {
        return new CeilingBlock(material, false);
    }

    private static NaturalCeilingSettings settings(int searchDistance, int thickness) {
        return new NaturalCeilingSettings(true, searchDistance, thickness, Set.of(), Set.of());
    }
}
