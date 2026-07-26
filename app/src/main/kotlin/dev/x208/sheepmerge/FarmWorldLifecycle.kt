package dev.x208.sheepmerge

import org.bukkit.Bukkit
import org.bukkit.Difficulty
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.WorldCreator
import org.bukkit.WorldType
import org.bukkit.entity.Player
import org.bukkit.entity.Sheep
import java.io.File
import java.util.UUID
import java.util.function.Consumer
import kotlin.math.floor

object FarmWorldLifecycle {
    private val initializedManagedWorldIdsByName = mutableMapOf<String, UUID>()
    private val managedWorldStateInitializing = mutableSetOf<String>()
    private val pendingManagedWorldStateCallbacks = mutableMapOf<String, MutableList<Consumer<World?>>>()
    private val farmWorldsInitializing = mutableSetOf<String>()
    private val pendingFarmWorldCallbacks = mutableMapOf<String, MutableList<Consumer<World?>>>()
    private val playerFarmLoadLock = Any()
    private val playersWithFarmLoadInProgress = mutableSetOf<UUID>()
    private val farmLoadTimeoutTaskIdByPlayer = mutableMapOf<UUID, Int>()
    private const val PLAYER_FARM_LOAD_TIMEOUT_TICKS = 25L * 20L

    @JvmStatic
    fun getWorldName(playerId: UUID): String = "sheepfarm_${playerId.toString().replace("-", "")}"

    @JvmStatic
    fun ensureFarmWorld(worldName: String): World? {
        val world = Bukkit.getWorld(worldName)
        if (world != null) {
            initializeManagedWorldState(world, false)
            return world
        }
        return createFlatWorld(worldName)
    }

    @JvmStatic
    fun ensureFarmWorldAsync(worldName: String?, callback: Consumer<World?>?) {
        if (worldName.isNullOrBlank()) {
            callback?.accept(null)
            return
        }
        val existing = Bukkit.getWorld(worldName)
        if (existing != null) {
            initializeManagedWorldState(existing, false) { readyWorld -> callback?.accept(readyWorld) }
            return
        }

        synchronized(pendingFarmWorldCallbacks) {
            if (callback != null) {
                pendingFarmWorldCallbacks.computeIfAbsent(worldName) { mutableListOf() }.add(callback)
            }
            if (farmWorldsInitializing.contains(worldName)) {
                return
            }
            farmWorldsInitializing.add(worldName)
        }

        val plugin = SheepMergePlugin.instance
        if (plugin == null) {
            completeFarmWorldAsync(worldName, null)
            return
        }

        Bukkit.getScheduler().runTask(
            plugin,
            Runnable {
                val world = ensureFarmWorld(worldName)
                initializeManagedWorldState(world, true) { readyWorld ->
                    completeFarmWorldAsync(worldName, readyWorld)
                }
            }
        )
    }

    @JvmStatic
    fun teleportPlayerToConfiguredSpawnAsync(
        player: Player?,
        world: World?,
        onSuccess: Runnable?,
        failureMessage: String?
    ) {
        teleportPlayerToConfiguredSpawnAsync(player, world, onSuccess, failureMessage, null)
    }

    @JvmStatic
    fun teleportPlayerToConfiguredSpawnAsync(
        player: Player?,
        world: World?,
        onSuccess: Runnable?,
        failureMessage: String?,
        onComplete: Runnable?
    ) {
        if (player == null || world == null) {
            onComplete?.run()
            return
        }
        player.teleportAsync(getConfiguredFarmTeleportLocation(world)).whenComplete { success, throwable ->
            val plugin = SheepMergePlugin.instance
            if (plugin == null) {
                onComplete?.run()
                return@whenComplete
            }
            Bukkit.getScheduler().runTask(
                plugin,
                Runnable {
                    if (!player.isOnline) {
                        onComplete?.run()
                        return@Runnable
                    }
                    if (throwable == null && success == true) {
                        onSuccess?.run()
                    } else if (!failureMessage.isNullOrBlank()) {
                        player.sendMessage(failureMessage)
                    }
                    onComplete?.run()
                }
            )
        }
    }

    @JvmStatic
    fun beginFarmLoadForPlayer(player: Player?): Boolean {
        if (player == null) {
            return false
        }
        val playerId = player.uniqueId
        synchronized(playerFarmLoadLock) {
            if (playersWithFarmLoadInProgress.contains(playerId)) {
                return false
            }
            playersWithFarmLoadInProgress.add(playerId)
        }

        val plugin = SheepMergePlugin.instance
        if (plugin != null) {
            val taskId = Bukkit.getScheduler().scheduleSyncDelayedTask(
                plugin,
                { finishFarmLoadForPlayer(playerId, true) },
                PLAYER_FARM_LOAD_TIMEOUT_TICKS
            )
            synchronized(playerFarmLoadLock) {
                farmLoadTimeoutTaskIdByPlayer[playerId] = taskId
            }
        }
        return true
    }

    @JvmStatic
    fun finishFarmLoadForPlayer(playerId: UUID?, timedOut: Boolean) {
        if (playerId == null) {
            return
        }

        val taskId: Int?
        val removed: Boolean
        synchronized(playerFarmLoadLock) {
            removed = playersWithFarmLoadInProgress.remove(playerId)
            taskId = farmLoadTimeoutTaskIdByPlayer.remove(playerId)
        }
        if (!removed) {
            return
        }

        if (taskId != null && SheepMergePlugin.instance != null) {
            Bukkit.getScheduler().cancelTask(taskId)
        }

        if (!timedOut) {
            return
        }
        val onlinePlayer = Bukkit.getPlayer(playerId)
        if (onlinePlayer != null && onlinePlayer.isOnline) {
            onlinePlayer.sendMessage("Farm loading took too long and was cancelled. Please try again.")
        }
    }

    private fun completeFarmWorldAsync(worldName: String, world: World?) {
        val callbacks: List<Consumer<World?>>?
        synchronized(pendingFarmWorldCallbacks) {
            farmWorldsInitializing.remove(worldName)
            callbacks = pendingFarmWorldCallbacks.remove(worldName)
        }
        callbacks?.forEach { callback -> callback.accept(world) }
    }

    @JvmStatic
    fun ensureFarmBuildWorld(): World? {
        val world = Bukkit.getWorld(SheepMergeManager.getFarmBuildWorldName())
        if (world != null) {
            initializeManagedWorldState(world, !SheepMergeManager.hasSavedFarmLayout())
            return world
        }
        return createFlatWorld(SheepMergeManager.getFarmBuildWorldName())
    }

    @JvmStatic
    fun invalidateManagedWorldInitialization(worldName: String?) {
        if (worldName.isNullOrBlank()) {
            return
        }

        val managedCallbacks: List<Consumer<World?>>?
        synchronized(pendingManagedWorldStateCallbacks) {
            initializedManagedWorldIdsByName.remove(worldName)
            managedWorldStateInitializing.remove(worldName)
            managedCallbacks = pendingManagedWorldStateCallbacks.remove(worldName)
        }
        managedCallbacks?.forEach { callback -> callback.accept(null) }

        val loadCallbacks: List<Consumer<World?>>?
        synchronized(pendingFarmWorldCallbacks) {
            farmWorldsInitializing.remove(worldName)
            loadCallbacks = pendingFarmWorldCallbacks.remove(worldName)
        }
        loadCallbacks?.forEach { callback -> callback.accept(null) }
    }

    @JvmStatic
    fun applyFarmRulesToLoadedWorlds() {
        Bukkit.getWorlds().forEach(::applyFarmWorldRules)
    }

    @JvmStatic
    fun ensureTutorialWorld(playerId: UUID?): World? {
        if (playerId == null) {
            return null
        }
        return ensureFarmWorld(SheepMergeManager.getTutorialWorldName(playerId))
    }

    @JvmStatic
    fun teleportToFarmWorld(player: Player?): Boolean {
        if (player == null) {
            return false
        }
        val world = ensureFarmWorld(getWorldName(player.uniqueId)) ?: return false
        applyConfiguredSpawn(world)
        player.teleportAsync(getConfiguredFarmTeleportLocation(world))
        return true
    }

    @JvmStatic
    fun teleportToTutorialWorld(player: Player?): Boolean {
        if (player == null) {
            return false
        }
        val world = ensureTutorialWorld(player.uniqueId) ?: return false
        applyConfiguredSpawn(world)
        player.teleportAsync(getConfiguredFarmTeleportLocation(world))
        return true
    }

    private fun createFlatWorld(worldName: String): World? {
        val existing = Bukkit.getWorld(worldName)
        if (existing != null) {
            initializeManagedWorldState(existing, false)
            return existing
        }
        val copiedCachedStructure = SheepMergeManager.prepareTransientWorldStructure(worldName)
        val creator = WorldCreator(worldName)
        creator.type(WorldType.FLAT)
        creator.generateStructures(false)
        creator.generatorSettings(
            "{" +
                "\"layers\": [{\"block\": \"minecraft:air\", \"height\": 1}]," +
                "\"biome\": \"minecraft:plains\"" +
                "}"
        )
        val world = Bukkit.createWorld(creator) ?: return null
        initializeManagedWorldState(world, !copiedCachedStructure)
        return world
    }

    private fun initializeManagedWorldState(world: World?, applyDefaultLayoutWhenMissing: Boolean) {
        initializeManagedWorldState(world, applyDefaultLayoutWhenMissing, null)
    }

    private fun initializeManagedWorldState(
        world: World?,
        applyDefaultLayoutWhenMissing: Boolean,
        onReady: Consumer<World?>?
    ) {
        if (world == null) {
            onReady?.accept(null)
            return
        }

        applyFarmWorldRules(world)
        ensureWorldStorageFolders(world)

        val managedWorldName = world.name
        if (managedWorldName.isBlank()) {
            onReady?.accept(world)
            return
        }

        val worldId = world.uid
        synchronized(pendingManagedWorldStateCallbacks) {
            var initializedWorldId = initializedManagedWorldIdsByName[managedWorldName]
            if (initializedWorldId != null && initializedWorldId != worldId) {
                initializedManagedWorldIdsByName.remove(managedWorldName)
                initializedWorldId = null
            }

            if (worldId == initializedWorldId && !applyDefaultLayoutWhenMissing) {
                onReady?.accept(world)
                return
            }

            if (onReady != null) {
                pendingManagedWorldStateCallbacks.computeIfAbsent(managedWorldName) { mutableListOf() }.add(onReady)
            }

            if (managedWorldStateInitializing.contains(managedWorldName)) {
                return
            }
            managedWorldStateInitializing.add(managedWorldName)
        }

        if (SheepMergePlugin.instance == null) {
            initializeManagedWorldStateImmediate(world, applyDefaultLayoutWhenMissing, managedWorldName, worldId)
            return
        }

        if (SheepMergeManager.isSheepFarmWorld(world)) {
            val needsBootstrap = SheepMergeManager.needsFarmLayoutBootstrap(world)
            val postLayout = Runnable {
                if (world.getEntitiesByClass(Sheep::class.java).isEmpty()) {
                    SheepMergeManager.restoreSavedSheepForWorldAsync(world) {
                        completeManagedWorldInitialization(managedWorldName, worldId)
                    }
                    return@Runnable
                }
                completeManagedWorldInitialization(managedWorldName, worldId)
            }

            if (applyDefaultLayoutWhenMissing || needsBootstrap) {
                SheepMergeManager.applyFarmLayout(world)
                postLayout.run()
            } else {
                postLayout.run()
            }
            return
        }

        if (SheepMergeManager.isFarmBuildWorld(world)) {
            val needsBootstrap = SheepMergeManager.needsFarmLayoutBootstrap(world)
            if (!applyDefaultLayoutWhenMissing && !needsBootstrap) {
                completeManagedWorldInitialization(managedWorldName, worldId)
                return
            }
            SheepMergeManager.applyFarmLayout(world)
            completeManagedWorldInitialization(managedWorldName, worldId)
            return
        }

        completeManagedWorldInitialization(managedWorldName, worldId)
    }

    private fun initializeManagedWorldStateImmediate(
        world: World?,
        applyDefaultLayoutWhenMissing: Boolean,
        managedWorldName: String?,
        worldId: UUID?
    ) {
        if (world == null || managedWorldName.isNullOrBlank() || worldId == null) {
            completeManagedWorldInitialization(managedWorldName, worldId)
            return
        }

        if (SheepMergeManager.isSheepFarmWorld(world)) {
            val needsBootstrap = SheepMergeManager.needsFarmLayoutBootstrap(world)
            val postLayout = Runnable {
                if (world.getEntitiesByClass(Sheep::class.java).isEmpty()) {
                    SheepMergeManager.restoreSavedSheepForWorld(world)
                }
                completeManagedWorldInitialization(managedWorldName, worldId)
            }

            if (applyDefaultLayoutWhenMissing || needsBootstrap) {
                SheepMergeManager.applyFarmLayout(world)
            }
            postLayout.run()
        } else if (SheepMergeManager.isFarmBuildWorld(world)) {
            if (applyDefaultLayoutWhenMissing || SheepMergeManager.needsFarmLayoutBootstrap(world)) {
                SheepMergeManager.applyFarmLayout(world)
            }
            completeManagedWorldInitialization(managedWorldName, worldId)
        } else {
            completeManagedWorldInitialization(managedWorldName, worldId)
        }
    }

    private fun completeManagedWorldInitialization(managedWorldName: String?, worldId: UUID?) {
        if (managedWorldName.isNullOrBlank()) {
            return
        }

        var loadedWorld = Bukkit.getWorld(managedWorldName)
        if (loadedWorld == null || (worldId != null && worldId != loadedWorld.uid)) {
            loadedWorld = null
        }

        val callbacks: List<Consumer<World?>>?
        synchronized(pendingManagedWorldStateCallbacks) {
            managedWorldStateInitializing.remove(managedWorldName)
            if (loadedWorld != null) {
                initializedManagedWorldIdsByName[managedWorldName] = loadedWorld.uid
            }
            callbacks = pendingManagedWorldStateCallbacks.remove(managedWorldName)
        }
        callbacks?.forEach { callback -> callback.accept(loadedWorld) }
    }

    private fun ensureWorldStorageFolders(world: World?) {
        val worldFolder = world?.worldFolder ?: return
        val dataFolder = File(worldFolder, "data")
        val regionFolder = File(worldFolder, "region")
        if (!dataFolder.exists()) {
            dataFolder.mkdirs()
        }
        if (!regionFolder.exists()) {
            regionFolder.mkdirs()
        }
    }

    private fun applyFarmWorldRules(world: World?) {
        if (world == null ||
            (!SheepMergeManager.isSheepFarmWorld(world) && !SheepMergeManager.isFarmBuildWorld(world))
        ) {
            return
        }
        world.pvp = false
        world.difficulty = Difficulty.PEACEFUL
        world.setStorm(false)
        world.isThundering = false
        world.weatherDuration = 0
        world.clearWeatherDuration = Int.MAX_VALUE
        world.worldBorder.setCenter(
            SheepMergeManager.getFarmWorldCenterX(),
            SheepMergeManager.getFarmWorldCenterZ()
        )
        world.worldBorder.size = SheepMergeManager.getFarmWorldBorderSizeBlocks()
        world.worldBorder.warningDistance = 0
        world.worldBorder.warningTime = 0
    }

    @JvmStatic
    fun getConfiguredFarmTeleportLocation(world: World): Location {
        val configuration = SheepMergeConfiguration.get()
        val x = configuration?.farmTeleportX ?: 0.5
        val y = configuration?.farmTeleportY ?: 101.0
        val z = configuration?.farmTeleportZ ?: 0.5
        return Location(world, x, y, z)
    }

    @JvmStatic
    fun applyConfiguredSpawn(world: World?) {
        if (world == null) {
            return
        }
        val configuration = SheepMergeConfiguration.get()
        val x = configuration?.farmTeleportX ?: 0.5
        val y = configuration?.farmTeleportY ?: 101.0
        val z = configuration?.farmTeleportZ ?: 0.5
        world.setSpawnLocation(floor(x).toInt(), floor(y).toInt(), floor(z).toInt())
    }
}