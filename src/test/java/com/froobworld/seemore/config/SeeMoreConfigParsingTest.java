package com.froobworld.seemore.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SeeMoreConfigParsingTest {
    @Test
    void parsesOrderedProfilesAndAfkSettings() throws Exception {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.loadFromString("""
                version: 8
                update-delay: 600
                log-changes: true
                world-settings:
                  default:
                    maximum-view-distance: 12
                  world_nether:
                    maximum-view-distance: 8
                permissions:
                  group-overrides:
                    - name: admin
                      permission: seemore.view-distance.admin
                      world-settings:
                        default:
                          maximum-view-distance: -1
                    - name: donor
                      permission: seemore.view-distance.donor
                      world-settings:
                        default:
                          maximum-view-distance: 18
                afk:
                  enabled: true
                  check-interval: 1m
                  timeout: 15m
                  maximum-view-distance: 10
                  minimum-reduction: 3
                  alerts:
                    reduced-message: "<yellow>Reduced</yellow>"
                    restoring-message: "<green>Restoring</green>"
                  wake-up:
                    minimum-look-change: 2.0
                    required-look-events: 2
                    look-event-window: 2s
                underground:
                  enabled: true
                  enable-bypass-permission: true
                  world-list-mode: whitelist
                  worlds:
                    - world
                    - Mining_World
                  check-interval: 5s
                  enter-after: 2m
                  exit-after: 5s
                  minimum-depth: 10
                  exit-depth: 5
                  maximum-view-distance: 8
                  natural-ceiling:
                    enabled: true
                    search-distance: 32
                    minimum-thickness: 2
                    additional-materials:
                      - SCULK
                    excluded-materials:
                      - GRAVEL
                """);

        SeeMoreConfig.Snapshot snapshot = SeeMoreConfig.parseSnapshot(yaml);

        assertEquals("admin", snapshot.permissionProfiles().get(0).name());
        assertEquals("donor", snapshot.permissionProfiles().get(1).name());
        assertEquals(18, snapshot.permissionProfiles().get(1).worldSettings().defaultMaximumViewDistance());
        assertTrue(snapshot.afkSettings().enabled());
        assertEquals(Duration.ofMinutes(1), snapshot.afkSettings().checkInterval());
        assertEquals(Duration.ofMinutes(15), snapshot.afkSettings().timeout());
        assertEquals(10, snapshot.afkSettings().maximumViewDistance());
        assertEquals(3, snapshot.afkSettings().minimumReduction());
        assertEquals("<yellow>Reduced</yellow>", snapshot.afkSettings().reducedMessage());
        assertEquals("<green>Restoring</green>", snapshot.afkSettings().restoringMessage());
        assertTrue(snapshot.undergroundSettings().enabled());
        assertTrue(snapshot.undergroundSettings().bypassPermissionEnabled());
        assertEquals(WorldListMode.WHITELIST, snapshot.undergroundSettings().worldListMode());
        assertEquals(Set.of("world", "mining_world"), snapshot.undergroundSettings().worlds());
        assertEquals(Duration.ofSeconds(5), snapshot.undergroundSettings().checkInterval());
        assertEquals(Duration.ofMinutes(2), snapshot.undergroundSettings().enterAfter());
        assertEquals(Duration.ofSeconds(5), snapshot.undergroundSettings().exitAfter());
        assertEquals(10, snapshot.undergroundSettings().minimumDepth());
        assertEquals(5, snapshot.undergroundSettings().exitDepth());
        assertEquals(8, snapshot.undergroundSettings().maximumViewDistance());
        NaturalCeilingSettings ceiling = snapshot.undergroundSettings().naturalCeiling();
        assertTrue(ceiling.enabled());
        assertEquals(32, ceiling.searchDistance());
        assertEquals(2, ceiling.minimumThickness());
        assertEquals(Set.of(Material.SCULK), ceiling.additionalMaterials());
        assertEquals(Set.of(Material.GRAVEL), ceiling.excludedMaterials());
    }

    @Test
    void leavesAfkMessagesBlankWhenTheyAreNotConfigured() throws Exception {
        YamlConfiguration yaml = minimumConfig();

        SeeMoreConfig.Snapshot snapshot = SeeMoreConfig.parseSnapshot(yaml);

        assertEquals("", snapshot.afkSettings().reducedMessage());
        assertEquals("", snapshot.afkSettings().restoringMessage());
    }

    @Test
    void permitsAGroupOverrideWithoutADefault() throws Exception {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.loadFromString("""
                version: 8
                world-settings:
                  default:
                    maximum-view-distance: 14
                  world_nether:
                    maximum-view-distance: 12
                permissions:
                  group-overrides:
                    - name: donor
                      permission: seemore.view-distance.donor
                      world-settings:
                        resource_world:
                          maximum-view-distance: 18
                """);

        SeeMoreConfig.Snapshot snapshot = assertDoesNotThrow(() -> SeeMoreConfig.parseSnapshot(yaml));

        assertEquals(1, snapshot.permissionProfiles().size());
        assertEquals(18, snapshot.permissionProfiles().getFirst().worldSettings()
                .worldMaximums().get("resource_world"));
    }

    @Test
    void parsesAnUndergroundBlacklistCaseInsensitively() throws Exception {
        YamlConfiguration yaml = minimumConfig();
        yaml.set("underground.enabled", true);
        yaml.set("underground.world-list-mode", "BLACKLIST");
        yaml.set("underground.worlds", java.util.List.of("world_the_end"));

        SeeMoreConfig.Snapshot snapshot = SeeMoreConfig.parseSnapshot(yaml);

        assertEquals(WorldListMode.BLACKLIST, snapshot.undergroundSettings().worldListMode());
        assertTrue(snapshot.undergroundSettings().isWorldEnabled("world"));
        assertFalse(snapshot.undergroundSettings().isWorldEnabled("WORLD_THE_END"));
    }

    @Test
    void rejectsAnUnknownUndergroundWorldListMode() throws Exception {
        YamlConfiguration yaml = minimumConfig();
        yaml.set("underground.world-list-mode", "sometimes");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> SeeMoreConfig.parseSnapshot(yaml));

        assertTrue(exception.getMessage().contains("underground.world-list-mode"));
    }

    @Test
    void rejectsAnUnknownNaturalCeilingMaterial() throws Exception {
        YamlConfiguration yaml = minimumConfig();
        yaml.set("underground.natural-ceiling.additional-materials", java.util.List.of("NOT_A_BLOCK"));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> SeeMoreConfig.parseSnapshot(yaml));

        assertTrue(exception.getMessage().contains("underground.natural-ceiling.additional-materials"));
    }

    private static YamlConfiguration minimumConfig() throws Exception {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.loadFromString("""
                version: 8
                update-delay: 600
                log-changes: true
                world-settings:
                  default:
                    maximum-view-distance: -1
                permissions:
                  group-overrides: []
                afk:
                  enabled: true
                  check-interval: 1m
                  timeout: 15m
                  maximum-view-distance: 10
                  minimum-reduction: 3
                  alerts:
                    reduced-message: ""
                    restoring-message: ""
                  wake-up:
                    minimum-look-change: 2.0
                    required-look-events: 2
                    look-event-window: 2s
                underground:
                  enabled: false
                  enable-bypass-permission: false
                  world-list-mode: whitelist
                  worlds:
                    - world
                  check-interval: 5s
                  enter-after: 2m
                  exit-after: 5s
                  minimum-depth: 10
                  exit-depth: 5
                  maximum-view-distance: 8
                  natural-ceiling:
                    enabled: true
                    search-distance: 32
                    minimum-thickness: 2
                    additional-materials: []
                    excluded-materials: []
                """);
        return yaml;
    }
}
