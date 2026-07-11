package com.froobworld.seemore.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SeeMoreConfigParsingTest {
    @Test
    void parsesOrderedProfilesAndAfkSettings() throws Exception {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.loadFromString("""
                version: 5
                update-delay: 600
                log-changes: true
                world-settings:
                  default:
                    maximum-view-distance: 12
                  world_nether:
                    maximum-view-distance: 8
                permissions:
                  check-interval: 30s
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
                  check-interval: 10s
                  timeout: 10m
                  maximum-view-distance: 8
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
                """);

        SeeMoreConfig.Snapshot snapshot = SeeMoreConfig.parseSnapshot(yaml);

        assertEquals(Duration.ofSeconds(30), snapshot.permissionCheckInterval());
        assertEquals("admin", snapshot.permissionProfiles().get(0).name());
        assertEquals("donor", snapshot.permissionProfiles().get(1).name());
        assertEquals(18, snapshot.permissionProfiles().get(1).worldSettings().defaultMaximumViewDistance());
        assertTrue(snapshot.afkSettings().enabled());
        assertEquals(Duration.ofSeconds(10), snapshot.afkSettings().checkInterval());
        assertEquals(Duration.ofMinutes(10), snapshot.afkSettings().timeout());
        assertEquals(8, snapshot.afkSettings().maximumViewDistance());
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
    }

    @Test
    void supportsDisablingPermissionPolling() throws Exception {
        YamlConfiguration yaml = minimumConfig("disabled");

        SeeMoreConfig.Snapshot snapshot = SeeMoreConfig.parseSnapshot(yaml);

        assertNull(snapshot.permissionCheckInterval());
    }

    @Test
    void permitsAGroupOverrideWithoutADefault() throws Exception {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.loadFromString("""
                version: 5
                world-settings:
                  default:
                    maximum-view-distance: 14
                  world_nether:
                    maximum-view-distance: 12
                permissions:
                  check-interval: 30s
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
        YamlConfiguration yaml = minimumConfig("30s");
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
        YamlConfiguration yaml = minimumConfig("30s");
        yaml.set("underground.world-list-mode", "sometimes");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> SeeMoreConfig.parseSnapshot(yaml));

        assertTrue(exception.getMessage().contains("underground.world-list-mode"));
    }

    private static YamlConfiguration minimumConfig(String permissionInterval) throws Exception {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.loadFromString("""
                version: 5
                update-delay: 600
                log-changes: true
                world-settings:
                  default:
                    maximum-view-distance: -1
                permissions:
                  check-interval: %s
                  group-overrides: []
                afk:
                  enabled: true
                  check-interval: 10s
                  timeout: 10m
                  maximum-view-distance: 8
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
                """.formatted(permissionInterval));
        return yaml;
    }
}
