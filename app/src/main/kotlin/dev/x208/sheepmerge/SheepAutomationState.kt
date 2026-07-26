package dev.x208.sheepmerge

import org.bukkit.configuration.file.FileConfiguration
import java.util.UUID

object SheepAutomationState {
    private const val POINTS_KEY = "automationPoints"
    private const val AUTO_BUY_KEY = "automationAutoBuy"
    private const val AUTO_ABILITY_KEY = "automationAutoAbility"
    private const val SLOW_AUTO_MERGE_KEY = "automationSlowAutoMerge"
    private const val SLOW_AUTO_SHEAR_KEY = "automationSlowAutoShear"
    private const val AUTO_SPAWN_KEY = "automationAutoSpawn"
    private const val AUTO_PRESTIGE_KEY = "automationAutoPrestige"
    private const val AUTO_BUY_ENABLED_KEY = "automationAutoBuyEnabled"
    private const val AUTO_ABILITY_ENABLED_KEY = "automationAutoAbilityEnabled"
    private const val SLOW_AUTO_MERGE_ENABLED_KEY = "automationSlowAutoMergeEnabled"
    private const val SLOW_AUTO_SHEAR_ENABLED_KEY = "automationSlowAutoShearEnabled"
    private const val AUTO_SPAWN_ENABLED_KEY = "automationAutoSpawnEnabled"
    private const val AUTO_PRESTIGE_ENABLED_KEY = "automationAutoPrestigeEnabled"

    private val automationPointsByPlayer: MutableMap<UUID, Int> = HashMap()
    private val automationAutoBuyUpgradeByPlayer: MutableMap<UUID, Int> = HashMap()
    private val automationAutoAbilityUpgradeByPlayer: MutableMap<UUID, Int> = HashMap()
    private val automationSlowAutoMergeUpgradeByPlayer: MutableMap<UUID, Int> = HashMap()
    private val automationSlowAutoShearUpgradeByPlayer: MutableMap<UUID, Int> = HashMap()
    private val automationAutoSpawnUpgradeByPlayer: MutableMap<UUID, Int> = HashMap()
    private val automationAutoPrestigeUpgradeByPlayer: MutableMap<UUID, Int> = HashMap()
    private val automationAutoBuyEnabledByPlayer: MutableMap<UUID, Boolean> = HashMap()
    private val automationAutoAbilityEnabledByPlayer: MutableMap<UUID, Boolean> = HashMap()
    private val automationSlowAutoMergeEnabledByPlayer: MutableMap<UUID, Boolean> = HashMap()
    private val automationSlowAutoShearEnabledByPlayer: MutableMap<UUID, Boolean> = HashMap()
    private val automationAutoSpawnEnabledByPlayer: MutableMap<UUID, Boolean> = HashMap()
    private val automationAutoPrestigeEnabledByPlayer: MutableMap<UUID, Boolean> = HashMap()

    private val nextAutomationPointAtByPlayer: MutableMap<UUID, Long> = HashMap()
    private val nextAutomationAutoBuyAtByPlayer: MutableMap<UUID, Long> = HashMap()
    private val nextAutomationAutoAbilityAtByPlayer: MutableMap<UUID, Long> = HashMap()
    private val nextAutomationSlowMergeAtByPlayer: MutableMap<UUID, Long> = HashMap()
    private val nextAutomationSlowShearAtByPlayer: MutableMap<UUID, Long> = HashMap()
    private val nextAutomationAutoSpawnAtByPlayer: MutableMap<UUID, Long> = HashMap()
    private val nextAutomationAutoPrestigeAtByPlayer: MutableMap<UUID, Long> = HashMap()

    @JvmStatic fun getPoints(playerId: UUID?): Int = playerId?.let { automationPointsByPlayer[it] } ?: 0
    @JvmStatic fun setPoints(playerId: UUID, points: Int) { automationPointsByPlayer[playerId] = points }

    @JvmStatic fun getAutoBuyUpgrade(playerId: UUID?): Int = playerId?.let { automationAutoBuyUpgradeByPlayer[it] } ?: 0
    @JvmStatic fun setAutoBuyUpgrade(playerId: UUID, level: Int) { automationAutoBuyUpgradeByPlayer[playerId] = level }
    @JvmStatic fun getAutoAbilityUpgrade(playerId: UUID?): Int = playerId?.let { automationAutoAbilityUpgradeByPlayer[it] } ?: 0
    @JvmStatic fun setAutoAbilityUpgrade(playerId: UUID, level: Int) { automationAutoAbilityUpgradeByPlayer[playerId] = level }
    @JvmStatic fun getSlowAutoMergeUpgrade(playerId: UUID?): Int = playerId?.let { automationSlowAutoMergeUpgradeByPlayer[it] } ?: 0
    @JvmStatic fun setSlowAutoMergeUpgrade(playerId: UUID, level: Int) { automationSlowAutoMergeUpgradeByPlayer[playerId] = level }
    @JvmStatic fun getSlowAutoShearUpgrade(playerId: UUID?): Int = playerId?.let { automationSlowAutoShearUpgradeByPlayer[it] } ?: 0
    @JvmStatic fun setSlowAutoShearUpgrade(playerId: UUID, level: Int) { automationSlowAutoShearUpgradeByPlayer[playerId] = level }
    @JvmStatic fun getAutoSpawnUpgrade(playerId: UUID?): Int = playerId?.let { automationAutoSpawnUpgradeByPlayer[it] } ?: 0
    @JvmStatic fun setAutoSpawnUpgrade(playerId: UUID, level: Int) { automationAutoSpawnUpgradeByPlayer[playerId] = level }
    @JvmStatic fun getAutoPrestigeUpgrade(playerId: UUID?): Int = playerId?.let { automationAutoPrestigeUpgradeByPlayer[it] } ?: 0
    @JvmStatic fun setAutoPrestigeUpgrade(playerId: UUID, level: Int) { automationAutoPrestigeUpgradeByPlayer[playerId] = level }

    @JvmStatic fun isAutoBuyEnabled(playerId: UUID?): Boolean = playerId != null && automationAutoBuyEnabledByPlayer.getOrDefault(playerId, false)
    @JvmStatic fun setAutoBuyEnabled(playerId: UUID, enabled: Boolean) { automationAutoBuyEnabledByPlayer[playerId] = enabled }
    @JvmStatic fun toggleAutoBuyEnabled(playerId: UUID): Boolean = toggle(playerId, automationAutoBuyEnabledByPlayer)
    @JvmStatic fun isAutoAbilityEnabled(playerId: UUID?): Boolean = playerId != null && automationAutoAbilityEnabledByPlayer.getOrDefault(playerId, false)
    @JvmStatic fun setAutoAbilityEnabled(playerId: UUID, enabled: Boolean) { automationAutoAbilityEnabledByPlayer[playerId] = enabled }
    @JvmStatic fun toggleAutoAbilityEnabled(playerId: UUID): Boolean = toggle(playerId, automationAutoAbilityEnabledByPlayer)
    @JvmStatic fun isSlowAutoMergeEnabled(playerId: UUID?): Boolean = playerId != null && automationSlowAutoMergeEnabledByPlayer.getOrDefault(playerId, false)
    @JvmStatic fun setSlowAutoMergeEnabled(playerId: UUID, enabled: Boolean) { automationSlowAutoMergeEnabledByPlayer[playerId] = enabled }
    @JvmStatic fun toggleSlowAutoMergeEnabled(playerId: UUID): Boolean = toggle(playerId, automationSlowAutoMergeEnabledByPlayer)
    @JvmStatic fun isSlowAutoShearEnabled(playerId: UUID?): Boolean = playerId != null && automationSlowAutoShearEnabledByPlayer.getOrDefault(playerId, false)
    @JvmStatic fun setSlowAutoShearEnabled(playerId: UUID, enabled: Boolean) { automationSlowAutoShearEnabledByPlayer[playerId] = enabled }
    @JvmStatic fun toggleSlowAutoShearEnabled(playerId: UUID): Boolean = toggle(playerId, automationSlowAutoShearEnabledByPlayer)
    @JvmStatic fun isAutoSpawnEnabled(playerId: UUID?): Boolean = playerId != null && automationAutoSpawnEnabledByPlayer.getOrDefault(playerId, false)
    @JvmStatic fun setAutoSpawnEnabled(playerId: UUID, enabled: Boolean) { automationAutoSpawnEnabledByPlayer[playerId] = enabled }
    @JvmStatic fun toggleAutoSpawnEnabled(playerId: UUID): Boolean = toggle(playerId, automationAutoSpawnEnabledByPlayer)
    @JvmStatic fun isAutoPrestigeEnabled(playerId: UUID?): Boolean = playerId != null && automationAutoPrestigeEnabledByPlayer.getOrDefault(playerId, false)
    @JvmStatic fun setAutoPrestigeEnabled(playerId: UUID, enabled: Boolean) { automationAutoPrestigeEnabledByPlayer[playerId] = enabled }
    @JvmStatic fun toggleAutoPrestigeEnabled(playerId: UUID): Boolean = toggle(playerId, automationAutoPrestigeEnabledByPlayer)

    @JvmStatic fun getNextPointAt(playerId: UUID): Long = nextAutomationPointAtByPlayer.getOrDefault(playerId, 0L)
    @JvmStatic fun setNextPointAt(playerId: UUID, timestamp: Long) { nextAutomationPointAtByPlayer[playerId] = timestamp }
    @JvmStatic fun getNextAutoBuyAt(playerId: UUID): Long = nextAutomationAutoBuyAtByPlayer.getOrDefault(playerId, 0L)
    @JvmStatic fun setNextAutoBuyAt(playerId: UUID, timestamp: Long) { nextAutomationAutoBuyAtByPlayer[playerId] = timestamp }
    @JvmStatic fun getNextAutoAbilityAt(playerId: UUID): Long = nextAutomationAutoAbilityAtByPlayer.getOrDefault(playerId, 0L)
    @JvmStatic fun setNextAutoAbilityAt(playerId: UUID, timestamp: Long) { nextAutomationAutoAbilityAtByPlayer[playerId] = timestamp }
    @JvmStatic fun getNextSlowMergeAt(playerId: UUID): Long = nextAutomationSlowMergeAtByPlayer.getOrDefault(playerId, 0L)
    @JvmStatic fun setNextSlowMergeAt(playerId: UUID, timestamp: Long) { nextAutomationSlowMergeAtByPlayer[playerId] = timestamp }
    @JvmStatic fun getNextSlowShearAt(playerId: UUID): Long = nextAutomationSlowShearAtByPlayer.getOrDefault(playerId, 0L)
    @JvmStatic fun setNextSlowShearAt(playerId: UUID, timestamp: Long) { nextAutomationSlowShearAtByPlayer[playerId] = timestamp }
    @JvmStatic fun getNextAutoSpawnAt(playerId: UUID): Long = nextAutomationAutoSpawnAtByPlayer.getOrDefault(playerId, 0L)
    @JvmStatic fun setNextAutoSpawnAt(playerId: UUID, timestamp: Long) { nextAutomationAutoSpawnAtByPlayer[playerId] = timestamp }
    @JvmStatic fun getNextAutoPrestigeAt(playerId: UUID): Long = nextAutomationAutoPrestigeAtByPlayer.getOrDefault(playerId, 0L)
    @JvmStatic fun setNextAutoPrestigeAt(playerId: UUID, timestamp: Long) { nextAutomationAutoPrestigeAtByPlayer[playerId] = timestamp }

    @JvmStatic
    fun resetPlayer(playerId: UUID?) {
        if (playerId == null) return
        automationPointsByPlayer.remove(playerId)
        automationAutoBuyUpgradeByPlayer.remove(playerId)
        automationAutoAbilityUpgradeByPlayer.remove(playerId)
        automationSlowAutoMergeUpgradeByPlayer.remove(playerId)
        automationSlowAutoShearUpgradeByPlayer.remove(playerId)
        automationAutoSpawnUpgradeByPlayer.remove(playerId)
        automationAutoPrestigeUpgradeByPlayer.remove(playerId)
        automationAutoBuyEnabledByPlayer.remove(playerId)
        automationAutoAbilityEnabledByPlayer.remove(playerId)
        automationSlowAutoMergeEnabledByPlayer.remove(playerId)
        automationSlowAutoShearEnabledByPlayer.remove(playerId)
        automationAutoSpawnEnabledByPlayer.remove(playerId)
        automationAutoPrestigeEnabledByPlayer.remove(playerId)
        nextAutomationPointAtByPlayer.remove(playerId)
        nextAutomationAutoBuyAtByPlayer.remove(playerId)
        nextAutomationAutoAbilityAtByPlayer.remove(playerId)
        nextAutomationSlowMergeAtByPlayer.remove(playerId)
        nextAutomationSlowShearAtByPlayer.remove(playerId)
        nextAutomationAutoSpawnAtByPlayer.remove(playerId)
        nextAutomationAutoPrestigeAtByPlayer.remove(playerId)
    }

    @JvmStatic
    fun clearPersisted() {
        automationPointsByPlayer.clear()
        automationAutoBuyUpgradeByPlayer.clear()
        automationAutoAbilityUpgradeByPlayer.clear()
        automationSlowAutoMergeUpgradeByPlayer.clear()
        automationSlowAutoShearUpgradeByPlayer.clear()
        automationAutoSpawnUpgradeByPlayer.clear()
        automationAutoPrestigeUpgradeByPlayer.clear()
        automationAutoBuyEnabledByPlayer.clear()
        automationAutoAbilityEnabledByPlayer.clear()
        automationSlowAutoMergeEnabledByPlayer.clear()
        automationSlowAutoShearEnabledByPlayer.clear()
        automationAutoSpawnEnabledByPlayer.clear()
        automationAutoPrestigeEnabledByPlayer.clear()
    }

    @JvmStatic
    fun clear() {
        clearPersisted()
        nextAutomationPointAtByPlayer.clear()
        nextAutomationAutoBuyAtByPlayer.clear()
        nextAutomationAutoAbilityAtByPlayer.clear()
        nextAutomationSlowMergeAtByPlayer.clear()
        nextAutomationSlowShearAtByPlayer.clear()
        nextAutomationAutoSpawnAtByPlayer.clear()
        nextAutomationAutoPrestigeAtByPlayer.clear()
    }

    @JvmStatic
    fun saveTo(dataConfig: FileConfiguration) {
        saveMap(dataConfig, POINTS_KEY, automationPointsByPlayer)
        saveMap(dataConfig, AUTO_BUY_KEY, automationAutoBuyUpgradeByPlayer)
        saveMap(dataConfig, AUTO_ABILITY_KEY, automationAutoAbilityUpgradeByPlayer)
        saveMap(dataConfig, SLOW_AUTO_MERGE_KEY, automationSlowAutoMergeUpgradeByPlayer)
        saveMap(dataConfig, SLOW_AUTO_SHEAR_KEY, automationSlowAutoShearUpgradeByPlayer)
        saveMap(dataConfig, AUTO_SPAWN_KEY, automationAutoSpawnUpgradeByPlayer)
        saveMap(dataConfig, AUTO_PRESTIGE_KEY, automationAutoPrestigeUpgradeByPlayer)
        saveMap(dataConfig, AUTO_BUY_ENABLED_KEY, automationAutoBuyEnabledByPlayer)
        saveMap(dataConfig, AUTO_ABILITY_ENABLED_KEY, automationAutoAbilityEnabledByPlayer)
        saveMap(dataConfig, SLOW_AUTO_MERGE_ENABLED_KEY, automationSlowAutoMergeEnabledByPlayer)
        saveMap(dataConfig, SLOW_AUTO_SHEAR_ENABLED_KEY, automationSlowAutoShearEnabledByPlayer)
        saveMap(dataConfig, AUTO_SPAWN_ENABLED_KEY, automationAutoSpawnEnabledByPlayer)
        saveMap(dataConfig, AUTO_PRESTIGE_ENABLED_KEY, automationAutoPrestigeEnabledByPlayer)
    }

    @JvmStatic
    fun loadFrom(
        dataConfig: FileConfiguration,
        autoBuyMaxLevel: Int,
        autoAbilityMaxLevel: Int,
        slowAutoMergeMaxLevel: Int,
        slowAutoShearMaxLevel: Int,
        autoSpawnMaxLevel: Int,
        autoPrestigeMaxLevel: Int
    ) {
        loadIntMap(dataConfig, POINTS_KEY, Int.MAX_VALUE, automationPointsByPlayer)
        loadIntMap(dataConfig, AUTO_BUY_KEY, autoBuyMaxLevel, automationAutoBuyUpgradeByPlayer)
        loadIntMap(dataConfig, AUTO_ABILITY_KEY, autoAbilityMaxLevel, automationAutoAbilityUpgradeByPlayer)
        loadIntMap(dataConfig, SLOW_AUTO_MERGE_KEY, slowAutoMergeMaxLevel, automationSlowAutoMergeUpgradeByPlayer)
        loadIntMap(dataConfig, SLOW_AUTO_SHEAR_KEY, slowAutoShearMaxLevel, automationSlowAutoShearUpgradeByPlayer)
        loadIntMap(dataConfig, AUTO_SPAWN_KEY, autoSpawnMaxLevel, automationAutoSpawnUpgradeByPlayer)
        loadIntMap(dataConfig, AUTO_PRESTIGE_KEY, autoPrestigeMaxLevel, automationAutoPrestigeUpgradeByPlayer)
        loadBooleanMap(dataConfig, AUTO_BUY_ENABLED_KEY, automationAutoBuyEnabledByPlayer)
        loadBooleanMap(dataConfig, AUTO_ABILITY_ENABLED_KEY, automationAutoAbilityEnabledByPlayer)
        loadBooleanMap(dataConfig, SLOW_AUTO_MERGE_ENABLED_KEY, automationSlowAutoMergeEnabledByPlayer)
        loadBooleanMap(dataConfig, SLOW_AUTO_SHEAR_ENABLED_KEY, automationSlowAutoShearEnabledByPlayer)
        loadBooleanMap(dataConfig, AUTO_SPAWN_ENABLED_KEY, automationAutoSpawnEnabledByPlayer)
        loadBooleanMap(dataConfig, AUTO_PRESTIGE_ENABLED_KEY, automationAutoPrestigeEnabledByPlayer)
    }

    private fun toggle(playerId: UUID, values: MutableMap<UUID, Boolean>): Boolean {
        val enabled = !values.getOrDefault(playerId, false)
        values[playerId] = enabled
        return enabled
    }

    private fun saveMap(dataConfig: FileConfiguration, key: String, values: Map<UUID, *>) {
        for ((playerId, value) in values) {
            dataConfig.set("$key.$playerId", value)
        }
    }

    private fun loadIntMap(
        dataConfig: FileConfiguration,
        key: String,
        maxValue: Int,
        destination: MutableMap<UUID, Int>
    ) {
        val section = dataConfig.getConfigurationSection(key) ?: return
        for (playerKey in section.getKeys(false)) {
            val playerId = runCatching { UUID.fromString(playerKey) }.getOrNull() ?: continue
            destination[playerId] = dataConfig.getInt("$key.$playerKey", 0).coerceIn(0, maxValue)
        }
    }

    private fun loadBooleanMap(
        dataConfig: FileConfiguration,
        key: String,
        destination: MutableMap<UUID, Boolean>
    ) {
        val section = dataConfig.getConfigurationSection(key) ?: return
        for (playerKey in section.getKeys(false)) {
            val playerId = runCatching { UUID.fromString(playerKey) }.getOrNull() ?: continue
            destination[playerId] = dataConfig.getBoolean("$key.$playerKey", false)
        }
    }
}