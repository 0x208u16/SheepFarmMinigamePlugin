package dev.x208.sheepmerge

import org.bukkit.configuration.file.FileConfiguration
import java.util.UUID

object SheepPrestigeState {
    private const val LEVEL_KEY = "prestigeLevel"
    private const val POINTS_KEY = "prestigePoints"
    private const val DOUBLE_POINTS_KEY = "prestigeDoublePoints"
    private const val HIGHER_MAX_KEY = "prestigeHigherMax"
    private const val START_EGGS_KEY = "prestigeStartEggs"
    private const val EGG_CAP_KEY = "prestigeEggCap"
    private const val BASE_SPAWN_TIER_KEY = "prestigeBaseSpawnTier"
    private const val QUEST_REWARD_KEY = "prestigeQuestReward"
    private const val REFUND_COOLDOWN_KEY = "prestigeRefundCooldown"
    private const val TOTAL_LEVELS_EARNED_KEY = "totalPrestigeLevelsEarned"

    private val levelByPlayer: MutableMap<UUID, Int> = HashMap()
    private val pointsByPlayer: MutableMap<UUID, Int> = HashMap()
    private val doublePointsChanceByPlayer: MutableMap<UUID, Int> = HashMap()
    private val higherMaxLevelByPlayer: MutableMap<UUID, Int> = HashMap()
    private val startEggsByPlayer: MutableMap<UUID, Int> = HashMap()
    private val eggCapByPlayer: MutableMap<UUID, Int> = HashMap()
    private val baseSpawnTierByPlayer: MutableMap<UUID, Int> = HashMap()
    private val questRewardByPlayer: MutableMap<UUID, Int> = HashMap()
    private val nextRefundTimestampByPlayer: MutableMap<UUID, Long> = HashMap()
    private val totalLevelsEarnedByPlayer: MutableMap<UUID, Int> = HashMap()

    private val lastReminderTimestampByPlayer: MutableMap<UUID, Long> = HashMap()
    private val titleReminderShownByPlayer: MutableMap<UUID, Boolean> = HashMap()

    @JvmStatic fun getLevel(playerId: UUID?): Int = playerId?.let { levelByPlayer[it] } ?: 0
    @JvmStatic fun setLevel(playerId: UUID, level: Int) { levelByPlayer[playerId] = level }
    @JvmStatic fun removeLevel(playerId: UUID) { levelByPlayer.remove(playerId) }
    @JvmStatic fun getPoints(playerId: UUID?): Int = playerId?.let { pointsByPlayer[it] } ?: 0
    @JvmStatic fun setPoints(playerId: UUID, points: Int) { pointsByPlayer[playerId] = points }
    @JvmStatic fun removePoints(playerId: UUID) { pointsByPlayer.remove(playerId) }
    @JvmStatic fun getDoublePointsChance(playerId: UUID?): Int = playerId?.let { doublePointsChanceByPlayer[it] } ?: 0
    @JvmStatic fun setDoublePointsChance(playerId: UUID, level: Int) { doublePointsChanceByPlayer[playerId] = level }
    @JvmStatic fun getHigherMaxLevel(playerId: UUID?): Int = playerId?.let { higherMaxLevelByPlayer[it] } ?: 0
    @JvmStatic fun setHigherMaxLevel(playerId: UUID, level: Int) { higherMaxLevelByPlayer[playerId] = level }
    @JvmStatic fun getStartEggs(playerId: UUID?): Int = playerId?.let { startEggsByPlayer[it] } ?: 0
    @JvmStatic fun setStartEggs(playerId: UUID, level: Int) { startEggsByPlayer[playerId] = level }
    @JvmStatic fun getEggCap(playerId: UUID?): Int = playerId?.let { eggCapByPlayer[it] } ?: 0
    @JvmStatic fun setEggCap(playerId: UUID, level: Int) { eggCapByPlayer[playerId] = level }
    @JvmStatic fun getBaseSpawnTier(playerId: UUID?): Int = playerId?.let { baseSpawnTierByPlayer[it] } ?: 0
    @JvmStatic fun setBaseSpawnTier(playerId: UUID, level: Int) { baseSpawnTierByPlayer[playerId] = level }
    @JvmStatic fun getQuestReward(playerId: UUID?): Int = playerId?.let { questRewardByPlayer[it] } ?: 0
    @JvmStatic fun setQuestReward(playerId: UUID, level: Int) { questRewardByPlayer[playerId] = level }
    @JvmStatic fun getNextRefundTimestamp(playerId: UUID?): Long = playerId?.let { nextRefundTimestampByPlayer[it] } ?: 0L
    @JvmStatic fun setNextRefundTimestamp(playerId: UUID, timestamp: Long) { nextRefundTimestampByPlayer[playerId] = timestamp }
    @JvmStatic fun getTotalLevelsEarned(playerId: UUID?): Int = playerId?.let { totalLevelsEarnedByPlayer[it] } ?: 0
    @JvmStatic fun setTotalLevelsEarned(playerId: UUID, levels: Int) { totalLevelsEarnedByPlayer[playerId] = levels }
    @JvmStatic fun getHigherMaxTrackedPlayerIds(): Set<UUID> = HashSet(higherMaxLevelByPlayer.keys)

    @JvmStatic fun getLastReminderTimestamp(playerId: UUID?): Long = playerId?.let { lastReminderTimestampByPlayer[it] } ?: 0L
    @JvmStatic fun setLastReminderTimestamp(playerId: UUID, timestamp: Long) { lastReminderTimestampByPlayer[playerId] = timestamp }
    @JvmStatic fun isTitleReminderShown(playerId: UUID?): Boolean = playerId != null && titleReminderShownByPlayer.getOrDefault(playerId, false)
    @JvmStatic fun setTitleReminderShown(playerId: UUID, shown: Boolean) { titleReminderShownByPlayer[playerId] = shown }
    @JvmStatic fun clearReminder(playerId: UUID?) {
        if (playerId == null) return
        lastReminderTimestampByPlayer.remove(playerId)
        titleReminderShownByPlayer.remove(playerId)
    }

    @JvmStatic
    fun resetUpgrades(playerId: UUID?, clearRefundCooldown: Boolean) {
        if (playerId == null) return
        doublePointsChanceByPlayer.remove(playerId)
        higherMaxLevelByPlayer.remove(playerId)
        startEggsByPlayer.remove(playerId)
        eggCapByPlayer.remove(playerId)
        baseSpawnTierByPlayer.remove(playerId)
        questRewardByPlayer.remove(playerId)
        if (clearRefundCooldown) nextRefundTimestampByPlayer.remove(playerId)
    }

    @JvmStatic
    fun resetAdminPlayer(playerId: UUID?) {
        if (playerId == null) return
        levelByPlayer.remove(playerId)
        pointsByPlayer.remove(playerId)
        doublePointsChanceByPlayer.remove(playerId)
        higherMaxLevelByPlayer.remove(playerId)
        startEggsByPlayer.remove(playerId)
        eggCapByPlayer.remove(playerId)
        baseSpawnTierByPlayer.remove(playerId)
        nextRefundTimestampByPlayer.remove(playerId)
        lastReminderTimestampByPlayer.remove(playerId)
        totalLevelsEarnedByPlayer.remove(playerId)
    }

    @JvmStatic
    fun clearBeforeDataLoad() {
        levelByPlayer.clear()
        pointsByPlayer.clear()
        doublePointsChanceByPlayer.clear()
        higherMaxLevelByPlayer.clear()
        startEggsByPlayer.clear()
        eggCapByPlayer.clear()
        baseSpawnTierByPlayer.clear()
        questRewardByPlayer.clear()
        nextRefundTimestampByPlayer.clear()
        totalLevelsEarnedByPlayer.clear()
    }

    @JvmStatic
    fun clearPersistedKeys(dataConfig: FileConfiguration) {
        dataConfig.set(LEVEL_KEY, null)
        dataConfig.set(POINTS_KEY, null)
        dataConfig.set(DOUBLE_POINTS_KEY, null)
        dataConfig.set(HIGHER_MAX_KEY, null)
        dataConfig.set(START_EGGS_KEY, null)
        dataConfig.set(EGG_CAP_KEY, null)
        dataConfig.set(BASE_SPAWN_TIER_KEY, null)
        dataConfig.set(QUEST_REWARD_KEY, null)
        dataConfig.set(REFUND_COOLDOWN_KEY, null)
        dataConfig.set(TOTAL_LEVELS_EARNED_KEY, null)
    }

    @JvmStatic
    fun saveTo(dataConfig: FileConfiguration) {
        saveMap(dataConfig, LEVEL_KEY, levelByPlayer)
        saveMap(dataConfig, POINTS_KEY, pointsByPlayer)
        saveMap(dataConfig, DOUBLE_POINTS_KEY, doublePointsChanceByPlayer)
        saveMap(dataConfig, HIGHER_MAX_KEY, higherMaxLevelByPlayer)
        saveMap(dataConfig, START_EGGS_KEY, startEggsByPlayer)
        saveMap(dataConfig, EGG_CAP_KEY, eggCapByPlayer)
        saveMap(dataConfig, BASE_SPAWN_TIER_KEY, baseSpawnTierByPlayer)
        saveMap(dataConfig, QUEST_REWARD_KEY, questRewardByPlayer)
        saveMap(dataConfig, REFUND_COOLDOWN_KEY, nextRefundTimestampByPlayer)
        for ((playerId, levels) in totalLevelsEarnedByPlayer) {
            dataConfig.set("$TOTAL_LEVELS_EARNED_KEY.$playerId", levels.coerceAtLeast(0))
        }
    }

    @JvmStatic
    fun loadFrom(dataConfig: FileConfiguration, doublePointsMaxLevel: Int, maximumSpawnTier: Int) {
        loadIntMap(dataConfig, LEVEL_KEY, levelByPlayer)
        loadIntMap(dataConfig, POINTS_KEY, pointsByPlayer)
        loadIntMap(dataConfig, DOUBLE_POINTS_KEY, doublePointsChanceByPlayer) {
            it.coerceIn(0, doublePointsMaxLevel)
        }
        loadIntMap(dataConfig, HIGHER_MAX_KEY, higherMaxLevelByPlayer)
        loadIntMap(dataConfig, START_EGGS_KEY, startEggsByPlayer)
        loadIntMap(dataConfig, EGG_CAP_KEY, eggCapByPlayer)
        loadIntMap(dataConfig, BASE_SPAWN_TIER_KEY, baseSpawnTierByPlayer) { it.coerceAtMost(maximumSpawnTier) }
        loadIntMap(dataConfig, QUEST_REWARD_KEY, questRewardByPlayer)
        loadLongMap(dataConfig, REFUND_COOLDOWN_KEY, nextRefundTimestampByPlayer) { it.coerceAtLeast(0L) }
        loadIntMap(dataConfig, TOTAL_LEVELS_EARNED_KEY, totalLevelsEarnedByPlayer) { it.coerceAtLeast(0) }
    }

    private fun saveMap(dataConfig: FileConfiguration, key: String, values: Map<UUID, *>) {
        for ((playerId, value) in values) dataConfig.set("$key.$playerId", value)
    }

    private fun loadIntMap(
        dataConfig: FileConfiguration,
        key: String,
        destination: MutableMap<UUID, Int>,
        transform: (Int) -> Int = { it }
    ) {
        val section = dataConfig.getConfigurationSection(key) ?: return
        for (playerKey in section.getKeys(false)) {
            val playerId = runCatching { UUID.fromString(playerKey) }.getOrNull() ?: continue
            destination[playerId] = transform(dataConfig.getInt("$key.$playerKey", 0))
        }
    }

    private fun loadLongMap(
        dataConfig: FileConfiguration,
        key: String,
        destination: MutableMap<UUID, Long>,
        transform: (Long) -> Long = { it }
    ) {
        val section = dataConfig.getConfigurationSection(key) ?: return
        for (playerKey in section.getKeys(false)) {
            val playerId = runCatching { UUID.fromString(playerKey) }.getOrNull() ?: continue
            destination[playerId] = transform(dataConfig.getLong("$key.$playerKey", 0L))
        }
    }
}