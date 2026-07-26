package dev.x208.sheepmerge

import dev.x208.sheepmerge.bstats.Metrics
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.entity.Sheep
import org.bukkit.plugin.PluginDescriptionFile
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.plugin.java.JavaPluginLoader
import java.io.File
import java.util.HashMap
import java.util.UUID
import java.util.function.Consumer
import java.util.logging.Logger

class SheepMergePlugin : JavaPlugin {

    private var liveSheepWorldCursor = 0
    private var sheepWorldCursor = 0
    private var sheepPlayerCursor = 0
    private var automationAutoSpawnCursor = 0
    private var eggDistributionCursor = 0
    private var gameplayMenuCursor = 0
    private var saturationCursor = 0
    private val sheepWorldOffsets: MutableMap<UUID, Int> = HashMap()

    constructor() : super()

    protected constructor(
        loader: JavaPluginLoader,
        description: PluginDescriptionFile,
        dataFolder: File,
        file: File
    ) : super(loader, description, dataFolder, file)

    override fun onEnable() {
        instance = this
        log = logger
        saveDefaultConfig()
        reloadConfig()
        SheepMergeConfiguration.initialize(this)
        SheepMergeManager.initialize(this)
        SheepMergeManager.applyConfiguration(SheepMergeConfiguration.get())
        SheepMergeManager.purgeMarkedBackupsIfEligibleOnStartup()
        SheepFarmWorldCleanupListener.cleanupFarmWorldsOnStartup()
        setup()
        SheepFarmWorldCommand.applyFarmRulesToLoadedWorlds()
        SheepMergeManager.warmFarmWorldStructureCacheOnStartup()
        SheepMergeManager.maybeCreateAutomaticBackup("restart")
        SheepMergeManager.restoreTopPointsDisplayAfterRestart(null)
        LiveUpdateCoordinator.reconcileStagedUpdateOnStartup(this)
        scheduleSheepEggDistribution()
        scheduleSheepNameUpdates()
        scheduleAutomationAutoSpawnEveryTick()
        scheduleLiveSheepCountUpdates()
        scheduleFarmLoadoutAndReminderUpdates()
        scheduleFarmSaturationUpdates()
        scheduleRandomFarmEvents()
        scheduleGameplayTips()
        scheduleAutomaticBackups()
        LiveUpdateCoordinator.scheduleAutomaticChecks(this)
        server.pluginManager.registerEvents(SheepMergeWorldListener(), this)
        log.info("Ready!")
    }

    private fun scheduleLiveSheepCountUpdates() {
        val configuration = SheepMergeConfiguration.get()
        val normalTickInterval = configuration?.schedulerNormalTickInterval ?: 20L
        val liveCountInterval = maxOf(normalTickInterval, normalTickInterval * 3L)
        server.scheduler.runTaskTimerAsynchronously(
            this,
            Runnable { server.scheduler.runTask(this, Runnable { runLiveSheepCountBatch() }) },
            liveCountInterval,
            liveCountInterval
        )
    }

    private fun scheduleSheepNameUpdates() {
        val configuration = SheepMergeConfiguration.get()
        val fastTickInterval = configuration?.schedulerFastTickInterval ?: 2L
        server.scheduler.runTaskTimerAsynchronously(
            this,
            Runnable { server.scheduler.runTask(this, Runnable { runSheepNameUpdateBatch() }) },
            fastTickInterval,
            fastTickInterval
        )
    }

    private fun scheduleAutomationAutoSpawnEveryTick() {
        server.scheduler.runTaskTimerAsynchronously(
            this,
            Runnable { server.scheduler.runTask(this, Runnable { runAutomationAutoSpawnBatch() }) },
            1L,
            1L
        )
    }

    private fun setup() {
        server.pluginManager.registerEvents(SheepFarmWorldProtectionListener(), this)
        server.pluginManager.registerEvents(SheepFarmWorldCleanupListener(), this)
        server.pluginManager.registerEvents(SheepFarmGameListener(), this)
        val command = SheepFarmWorldCommand()
        getCommand("sheepmerge")!!.setExecutor(command)
        getCommand("sheepmerge")!!.tabCompleter = command
        Metrics(this, BSTATS_PLUGIN_ID)
    }

    private fun scheduleSheepEggDistribution() {
        val configuration = SheepMergeConfiguration.get()
        val fastTickInterval = configuration?.schedulerFastTickInterval ?: 2L
        server.scheduler.runTaskTimerAsynchronously(
            this,
            Runnable { server.scheduler.runTask(this, Runnable { runEggDistributionBatch() }) },
            fastTickInterval,
            fastTickInterval
        )
    }

    private fun scheduleFarmLoadoutAndReminderUpdates() {
        val configuration = SheepMergeConfiguration.get()
        val reminderTickInterval = configuration?.schedulerReminderTickInterval ?: 20L
        server.scheduler.runTaskTimerAsynchronously(
            this,
            Runnable { server.scheduler.runTask(this, Runnable { runGameplayAndMenuBatch() }) },
            reminderTickInterval,
            reminderTickInterval
        )
    }

    private fun scheduleFarmSaturationUpdates() {
        val configuration = SheepMergeConfiguration.get()
        val normalTickInterval = configuration?.schedulerNormalTickInterval ?: 20L
        val saturationInterval = maxOf(normalTickInterval, normalTickInterval * 4L)
        server.scheduler.runTaskTimerAsynchronously(
            this,
            Runnable { server.scheduler.runTask(this, Runnable { runFarmSaturationBatch() }) },
            saturationInterval,
            saturationInterval
        )
    }

    private fun runLiveSheepCountBatch() {
        val farmWorlds = server.worlds.filter(SheepMergeManager::isSheepFarmWorld)
        if (farmWorlds.isEmpty()) {
            SheepMergeManager.refreshLiveSheepCounts(server.worlds)
            liveSheepWorldCursor = 0
            return
        }

        liveSheepWorldCursor = runRoundRobinBatch(
            farmWorlds,
            liveSheepWorldCursor,
            LIVE_SHEEP_COUNT_WORLD_BATCH,
            SheepMergeManager::refreshLiveSheepCount
        )
    }

    private fun runSheepNameUpdateBatch() {
        val farmWorlds = server.worlds.filter(SheepMergeManager::isSheepFarmWorld)
        if (farmWorlds.isEmpty()) {
            sheepWorldCursor = 0
            sheepWorldOffsets.clear()
        } else {
            sheepWorldCursor = runRoundRobinBatch(
                farmWorlds,
                sheepWorldCursor,
                LIVE_SHEEP_COUNT_WORLD_BATCH,
                this::runSheepEatBatchForWorld
            )
        }

        val onlinePlayers = server.onlinePlayers.toList()
        sheepPlayerCursor = runRoundRobinBatch(
            onlinePlayers,
            sheepPlayerCursor,
            SHEEP_PLAYER_MOVEMENT_BATCH
        ) { player ->
            SheepMergeManager.recoverPlayerIfFallenFromPlatform(player)
            SheepMergeManager.updateCarriedSheepPosition(player)
        }
    }

    private fun runSheepEatBatchForWorld(world: World?) {
        if (world == null) {
            return
        }

        val sheepInWorld = world.getEntitiesByClass(Sheep::class.java).toList()
        if (sheepInWorld.isEmpty()) {
            sheepWorldOffsets.remove(world.uid)
            return
        }

        val size = sheepInWorld.size
        var offset = Math.floorMod(sheepWorldOffsets.getOrDefault(world.uid, 0), size)
        var processed = 0

        while (processed < SHEEP_EAT_BATCH_PER_WORLD && processed < size) {
            SheepMergeManager.processSheepEatTimer(sheepInWorld[offset])
            offset = (offset + 1) % size
            processed++
        }

        sheepWorldOffsets[world.uid] = offset
    }

    private fun runAutomationAutoSpawnBatch() {
        val onlinePlayers = server.onlinePlayers.toList()
        automationAutoSpawnCursor = runRoundRobinBatch(
            onlinePlayers,
            automationAutoSpawnCursor,
            AUTOMATION_AUTOSPAWN_PLAYER_BATCH,
            SheepMergeManager::tickAutomationAutoSpawnRealtime
        )
    }

    private fun runEggDistributionBatch() {
        val onlinePlayers = server.onlinePlayers.toList()
        eggDistributionCursor = runRoundRobinBatch(
            onlinePlayers,
            eggDistributionCursor,
            EGG_DISTRIBUTION_PLAYER_BATCH,
            SheepMergeManager::tickEggDistribution
        )
    }

    private fun runGameplayAndMenuBatch() {
        val onlinePlayers = server.onlinePlayers.toList()
        gameplayMenuCursor = runRoundRobinBatch(
            onlinePlayers,
            gameplayMenuCursor,
            GAMEPLAY_MENU_PLAYER_BATCH
        ) { player ->
            SheepMergeManager.tickOpenMenuStatRefresh(player)
            SheepMergeManager.tickAutomationPlaytimePoints(player)
            if (!SheepMergeManager.isSheepFarmWorld(player.world)) {
                return@runRoundRobinBatch
            }
            SheepMergeManager.enforceFarmLoadout(player)
            SheepMergeManager.tickTutorialReminder(player)
            SheepMergeManager.tickPrestigeReminder(player)
            SheepMergeManager.tickMergeReminder(player)
            SheepMergeManager.tickQuestSystem(player)
            SheepMergeManager.tickActiveAbilities(player)
            SheepMergeManager.tickCombo(player)
            SheepMergeManager.tickPointsGainOverlay(player)
        }
    }

    private fun runFarmSaturationBatch() {
        val onlinePlayers = server.onlinePlayers.toList()
        saturationCursor = runRoundRobinBatch(
            onlinePlayers,
            saturationCursor,
            SATURATION_PLAYER_BATCH,
            SheepMergeManager::applyFarmSaturation
        )
    }

    private fun scheduleRandomFarmEvents() {
        val configuration = SheepMergeConfiguration.get()
        val normalTickInterval = configuration?.schedulerNormalTickInterval ?: 20L
        server.scheduler.runTaskTimer(this, Runnable { SheepMergeManager.tickRandomFarmEvents() }, normalTickInterval, normalTickInterval)
    }

    private fun scheduleGameplayTips() {
        val configuration = SheepMergeConfiguration.get()
        val tipIntervalTicks = configuration?.schedulerTipIntervalTicks ?: 60L * 20L
        server.scheduler.runTaskTimer(this, Runnable { SheepMergeManager.broadcastRandomGameplayTip() }, tipIntervalTicks, tipIntervalTicks)
    }

    private fun scheduleAutomaticBackups() {
        val intervalTicks = SheepMergeManager.getAutomaticBackupIntervalTicks()
        server.scheduler.runTaskTimer(this, Runnable { SheepMergeManager.maybeCreateAutomaticBackup("hourly") }, intervalTicks, intervalTicks)
    }

    override fun onDisable() {
        SheepMergeManager.restoreAllPlayerStates()
        LiveUpdateCoordinator.applyStagedBinaryOnShutdown(this)
        SheepMergeManager.saveData()
        SheepFarmWorldCleanupListener.cleanupFarmWorldsOnShutdown()
        log.info("Thanks for using $NAME!")
    }

    companion object {
        @JvmField
        var instance: SheepMergePlugin? = null

        @JvmField
        var log: Logger = Logger.getLogger(SheepMergePlugin::class.java.name)

        const val NAME: String = "SheepMerge"
        const val BSTATS_PLUGIN_ID: Int = 20765

        private const val LIVE_SHEEP_COUNT_WORLD_BATCH = 2
        private const val SHEEP_EAT_BATCH_PER_WORLD = 120
        private const val SHEEP_PLAYER_MOVEMENT_BATCH = 24
        private const val AUTOMATION_AUTOSPAWN_PLAYER_BATCH = 32
        private const val EGG_DISTRIBUTION_PLAYER_BATCH = 32
        private const val GAMEPLAY_MENU_PLAYER_BATCH = 24
        private const val SATURATION_PLAYER_BATCH = 40

        private fun <T> runRoundRobinBatch(
            values: List<T>?,
            cursor: Int,
            batchSize: Int,
            action: Consumer<T>?
        ): Int {
            if (values.isNullOrEmpty() || batchSize <= 0 || action == null) {
                return 0
            }

            val size = values.size
            val safeCursor = Math.floorMod(cursor, size)
            val toProcess = minOf(batchSize, size)

            for (index in 0 until toProcess) {
                val currentIndex = (safeCursor + index) % size
                action.accept(values[currentIndex])
            }

            return (safeCursor + toProcess) % size
        }
    }
}
