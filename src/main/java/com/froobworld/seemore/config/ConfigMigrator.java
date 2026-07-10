package com.froobworld.seemore.config;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ConfigMigrator {
    public static final int CURRENT_VERSION = 3;
    private static final Pattern VERSION_LINE = Pattern.compile("(?m)^([ \\t]*version[ \\t]*:[ \\t]*)\\d+([ \\t]*(?:#.*)?)$");

    private ConfigMigrator() {
    }

    public static boolean migrate(Path configFile) throws Exception {
        YamlConfiguration existing = new YamlConfiguration();
        existing.load(configFile.toFile());
        int version = existing.getInt("version", -1);
        if (version == CURRENT_VERSION) {
            return false;
        }
        if (version < 1 || version > CURRENT_VERSION) {
            throw new IllegalArgumentException("Unsupported config version " + version + ".");
        }

        String original = Files.readString(configFile);
        Path backup = nextBackupPath(configFile, version);
        Files.copy(configFile, backup, StandardCopyOption.COPY_ATTRIBUTES);

        String lineSeparator = original.contains("\r\n") ? "\r\n" : "\n";
        String migrated = replaceVersion(original);
        StringBuilder additions = new StringBuilder();
        if (version == 1 && !existing.contains("log-changes")) {
            additions.append(lineSeparator)
                    .append("# Whether the plugin should log view distance changes.").append(lineSeparator)
                    .append("log-changes: false").append(lineSeparator);
        }
        if (!existing.contains("permissions")) {
            additions.append(lineSeparator).append(PERMISSION_DEFAULTS.replace("\n", lineSeparator));
        }
        if (!existing.contains("afk")) {
            additions.append(lineSeparator).append(AFK_DEFAULTS.replace("\n", lineSeparator));
        }
        if (!migrated.endsWith("\n") && !migrated.endsWith("\r")) {
            migrated += lineSeparator;
        }
        migrated += additions;

        Path temporaryFile = Files.createTempFile(configFile.getParent(), "config", ".migrating");
        try {
            Files.writeString(temporaryFile, migrated);
            YamlConfiguration validation = new YamlConfiguration();
            validation.load(temporaryFile.toFile());
            if (validation.getInt("version", -1) != CURRENT_VERSION) {
                throw new IllegalStateException("Migrated configuration has an invalid version.");
            }
            moveAtomically(temporaryFile, configFile);
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
        return true;
    }

    private static String replaceVersion(String config) {
        Matcher matcher = VERSION_LINE.matcher(config);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Could not find the config version line.");
        }
        return matcher.replaceFirst("$1" + CURRENT_VERSION + "$2");
    }

    private static Path nextBackupPath(Path configFile, int version) {
        Path candidate = configFile.resolveSibling(configFile.getFileName() + ".v" + version + ".bak");
        int suffix = 1;
        while (Files.exists(candidate)) {
            candidate = configFile.resolveSibling(configFile.getFileName() + ".v" + version + ".bak." + suffix++);
        }
        return candidate;
    }

    private static void moveAtomically(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static final String PERMISSION_DEFAULTS = """
            # Permission profiles are checked from top to bottom. The first matching permission wins.
            permissions:
              # Set to disabled to check only when another event recalculates view distance.
              check-interval: 30s
              groups: []
            """;

    private static final String AFK_DEFAULTS = """
            # Reduce the view distance of inactive players without treating passive position changes as activity.
            afk:
              enabled: true
              check-interval: 10s
              timeout: 10m
              maximum-view-distance: 8
              wake-up:
                minimum-look-change: 2.0
                required-look-events: 2
                look-event-window: 2s
            """;
}
