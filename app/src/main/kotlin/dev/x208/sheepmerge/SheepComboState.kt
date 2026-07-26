package dev.x208.sheepmerge

import org.bukkit.configuration.file.FileConfiguration
import java.util.UUID

object SheepComboState {
    private const val DECAY_UPGRADE_KEY = "comboDecayUpgrade"
    private const val MAX_UPGRADE_KEY = "comboMaxUpgrade"
    private const val GAIN_UPGRADE_KEY = "comboGainUpgrade"

    private val comboScoreByPlayer = HashMap<UUID, Double>()
    private val comboLastUpdateTimestampByPlayer = HashMap<UUID, Long>()
    private val comboDecayUpgradeByPlayer = HashMap<UUID, Int>()
    private val comboMaxUpgradeByPlayer = HashMap<UUID, Int>()
    private val comboGainUpgradeByPlayer = HashMap<UUID, Int>()

    @JvmStatic
    fun getScore(playerId: UUID): Double = comboScoreByPlayer.getOrDefault(playerId, 0.0)

    @JvmStatic
    fun setScore(playerId: UUID, score: Double) {
        comboScoreByPlayer[playerId] = score
    }

    @JvmStatic
    fun removeScore(playerId: UUID) {
        comboScoreByPlayer.remove(playerId)
    }

    @JvmStatic
    fun getLastUpdateTimestamp(playerId: UUID, defaultValue: Long): Long =
        comboLastUpdateTimestampByPlayer.getOrDefault(playerId, defaultValue)

    @JvmStatic
    fun setLastUpdateTimestamp(playerId: UUID, timestamp: Long) {
        comboLastUpdateTimestampByPlayer[playerId] = timestamp
    }

    @JvmStatic
    fun getDecayUpgrade(playerId: UUID): Int = comboDecayUpgradeByPlayer.getOrDefault(playerId, 0)

    @JvmStatic
    fun setDecayUpgrade(playerId: UUID, level: Int) {
        comboDecayUpgradeByPlayer[playerId] = level
    }

    @JvmStatic
    fun getMaxUpgrade(playerId: UUID): Int = comboMaxUpgradeByPlayer.getOrDefault(playerId, 0)

    @JvmStatic
    fun setMaxUpgrade(playerId: UUID, level: Int) {
        comboMaxUpgradeByPlayer[playerId] = level
    }

    @JvmStatic
    fun getGainUpgrade(playerId: UUID): Int = comboGainUpgradeByPlayer.getOrDefault(playerId, 0)

    @JvmStatic
    fun setGainUpgrade(playerId: UUID, level: Int) {
        comboGainUpgradeByPlayer[playerId] = level
    }

    @JvmStatic
    fun resetRuntime(playerId: UUID) {
        comboScoreByPlayer.remove(playerId)
        comboLastUpdateTimestampByPlayer.remove(playerId)
    }

    @JvmStatic
    fun resetRegularUpgrades(playerId: UUID) {
        comboDecayUpgradeByPlayer.remove(playerId)
        comboGainUpgradeByPlayer.remove(playerId)
    }

    @JvmStatic
    fun resetMaxUpgrade(playerId: UUID, baseMaxScore: Double) {
        comboMaxUpgradeByPlayer.remove(playerId)
        comboScoreByPlayer.computeIfPresent(playerId) { _, score -> score.coerceIn(0.0, baseMaxScore) }
    }

    @JvmStatic
    fun resetPlayer(playerId: UUID) {
        resetRuntime(playerId)
        resetRegularUpgrades(playerId)
        comboMaxUpgradeByPlayer.remove(playerId)
    }

    @JvmStatic
    fun clearPersisted() {
        comboDecayUpgradeByPlayer.clear()
        comboMaxUpgradeByPlayer.clear()
        comboGainUpgradeByPlayer.clear()
    }

    @JvmStatic
    fun clear() {
        resetAllRuntime()
        clearPersisted()
    }

    @JvmStatic
    fun saveTo(configuration: FileConfiguration) {
        configuration.set(DECAY_UPGRADE_KEY, null)
        configuration.set(MAX_UPGRADE_KEY, null)
        configuration.set(GAIN_UPGRADE_KEY, null)
        saveMap(configuration, DECAY_UPGRADE_KEY, comboDecayUpgradeByPlayer)
        saveMap(configuration, MAX_UPGRADE_KEY, comboMaxUpgradeByPlayer)
        saveMap(configuration, GAIN_UPGRADE_KEY, comboGainUpgradeByPlayer)
    }

    @JvmStatic
    fun loadFrom(configuration: FileConfiguration, decayMaxLevel: Int, gainMaxLevel: Int) {
        loadUpgradeMap(configuration, DECAY_UPGRADE_KEY, decayMaxLevel, comboDecayUpgradeByPlayer)
        loadUpgradeMap(configuration, MAX_UPGRADE_KEY, Int.MAX_VALUE, comboMaxUpgradeByPlayer)
        loadUpgradeMap(configuration, GAIN_UPGRADE_KEY, gainMaxLevel, comboGainUpgradeByPlayer)
    }

    private fun resetAllRuntime() {
        comboScoreByPlayer.clear()
        comboLastUpdateTimestampByPlayer.clear()
    }

    private fun saveMap(configuration: FileConfiguration, key: String, values: Map<UUID, Int>) {
        values.forEach { (playerId, value) -> configuration.set("$key.$playerId", value) }
    }

    private fun loadUpgradeMap(
        configuration: FileConfiguration,
        key: String,
        maxLevel: Int,
        destination: MutableMap<UUID, Int>,
    ) {
        val section = configuration.getConfigurationSection(key) ?: return
        section.getKeys(false).forEach { playerKey ->
            val playerId = runCatching { UUID.fromString(playerKey) }.getOrNull() ?: return@forEach
            destination[playerId] = configuration.getInt("$key.$playerKey", 0).coerceIn(0, maxLevel)
        }
    }
}