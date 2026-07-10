package com.froobworld.seemore.controller;

import com.froobworld.seemore.SeeMore;
import com.froobworld.seemore.config.DurationParser;
import com.froobworld.seemore.config.ResolvedProfile;
import com.froobworld.seemore.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public class ViewDistanceController {
    private static final int MAX_UPDATE_ATTEMPTS = 10;
    private final SeeMore seeMore;
    private final Map<UUID, Integer> targetViewDistanceMap = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> targetSendDistanceMap = new ConcurrentHashMap<>();
    private final Map<UUID, ScheduledTask> viewDistanceUpdateTasks = new ConcurrentHashMap<>();
    private final Map<UUID, ScheduledTask> sendDistanceUpdateTasks = new ConcurrentHashMap<>();
    private final Map<UUID, String> selectedProfileNames = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> simulationDistances = new ConcurrentHashMap<>();
    private final Set<UUID> afkPlayers = ConcurrentHashMap.newKeySet();
    private final ViewDistanceUpdateLogger viewDistanceUpdateLogger;
    private ScheduledTask permissionCheckTask;

    public ViewDistanceController(SeeMore seeMore) {
        this.seeMore = seeMore;
        this.viewDistanceUpdateLogger = new ViewDistanceUpdateLogger(seeMore);
        seeMore.getSchedulerHook().runRepeatingTask(this::cleanMaps, 1200, 1200);
        Bukkit.getPluginManager().registerEvents(new ViewDistanceUpdater(this), seeMore);
        schedulePermissionChecks();
    }

    public void updateAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            seeMore.getSchedulerHook().runEntityTaskAsap(() -> {
                setTargetViewDistance(player, player.getClientViewDistance(), false, true);
            }, null, player);
        }
    }

    public void setTargetViewDistance(Player player, int clientViewDistance, boolean testDelay, boolean initialUpdate) {
        UUID playerId = player.getUniqueId();
        ResolvedProfile profile = seeMore.getSeeMoreConfig().resolveProfile(player);
        int simulationDistance = player.getSimulationDistance();
        ViewDistancePolicy.Result result = ViewDistancePolicy.calculate(
                clientViewDistance,
                profile.maximumViewDistance(),
                player.getWorld().getViewDistance(),
                simulationDistance,
                afkPlayers.contains(playerId),
                seeMore.getSeeMoreConfig().afkSettings().maximumViewDistance()
        );

        selectedProfileNames.put(playerId, profile.name());
        simulationDistances.put(playerId, simulationDistance);
        targetViewDistanceMap.put(playerId, result.viewDistance());
        targetSendDistanceMap.put(playerId, result.sendDistance());

        // Update the view distance with a delay if it is being lowered
        long delay = 0;
        try {
            if (testDelay && player.getViewDistance() > targetViewDistanceMap.get(player.getUniqueId())) {
                delay = seeMore.getSeeMoreConfig().updateDelayTicks();
            }
        } catch (Exception ignored) {}

        updateSendDistance(player);
        updateViewDistance(player, delay, clientViewDistance, initialUpdate);
    }

    public void setAfk(Player player, boolean afk) {
        boolean changed = afk ? afkPlayers.add(player.getUniqueId()) : afkPlayers.remove(player.getUniqueId());
        if (changed) {
            setTargetViewDistance(player, player.getClientViewDistance(), false, false);
        }
    }

    public boolean isAfk(Player player) {
        return afkPlayers.contains(player.getUniqueId());
    }

    public void refreshCachedState(Player player) {
        ResolvedProfile profile = seeMore.getSeeMoreConfig().resolveProfile(player);
        String cachedProfile = selectedProfileNames.get(player.getUniqueId());
        int simulationDistance = player.getSimulationDistance();
        Integer cachedSimulationDistance = simulationDistances.get(player.getUniqueId());
        if (!Objects.equals(profile.name(), cachedProfile)
                || !Objects.equals(simulationDistance, cachedSimulationDistance)) {
            setTargetViewDistance(player, player.getClientViewDistance(), false, false);
        }
    }

    public void reloadConfiguration() {
        schedulePermissionChecks();
        updateAllPlayers();
    }

    public void removePlayer(Player player) {
        UUID playerId = player.getUniqueId();
        selectedProfileNames.remove(playerId);
        simulationDistances.remove(playerId);
        afkPlayers.remove(playerId);
        targetSendDistanceMap.remove(playerId);
        targetViewDistanceMap.remove(playerId);
        cancelAndRemove(sendDistanceUpdateTasks, playerId);
        cancelAndRemove(viewDistanceUpdateTasks, playerId);
    }

    private void updateSendDistance(Player player) {
        updateDistance(player, 0, 0, targetSendDistanceMap, sendDistanceUpdateTasks, Player::setSendViewDistance);
    }

    private void updateViewDistance(Player player, long delay, int clientViewDistance, boolean initialUpdate) {
        updateDistance(player, delay, 0, targetViewDistanceMap, viewDistanceUpdateTasks, (p, viewDistance) -> {
            if (p.getViewDistance() != viewDistance || initialUpdate) { // always update if we've not seen them before
                p.setViewDistance(viewDistance);
                if (seeMore.getSeeMoreConfig().logChanges()) {
                    viewDistanceUpdateLogger.logUpdate(player, String.format("Set view distance of %s to %s (client view distance is %s).", p.getName(), viewDistance, clientViewDistance));
                }
            }
        });
    }

    private void updateDistance(Player player, long delay, int attempts, Map<UUID, Integer> distanceMap, Map<UUID, ScheduledTask> taskMap, BiConsumer<Player, Integer> distanceConsumer) {
        if (attempts >= MAX_UPDATE_ATTEMPTS) {
            return; // give up if attempted too many times
        }
        Integer distance = distanceMap.get(player.getUniqueId());
        if (distance == null) {
            return; // might be null if the player has left
        }
        taskMap.compute(player.getUniqueId(), (uuid, task) -> {
            if (task != null) {
                task.cancel(); // cancel the previous task in case it is still running
            }
            if (delay > 0) {
                return seeMore.getSchedulerHook().runTaskDelayed(() -> updateDistance(player, 0, attempts, distanceMap, taskMap, distanceConsumer), delay);
            }
            CompletableFuture<ScheduledTask> retryTask = new CompletableFuture<>();
            ScheduledTask updateTask = seeMore.getSchedulerHook().runEntityTaskAsap(() -> {
                try {
                    distanceConsumer.accept(player, distance);
                } catch (Throwable ex) {

                    // will sometimes fail if the player is not attached to a world yet, so retry after 20 ticks
                    retryTask.complete(seeMore.getSchedulerHook().runTask(() -> updateDistance(player, 20, attempts + 1, distanceMap, taskMap, distanceConsumer)));
                }
            }, null, player);
            return retryTask.getNow(updateTask);
        });
    }

    private void cleanMaps() {
        sendDistanceUpdateTasks.entrySet().removeIf(entry -> Bukkit.getPlayer(entry.getKey()) == null);
        viewDistanceUpdateTasks.entrySet().removeIf(entry -> Bukkit.getPlayer(entry.getKey()) == null);
        targetSendDistanceMap.entrySet().removeIf(entry -> Bukkit.getPlayer(entry.getKey()) == null);
        targetViewDistanceMap.entrySet().removeIf(entry -> Bukkit.getPlayer(entry.getKey()) == null);
        selectedProfileNames.entrySet().removeIf(entry -> Bukkit.getPlayer(entry.getKey()) == null);
        simulationDistances.entrySet().removeIf(entry -> Bukkit.getPlayer(entry.getKey()) == null);
        afkPlayers.removeIf(uuid -> Bukkit.getPlayer(uuid) == null);
    }

    private void schedulePermissionChecks() {
        if (permissionCheckTask != null) {
            permissionCheckTask.cancel();
            permissionCheckTask = null;
        }
        Duration interval = seeMore.getSeeMoreConfig().permissionCheckInterval();
        if (interval == null) {
            return;
        }
        long ticks = DurationParser.toTicks(interval);
        permissionCheckTask = seeMore.getSchedulerHook().runRepeatingTask(() -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                seeMore.getSchedulerHook().runEntityTaskAsap(
                        () -> refreshCachedState(player),
                        () -> removePlayer(player),
                        player
                );
            }
        }, ticks, ticks);
    }

    private static void cancelAndRemove(Map<UUID, ScheduledTask> tasks, UUID playerId) {
        ScheduledTask task = tasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
    }

}
