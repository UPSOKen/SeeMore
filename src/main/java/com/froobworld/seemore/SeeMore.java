package com.froobworld.seemore;

import com.froobworld.seemore.afk.AfkTracker;
import com.froobworld.seemore.command.SeeMoreCommand;
import com.froobworld.seemore.config.SeeMoreConfig;
import com.froobworld.seemore.controller.ViewDistanceController;
import com.froobworld.seemore.metrics.SeeMoreMetrics;
import com.froobworld.seemore.scheduler.BukkitSchedulerHook;
import com.froobworld.seemore.scheduler.RegionisedSchedulerHook;
import com.froobworld.seemore.scheduler.SchedulerHook;
import com.froobworld.seemore.underground.UndergroundTracker;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class SeeMore extends JavaPlugin {
    private SeeMoreConfig config;
    private SchedulerHook schedulerHook;
    private ViewDistanceController viewDistanceController;
    private AfkTracker afkTracker;
    private UndergroundTracker undergroundTracker;

    @Override
    public void onEnable() {
        config = new SeeMoreConfig(this);
        try {
            config.load();
        } catch (Exception e) {
            getLogger().severe("Error loading config");
            e.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        if (RegionisedSchedulerHook.isCompatible()) {
            schedulerHook = new RegionisedSchedulerHook(this);
        } else {
            schedulerHook = new BukkitSchedulerHook(this);
        }

        viewDistanceController = new ViewDistanceController(this);
        undergroundTracker = new UndergroundTracker(this, viewDistanceController);
        afkTracker = new AfkTracker(this, viewDistanceController, undergroundTracker);

        registerCommand();

        new SeeMoreMetrics(this);
    }

    @Override
    public void onDisable() {

    }

    private void registerCommand() {
        PluginCommand pluginCommand = getCommand("seemore");
        if (pluginCommand != null) {
            SeeMoreCommand seeMoreCommand = new SeeMoreCommand(this);
            pluginCommand.setExecutor(seeMoreCommand);
            pluginCommand.setTabCompleter(seeMoreCommand);
            pluginCommand.setPermission("seemore.command.seemore");
        }
    }

    public void reload() throws Exception {
        config.load();
        if (undergroundTracker != null) {
            undergroundTracker.reloadConfiguration();
        }
        if (afkTracker != null) {
            afkTracker.reloadConfiguration();
        }
        if (viewDistanceController != null) {
            viewDistanceController.reloadConfiguration();
        }
    }

    public SeeMoreConfig getSeeMoreConfig() {
        return config;
    }

    public SchedulerHook getSchedulerHook() {
        return schedulerHook;
    }

    public ViewDistanceController getViewDistanceController() {
        return viewDistanceController;
    }
}
