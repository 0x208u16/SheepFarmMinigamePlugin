// Copyright (c) 2023 0x208u16
// 
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at http://mozilla.org/MPL/2.0/.

package dev.thehale.sheepmerge;

import java.io.File;
import java.util.logging.Logger;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;

import dev.thehale.sheepmerge.bstats.Metrics;

public class SheepMergePlugin extends JavaPlugin {

    public static SheepMergePlugin instance;
    public static Logger log;
    public final static String NAME = "SheepMerge";
    public final static int BSTATS_PLUGIN_ID = 20765; // Optional: Replace with your own bStats plugin ID

    /**
     * Default constructor.
     * 
     * <p>
     * Used solely by MockBukkit during unit tests.
     */
    public SheepMergePlugin() {
        super();
    }

    /**
     * Parameterized constructor.
     * 
     * <p>
     * Used solely by MockBukkit during unit tests.
     */
    protected SheepMergePlugin(JavaPluginLoader loader, PluginDescriptionFile description, File dataFolder,
            File file) {
        super(loader, description, dataFolder, file);
    }

    @Override
    public void onEnable() {
        instance = this;
        log = getLogger();
        saveDefaultConfig();
        reloadConfig();
        SheepMergeConfiguration.initialize(this);
        SheepMergeManager.initialize(this);
        SheepMergeManager.applyConfiguration(SheepMergeConfiguration.get());
        SheepMergeManager.purgeMarkedBackupsIfEligibleOnStartup();
        SheepFarmWorldCleanupListener.cleanupFarmWorldsOnStartup();
        setup();
        SheepFarmWorldCommand.applyFarmRulesToLoadedWorlds();
        SheepMergeManager.maybeCreateAutomaticBackup("restart");
        SheepMergeManager.restoreTopPointsDisplayAfterRestart(null);
        scheduleSheepEggDistribution();
        scheduleSheepNameUpdates();
        scheduleLiveSheepCountUpdates();
        scheduleFarmLoadoutAndReminderUpdates();
        scheduleFarmSaturationUpdates();
        scheduleRandomFarmEvents();
        scheduleGameplayTips();
        scheduleAutomaticBackups();
        getServer().getPluginManager().registerEvents(new SheepMergeWorldListener(), this);
        log.info("Ready!");
    }

    private void scheduleLiveSheepCountUpdates() {
        SheepMergeConfiguration configuration = SheepMergeConfiguration.get();
        long normalTickInterval = configuration == null ? 20L : configuration.getSchedulerNormalTickInterval();
        getServer().getScheduler().runTaskTimer(this,
                () -> SheepMergeManager.refreshLiveSheepCounts(getServer().getWorlds()),
                normalTickInterval,
                normalTickInterval);
    }

    private void scheduleSheepNameUpdates() {
        SheepMergeConfiguration configuration = SheepMergeConfiguration.get();
        long fastTickInterval = configuration == null ? 2L : configuration.getSchedulerFastTickInterval();
        getServer().getScheduler().runTaskTimer(this, () -> {
            for (World world : getServer().getWorlds()) {
                if (!SheepMergeManager.isSheepFarmWorld(world)) {
                    continue;
                }
                for (Sheep sheep : world.getEntitiesByClass(Sheep.class)) {
                    SheepMergeManager.processSheepEatTimer(sheep);
                }
            }
            for (Player player : getServer().getOnlinePlayers()) {
                SheepMergeManager.updateCarriedSheepPosition(player);
            }
        }, fastTickInterval, fastTickInterval);
    }

    private void setup() {
        getServer().getPluginManager().registerEvents(new SheepFarmWorldProtectionListener(), this);
        getServer().getPluginManager().registerEvents(new SheepFarmWorldCleanupListener(), this);
        getServer().getPluginManager().registerEvents(new SheepFarmGameListener(), this);
        SheepFarmWorldCommand command = new SheepFarmWorldCommand();
        getCommand("sheepmerge").setExecutor(command);
        getCommand("sheepmerge").setTabCompleter(command);
        new Metrics(this, BSTATS_PLUGIN_ID); // Enable bStats metrics
    }

    private void scheduleSheepEggDistribution() {
        SheepMergeConfiguration configuration = SheepMergeConfiguration.get();
        long fastTickInterval = configuration == null ? 2L : configuration.getSchedulerFastTickInterval();
        getServer().getScheduler().runTaskTimer(this, () -> {
            for (Player player : getServer().getOnlinePlayers()) {
                SheepMergeManager.tickEggDistribution(player);
            }
        }, fastTickInterval, fastTickInterval);
    }

    private void scheduleFarmLoadoutAndReminderUpdates() {
        SheepMergeConfiguration configuration = SheepMergeConfiguration.get();
        long reminderTickInterval = configuration == null ? 20L : configuration.getSchedulerReminderTickInterval();
        getServer().getScheduler().runTaskTimer(this, () -> {
            for (Player player : getServer().getOnlinePlayers()) {
                SheepMergeManager.tickAutomationPlaytimePoints(player);
                if (!SheepMergeManager.isSheepFarmWorld(player.getWorld())) {
                    continue;
                }
                SheepMergeManager.enforceFarmLoadout(player);
                SheepMergeManager.tickTutorialReminder(player);
                SheepMergeManager.tickPrestigeReminder(player);
                SheepMergeManager.tickMergeReminder(player);
                SheepMergeManager.tickQuestSystem(player);
                SheepMergeManager.tickActiveAbilities(player);
                SheepMergeManager.tickCombo(player);
                SheepMergeManager.tickPointsGainOverlay(player);
            }
        }, reminderTickInterval, reminderTickInterval);
    }

    private void scheduleFarmSaturationUpdates() {
        SheepMergeConfiguration configuration = SheepMergeConfiguration.get();
        long normalTickInterval = configuration == null ? 20L : configuration.getSchedulerNormalTickInterval();
        getServer().getScheduler().runTaskTimer(this, () -> {
            for (Player player : getServer().getOnlinePlayers()) {
                SheepMergeManager.applyFarmSaturation(player);
            }
        }, normalTickInterval, normalTickInterval);
    }

    private void scheduleRandomFarmEvents() {
        SheepMergeConfiguration configuration = SheepMergeConfiguration.get();
        long normalTickInterval = configuration == null ? 20L : configuration.getSchedulerNormalTickInterval();
        getServer().getScheduler().runTaskTimer(this,
                SheepMergeManager::tickRandomFarmEvents,
                normalTickInterval,
                normalTickInterval);
    }

    private void scheduleGameplayTips() {
        SheepMergeConfiguration configuration = SheepMergeConfiguration.get();
        long tipIntervalTicks = configuration == null ? 60L * 20L : configuration.getSchedulerTipIntervalTicks();
        getServer().getScheduler().runTaskTimer(this,
                SheepMergeManager::broadcastRandomGameplayTip,
                tipIntervalTicks,
                tipIntervalTicks);
    }

    private void scheduleAutomaticBackups() {
        long intervalTicks = SheepMergeManager.getAutomaticBackupIntervalTicks();
        getServer().getScheduler().runTaskTimer(this,
                () -> SheepMergeManager.maybeCreateAutomaticBackup("hourly"),
                intervalTicks,
                intervalTicks);
    }

    @Override
    public void onDisable() {
        SheepMergeManager.restoreAllPlayerStates();
        SheepMergeManager.saveData();
        SheepFarmWorldCleanupListener.cleanupFarmWorldsOnShutdown();
        log.info("Thanks for using " + NAME + "!");
    }
}