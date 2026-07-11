package com.froobworld.seemore.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ConfigMigratorTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void migratesVersionTwoWithoutChangingExistingSettings() throws Exception {
        Path configFile = temporaryDirectory.resolve("config.yml");
        String originalConfig = """
                # Existing server configuration
                version: 2
                update-delay: 123
                log-changes: false
                world-settings:
                  default:
                    maximum-view-distance: 17
                  custom_world:
                    maximum-view-distance: 9
                """;
        Files.writeString(configFile, originalConfig);

        boolean migrated = ConfigMigrator.migrate(configFile);

        assertTrue(migrated);
        YamlConfiguration migratedConfig = YamlConfiguration.loadConfiguration(configFile.toFile());
        assertEquals(4, migratedConfig.getInt("version"));
        assertEquals(123, migratedConfig.getInt("update-delay"));
        assertFalse(migratedConfig.getBoolean("log-changes"));
        assertEquals(17, migratedConfig.getInt("world-settings.default.maximum-view-distance"));
        assertEquals(9, migratedConfig.getInt("world-settings.custom_world.maximum-view-distance"));
        assertEquals("30s", migratedConfig.getString("permissions.check-interval"));
        assertEquals("10s", migratedConfig.getString("afk.check-interval"));
        assertEquals("10m", migratedConfig.getString("afk.timeout"));
        assertEquals(8, migratedConfig.getInt("afk.maximum-view-distance"));
        assertDoesNotThrow(() -> SeeMoreConfig.parseSnapshot(migratedConfig));
        assertTrue(Files.exists(temporaryDirectory.resolve("config.yml.v2.bak")));
        assertEquals(originalConfig, Files.readString(temporaryDirectory.resolve("config.yml.v2.bak")));
    }

    @Test
    void migratesVersionOneWithTheUpstreamLoggingDefault() throws Exception {
        Path configFile = temporaryDirectory.resolve("config.yml");
        Files.writeString(configFile, """
                version: 1
                update-delay: 600
                world-settings:
                  default:
                    maximum-view-distance: -1
                """);

        ConfigMigrator.migrate(configFile);

        YamlConfiguration migratedConfig = YamlConfiguration.loadConfiguration(configFile.toFile());
        assertEquals(4, migratedConfig.getInt("version"));
        assertFalse(migratedConfig.getBoolean("log-changes"));
        assertTrue(Files.exists(temporaryDirectory.resolve("config.yml.v1.bak")));
    }

    @Test
    void migratesVersionThreeGroupsToGroupOverrides() throws Exception {
        Path configFile = temporaryDirectory.resolve("config.yml");
        String versionThreeConfig = """
                version: 3
                update-delay: 600
                world-settings:
                  default:
                    maximum-view-distance: 14
                permissions:
                  check-interval: 30s
                  groups:
                    - name: admin
                      permission: seemore.view-distance.admin
                      world-settings:
                        default:
                          maximum-view-distance: 20
                """;
        Files.writeString(configFile, versionThreeConfig);

        boolean migrated = ConfigMigrator.migrate(configFile);

        assertTrue(migrated);
        YamlConfiguration migratedConfig = YamlConfiguration.loadConfiguration(configFile.toFile());
        assertEquals(4, migratedConfig.getInt("version"));
        assertFalse(migratedConfig.contains("permissions.groups"));
        java.util.Map<?, ?> override = migratedConfig.getMapList("permissions.group-overrides").getFirst();
        assertEquals("admin", override.get("name"));
        java.util.Map<?, ?> worldSettings = (java.util.Map<?, ?>) override.get("world-settings");
        java.util.Map<?, ?> defaultSettings = (java.util.Map<?, ?>) worldSettings.get("default");
        assertEquals(20, defaultSettings.get("maximum-view-distance"));
        assertEquals(versionThreeConfig, Files.readString(temporaryDirectory.resolve("config.yml.v3.bak")));
        assertDoesNotThrow(() -> SeeMoreConfig.parseSnapshot(migratedConfig));
    }

    @Test
    void leavesVersionFourByteForByteUnchanged() throws Exception {
        Path configFile = temporaryDirectory.resolve("config.yml");
        String versionFourConfig = """
                version: 4
                update-delay: 600
                """;
        Files.writeString(configFile, versionFourConfig);

        boolean migrated = ConfigMigrator.migrate(configFile);

        assertFalse(migrated);
        assertEquals(versionFourConfig, Files.readString(configFile));
        assertFalse(Files.exists(temporaryDirectory.resolve("config.yml.v4.bak")));
    }

    @Test
    void renamesOnlyTheGroupsKeyInsidePermissions() throws Exception {
        Path configFile = temporaryDirectory.resolve("config.yml");
        Files.writeString(configFile, """
                version: 3
                world-settings:
                  default:
                    maximum-view-distance: 14
                  groups:
                    maximum-view-distance: 9
                permissions:
                  check-interval: 30s
                  groups: []
                """);

        ConfigMigrator.migrate(configFile);

        YamlConfiguration migratedConfig = YamlConfiguration.loadConfiguration(configFile.toFile());
        assertEquals(9, migratedConfig.getInt("world-settings.groups.maximum-view-distance"));
        assertTrue(migratedConfig.isList("permissions.group-overrides"));
        assertFalse(migratedConfig.contains("permissions.groups"));
    }
}
