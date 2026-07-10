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
        assertEquals(3, migratedConfig.getInt("version"));
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
        assertEquals(3, migratedConfig.getInt("version"));
        assertFalse(migratedConfig.getBoolean("log-changes"));
        assertTrue(Files.exists(temporaryDirectory.resolve("config.yml.v1.bak")));
    }

    @Test
    void leavesVersionThreeByteForByteUnchanged() throws Exception {
        Path configFile = temporaryDirectory.resolve("config.yml");
        String versionThreeConfig = """
                version: 3
                update-delay: 600
                """;
        Files.writeString(configFile, versionThreeConfig);

        boolean migrated = ConfigMigrator.migrate(configFile);

        assertFalse(migrated);
        assertEquals(versionThreeConfig, Files.readString(configFile));
        assertFalse(Files.exists(temporaryDirectory.resolve("config.yml.v3.bak")));
    }
}
