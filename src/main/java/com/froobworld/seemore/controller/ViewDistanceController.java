package com.froobworld.seemore.controller;

import com.froobworld.seemore.SeeMore;
import com.froobworld.seemore.config.ResolvedProfile;
import com.froobworld.seemore.config.UndergroundSettings;
import com.froobworld.seemore.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
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
    private final Map<UUID, Integer> selectedProfileMaximums = new ConcurrentHashMap<>();
    private final Set<UUID> afkPlayers = ConcurrentHashMap.newKeySet();
    private final Set<UUID> undergroundPlayers = ConcurrentHashMap.newKeySet();
    private final Set<UUID> undergroundBypassPlayers = ConcurrentHashMap.newKeySet();
    private final ViewDistanceUpdateLogger viewDistanceUpdateLogger;

    public ViewDistanceController(SeeMore seeMore) {
        this.seeMore = seeMore;
        this.viewDistanceUpdateLogger = new ViewDistanceUpdateLogger(seeMore);
        seeMore.getSchedulerHook().runRepeatingTask(this::cleanMaps, 1200, 1200);
        Bukkit.getPluginManager().registerEvents(new ViewDistanceUpdater(this), seeMore);
    }

    public void updateAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            seeMore.getSchedulerHook().runEntityTaskAsap(() -> {
                refreshProfileAndSetTargetViewDistance(
                        player, player.getClientViewDistance(), false, false);
            }, null, player);
        }
    }

    public void setTargetViewDistance(Player player, int clientViewDistance, boolean testDelay, boolean initialUpdate) {
        setTargetViewDistance(player, clientViewDistance, testDelay, initialUpdate, initialUpdate);
    }

    public void refreshProfileAndSetTargetViewDistance(Player player, int clientViewDistance,
                                                       boolean testDelay, boolean initialUpdate) {
        setTargetViewDistance(player, clientViewDistance, testDelay, initialUpdate, true);
    }

    private void setTargetViewDistance(Player player, int clientViewDistance, boolean testDelay,
                                       boolean initialUpdate, boolean refreshProfile) {
        UUID playerId = player.getUniqueId();
        boolean firstUpdate = !targetSendDistanceMap.containsKey(playerId);
        boolean permissionsRefreshing = shouldResolveProfile(
                refreshProfile, selectedProfileNames.get(playerId), selectedProfileMaximums.get(playerId));
        if (permissionsRefreshing) {
            boolean bypassEnabled = seeMore.getSeeMoreConfig().undergroundSettings().bypassPermissionEnabled();
            if (shouldCheckUndergroundBypass(true, bypassEnabled)
                    && player.hasPermission(UndergroundSettings.BYPASS_PERMISSION)) {
                undergroundBypassPlayers.add(playerId);
            } else {
                undergroundBypassPlayers.remove(playerId);
            }
        }
        ResolvedProfile profile = resolveProfile(player, refreshProfile);
        int simulationDistance = player.getSimulationDistance();
        ViewDistancePolicy.Result result = ViewDistancePolicy.calculate(
                clientViewDistance,
                profile.maximumViewDistance(),
                player.getWorld().getViewDistance(),
                simulationDistance,
                afkPlayers.contains(playerId),
                seeMore.getSeeMoreConfig().afkSettings().maximumViewDistance(),
                seeMore.getSeeMoreConfig().afkSettings().minimumReduction(),
                undergroundPlayers.contains(playerId),
                seeMore.getSeeMoreConfig().undergroundSettings().maximumViewDistance()
        );

        selectedProfileNames.put(playerId, profile.name());
        selectedProfileMaximums.put(playerId, profile.maximumViewDistance());
        targetViewDistanceMap.put(playerId, result.viewDistance());
        targetSendDistanceMap.put(playerId, result.sendDistance());

        // Update the view distance with a delay if it is being lowered
        long delay = 0;
        try {
            if (testDelay && player.getViewDistance() > targetViewDistanceMap.get(player.getUniqueId())) {
                delay = seeMore.getSeeMoreConfig().updateDelayTicks();
            }
        } catch (Exception ignored) {}

        updateSendDistance(player, initialUpdate && firstUpdate);
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

    public boolean shouldRunNormalChecks(UUID playerId) {
        return shouldRunNormalChecks(afkPlayers.contains(playerId));
    }

    static boolean shouldRunNormalChecks(boolean afk) {
        return !afk;
    }

    public void setUnderground(Player player, boolean underground) {
        boolean changed = underground
                ? undergroundPlayers.add(player.getUniqueId())
                : undergroundPlayers.remove(player.getUniqueId());
        if (changed) {
            setTargetViewDistance(player, player.getClientViewDistance(), false, false);
        }
    }

    public boolean isUnderground(Player player) {
        return undergroundPlayers.contains(player.getUniqueId());
    }

    public boolean isUndergroundBypassGranted(Player player) {
        return undergroundBypassPlayers.contains(player.getUniqueId());
    }

    public PlayerDistanceStatus captureStatus(Player player) {
        UUID playerId = player.getUniqueId();
        ResolvedProfile currentProfile = resolveProfile(player, false);
        String selectedProfileName = selectedProfileNames.getOrDefault(playerId, currentProfile.name());
        int selectedProfileMaximum = selectedProfileMaximums.getOrDefault(
                playerId, currentProfile.maximumViewDistance());
        int worldViewDistance = player.getWorld().getViewDistance();
        int simulationDistance = player.getSimulationDistance();
        boolean afk = afkPlayers.contains(playerId);
        boolean underground = undergroundPlayers.contains(playerId);
        int afkMaximum = seeMore.getSeeMoreConfig().afkSettings().maximumViewDistance();
        UndergroundSettings undergroundSettings = seeMore.getSeeMoreConfig().undergroundSettings();
        int undergroundMaximum = undergroundSettings.maximumViewDistance();
        ViewDistancePolicy.Result calculated = ViewDistancePolicy.calculate(
                player.getClientViewDistance(), selectedProfileMaximum, worldViewDistance,
                simulationDistance, afk, afkMaximum,
                seeMore.getSeeMoreConfig().afkSettings().minimumReduction(), underground, undergroundMaximum);
        DistanceMode mode = afk ? DistanceMode.AFK
                : underground ? DistanceMode.UNDERGROUND : DistanceMode.NORMAL;
        return new PlayerDistanceStatus(
                player.getName(), player.getWorld().getName(), selectedProfileName, mode,
                player.getClientViewDistance(), selectedProfileMaximum, worldViewDistance,
                targetViewDistanceMap.getOrDefault(playerId, calculated.viewDistance()),
                targetSendDistanceMap.getOrDefault(playerId, calculated.sendDistance()),
                player.getViewDistance(), player.getSendViewDistance(), simulationDistance,
                afkMaximum, seeMore.getSeeMoreConfig().afkSettings().minimumReduction(), undergroundMaximum,
                undergroundSettings.naturalCeiling().enabled(),
                undergroundSettings.naturalCeiling().searchDistance(),
                undergroundSettings.naturalCeiling().minimumThickness(),
                undergroundSettings.bypassPermissionEnabled(),
                undergroundBypassPlayers.contains(playerId));
    }

    public void reloadConfiguration() {
        updateAllPlayers();
    }

    public void removePlayer(Player player) {
        UUID playerId = player.getUniqueId();
        selectedProfileNames.remove(playerId);
        selectedProfileMaximums.remove(playerId);
        afkPlayers.remove(playerId);
        undergroundPlayers.remove(playerId);
        undergroundBypassPlayers.remove(playerId);
        targetSendDistanceMap.remove(playerId);
        targetViewDistanceMap.remove(playerId);
        cancelAndRemove(sendDistanceUpdateTasks, playerId);
        cancelAndRemove(viewDistanceUpdateTasks, playerId);
    }

    private void updateSendDistance(Player player, boolean force) {
        UUID playerId = player.getUniqueId();
        Integer targetDistance = targetSendDistanceMap.get(playerId);
        if (!force && targetDistance != null && player.getSendViewDistance() == targetDistance) {
            cancelAndRemove(sendDistanceUpdateTasks, playerId);
            return;
        }
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
        selectedProfileMaximums.entrySet().removeIf(entry -> Bukkit.getPlayer(entry.getKey()) == null);
        afkPlayers.removeIf(uuid -> Bukkit.getPlayer(uuid) == null);
        undergroundPlayers.removeIf(uuid -> Bukkit.getPlayer(uuid) == null);
        undergroundBypassPlayers.removeIf(uuid -> Bukkit.getPlayer(uuid) == null);
    }

    private ResolvedProfile resolveProfile(Player player, boolean refresh) {
        UUID playerId = player.getUniqueId();
        String cachedName = selectedProfileNames.get(playerId);
        Integer cachedMaximum = selectedProfileMaximums.get(playerId);
        if (!shouldResolveProfile(refresh, cachedName, cachedMaximum)) {
            return new ResolvedProfile(cachedName, cachedMaximum);
        }
        return seeMore.getSeeMoreConfig().resolveProfile(player);
    }

    static boolean shouldResolveProfile(boolean refresh, String cachedName, Integer cachedMaximum) {
        return refresh || cachedName == null || cachedMaximum == null;
    }

    static boolean shouldCheckUndergroundBypass(boolean permissionsRefreshing, boolean bypassEnabled) {
        return permissionsRefreshing && bypassEnabled;
    }

    private static void cancelAndRemove(Map<UUID, ScheduledTask> tasks, UUID playerId) {
        ScheduledTask task = tasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
    }

}
