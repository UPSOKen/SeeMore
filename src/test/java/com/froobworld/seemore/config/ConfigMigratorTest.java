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
        assertEquals(8, migratedConfig.getInt("version"));
        assertFalse(migratedConfig.contains("chunk-refresh-system"));
        assertEquals(123, migratedConfig.getInt("update-delay"));
        assertFalse(migratedConfig.getBoolean("log-changes"));
        assertEquals(17, migratedConfig.getInt("world-settings.default.maximum-view-distance"));
        assertEquals(9, migratedConfig.getInt("world-settings.custom_world.maximum-view-distance"));
        assertFalse(migratedConfig.contains("permissions.check-interval"));
        assertEquals("1m", migratedConfig.getString("afk.check-interval"));
        assertEquals("15m", migratedConfig.getString("afk.timeout"));
        assertEquals(10, migratedConfig.getInt("afk.maximum-view-distance"));
        assertEquals(3, migratedConfig.getInt("afk.minimum-reduction"));
        assertEquals("", migratedConfig.getString("afk.alerts.reduced-message"));
        assertEquals("", migratedConfig.getString("afk.alerts.restoring-message"));
        assertFalse(migratedConfig.getBoolean("underground.enabled"));
        assertFalse(migratedConfig.getBoolean("underground.enable-bypass-permission"));
        assertEquals("whitelist", migratedConfig.getString("underground.world-list-mode"));
        assertEquals(java.util.List.of("world"), migratedConfig.getStringList("underground.worlds"));
        assertEquals("5s", migratedConfig.getString("underground.check-interval"));
        assertEquals("2m", migratedConfig.getString("underground.enter-after"));
        assertEquals("5s", migratedConfig.getString("underground.exit-after"));
        assertEquals(10, migratedConfig.getInt("underground.minimum-depth"));
        assertEquals(5, migratedConfig.getInt("underground.exit-depth"));
        assertEquals(8, migratedConfig.getInt("underground.maximum-view-distance"));
        assertTrue(migratedConfig.getBoolean("underground.natural-ceiling.enabled"));
        assertEquals(32, migratedConfig.getInt("underground.natural-ceiling.search-distance"));
        assertEquals(2, migratedConfig.getInt("underground.natural-ceiling.minimum-thickness"));
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
        assertEquals(8, migratedConfig.getInt("version"));
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
        assertEquals(8, migratedConfig.getInt("version"));
        assertFalse(migratedConfig.contains("permissions.groups"));
        assertFalse(migratedConfig.contains("permissions.check-interval"));
        java.util.Map<?, ?> override = migratedConfig.getMapList("permissions.group-overrides").getFirst();
        assertEquals("admin", override.get("name"));
        java.util.Map<?, ?> worldSettings = (java.util.Map<?, ?>) override.get("world-settings");
        java.util.Map<?, ?> defaultSettings = (java.util.Map<?, ?>) worldSettings.get("default");
        assertEquals(20, defaultSettings.get("maximum-view-distance"));
        assertEquals(versionThreeConfig, Files.readString(temporaryDirectory.resolve("config.yml.v3.bak")));
        assertDoesNotThrow(() -> SeeMoreConfig.parseSnapshot(migratedConfig));
    }

    @Test
    void migratesVersionFourByAddingUndergroundDefaults() throws Exception {
        Path configFile = temporaryDirectory.resolve("config.yml");
        String versionFourConfig = """
                version: 4
                update-delay: 600
                """;
        Files.writeString(configFile, versionFourConfig);

        boolean migrated = ConfigMigrator.migrate(configFile);

        assertTrue(migrated);
        YamlConfiguration migratedConfig = YamlConfiguration.loadConfiguration(configFile.toFile());
        assertEquals(8, migratedConfig.getInt("version"));
        assertFalse(migratedConfig.getBoolean("underground.enabled"));
        assertTrue(Files.exists(temporaryDirectory.resolve("config.yml.v4.bak")));
        assertEquals(versionFourConfig, Files.readString(temporaryDirectory.resolve("config.yml.v4.bak")));
    }

    @Test
    void migratesVersionFiveWithoutReplacingExistingUndergroundSettings() throws Exception {
        Path configFile = temporaryDirectory.resolve("config.yml");
        String versionFiveConfig = """
                version: 5
                update-delay: 600
                underground:
                  enabled: true
                  enable-bypass-permission: true
                  maximum-view-distance: 6
                """;
        Files.writeString(configFile, versionFiveConfig);

        boolean migrated = ConfigMigrator.migrate(configFile);

        assertTrue(migrated);
        YamlConfiguration migratedConfig = YamlConfiguration.loadConfiguration(configFile.toFile());
        assertEquals(8, migratedConfig.getInt("version"));
        assertTrue(migratedConfig.getBoolean("underground.enabled"));
        assertTrue(migratedConfig.getBoolean("underground.enable-bypass-permission"));
        assertEquals(6, migratedConfig.getInt("underground.maximum-view-distance"));
        assertTrue(migratedConfig.getBoolean("underground.natural-ceiling.enabled"));
        assertEquals(32, migratedConfig.getInt("underground.natural-ceiling.search-distance"));
        assertEquals(2, migratedConfig.getInt("underground.natural-ceiling.minimum-thickness"));
        assertEquals(versionFiveConfig, Files.readString(temporaryDirectory.resolve("config.yml.v5.bak")));
    }

    @Test
    void migratesVersionSixToTheConservativeAfkConfiguration() throws Exception {
        Path configFile = temporaryDirectory.resolve("config.yml");
        String versionSixConfig = """
                version: 6
                update-delay: 600
                """;
        Files.writeString(configFile, versionSixConfig);

        boolean migrated = ConfigMigrator.migrate(configFile);

        assertTrue(migrated);
        YamlConfiguration migratedConfig = YamlConfiguration.loadConfiguration(configFile.toFile());
        assertEquals(8, migratedConfig.getInt("version"));
        assertFalse(migratedConfig.contains("chunk-refresh-system"));
        assertEquals(3, migratedConfig.getInt("afk.minimum-reduction"));
        assertEquals("", migratedConfig.getString("afk.alerts.reduced-message"));
        assertEquals(versionSixConfig, Files.readString(temporaryDirectory.resolve("config.yml.v6.bak")));
    }

    @Test
    void migratesVersionSevenByRemovingExperimentalAndPollingSettings() throws Exception {
        Path configFile = temporaryDirectory.resolve("config.yml");
        String versionSevenConfig = """
                version: 7
                update-delay: 600
                chunk-refresh-system: old
                permissions:
                  check-interval: 30s
                  group-overrides: []
                afk:
                  enabled: true
                  check-interval: 10s
                  timeout: 10m
                  maximum-view-distance: 8
                """;
        Files.writeString(configFile, versionSevenConfig);

        boolean migrated = ConfigMigrator.migrate(configFile);

        assertTrue(migrated);
        YamlConfiguration migratedConfig = YamlConfiguration.loadConfiguration(configFile.toFile());
        assertEquals(8, migratedConfig.getInt("version"));
        assertFalse(migratedConfig.contains("chunk-refresh-system"));
        assertFalse(migratedConfig.contains("permissions.check-interval"));
        assertEquals("10s", migratedConfig.getString("afk.check-interval"));
        assertEquals("10m", migratedConfig.getString("afk.timeout"));
        assertEquals(8, migratedConfig.getInt("afk.maximum-view-distance"));
        assertEquals(3, migratedConfig.getInt("afk.minimum-reduction"));
        assertEquals("", migratedConfig.getString("afk.alerts.reduced-message"));
        assertEquals("", migratedConfig.getString("afk.alerts.restoring-message"));
        assertEquals(versionSevenConfig, Files.readString(temporaryDirectory.resolve("config.yml.v7.bak")));
    }

    @Test
    void leavesVersionEightByteForByteUnchanged() throws Exception {
        Path configFile = temporaryDirectory.resolve("config.yml");
        String versionEightConfig = """
                version: 8
                update-delay: 600
                """;
        Files.writeString(configFile, versionEightConfig);

        boolean migrated = ConfigMigrator.migrate(configFile);

        assertFalse(migrated);
        assertEquals(versionEightConfig, Files.readString(configFile));
        assertFalse(Files.exists(temporaryDirectory.resolve("config.yml.v8.bak")));
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
