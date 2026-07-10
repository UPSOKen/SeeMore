package com.froobworld.seemore.afk;

import com.froobworld.seemore.SeeMore;
import com.froobworld.seemore.config.AfkSettings;
import com.froobworld.seemore.config.DurationParser;
import com.froobworld.seemore.controller.ViewDistanceController;
import com.froobworld.seemore.scheduler.ScheduledTask;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.Bukkit;
import org.bukkit.Input;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AfkTracker implements Listener {
    private final SeeMore seeMore;
    private final ViewDistanceController controller;
    private final Map<UUID, TrackedPlayer> trackedPlayers = new ConcurrentHashMap<>();
    private ScheduledTask checkTask;

    public AfkTracker(SeeMore seeMore, ViewDistanceController controller) {
        this.seeMore = seeMore;
        this.controller = controller;
        Bukkit.getPluginManager().registerEvents(this, seeMore);
        long now = System.nanoTime();
        for (Player player : Bukkit.getOnlinePlayers()) {
            trackedPlayers.put(player.getUniqueId(), new TrackedPlayer(player, new AfkState(now)));
        }
        scheduleChecks();
    }

    public void reloadConfiguration() {
        if (!seeMore.getSeeMoreConfig().afkSettings().enabled()) {
            long now = System.nanoTime();
            for (TrackedPlayer trackedPlayer : trackedPlayers.values()) {
                if (trackedPlayer.state().recordDefiniteActivity(now) == AfkState.Transition.BECAME_ACTIVE) {
                    applyTransition(trackedPlayer.player(), AfkState.Transition.BECAME_ACTIVE);
                }
            }
        }
        scheduleChecks();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onJoin(PlayerJoinEvent event) {
        trackedPlayers.put(event.getPlayer().getUniqueId(),
                new TrackedPlayer(event.getPlayer(), new AfkState(System.nanoTime())));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onQuit(PlayerQuitEvent event) {
        trackedPlayers.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onInput(PlayerInputEvent event) {
        recordDefiniteActivity(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onInteract(PlayerInteractEvent event) {
        recordDefiniteActivity(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onAnimation(PlayerAnimationEvent event) {
        recordDefiniteActivity(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onCommand(PlayerCommandPreprocessEvent event) {
        recordDefiniteActivity(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onChat(AsyncChatEvent event) {
        recordDefiniteActivity(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            recordDefiniteActivity(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onMove(PlayerMoveEvent event) {
        if (event instanceof PlayerTeleportEvent) {
            return;
        }
        Location to = event.getTo();
        if (to == null) {
            return;
        }
        double yawChange = angleDifference(event.getFrom().getYaw(), to.getYaw());
        double pitchChange = Math.abs(event.getFrom().getPitch() - to.getPitch());
        double lookChange = Math.hypot(yawChange, pitchChange);
        if (lookChange == 0) {
            return; // Position-only movement, including AFK pools, is deliberately ignored.
        }

        AfkSettings settings = seeMore.getSeeMoreConfig().afkSettings();
        TrackedPlayer trackedPlayer = trackedPlayers.computeIfAbsent(event.getPlayer().getUniqueId(),
                ignored -> new TrackedPlayer(event.getPlayer(), new AfkState(System.nanoTime())));
        AfkState.Transition transition = trackedPlayer.state().recordLookActivity(
                lookChange,
                System.nanoTime(),
                settings.minimumLookChange(),
                settings.requiredLookEvents(),
                settings.lookEventWindow()
        );
        applyTransition(event.getPlayer(), transition);
    }

    private void recordDefiniteActivity(Player player) {
        TrackedPlayer trackedPlayer = trackedPlayers.computeIfAbsent(player.getUniqueId(),
                ignored -> new TrackedPlayer(player, new AfkState(System.nanoTime())));
        applyTransition(player, trackedPlayer.state().recordDefiniteActivity(System.nanoTime()));
    }

    private void checkPlayers() {
        AfkSettings settings = seeMore.getSeeMoreConfig().afkSettings();
        for (TrackedPlayer trackedPlayer : trackedPlayers.values()) {
            seeMore.getSchedulerHook().runEntityTaskAsap(() -> {
                AfkState.Transition transition = checkAfkState(trackedPlayer, settings);
                controller.refreshCachedState(trackedPlayer.player());
                if (transition != AfkState.Transition.NONE) {
                    synchronizeControllerState(trackedPlayer.player(), trackedPlayer.state());
                }
            }, () -> trackedPlayers.remove(trackedPlayer.player().getUniqueId()), trackedPlayer.player());
        }
    }

    private static AfkState.Transition checkAfkState(TrackedPlayer trackedPlayer, AfkSettings settings) {
        if (!settings.enabled()) {
            return AfkState.Transition.NONE;
        }
        long now = System.nanoTime();
        if (hasMovementInput(trackedPlayer.player().getCurrentInput())) {
            return trackedPlayer.state().recordDefiniteActivity(now);
        }
        return trackedPlayer.state().checkTimeout(now, settings.timeout());
    }

    static boolean hasMovementInput(Input input) {
        return input.isForward() || input.isBackward() || input.isLeft() || input.isRight()
                || input.isJump() || input.isSneak() || input.isSprint();
    }

    private void applyTransition(Player player, AfkState.Transition transition) {
        if (transition == AfkState.Transition.NONE) {
            return;
        }
        seeMore.getSchedulerHook().runEntityTaskAsap(
                () -> {
                    TrackedPlayer trackedPlayer = trackedPlayers.get(player.getUniqueId());
                    if (trackedPlayer != null) {
                        synchronizeControllerState(player, trackedPlayer.state());
                    }
                },
                () -> trackedPlayers.remove(player.getUniqueId()),
                player
        );
    }

    private void synchronizeControllerState(Player player, AfkState state) {
        controller.setAfk(player, state.isAfk());
    }

    private void scheduleChecks() {
        if (checkTask != null) {
            checkTask.cancel();
        }
        long ticks = DurationParser.toTicks(seeMore.getSeeMoreConfig().afkSettings().checkInterval());
        checkTask = seeMore.getSchedulerHook().runRepeatingTask(this::checkPlayers, ticks, ticks);
    }

    private static double angleDifference(float first, float second) {
        double difference = Math.abs(first - second) % 360.0;
        return difference > 180.0 ? 360.0 - difference : difference;
    }

    private record TrackedPlayer(Player player, AfkState state) {
    }
}
