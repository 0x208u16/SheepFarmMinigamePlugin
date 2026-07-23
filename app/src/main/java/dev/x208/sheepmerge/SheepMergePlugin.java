// Copyright (c) 2023 x208
// 
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at http://mozilla.org/MPL/2.0/.

package dev.x208.sheepmerge;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.function.Consumer;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;

import dev.x208.sheepmerge.bstats.Metrics;

public class SheepMergePlugin extends JavaPlugin {

    public static SheepMergePlugin instance;
    public static Logger log;
    public final static String NAME = "SheepMerge";
    public final static int BSTATS_PLUGIN_ID = 20765; // Optional: Replace with your own bStats plugin ID

    private static final int LIVE_SHEEP_COUNT_WORLD_BATCH = 2;
    private static final int SHEEP_EAT_BATCH_PER_WORLD = 120;
    private static final int SHEEP_PLAYER_MOVEMENT_BATCH = 24;
    private static final int AUTOMATION_AUTOSPAWN_PLAYER_BATCH = 32;
    private static final int EGG_DISTRIBUTION_PLAYER_BATCH = 32;
    private static final int GAMEPLAY_MENU_PLAYER_BATCH = 24;
    private static final int SATURATION_PLAYER_BATCH = 40;

    private int liveSheepWorldCursor = 0;
    private int sheepWorldCursor = 0;
    private int sheepPlayerCursor = 0;
    private int automationAutoSpawnCursor = 0;
    private int eggDistributionCursor = 0;
    private int gameplayMenuCursor = 0;
    private int saturationCursor = 0;
    private final Map<UUID, Integer> sheepWorldOffsets = new HashMap<>();

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
        SheepMergeManager.warmFarmWorldStructureCacheOnStartup();
        SheepMergeManager.maybeCreateAutomaticBackup("restart");
        SheepMergeManager.restoreTopPointsDisplayAfterRestart(null);
        scheduleSheepEggDistribution();
        scheduleSheepNameUpdates();
        scheduleAutomationAutoSpawnEveryTick();
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
        getServer().getScheduler().runTaskTimerAsynchronously(this,
                () -> getServer().getScheduler().runTask(this, this::runLiveSheepCountBatch),
                normalTickInterval,
                normalTickInterval);
    }

    private void scheduleSheepNameUpdates() {
        SheepMergeConfiguration configuration = SheepMergeConfiguration.get();
        long fastTickInterval = configuration == null ? 2L : configuration.getSchedulerFastTickInterval();
        getServer().getScheduler().runTaskTimerAsynchronously(this,
                () -> getServer().getScheduler().runTask(this, this::runSheepNameUpdateBatch),
                fastTickInterval,
                fastTickInterval);
    }

    private void scheduleAutomationAutoSpawnEveryTick() {
        getServer().getScheduler().runTaskTimerAsynchronously(this,
                () -> getServer().getScheduler().runTask(this, this::runAutomationAutoSpawnBatch),
                1L,
                1L);
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
        getServer().getScheduler().runTaskTimerAsynchronously(this,
                () -> getServer().getScheduler().runTask(this, this::runEggDistributionBatch),
                fastTickInterval,
                fastTickInterval);
    }

    private void scheduleFarmLoadoutAndReminderUpdates() {
        SheepMergeConfiguration configuration = SheepMergeConfiguration.get();
        long reminderTickInterval = configuration == null ? 20L : configuration.getSchedulerReminderTickInterval();
        getServer().getScheduler().runTaskTimerAsynchronously(this,
                () -> getServer().getScheduler().runTask(this, this::runGameplayAndMenuBatch),
                reminderTickInterval,
                reminderTickInterval);
    }

    private void scheduleFarmSaturationUpdates() {
        SheepMergeConfiguration configuration = SheepMergeConfiguration.get();
        long normalTickInterval = configuration == null ? 20L : configuration.getSchedulerNormalTickInterval();
        getServer().getScheduler().runTaskTimerAsynchronously(this,
                () -> getServer().getScheduler().runTask(this, this::runFarmSaturationBatch),
                normalTickInterval,
                normalTickInterval);
    }

    private void runLiveSheepCountBatch() {
        List<World> farmWorlds = getServer().getWorlds().stream()
                .filter(SheepMergeManager::isSheepFarmWorld)
                .toList();
        if (farmWorlds.isEmpty()) {
            SheepMergeManager.refreshLiveSheepCounts(getServer().getWorlds());
            liveSheepWorldCursor = 0;
            return;
        }

        liveSheepWorldCursor = runRoundRobinBatch(farmWorlds, liveSheepWorldCursor, LIVE_SHEEP_COUNT_WORLD_BATCH,
                SheepMergeManager::refreshLiveSheepCount);
    }

    private void runSheepNameUpdateBatch() {
        List<World> farmWorlds = getServer().getWorlds().stream()
                .filter(SheepMergeManager::isSheepFarmWorld)
                .toList();
        if (farmWorlds.isEmpty()) {
            sheepWorldCursor = 0;
            sheepWorldOffsets.clear();
        } else {
            sheepWorldCursor = runRoundRobinBatch(farmWorlds, sheepWorldCursor, LIVE_SHEEP_COUNT_WORLD_BATCH,
                    this::runSheepEatBatchForWorld);
        }

        List<Player> onlinePlayers = List.copyOf(getServer().getOnlinePlayers());
        sheepPlayerCursor = runRoundRobinBatch(onlinePlayers, sheepPlayerCursor, SHEEP_PLAYER_MOVEMENT_BATCH,
                player -> {
                    SheepMergeManager.recoverPlayerIfFallenFromPlatform(player);
                    SheepMergeManager.updateCarriedSheepPosition(player);
                });
    }

    private void runSheepEatBatchForWorld(World world) {
        if (world == null) {
            return;
        }

        List<Sheep> sheepInWorld = List.copyOf(world.getEntitiesByClass(Sheep.class));
        if (sheepInWorld.isEmpty()) {
            sheepWorldOffsets.remove(world.getUID());
            return;
        }

        int size = sheepInWorld.size();
        int offset = Math.floorMod(sheepWorldOffsets.getOrDefault(world.getUID(), 0), size);
        int processed = 0;

        while (processed < SHEEP_EAT_BATCH_PER_WORLD && processed < size) {
            SheepMergeManager.processSheepEatTimer(sheepInWorld.get(offset));
            offset = (offset + 1) % size;
            processed++;
        }

        sheepWorldOffsets.put(world.getUID(), offset);
    }

    private void runAutomationAutoSpawnBatch() {
        List<Player> onlinePlayers = List.copyOf(getServer().getOnlinePlayers());
        automationAutoSpawnCursor = runRoundRobinBatch(onlinePlayers,
                automationAutoSpawnCursor,
                AUTOMATION_AUTOSPAWN_PLAYER_BATCH,
                SheepMergeManager::tickAutomationAutoSpawnRealtime);
    }

    private void runEggDistributionBatch() {
        List<Player> onlinePlayers = List.copyOf(getServer().getOnlinePlayers());
        eggDistributionCursor = runRoundRobinBatch(onlinePlayers,
                eggDistributionCursor,
                EGG_DISTRIBUTION_PLAYER_BATCH,
                SheepMergeManager::tickEggDistribution);
    }

    private void runGameplayAndMenuBatch() {
        List<Player> onlinePlayers = List.copyOf(getServer().getOnlinePlayers());
        gameplayMenuCursor = runRoundRobinBatch(onlinePlayers, gameplayMenuCursor, GAMEPLAY_MENU_PLAYER_BATCH,
                player -> {
                    SheepMergeManager.tickOpenMenuStatRefresh(player);
                    SheepMergeManager.tickAutomationPlaytimePoints(player);
                    if (!SheepMergeManager.isSheepFarmWorld(player.getWorld())) {
                        return;
                    }
                    SheepMergeManager.enforceFarmLoadout(player);
                    SheepMergeManager.tickTutorialReminder(player);
                    SheepMergeManager.tickPrestigeReminder(player);
                    SheepMergeManager.tickMergeReminder(player);
                    SheepMergeManager.tickQuestSystem(player);
                    SheepMergeManager.tickActiveAbilities(player);
                    SheepMergeManager.tickCombo(player);
                    SheepMergeManager.tickPointsGainOverlay(player);
                });
    }

    private void runFarmSaturationBatch() {
        List<Player> onlinePlayers = List.copyOf(getServer().getOnlinePlayers());
        saturationCursor = runRoundRobinBatch(onlinePlayers,
                saturationCursor,
                SATURATION_PLAYER_BATCH,
                SheepMergeManager::applyFarmSaturation);
    }

    private static <T> int runRoundRobinBatch(List<T> values, int cursor, int batchSize, Consumer<T> action) {
        if (values == null || values.isEmpty() || batchSize <= 0 || action == null) {
            return 0;
        }

        int size = values.size();
        int safeCursor = Math.floorMod(cursor, size);
        int toProcess = Math.min(batchSize, size);

        for (int i = 0; i < toProcess; i++) {
            int index = (safeCursor + i) % size;
            action.accept(values.get(index));
        }

        return (safeCursor + toProcess) % size;
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