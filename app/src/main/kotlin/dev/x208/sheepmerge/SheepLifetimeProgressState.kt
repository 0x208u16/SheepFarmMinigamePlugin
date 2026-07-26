package dev.x208.sheepmerge

import org.bukkit.configuration.file.FileConfiguration
import java.util.UUID

object SheepLifetimeProgressState {
    private const val LIFETIME_SHEARS_KEY = "lifetimeShears"
    private const val LIFETIME_SPAWNS_KEY = "lifetimeSpawns"
    private const val LIFETIME_MERGES_KEY = "lifetimeMerges"
    private const val LIFETIME_OTHER_FARM_VISITS_KEY = "lifetimeOtherFarmVisits"
    private const val VISITED_OWNER_FARM_KEY = "visitedOwnerFarm"
    private const val COMPLETED_QUEST_CYCLES_KEY = "completedQuestCycles"

    private val lifetimeShearsByPlayer = HashMap<UUID, Int>()
    private val lifetimeSpawnsByPlayer = HashMap<UUID, Int>()
    private val lifetimeMergesByPlayer = HashMap<UUID, Int>()
    private val lifetimeOtherFarmVisitsByPlayer = HashMap<UUID, Int>()
    private val visitedOwnerFarmByPlayer = HashMap<UUID, Boolean>()
    private val completedQuestCyclesByPlayer = HashMap<UUID, Int>()

    @JvmStatic
    fun getLifetimeShears(playerId: UUID): Int = lifetimeShearsByPlayer.getOrDefault(playerId, 0)

    @JvmStatic
    fun setLifetimeShears(playerId: UUID, value: Int) {
        lifetimeShearsByPlayer[playerId] = value.coerceAtLeast(0)
    }

    @JvmStatic
    fun incrementLifetimeShears(playerId: UUID): Int = increment(lifetimeShearsByPlayer, playerId)

    @JvmStatic
    fun getLifetimeSpawns(playerId: UUID): Int = lifetimeSpawnsByPlayer.getOrDefault(playerId, 0)

    @JvmStatic
    fun setLifetimeSpawns(playerId: UUID, value: Int) {
        lifetimeSpawnsByPlayer[playerId] = value.coerceAtLeast(0)
    }

    @JvmStatic
    fun incrementLifetimeSpawns(playerId: UUID): Int = increment(lifetimeSpawnsByPlayer, playerId)

    @JvmStatic
    fun getLifetimeMerges(playerId: UUID): Int = lifetimeMergesByPlayer.getOrDefault(playerId, 0)

    @JvmStatic
    fun setLifetimeMerges(playerId: UUID, value: Int) {
        lifetimeMergesByPlayer[playerId] = value.coerceAtLeast(0)
    }

    @JvmStatic
    fun incrementLifetimeMerges(playerId: UUID): Int = increment(lifetimeMergesByPlayer, playerId)

    @JvmStatic
    fun getLifetimeOtherFarmVisits(playerId: UUID): Int =
        lifetimeOtherFarmVisitsByPlayer.getOrDefault(playerId, 0)

    @JvmStatic
    fun setLifetimeOtherFarmVisits(playerId: UUID, value: Int) {
        lifetimeOtherFarmVisitsByPlayer[playerId] = value.coerceAtLeast(0)
    }

    @JvmStatic
    fun incrementLifetimeOtherFarmVisits(playerId: UUID): Int =
        increment(lifetimeOtherFarmVisitsByPlayer, playerId)

    @JvmStatic
    fun hasVisitedOwnerFarm(playerId: UUID): Boolean = visitedOwnerFarmByPlayer.getOrDefault(playerId, false)

    @JvmStatic
    fun setVisitedOwnerFarm(playerId: UUID, visited: Boolean) {
        if (visited) {
            visitedOwnerFarmByPlayer[playerId] = true
        } else {
            visitedOwnerFarmByPlayer.remove(playerId)
        }
    }

    @JvmStatic
    fun getCompletedQuestCycles(playerId: UUID): Int = completedQuestCyclesByPlayer.getOrDefault(playerId, 0)

    @JvmStatic
    fun setCompletedQuestCycles(playerId: UUID, value: Int) {
        completedQuestCyclesByPlayer[playerId] = value.coerceAtLeast(0)
    }

    @JvmStatic
    fun incrementCompletedQuestCycles(playerId: UUID): Int = increment(completedQuestCyclesByPlayer, playerId)

    @JvmStatic
    fun resetPlayer(playerId: UUID) {
        lifetimeShearsByPlayer.remove(playerId)
        lifetimeSpawnsByPlayer.remove(playerId)
        lifetimeMergesByPlayer.remove(playerId)
        lifetimeOtherFarmVisitsByPlayer.remove(playerId)
        visitedOwnerFarmByPlayer.remove(playerId)
        completedQuestCyclesByPlayer.remove(playerId)
    }

    @JvmStatic
    fun clear() {
        lifetimeShearsByPlayer.clear()
        lifetimeSpawnsByPlayer.clear()
        lifetimeMergesByPlayer.clear()
        lifetimeOtherFarmVisitsByPlayer.clear()
        visitedOwnerFarmByPlayer.clear()
        completedQuestCyclesByPlayer.clear()
    }

    @JvmStatic
    fun clearPersistedKeys(configuration: FileConfiguration) {
        persistedKeys.forEach { configuration.set(it, null) }
    }

    @JvmStatic
    fun saveTo(configuration: FileConfiguration) {
        saveIntMap(configuration, LIFETIME_SHEARS_KEY, lifetimeShearsByPlayer)
        saveIntMap(configuration, LIFETIME_SPAWNS_KEY, lifetimeSpawnsByPlayer)
        saveIntMap(configuration, LIFETIME_MERGES_KEY, lifetimeMergesByPlayer)
        saveIntMap(configuration, LIFETIME_OTHER_FARM_VISITS_KEY, lifetimeOtherFarmVisitsByPlayer)
        visitedOwnerFarmByPlayer.forEach { (playerId, visited) ->
            if (visited) {
                configuration.set("$VISITED_OWNER_FARM_KEY.$playerId", true)
            }
        }
        saveIntMap(configuration, COMPLETED_QUEST_CYCLES_KEY, completedQuestCyclesByPlayer)
    }

    @JvmStatic
    fun loadFrom(configuration: FileConfiguration) {
        loadIntMap(configuration, LIFETIME_SHEARS_KEY, lifetimeShearsByPlayer)
        loadIntMap(configuration, LIFETIME_SPAWNS_KEY, lifetimeSpawnsByPlayer)
        loadIntMap(configuration, LIFETIME_MERGES_KEY, lifetimeMergesByPlayer)
        loadIntMap(configuration, LIFETIME_OTHER_FARM_VISITS_KEY, lifetimeOtherFarmVisitsByPlayer)
        loadKeys(configuration, VISITED_OWNER_FARM_KEY) { playerId, path ->
            if (configuration.getBoolean(path, false)) {
                visitedOwnerFarmByPlayer[playerId] = true
            }
        }
        loadIntMap(configuration, COMPLETED_QUEST_CYCLES_KEY, completedQuestCyclesByPlayer)
    }

    private fun increment(values: MutableMap<UUID, Int>, playerId: UUID): Int {
        val updated = (values.getOrDefault(playerId, 0).toLong() + 1L)
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()
        values[playerId] = updated
        return updated
    }

    private fun saveIntMap(configuration: FileConfiguration, key: String, values: Map<UUID, Int>) {
        values.forEach { (playerId, value) -> configuration.set("$key.$playerId", value.coerceAtLeast(0)) }
    }

    private fun loadIntMap(configuration: FileConfiguration, key: String, destination: MutableMap<UUID, Int>) {
        loadKeys(configuration, key) { playerId, path ->
            destination[playerId] = configuration.getInt(path, 0).coerceAtLeast(0)
        }
    }

    private inline fun loadKeys(
        configuration: FileConfiguration,
        key: String,
        load: (UUID, String) -> Unit,
    ) {
        val section = configuration.getConfigurationSection(key) ?: return
        section.getKeys(false).forEach { playerKey ->
            try {
                load(UUID.fromString(playerKey), "$key.$playerKey")
            } catch (_: IllegalArgumentException) {
                // Ignore invalid UUIDs.
            }
        }
    }

    private val persistedKeys = listOf(
        LIFETIME_SHEARS_KEY,
        LIFETIME_SPAWNS_KEY,
        LIFETIME_MERGES_KEY,
        LIFETIME_OTHER_FARM_VISITS_KEY,
        VISITED_OWNER_FARM_KEY,
        COMPLETED_QUEST_CYCLES_KEY,
    )
}
