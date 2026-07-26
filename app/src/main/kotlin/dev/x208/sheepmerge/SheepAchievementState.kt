package dev.x208.sheepmerge

import org.bukkit.configuration.file.FileConfiguration
import java.util.LinkedHashSet
import java.util.UUID
import java.util.function.Predicate

object SheepAchievementState {

    private const val AUTOMATION_POINTS_GRANTED_KEY = "achievementAutomationPointsGranted"
    private const val UNLOCKED_ACHIEVEMENTS_KEY = "achievementUnlocked"
    private const val UNLOCKED_MILESTONES_KEY = "achievementMilestonesUnlocked"

    private val achievementAutomationPointsGrantedByPlayer: MutableMap<UUID, Int> = HashMap()
    private val unlockedAchievementIdsByPlayer: MutableMap<UUID, MutableSet<String>> = HashMap()
    private val unlockedAchievementMilestoneIdsByPlayer: MutableMap<UUID, MutableSet<String>> = HashMap()

    @JvmStatic
    fun getUnlockedAchievementIds(playerId: UUID?): Set<String> {
        return playerId?.let { unlockedAchievementIdsByPlayer[it] } ?: emptySet()
    }

    @JvmStatic
    fun getOrCreateUnlockedAchievementIds(playerId: UUID): MutableSet<String> {
        return unlockedAchievementIdsByPlayer.getOrPut(playerId) { LinkedHashSet() }
    }

    @JvmStatic
    fun getUnlockedAchievementMilestoneIds(playerId: UUID?): Set<String> {
        return playerId?.let { unlockedAchievementMilestoneIdsByPlayer[it] } ?: emptySet()
    }

    @JvmStatic
    fun getOrCreateUnlockedAchievementMilestoneIds(playerId: UUID): MutableSet<String> {
        return unlockedAchievementMilestoneIdsByPlayer.getOrPut(playerId) { LinkedHashSet() }
    }

    @JvmStatic
    fun getAutomationPointsGranted(playerId: UUID?): Int {
        return playerId?.let { achievementAutomationPointsGrantedByPlayer[it] } ?: 0
    }

    @JvmStatic
    fun setAutomationPointsGranted(playerId: UUID?, points: Int) {
        if (playerId != null) {
            achievementAutomationPointsGrantedByPlayer[playerId] = points.coerceAtLeast(0)
        }
    }

    @JvmStatic
    fun removeAutomationPointsGranted(playerId: UUID?) {
        if (playerId != null) {
            achievementAutomationPointsGrantedByPlayer.remove(playerId)
        }
    }

    @JvmStatic
    fun getTrackedPlayerIds(): Set<UUID> {
        val playerIds = HashSet<UUID>()
        playerIds.addAll(unlockedAchievementIdsByPlayer.keys)
        playerIds.addAll(achievementAutomationPointsGrantedByPlayer.keys)
        return playerIds
    }

    @JvmStatic
    fun resetPlayer(playerId: UUID?) {
        if (playerId == null) {
            return
        }
        achievementAutomationPointsGrantedByPlayer.remove(playerId)
        unlockedAchievementIdsByPlayer.remove(playerId)
        unlockedAchievementMilestoneIdsByPlayer.remove(playerId)
    }

    @JvmStatic
    fun clear() {
        achievementAutomationPointsGrantedByPlayer.clear()
        unlockedAchievementIdsByPlayer.clear()
        unlockedAchievementMilestoneIdsByPlayer.clear()
    }

    @JvmStatic
    fun clearPersistedKeys(dataConfig: FileConfiguration?) {
        if (dataConfig == null) {
            return
        }
        dataConfig.set(AUTOMATION_POINTS_GRANTED_KEY, null)
        dataConfig.set(UNLOCKED_ACHIEVEMENTS_KEY, null)
        dataConfig.set(UNLOCKED_MILESTONES_KEY, null)
    }

    @JvmStatic
    fun saveTo(dataConfig: FileConfiguration?) {
        if (dataConfig == null) {
            return
        }
        for ((playerId, points) in achievementAutomationPointsGrantedByPlayer) {
            dataConfig.set("$AUTOMATION_POINTS_GRANTED_KEY.$playerId", points)
        }
        saveUnlockedIds(dataConfig, UNLOCKED_ACHIEVEMENTS_KEY, unlockedAchievementIdsByPlayer)
        saveUnlockedIds(dataConfig, UNLOCKED_MILESTONES_KEY, unlockedAchievementMilestoneIdsByPlayer)
    }

    @JvmStatic
    fun loadFrom(
        dataConfig: FileConfiguration?,
        isValidAchievementId: Predicate<String>,
        isValidMilestoneId: Predicate<String>
    ) {
        if (dataConfig == null) {
            return
        }
        loadAutomationPointsGranted(dataConfig)
        loadUnlockedIds(dataConfig, UNLOCKED_ACHIEVEMENTS_KEY, isValidAchievementId, unlockedAchievementIdsByPlayer)
        loadUnlockedIds(dataConfig, UNLOCKED_MILESTONES_KEY, isValidMilestoneId, unlockedAchievementMilestoneIdsByPlayer)
    }

    private fun saveUnlockedIds(
        dataConfig: FileConfiguration,
        key: String,
        unlockedIdsByPlayer: Map<UUID, Set<String>>
    ) {
        for ((playerId, unlockedIds) in unlockedIdsByPlayer) {
            if (unlockedIds.isNotEmpty()) {
                dataConfig.set("$key.$playerId", ArrayList(unlockedIds))
            }
        }
    }

    private fun loadAutomationPointsGranted(dataConfig: FileConfiguration) {
        dataConfig.getConfigurationSection(AUTOMATION_POINTS_GRANTED_KEY)?.getKeys(false)?.forEach { key ->
            try {
                val playerId = UUID.fromString(key)
                achievementAutomationPointsGrantedByPlayer[playerId] =
                    dataConfig.getInt("$AUTOMATION_POINTS_GRANTED_KEY.$key", 0).coerceAtLeast(0)
            } catch (_: IllegalArgumentException) {
                // Ignore invalid UUIDs.
            }
        }
    }

    private fun loadUnlockedIds(
        dataConfig: FileConfiguration,
        key: String,
        isValidId: Predicate<String>,
        unlockedIdsByPlayer: MutableMap<UUID, MutableSet<String>>
    ) {
        dataConfig.getConfigurationSection(key)?.getKeys(false)?.forEach { playerKey ->
            try {
                val playerId = UUID.fromString(playerKey)
                val normalized = LinkedHashSet<String>()
                for (id in dataConfig.getStringList("$key.$playerKey")) {
                    if (isValidId.test(id)) {
                        normalized.add(id)
                    }
                }
                if (normalized.isNotEmpty()) {
                    unlockedIdsByPlayer[playerId] = normalized
                }
            } catch (_: IllegalArgumentException) {
                // Ignore invalid UUIDs.
            }
        }
    }
}