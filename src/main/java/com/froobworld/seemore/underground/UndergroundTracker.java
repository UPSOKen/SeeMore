package com.froobworld.seemore.underground;

import com.froobworld.seemore.SeeMore;
import com.froobworld.seemore.config.DurationParser;
import com.froobworld.seemore.config.NaturalCeilingSettings;
import com.froobworld.seemore.config.UndergroundSettings;
import com.froobworld.seemore.controller.ViewDistanceController;
import com.froobworld.seemore.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class UndergroundTracker implements Listener {
    private final SeeMore seeMore;
    private final ViewDistanceController controller;
    private final Map<UUID, TrackedPlayer> trackedPlayers = new ConcurrentHashMap<>();
    private final NaturalCeilingClassifier ceilingClassifier = new NaturalCeilingClassifier();
    private ScheduledTask checkTask;

    public UndergroundTracker(SeeMore seeMore, ViewDistanceController controller) {
        this.seeMore = seeMore;
        this.controller = controller;
        for (Player player : Bukkit.getOnlinePlayers()) {
            trackedPlayers.put(player.getUniqueId(), new TrackedPlayer(player, new UndergroundState()));
        }
        Bukkit.getPluginManager().registerEvents(this, seeMore);
        scheduleChecks();
    }

    public void reloadConfiguration() {
        for (TrackedPlayer trackedPlayer : trackedPlayers.values()) {
            seeMore.getSchedulerHook().runEntityTaskAsap(
                    () -> reset(trackedPlayer),
                    () -> trackedPlayers.remove(trackedPlayer.player().getUniqueId()),
                    trackedPlayer.player());
        }
        scheduleChecks();
    }

    public void onAfkChanged(Player player, boolean afk) {
        if (!afk) {
            return;
        }
        TrackedPlayer trackedPlayer = trackedPlayers.computeIfAbsent(player.getUniqueId(),
                ignored -> new TrackedPlayer(player, new UndergroundState()));
        reset(trackedPlayer);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onJoin(PlayerJoinEvent event) {
        trackedPlayers.put(event.getPlayer().getUniqueId(),
                new TrackedPlayer(event.getPlayer(), new UndergroundState()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onQuit(PlayerQuitEvent event) {
        trackedPlayers.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onTeleport(PlayerTeleportEvent event) {
        TrackedPlayer trackedPlayer = trackedPlayers.computeIfAbsent(event.getPlayer().getUniqueId(),
                ignored -> new TrackedPlayer(event.getPlayer(), new UndergroundState()));
        reset(trackedPlayer);
    }

    private void checkPlayers() {
        UndergroundSettings settings = seeMore.getSeeMoreConfig().undergroundSettings();
        for (Map.Entry<UUID, TrackedPlayer> entry : trackedPlayers.entrySet()) {
            if (!controller.shouldRunNormalChecks(entry.getKey())) {
                continue;
            }
            TrackedPlayer trackedPlayer = entry.getValue();
            seeMore.getSchedulerHook().runEntityTaskAsap(
                    () -> checkPlayer(trackedPlayer, settings),
                    () -> trackedPlayers.remove(trackedPlayer.player().getUniqueId()),
                    trackedPlayer.player());
        }
    }

    private void checkPlayer(TrackedPlayer trackedPlayer, UndergroundSettings settings) {
        Player player = trackedPlayer.player();
        boolean eligible = UndergroundDetector.isEligible(
                settings.enabled(),
                settings.isWorldEnabled(player.getWorld().getName()),
                controller.isAfk(player),
                UndergroundDetector.isBypassed(settings.bypassPermissionEnabled(),
                        controller.isUndergroundBypassGranted(player)));
        if (!eligible) {
            reset(trackedPlayer);
            return;
        }

        Location eyeLocation = player.getEyeLocation();
        World world = player.getWorld();
        int surfaceY = world.getHighestBlockAt(eyeLocation, HeightMap.MOTION_BLOCKING_NO_LEAVES).getY();
        int requiredDepth = UndergroundDetector.requiredDepth(
                trackedPlayer.state().isUnderground(), settings.minimumDepth(), settings.exitDepth());
        boolean depthCandidate = UndergroundDetector.isCandidate(
                world.hasSkyLight(), surfaceY, eyeLocation.getBlockY(), requiredDepth);
        NaturalCeilingSettings ceilingSettings = settings.naturalCeiling();
        boolean naturalCeiling = false;
        if (depthCandidate && ceilingSettings.enabled()) {
            naturalCeiling = NaturalCeilingDetector.hasNaturalCeiling(
                    offset -> ceilingBlock(world, eyeLocation, offset),
                    ceilingSettings,
                    ceilingClassifier);
        }
        boolean candidate = UndergroundDetector.hasRequiredEvidence(
                depthCandidate, ceilingSettings.enabled(), naturalCeiling);
        UndergroundState.Transition transition = trackedPlayer.state().update(
                candidate, System.nanoTime(), settings.enterAfter(), settings.exitAfter());
        if (transition == UndergroundState.Transition.BECAME_UNDERGROUND) {
            controller.setUnderground(player, true);
        } else if (transition == UndergroundState.Transition.BECAME_SURFACE) {
            controller.setUnderground(player, false);
        }
    }

    private void reset(TrackedPlayer trackedPlayer) {
        UndergroundState.Transition transition = trackedPlayer.state().reset();
        if (transition == UndergroundState.Transition.BECAME_SURFACE
                || controller.isUnderground(trackedPlayer.player())) {
            controller.setUnderground(trackedPlayer.player(), false);
        }
    }

    private static CeilingBlock ceilingBlock(World world, Location eyeLocation, int offset) {
        int y = eyeLocation.getBlockY() + offset;
        if (y < world.getMinHeight() || y >= world.getMaxHeight()) {
            return null;
        }
        org.bukkit.block.Block block = world.getBlockAt(
                eyeLocation.getBlockX(), y, eyeLocation.getBlockZ());
        return new CeilingBlock(block.getType(), block.isPassable());
    }

    private void scheduleChecks() {
        if (checkTask != null) {
            checkTask.cancel();
            checkTask = null;
        }
        UndergroundSettings settings = seeMore.getSeeMoreConfig().undergroundSettings();
        if (!settings.enabled()) {
            return;
        }
        long ticks = DurationParser.toTicks(settings.checkInterval());
        checkTask = seeMore.getSchedulerHook().runRepeatingTask(this::checkPlayers, ticks, ticks);
    }

    private record TrackedPlayer(Player player, UndergroundState state) {
    }
}
