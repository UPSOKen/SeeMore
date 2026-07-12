package com.froobworld.seemore.config;

import com.froobworld.seemore.SeeMore;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class SeeMoreConfig {
    private final SeeMore seeMore;
    private final File configFile;
    private volatile Snapshot snapshot;

    public SeeMoreConfig(SeeMore seeMore) {
        this.seeMore = seeMore;
        this.configFile = new File(seeMore.getDataFolder(), "config.yml");
    }

    public void load() throws Exception {
        copyDefaultConfigIfMissing();
        ConfigMigrator.migrate(configFile.toPath());

        YamlConfiguration yaml = new YamlConfiguration();
        yaml.load(configFile);
        Snapshot loaded = parseSnapshot(yaml);
        snapshot = loaded;
    }

    public int updateDelayTicks() {
        return requireSnapshot().updateDelayTicks();
    }

    public boolean logChanges() {
        return requireSnapshot().logChanges();
    }

    public int maximumViewDistance(World world) {
        Snapshot current = requireSnapshot();
        return current.defaultWorldSettings().maximumViewDistance(world.getName());
    }

    public ResolvedProfile resolveProfile(org.bukkit.entity.Player player) {
        Snapshot current = requireSnapshot();
        return PermissionProfileResolver.resolve(current.permissionProfiles(), player::hasPermission,
                current.defaultWorldSettings(), player.getWorld().getName());
    }

    public AfkSettings afkSettings() {
        return requireSnapshot().afkSettings();
    }

    public UndergroundSettings undergroundSettings() {
        return requireSnapshot().undergroundSettings();
    }

    public WorldSettings defaultWorldSettings() {
        return requireSnapshot().defaultWorldSettings();
    }

    public List<DistanceProfile> permissionProfiles() {
        return requireSnapshot().permissionProfiles();
    }

    static Snapshot parseSnapshot(YamlConfiguration yaml) {
        int version = yaml.getInt("version", -1);
        if (version != ConfigMigrator.CURRENT_VERSION) {
            throw new IllegalArgumentException("Unsupported config version " + version + ".");
        }

        int updateDelay = yaml.getInt("update-delay", 600);
        if (updateDelay < 0) {
            throw new IllegalArgumentException("update-delay must not be negative.");
        }
        ConfigurationSection worldSection = yaml.getConfigurationSection("world-settings");
        if (worldSection == null) {
            throw new IllegalArgumentException("Missing world-settings section.");
        }

        WorldSettings defaultWorldSettings = parseWorldSettings(worldSection, "world-settings");
        if (yaml.contains("permissions.groups")) {
            throw new IllegalArgumentException(
                    "permissions.groups has been renamed to permissions.group-overrides in config version 4.");
        }
        List<DistanceProfile> permissionProfiles = parsePermissionProfiles(
                yaml.getList("permissions.group-overrides", List.of()));

        int afkMaximum = yaml.getInt("afk.maximum-view-distance", 10);
        validateDistance("afk.maximum-view-distance", afkMaximum);
        if (afkMaximum < 0) {
            throw new IllegalArgumentException("afk.maximum-view-distance must be between 2 and 32.");
        }
        int minimumAfkReduction = yaml.getInt("afk.minimum-reduction", 3);
        if (minimumAfkReduction < 1 || minimumAfkReduction > 30) {
            throw new IllegalArgumentException("afk.minimum-reduction must be between 1 and 30.");
        }
        Duration afkCheckInterval = DurationParser.parse(yaml.getString("afk.check-interval", "1m"), "afk.check-interval");
        requireMinimumInterval(afkCheckInterval, "afk.check-interval");
        Duration afkTimeout = DurationParser.parse(yaml.getString("afk.timeout", "15m"), "afk.timeout");
        double minimumLookChange = yaml.getDouble("afk.wake-up.minimum-look-change", 2.0);
        int requiredLookEvents = yaml.getInt("afk.wake-up.required-look-events", 2);
        Duration lookEventWindow = DurationParser.parse(
                yaml.getString("afk.wake-up.look-event-window", "2s"), "afk.wake-up.look-event-window");
        if (minimumLookChange <= 0) {
            throw new IllegalArgumentException("afk.wake-up.minimum-look-change must be greater than zero.");
        }
        if (requiredLookEvents < 1) {
            throw new IllegalArgumentException("afk.wake-up.required-look-events must be at least 1.");
        }

        AfkSettings afkSettings = new AfkSettings(yaml.getBoolean("afk.enabled", true), afkCheckInterval,
                afkTimeout, afkMaximum, minimumAfkReduction, minimumLookChange, requiredLookEvents,
                lookEventWindow, yaml.getString("afk.alerts.reduced-message", ""),
                yaml.getString("afk.alerts.restoring-message", ""));

        WorldListMode worldListMode = WorldListMode.parse(
                yaml.getString("underground.world-list-mode", "whitelist"), "underground.world-list-mode");
        Set<String> undergroundWorlds = new HashSet<>();
        for (String worldName : yaml.getStringList("underground.worlds")) {
            if (worldName.isBlank()) {
                throw new IllegalArgumentException("underground.worlds must not contain blank world names.");
            }
            undergroundWorlds.add(normalize(worldName));
        }
        Duration undergroundCheckInterval = DurationParser.parse(
                yaml.getString("underground.check-interval", "5s"), "underground.check-interval");
        requireMinimumInterval(undergroundCheckInterval, "underground.check-interval");
        Duration enterAfter = DurationParser.parse(
                yaml.getString("underground.enter-after", "2m"), "underground.enter-after");
        Duration exitAfter = DurationParser.parse(
                yaml.getString("underground.exit-after", "5s"), "underground.exit-after");
        int minimumDepth = yaml.getInt("underground.minimum-depth", 10);
        if (minimumDepth < 1) {
            throw new IllegalArgumentException("underground.minimum-depth must be at least 1.");
        }
        int exitDepth = yaml.getInt("underground.exit-depth", 5);
        if (exitDepth < 1 || exitDepth > minimumDepth) {
            throw new IllegalArgumentException(
                    "underground.exit-depth must be between 1 and underground.minimum-depth.");
        }
        int undergroundMaximum = yaml.getInt("underground.maximum-view-distance", 8);
        validateDistance("underground.maximum-view-distance", undergroundMaximum);
        if (undergroundMaximum < 0) {
            throw new IllegalArgumentException(
                    "underground.maximum-view-distance must be between 2 and 32.");
        }
        int ceilingSearchDistance = yaml.getInt("underground.natural-ceiling.search-distance", 32);
        if (ceilingSearchDistance < 1 || ceilingSearchDistance > 128) {
            throw new IllegalArgumentException(
                    "underground.natural-ceiling.search-distance must be between 1 and 128.");
        }
        int minimumCeilingThickness = yaml.getInt("underground.natural-ceiling.minimum-thickness", 2);
        if (minimumCeilingThickness < 1 || minimumCeilingThickness > 16) {
            throw new IllegalArgumentException(
                    "underground.natural-ceiling.minimum-thickness must be between 1 and 16.");
        }
        Set<Material> additionalCeilingMaterials = parseMaterials(
                yaml, "underground.natural-ceiling.additional-materials");
        Set<Material> excludedCeilingMaterials = parseMaterials(
                yaml, "underground.natural-ceiling.excluded-materials");
        NaturalCeilingSettings naturalCeilingSettings = new NaturalCeilingSettings(
                yaml.getBoolean("underground.natural-ceiling.enabled", true),
                ceilingSearchDistance, minimumCeilingThickness,
                additionalCeilingMaterials, excludedCeilingMaterials);
        UndergroundSettings undergroundSettings = new UndergroundSettings(
                yaml.getBoolean("underground.enabled", false),
                yaml.getBoolean("underground.enable-bypass-permission", false),
                worldListMode, undergroundWorlds,
                undergroundCheckInterval, enterAfter, exitAfter, minimumDepth, exitDepth, undergroundMaximum,
                naturalCeilingSettings);
        return new Snapshot(updateDelay, yaml.getBoolean("log-changes", true), defaultWorldSettings,
                List.copyOf(permissionProfiles), afkSettings, undergroundSettings);
    }

    private static List<DistanceProfile> parsePermissionProfiles(List<?> rawProfiles) {
        List<DistanceProfile> profiles = new ArrayList<>();
        Set<String> names = new HashSet<>();
        for (int index = 0; index < rawProfiles.size(); index++) {
            Object rawProfile = rawProfiles.get(index);
            if (!(rawProfile instanceof Map<?, ?> profileMap)) {
                throw new IllegalArgumentException("permissions.group-overrides[" + index + "] must be a section.");
            }
            String path = "permissions.group-overrides[" + index + "]";
            String name = requiredString(profileMap, "name", path);
            String permission = requiredString(profileMap, "permission", path);
            if (!names.add(normalize(name))) {
                throw new IllegalArgumentException("Duplicate permission profile name: " + name);
            }
            Object rawWorldSettings = profileMap.get("world-settings");
            if (!(rawWorldSettings instanceof Map<?, ?> worldSettingsMap)) {
                throw new IllegalArgumentException(path + ".world-settings is required.");
            }
            profiles.add(new DistanceProfile(name, permission,
                    parseWorldOverrides(worldSettingsMap, path + ".world-settings")));
        }
        return profiles;
    }

    private static WorldSettings parseWorldSettings(ConfigurationSection section, String path) {
        Map<String, Integer> maximums = new LinkedHashMap<>();
        for (String worldName : section.getKeys(false)) {
            int maximum = section.getInt(worldName + ".maximum-view-distance", Integer.MIN_VALUE);
            validateDistance(path + "." + worldName + ".maximum-view-distance", maximum);
            maximums.put(normalize(worldName), maximum);
        }
        return createWorldSettings(maximums, path, true);
    }

    private static WorldSettings parseWorldOverrides(Map<?, ?> section, String path) {
        Map<String, Integer> maximums = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : section.entrySet()) {
            String worldName = String.valueOf(entry.getKey());
            if (!(entry.getValue() instanceof Map<?, ?> values)) {
                throw new IllegalArgumentException(path + "." + worldName + " must be a section.");
            }
            Object rawMaximum = values.get("maximum-view-distance");
            if (!(rawMaximum instanceof Number number)) {
                throw new IllegalArgumentException(path + "." + worldName + ".maximum-view-distance is required.");
            }
            int maximum = number.intValue();
            validateDistance(path + "." + worldName + ".maximum-view-distance", maximum);
            maximums.put(normalize(worldName), maximum);
        }
        return createWorldSettings(maximums, path, false);
    }

    private static WorldSettings createWorldSettings(Map<String, Integer> maximums, String path,
                                                     boolean defaultRequired) {
        Integer defaultMaximum = maximums.remove("default");
        if (defaultRequired && defaultMaximum == null) {
            throw new IllegalArgumentException(path + ".default.maximum-view-distance is required.");
        }
        return new WorldSettings(defaultMaximum, maximums);
    }

    private static String requiredString(Map<?, ?> values, String key, String path) {
        Object rawValue = values.get(key);
        if (!(rawValue instanceof String value) || value.isBlank()) {
            throw new IllegalArgumentException(path + "." + key + " is required.");
        }
        return value;
    }

    private static Set<Material> parseMaterials(YamlConfiguration yaml, String path) {
        Set<Material> materials = new HashSet<>();
        for (Object rawMaterial : yaml.getList(path, List.of())) {
            if (!(rawMaterial instanceof String materialName) || materialName.isBlank()) {
                throw new IllegalArgumentException(path + " must contain only material names.");
            }
            Material material;
            try {
                material = Material.valueOf(materialName.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException(path + " contains an unknown block material: " + materialName);
            }
            materials.add(material);
        }
        return Set.copyOf(materials);
    }

    private static void requireMinimumInterval(Duration interval, String path) {
        if (interval != null && interval.compareTo(Duration.ofSeconds(1)) < 0) {
            throw new IllegalArgumentException(path + " must be at least 1s.");
        }
    }

    static void validateDistance(String path, int distance) {
        if (distance != -1 && (distance < 2 || distance > 32)) {
            throw new IllegalArgumentException(path + " must be -1 or between 2 and 32.");
        }
    }

    static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT);
    }

    private Snapshot requireSnapshot() {
        Snapshot current = snapshot;
        if (current == null) {
            throw new IllegalStateException("Configuration has not been loaded.");
        }
        return current;
    }

    private void copyDefaultConfigIfMissing() throws IOException {
        if (configFile.exists()) {
            return;
        }
        Files.createDirectories(configFile.toPath().getParent());
        try (InputStream input = seeMore.getResource("config.yml")) {
            if (input == null) {
                throw new IOException("The bundled config.yml resource is missing.");
            }
            Files.copy(input, configFile.toPath());
        }
    }

    record Snapshot(int updateDelayTicks, boolean logChanges, WorldSettings defaultWorldSettings,
                    List<DistanceProfile> permissionProfiles,
                    AfkSettings afkSettings, UndergroundSettings undergroundSettings) {
    }
}
