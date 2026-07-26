package dev.x208.sheepmerge

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Sheep
import org.bukkit.scheduler.BukkitRunnable
import java.util.UUID

object SheepSnapshotState {
    private const val FARM_SHEEP_KEY = "farmSheep"
    private const val TUTORIAL_SHEEP_KEY = "tutorialSheep"
    private const val RESTORE_BATCH_SIZE = 25

    data class SheepSnapshot(
        val tierLevel: Int,
        val x: Double,
        val y: Double,
        val z: Double,
        val sheared: Boolean,
        val nextEatAt: Long,
        val mergedCount: Int,
    )

    private val savedFarmSheepByPlayer = HashMap<UUID, List<SheepSnapshot>>()
    private val savedTutorialSheepByPlayer = HashMap<UUID, List<SheepSnapshot>>()

    @JvmStatic
    fun saveForWorld(world: World?) {
        if (world == null || !SheepMergeManager.isSheepFarmWorld(world)) return
        val ownerId = SheepMergeManager.getOwnerId(world) ?: return
        snapshotsFor(world)[ownerId] = capture(world)
    }

    @JvmStatic
    fun restoreForWorld(world: World?) {
        if (world == null || !SheepMergeManager.isSheepFarmWorld(world)) return
        val ownerId = SheepMergeManager.getOwnerId(world) ?: return
        val snapshots = snapshotsFor(world)[ownerId]
        if (snapshots.isNullOrEmpty()) {
            SheepMergeManager.refreshLiveSheepCount(world)
            return
        }

        val now = System.currentTimeMillis()
        snapshots.forEach { restoreSnapshot(world, it, now) }
        SheepMergeManager.refreshLiveSheepCount(world)
    }

    @JvmStatic
    fun restoreForWorldAsync(world: World?, onComplete: Runnable?) {
        val plugin = SheepMergePlugin.instance
        if (plugin == null || world == null || !SheepMergeManager.isSheepFarmWorld(world)) {
            restoreForWorld(world)
            onComplete?.run()
            return
        }
        val ownerId = SheepMergeManager.getOwnerId(world)
        if (ownerId == null) {
            SheepMergeManager.refreshLiveSheepCount(world)
            onComplete?.run()
            return
        }
        val snapshots = snapshotsFor(world)[ownerId]
        if (snapshots.isNullOrEmpty()) {
            SheepMergeManager.refreshLiveSheepCount(world)
            onComplete?.run()
            return
        }

        val now = System.currentTimeMillis()
        val worldId = world.uid
        object : BukkitRunnable() {
            private var index = 0

            override fun run() {
                if (Bukkit.getWorld(worldId) == null || !SheepMergeManager.isSheepFarmWorld(world)) {
                    cancel()
                    onComplete?.run()
                    return
                }
                var processed = 0
                while (index < snapshots.size && processed < RESTORE_BATCH_SIZE) {
                    restoreSnapshot(world, snapshots[index++], now)
                    processed++
                }
                if (index >= snapshots.size) {
                    SheepMergeManager.refreshLiveSheepCount(world)
                    cancel()
                    onComplete?.run()
                }
            }
        }.runTaskTimer(plugin, 1L, 1L)
    }

    @JvmStatic
    fun migrateTutorialToFarm(playerId: UUID?, tutorialWorld: World?) {
        if (playerId == null || tutorialWorld == null) return
        savedFarmSheepByPlayer[playerId] = capture(tutorialWorld)
        savedTutorialSheepByPlayer.remove(playerId)
    }

    @JvmStatic
    fun removeTutorial(playerId: UUID?) {
        if (playerId != null) savedTutorialSheepByPlayer.remove(playerId)
    }

    @JvmStatic
    fun resetPlayer(playerId: UUID?) {
        if (playerId == null) return
        savedFarmSheepByPlayer.remove(playerId)
        savedTutorialSheepByPlayer.remove(playerId)
    }

    @JvmStatic
    fun farmOwnerIds(): Set<UUID> = HashSet(savedFarmSheepByPlayer.keys)

    @JvmStatic
    fun clear() {
        savedFarmSheepByPlayer.clear()
        savedTutorialSheepByPlayer.clear()
    }

    @JvmStatic
    fun clearPersistedKeys(dataConfig: FileConfiguration) {
        dataConfig.set(FARM_SHEEP_KEY, null)
        dataConfig.set(TUTORIAL_SHEEP_KEY, null)
    }

    @JvmStatic
    fun saveTo(dataConfig: FileConfiguration) {
        saveSnapshots(dataConfig, FARM_SHEEP_KEY, savedFarmSheepByPlayer)
        saveSnapshots(dataConfig, TUTORIAL_SHEEP_KEY, savedTutorialSheepByPlayer)
    }

    @JvmStatic
    fun loadFrom(dataConfig: FileConfiguration) {
        loadSnapshots(dataConfig, FARM_SHEEP_KEY, savedFarmSheepByPlayer)
        loadSnapshots(dataConfig, TUTORIAL_SHEEP_KEY, savedTutorialSheepByPlayer)
    }

    private fun capture(world: World): List<SheepSnapshot> = world.getEntitiesByClass(Sheep::class.java)
        .asSequence()
        .filter { it.isValid && !it.isDead }
        .map { sheep ->
            val tier = SheepMergeManager.getSheepTier(sheep)
            val location = sheep.location
            SheepSnapshot(
                tier?.level ?: SheepTier.WHITE.level,
                location.x,
                location.y,
                location.z,
                sheep.isSheared,
                SheepMergeManager.getNextEatTimestamp(sheep),
                SheepMergeManager.getRainbowTier(sheep).coerceAtLeast(1),
            )
        }
        .toList()

    private fun restoreSnapshot(world: World, snapshot: SheepSnapshot, now: Long) {
        val sheep = world.spawn(Location(world, snapshot.x, snapshot.y, snapshot.z), Sheep::class.java)
        SheepMergeManager.setSheepTier(sheep, SheepTier.byLevel(snapshot.tierLevel))
        SheepMergeManager.setRainbowTier(sheep, snapshot.mergedCount)
        sheep.setAdult()
        if (snapshot.sheared && snapshot.nextEatAt > now) {
            sheep.isSheared = true
            SheepMergeManager.setNextEatTimestamp(sheep, snapshot.nextEatAt)
        } else {
            sheep.isSheared = false
            SheepMergeManager.setNextEatTimestamp(sheep, 0L)
        }
        SheepMergeManager.updateSheepName(sheep)
    }

    private fun snapshotsFor(world: World): MutableMap<UUID, List<SheepSnapshot>> =
        if (SheepMergeManager.isTutorialWorld(world)) savedTutorialSheepByPlayer else savedFarmSheepByPlayer

    private fun saveSnapshots(
        dataConfig: FileConfiguration,
        basePath: String,
        snapshotsByPlayer: Map<UUID, List<SheepSnapshot>>,
    ) {
        snapshotsByPlayer.forEach { (playerId, snapshots) ->
            snapshots.forEachIndexed { index, snapshot ->
                val path = "$basePath.$playerId.$index"
                dataConfig.set("$path.tier", snapshot.tierLevel)
                dataConfig.set("$path.x", snapshot.x)
                dataConfig.set("$path.y", snapshot.y)
                dataConfig.set("$path.z", snapshot.z)
                dataConfig.set("$path.sheared", snapshot.sheared)
                dataConfig.set("$path.nextEatAt", snapshot.nextEatAt)
                dataConfig.set("$path.mergedCount", snapshot.mergedCount)
            }
        }
    }

    private fun loadSnapshots(
        dataConfig: FileConfiguration,
        basePath: String,
        snapshotsByPlayer: MutableMap<UUID, List<SheepSnapshot>>,
    ) {
        val root = dataConfig.getConfigurationSection(basePath) ?: return
        root.getKeys(false).forEach { key ->
            val playerId = runCatching { UUID.fromString(key) }.getOrNull() ?: return@forEach
            val playerSection = root.getConfigurationSection(key) ?: return@forEach
            val snapshots = playerSection.getKeys(false).sorted().map { indexKey ->
                val path = "$basePath.$key.$indexKey"
                SheepSnapshot(
                    dataConfig.getInt("$path.tier", SheepTier.WHITE.level),
                    dataConfig.getDouble("$path.x", 0.5),
                    dataConfig.getDouble("$path.y", SheepMergeManager.getFarmBaseY() + 1.0),
                    dataConfig.getDouble("$path.z", 0.5),
                    dataConfig.getBoolean("$path.sheared", false),
                    dataConfig.getLong("$path.nextEatAt", 0L).coerceAtLeast(0L),
                    dataConfig.getInt("$path.mergedCount", 1).coerceAtLeast(1),
                )
            }
            snapshotsByPlayer[playerId] = snapshots
        }
    }
}