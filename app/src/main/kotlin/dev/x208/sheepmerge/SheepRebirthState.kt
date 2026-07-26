package dev.x208.sheepmerge

import org.bukkit.configuration.file.FileConfiguration
import java.util.UUID

object SheepRebirthState {
    private const val LEVEL_KEY = "rebirthLevel"
    private const val POINTS_KEY = "rebirthPoints"
    private const val SKILL_UNLOCK_MASK_KEY = "rebirthSkillUnlockMask"
    private const val SKILL_PENDING_MASK_KEY = "rebirthSkillPendingMask"
    private const val RESPEC_COOLDOWN_KEY = "rebirthRespecCooldown"

    private val levelByPlayer: MutableMap<UUID, Int> = HashMap()
    private val pointsByPlayer: MutableMap<UUID, Int> = HashMap()
    private val skillUnlockMaskByPlayer: MutableMap<UUID, Int> = HashMap()
    private val skillPendingMaskByPlayer: MutableMap<UUID, Int> = HashMap()
    private val nextRespecTimestampByPlayer: MutableMap<UUID, Long> = HashMap()

    private val lastReminderTimestampByPlayer: MutableMap<UUID, Long> = HashMap()
    private val titleReminderShownByPlayer: MutableMap<UUID, Boolean> = HashMap()

    @JvmStatic fun getLevel(playerId: UUID?): Int = playerId?.let { levelByPlayer[it] } ?: 0
    @JvmStatic fun setLevel(playerId: UUID, level: Int) { levelByPlayer[playerId] = level }
    @JvmStatic fun getPoints(playerId: UUID?): Int = playerId?.let { pointsByPlayer[it] } ?: 0
    @JvmStatic fun setPoints(playerId: UUID, points: Int) { pointsByPlayer[playerId] = points }
    @JvmStatic fun getSkillUnlockMask(playerId: UUID?): Int = playerId?.let { skillUnlockMaskByPlayer[it] } ?: 0
    @JvmStatic fun setSkillUnlockMask(playerId: UUID, mask: Int) { skillUnlockMaskByPlayer[playerId] = mask }
    @JvmStatic fun clearSkillUnlockMask(playerId: UUID) { skillUnlockMaskByPlayer.remove(playerId) }
    @JvmStatic fun getSkillPendingMask(playerId: UUID?): Int = playerId?.let { skillPendingMaskByPlayer[it] } ?: 0
    @JvmStatic fun setSkillPendingMask(playerId: UUID, mask: Int) { skillPendingMaskByPlayer[playerId] = mask }
    @JvmStatic fun clearSkillPendingMask(playerId: UUID) { skillPendingMaskByPlayer.remove(playerId) }
    @JvmStatic fun getNextRespecTimestamp(playerId: UUID?): Long = playerId?.let { nextRespecTimestampByPlayer[it] } ?: 0L
    @JvmStatic fun setNextRespecTimestamp(playerId: UUID, timestamp: Long) { nextRespecTimestampByPlayer[playerId] = timestamp }

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
    fun resetPlayer(playerId: UUID?) {
        if (playerId == null) return
        levelByPlayer.remove(playerId)
        pointsByPlayer.remove(playerId)
        skillUnlockMaskByPlayer.remove(playerId)
        skillPendingMaskByPlayer.remove(playerId)
        nextRespecTimestampByPlayer.remove(playerId)
        lastReminderTimestampByPlayer.remove(playerId)
        titleReminderShownByPlayer.remove(playerId)
    }

    @JvmStatic
    fun clear() {
        levelByPlayer.clear()
        pointsByPlayer.clear()
        skillUnlockMaskByPlayer.clear()
        skillPendingMaskByPlayer.clear()
        nextRespecTimestampByPlayer.clear()
        lastReminderTimestampByPlayer.clear()
        titleReminderShownByPlayer.clear()
    }

    @JvmStatic
    fun clearPersistedKeys(dataConfig: FileConfiguration) {
        dataConfig.set(LEVEL_KEY, null)
        dataConfig.set(POINTS_KEY, null)
        dataConfig.set(SKILL_UNLOCK_MASK_KEY, null)
        dataConfig.set(SKILL_PENDING_MASK_KEY, null)
        dataConfig.set(RESPEC_COOLDOWN_KEY, null)
    }

    @JvmStatic
    fun saveTo(dataConfig: FileConfiguration, treeMask: Int) {
        for ((playerId, level) in levelByPlayer) dataConfig.set("$LEVEL_KEY.$playerId", level.coerceAtLeast(0))
        for ((playerId, points) in pointsByPlayer) dataConfig.set("$POINTS_KEY.$playerId", points.coerceAtLeast(0))
        for ((playerId, mask) in skillUnlockMaskByPlayer) {
            dataConfig.set("$SKILL_UNLOCK_MASK_KEY.$playerId", mask and treeMask)
        }
        for ((playerId, mask) in skillPendingMaskByPlayer) {
            dataConfig.set("$SKILL_PENDING_MASK_KEY.$playerId", mask and treeMask)
        }
        for ((playerId, timestamp) in nextRespecTimestampByPlayer) {
            dataConfig.set("$RESPEC_COOLDOWN_KEY.$playerId", timestamp)
        }
    }

    @JvmStatic
    fun loadFrom(dataConfig: FileConfiguration, treeMask: Int) {
        loadIntMap(dataConfig, LEVEL_KEY, levelByPlayer) { it.coerceAtLeast(0) }
        loadIntMap(dataConfig, POINTS_KEY, pointsByPlayer) { it.coerceAtLeast(0) }
        loadIntMap(dataConfig, SKILL_UNLOCK_MASK_KEY, skillUnlockMaskByPlayer) { it and treeMask }
        loadIntMap(dataConfig, SKILL_PENDING_MASK_KEY, skillPendingMaskByPlayer) { it and treeMask }
        loadLongMap(dataConfig, RESPEC_COOLDOWN_KEY, nextRespecTimestampByPlayer) { it.coerceAtLeast(0L) }
        skillPendingMaskByPlayer.replaceAll { playerId, pendingMask ->
            pendingMask and skillUnlockMaskByPlayer.getOrDefault(playerId, 0)
        }
    }

    private fun loadIntMap(
        dataConfig: FileConfiguration,
        key: String,
        destination: MutableMap<UUID, Int>,
        transform: (Int) -> Int
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
        transform: (Long) -> Long
    ) {
        val section = dataConfig.getConfigurationSection(key) ?: return
        for (playerKey in section.getKeys(false)) {
            val playerId = runCatching { UUID.fromString(playerKey) }.getOrNull() ?: continue
            destination[playerId] = transform(dataConfig.getLong("$key.$playerKey", 0L))
        }
    }
}