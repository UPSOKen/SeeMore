package com.froobworld.seemore.command;

import com.froobworld.seemore.SeeMore;
import com.froobworld.seemore.config.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static net.kyori.adventure.text.Component.*;

public final class InfoCommand implements CommandExecutor, TabCompleter {
    private final SeeMore seeMore;

    public InfoCommand(SeeMore seeMore) {
        this.seeMore = seeMore;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length > 2) {
            sender.sendMessage(text("Usage: /" + label + " info [world]", NamedTextColor.RED));
            return true;
        }

        List<World> worlds;
        if (args.length == 2) {
            World world = findWorld(args[1]);
            if (world == null) {
                sender.sendMessage(text("That world is not loaded.", NamedTextColor.RED));
                return true;
            }
            worlds = List.of(world);
        } else {
            worlds = Bukkit.getWorlds();
        }

        sender.sendMessage(CommandFormat.header("SeeMore Server Information"));
        AfkSettings afk = seeMore.getSeeMoreConfig().afkSettings();
        Component afkState = afk.enabled()
                ? text("Enabled", NamedTextColor.GREEN)
                    .append(text(" (timeout ", NamedTextColor.GRAY))
                    .append(CommandFormat.value(formatDuration(afk.timeout())))
                    .append(text(", check ", NamedTextColor.GRAY))
                    .append(CommandFormat.value(formatDuration(afk.checkInterval())))
                    .append(text(", cap ", NamedTextColor.GRAY))
                    .append(CommandFormat.value(afk.maximumViewDistance()))
                    .append(text(", minimum reduction ", NamedTextColor.GRAY))
                    .append(CommandFormat.value(afk.minimumReduction()))
                    .append(text(")", NamedTextColor.GRAY))
                : text("Disabled", NamedTextColor.RED);
        sender.sendMessage(CommandFormat.line("AFK reduction", afkState));
        boolean alertsConfigured = !afk.reducedMessage().isBlank() || !afk.restoringMessage().isBlank();
        sender.sendMessage(CommandFormat.line("AFK alerts", alertsConfigured
                ? text("Configured", NamedTextColor.GREEN)
                : text("Silent", NamedTextColor.GRAY)));
        for (World world : worlds) {
            sender.sendMessage(empty());
            sendWorldInformation(sender, world);
        }
        return true;
    }

    private void sendWorldInformation(CommandSender sender, World world) {
        SeeMoreConfig config = seeMore.getSeeMoreConfig();
        WorldSettings defaults = config.defaultWorldSettings();
        sender.sendMessage(CommandFormat.header(world.getName()));
        sender.sendMessage(CommandFormat.line("Server view distance", CommandFormat.value(world.getViewDistance())));
        sender.sendMessage(CommandFormat.line("Server simulation distance",
                CommandFormat.value(world.getSimulationDistance())));

        ResolvedDistance defaultDistance = PermissionProfileResolver.resolveDefaultDistance(
                defaults, world.getName());
        sender.sendMessage(profileLine("default", defaultDistance, world.getViewDistance()));
        for (DistanceProfile profile : config.permissionProfiles()) {
            ResolvedDistance distance = PermissionProfileResolver.resolveDistance(
                    profile, defaults, world.getName());
            sender.sendMessage(profileLine(profile.name(), distance, world.getViewDistance()));
        }

        UndergroundSettings underground = config.undergroundSettings();
        Component bypass = underground.bypassPermissionEnabled()
                ? text("Enabled", NamedTextColor.GREEN)
                    .append(text(" (" + UndergroundSettings.BYPASS_PERMISSION + ")", NamedTextColor.DARK_GRAY))
                : text("Disabled", NamedTextColor.RED)
                    .append(text(" (permission grants are ignored)", NamedTextColor.DARK_GRAY));
        sender.sendMessage(CommandFormat.line("Underground bypass permission", bypass));
        if (!underground.enabled()) {
            sender.sendMessage(CommandFormat.line("Underground detection",
                    text("Disabled", NamedTextColor.RED)));
        } else if (!underground.isWorldEnabled(world.getName())) {
            sender.sendMessage(CommandFormat.line("Underground detection",
                    text("Excluded", NamedTextColor.RED)));
        } else {
            NaturalCeilingSettings ceiling = underground.naturalCeiling();
            Component state = text("Enabled", NamedTextColor.GREEN)
                    .append(text(" (cap ", NamedTextColor.GRAY))
                    .append(text(underground.maximumViewDistance(), NamedTextColor.YELLOW))
                    .append(text(")", NamedTextColor.GRAY));
            if (!world.hasSkyLight()) {
                String noSkyBehavior = ceiling.enabled()
                        ? " — no skylight; natural ceiling still required"
                        : " — no skylight; depth automatically qualifies";
                state = state.append(text(noSkyBehavior, NamedTextColor.DARK_GRAY));
            }
            sender.sendMessage(CommandFormat.line("Underground detection", state));
            sender.sendMessage(CommandFormat.line("  Entry",
                    CommandFormat.value(underground.minimumDepth() + " blocks for "
                            + formatDuration(underground.enterAfter()))));
            sender.sendMessage(CommandFormat.line("  Exit",
                    CommandFormat.value(underground.exitDepth() + " blocks for "
                            + formatDuration(underground.exitAfter()))));
            sender.sendMessage(CommandFormat.line("  Check interval",
                    CommandFormat.value(formatDuration(underground.checkInterval()))));
            Component ceilingStatus = ceiling.enabled()
                    ? text("Enabled", NamedTextColor.GREEN)
                        .append(text(" (search " + ceiling.searchDistance() + " blocks, thickness "
                                + ceiling.minimumThickness() + ")", NamedTextColor.DARK_GRAY))
                    : text("Disabled", NamedTextColor.RED)
                        .append(text(" (depth only)", NamedTextColor.DARK_GRAY));
            sender.sendMessage(CommandFormat.line("  Natural ceiling", ceilingStatus));
            if (!ceiling.additionalMaterials().isEmpty()) {
                sender.sendMessage(CommandFormat.line("    Additional materials",
                        CommandFormat.value(materialNames(ceiling.additionalMaterials()))));
            }
            if (!ceiling.excludedMaterials().isEmpty()) {
                sender.sendMessage(CommandFormat.line("    Excluded materials",
                        CommandFormat.value(materialNames(ceiling.excludedMaterials()))));
            }
        }
    }

    private static Component profileLine(String profileName, ResolvedDistance distance, int serverViewDistance) {
        return text("  " + profileName + ": ", NamedTextColor.GRAY)
                .append(CommandFormat.configuredDistance(distance.maximumViewDistance(), serverViewDistance))
                .append(text(" (" + distance.source().description() + ")", NamedTextColor.DARK_GRAY));
    }

    private static World findWorld(String name) {
        return Bukkit.getWorlds().stream()
                .filter(world -> world.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    private static String formatDuration(Duration duration) {
        long seconds = duration.toSeconds();
        if (seconds % 60 == 0) {
            return seconds / 60 + "m";
        }
        return seconds + "s";
    }

    private static String materialNames(Set<Material> materials) {
        return materials.stream().map(Enum::name).sorted()
                .collect(Collectors.joining(", "));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String label, @NotNull String[] args) {
        if (args.length != 2) {
            return List.of();
        }
        List<String> worldNames = Bukkit.getWorlds().stream().map(World::getName).toList();
        return StringUtil.copyPartialMatches(args[1], worldNames, new ArrayList<>());
    }
}
