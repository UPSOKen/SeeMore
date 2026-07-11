package com.froobworld.seemore.command;

import com.froobworld.seemore.SeeMore;
import com.froobworld.seemore.controller.DistanceMode;
import com.froobworld.seemore.controller.PlayerDistanceStatus;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static net.kyori.adventure.text.Component.*;

public final class StatusCommand implements CommandExecutor, TabCompleter {
    private static final Component NO_OTHERS_PERMISSION = text(
            "You don't have permission to inspect another player.", NamedTextColor.RED);
    private final SeeMore seeMore;

    public StatusCommand(SeeMore seeMore) {
        this.seeMore = seeMore;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length > 2) {
            sender.sendMessage(text("Usage: /" + label + " status [player]", NamedTextColor.RED));
            return true;
        }

        Player target;
        if (args.length == 1) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(text("Console must specify an online player.", NamedTextColor.RED));
                return true;
            }
            target = player;
        } else {
            target = findPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(text("That player is not online.", NamedTextColor.RED));
                return true;
            }
            if (!(sender instanceof Player player && player.getUniqueId().equals(target.getUniqueId()))
                    && !sender.hasPermission("seemore.command.status.others")) {
                sender.sendMessage(NO_OTHERS_PERMISSION);
                return true;
            }
        }

        seeMore.getSchedulerHook().runEntityTaskAsap(() -> {
            PlayerDistanceStatus status = seeMore.getViewDistanceController().captureStatus(target);
            deliver(sender, render(status));
        }, () -> deliver(sender, List.of(text("That player is no longer online.", NamedTextColor.RED))), target);
        return true;
    }

    private void deliver(CommandSender sender, List<Component> messages) {
        Runnable delivery = () -> messages.forEach(sender::sendMessage);
        if (sender instanceof Player player) {
            seeMore.getSchedulerHook().runEntityTaskAsap(delivery, null, player);
        } else {
            seeMore.getSchedulerHook().runTask(delivery);
        }
    }

    private static List<Component> render(PlayerDistanceStatus status) {
        List<Component> messages = new ArrayList<>();
        messages.add(CommandFormat.header("SeeMore Status — " + status.playerName()));
        messages.add(empty());
        messages.add(CommandFormat.line("World", CommandFormat.value(status.worldName())));
        messages.add(CommandFormat.line("Profile", CommandFormat.value(status.profileName())));
        messages.add(CommandFormat.line("Current mode", mode(status.mode())));
        messages.add(CommandFormat.line("Underground bypass", bypassStatus(status)));
        messages.add(CommandFormat.line("Client requested distance",
                CommandFormat.value(status.clientViewDistance())));
        messages.add(CommandFormat.line("Configured profile cap",
                CommandFormat.configuredDistance(status.configuredMaximum(), status.worldViewDistance())));
        messages.add(CommandFormat.line("Limiting cap", limitingCap(status)));
        messages.add(empty());
        messages.add(CommandFormat.line("Target view distance", CommandFormat.value(status.targetViewDistance())));
        messages.add(CommandFormat.line("Target send distance", CommandFormat.value(status.targetSendDistance())));
        messages.add(CommandFormat.line("Live view distance", CommandFormat.value(status.liveViewDistance())));
        messages.add(CommandFormat.line("Live send distance", CommandFormat.value(status.liveSendDistance())));
        messages.add(CommandFormat.line("Live simulation distance",
                CommandFormat.value(status.simulationDistance())));
        return messages;
    }

    private static Component mode(DistanceMode mode) {
        return switch (mode) {
            case NORMAL -> text("NORMAL", NamedTextColor.GREEN);
            case UNDERGROUND -> text("UNDERGROUND", NamedTextColor.YELLOW);
            case AFK -> text("AFK", NamedTextColor.RED);
        };
    }

    private static Component limitingCap(PlayerDistanceStatus status) {
        int profileMaximum = status.configuredMaximum() < 0
                ? status.worldViewDistance() : status.configuredMaximum();
        return switch (status.mode()) {
            case NORMAL -> CommandFormat.configuredDistance(
                    status.configuredMaximum(), status.worldViewDistance());
            case UNDERGROUND -> text(Math.min(profileMaximum, status.undergroundMaximum()),
                    NamedTextColor.YELLOW)
                    .append(text(" (underground ceiling " + status.undergroundMaximum() + ")",
                            NamedTextColor.DARK_GRAY));
            case AFK -> text(Math.min(profileMaximum, status.afkMaximum()), NamedTextColor.RED)
                    .append(text(" (AFK ceiling " + status.afkMaximum() + ")", NamedTextColor.DARK_GRAY));
        };
    }

    private static Component bypassStatus(PlayerDistanceStatus status) {
        if (!status.undergroundBypassPermissionEnabled()) {
            return text("DISABLED", NamedTextColor.RED);
        }
        if (status.undergroundBypassPermissionGranted()) {
            return text("ACTIVE", NamedTextColor.GREEN);
        }
        return text("NOT GRANTED", NamedTextColor.GRAY);
    }

    private static Player findPlayer(String name) {
        return Bukkit.getOnlinePlayers().stream()
                .filter(player -> player.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String label, @NotNull String[] args) {
        if (args.length != 2 || !sender.hasPermission("seemore.command.status.others")) {
            return List.of();
        }
        List<String> playerNames = Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        return StringUtil.copyPartialMatches(args[1], playerNames, new ArrayList<>());
    }
}
