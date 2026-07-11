package com.froobworld.seemore.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class SeeMoreConfigParsingTest {
    @Test
    void parsesOrderedProfilesAndAfkSettings() throws Exception {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.loadFromString("""
                version: 4
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
                version: 4
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

    private static YamlConfiguration minimumConfig(String permissionInterval) throws Exception {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.loadFromString("""
                version: 4
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
                """.formatted(permissionInterval));
        return yaml;
    }
}
