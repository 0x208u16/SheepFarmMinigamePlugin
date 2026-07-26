package dev.x208.sheepmerge

import org.bukkit.configuration.file.FileConfiguration
import java.util.UUID
import java.util.function.Predicate

object SheepUiPreferences {

    private val scoreboardLayoutModeByPlayer: MutableMap<UUID, Int> = HashMap()
    private val scoreboardShowAchievementPointsByPlayer: MutableMap<UUID, Boolean> = HashMap()
    private val scoreboardShowQuestPointsByPlayer: MutableMap<UUID, Boolean> = HashMap()
    private val scoreboardShowAutomationPointsByPlayer: MutableMap<UUID, Boolean> = HashMap()
    private val scoreboardShowSacrificePointsByPlayer: MutableMap<UUID, Boolean> = HashMap()
    private val scoreboardShowPrestigeStatsByPlayer: MutableMap<UUID, Boolean> = HashMap()
    private val scoreboardShowQuestProgressByPlayer: MutableMap<UUID, Boolean> = HashMap()
    private val scoreboardShowAbilityStatusByPlayer: MutableMap<UUID, Boolean> = HashMap()
    private val inventoryQuickAccessByPlayer: MutableMap<UUID, List<String>> = HashMap()
    private val inventoryQuickAccessCastingEnabledByPlayer: MutableMap<UUID, Boolean> = HashMap()

    @JvmStatic
    fun getScoreboardLayoutMode(playerId: UUID?): Int {
        return playerId?.let { scoreboardLayoutModeByPlayer[it] }?.coerceIn(0, 1) ?: 0
    }

    @JvmStatic
    fun setScoreboardLayoutMode(playerId: UUID?, layoutMode: Int) {
        if (playerId != null) {
            scoreboardLayoutModeByPlayer[playerId] = layoutMode.coerceIn(0, 1)
        }
    }

    @JvmStatic
    fun shouldShowScoreboardAchievementPoints(playerId: UUID?): Boolean {
        return playerId != null && scoreboardShowAchievementPointsByPlayer.getOrDefault(playerId, false)
    }

    @JvmStatic
    fun setShowScoreboardAchievementPoints(playerId: UUID?, show: Boolean) {
        if (playerId != null) scoreboardShowAchievementPointsByPlayer[playerId] = show
    }

    @JvmStatic
    fun shouldShowScoreboardQuestPoints(playerId: UUID?): Boolean {
        return playerId != null && scoreboardShowQuestPointsByPlayer.getOrDefault(playerId, false)
    }

    @JvmStatic
    fun setShowScoreboardQuestPoints(playerId: UUID?, show: Boolean) {
        if (playerId != null) scoreboardShowQuestPointsByPlayer[playerId] = show
    }

    @JvmStatic
    fun shouldShowScoreboardAutomationPoints(playerId: UUID?): Boolean {
        return playerId != null && scoreboardShowAutomationPointsByPlayer.getOrDefault(playerId, false)
    }

    @JvmStatic
    fun setShowScoreboardAutomationPoints(playerId: UUID?, show: Boolean) {
        if (playerId != null) scoreboardShowAutomationPointsByPlayer[playerId] = show
    }

    @JvmStatic
    fun shouldShowScoreboardSacrificePoints(playerId: UUID?): Boolean {
        return playerId != null && scoreboardShowSacrificePointsByPlayer.getOrDefault(playerId, false)
    }

    @JvmStatic
    fun setShowScoreboardSacrificePoints(playerId: UUID?, show: Boolean) {
        if (playerId != null) scoreboardShowSacrificePointsByPlayer[playerId] = show
    }

    @JvmStatic
    fun shouldShowScoreboardPrestigeStats(playerId: UUID?): Boolean {
        return playerId != null && scoreboardShowPrestigeStatsByPlayer.getOrDefault(playerId, true)
    }

    @JvmStatic
    fun setShowScoreboardPrestigeStats(playerId: UUID?, show: Boolean) {
        if (playerId != null) scoreboardShowPrestigeStatsByPlayer[playerId] = show
    }

    @JvmStatic
    fun shouldShowScoreboardQuestProgress(playerId: UUID?): Boolean {
        return playerId != null && scoreboardShowQuestProgressByPlayer.getOrDefault(playerId, true)
    }

    @JvmStatic
    fun setShowScoreboardQuestProgress(playerId: UUID?, show: Boolean) {
        if (playerId != null) scoreboardShowQuestProgressByPlayer[playerId] = show
    }

    @JvmStatic
    fun shouldShowScoreboardAbilityStatus(playerId: UUID?): Boolean {
        return playerId != null && scoreboardShowAbilityStatusByPlayer.getOrDefault(playerId, true)
    }

    @JvmStatic
    fun setShowScoreboardAbilityStatus(playerId: UUID?, show: Boolean) {
        if (playerId != null) scoreboardShowAbilityStatusByPlayer[playerId] = show
    }

    @JvmStatic
    fun getInventoryQuickAccessActions(
        playerId: UUID?,
        maxItems: Int,
        isValidAction: Predicate<String>
    ): List<String> {
        if (playerId == null) {
            return emptyList()
        }
        return normalizeQuickAccessActions(inventoryQuickAccessByPlayer[playerId], maxItems, isValidAction)
    }

    @JvmStatic
    fun setInventoryQuickAccessActions(
        playerId: UUID?,
        actions: Collection<String>?,
        maxItems: Int,
        isValidAction: Predicate<String>
    ) {
        if (playerId == null) {
            return
        }
        val normalized = normalizeQuickAccessActions(actions, maxItems, isValidAction)
        if (normalized.isEmpty()) {
            inventoryQuickAccessByPlayer.remove(playerId)
        } else {
            inventoryQuickAccessByPlayer[playerId] = normalized
        }
    }

    @JvmStatic
    fun isInventoryQuickAccessCastingEnabled(playerId: UUID?): Boolean {
        return playerId == null || inventoryQuickAccessCastingEnabledByPlayer.getOrDefault(playerId, true)
    }

    @JvmStatic
    fun setInventoryQuickAccessCastingEnabled(playerId: UUID?, enabled: Boolean) {
        if (playerId == null) {
            return
        }
        if (enabled) {
            inventoryQuickAccessCastingEnabledByPlayer.remove(playerId)
        } else {
            inventoryQuickAccessCastingEnabledByPlayer[playerId] = false
        }
    }

    @JvmStatic
    fun resetPlayer(playerId: UUID?) {
        if (playerId == null) {
            return
        }
        scoreboardLayoutModeByPlayer.remove(playerId)
        scoreboardShowAchievementPointsByPlayer.remove(playerId)
        scoreboardShowQuestPointsByPlayer.remove(playerId)
        scoreboardShowAutomationPointsByPlayer.remove(playerId)
        scoreboardShowSacrificePointsByPlayer.remove(playerId)
        scoreboardShowPrestigeStatsByPlayer.remove(playerId)
        scoreboardShowQuestProgressByPlayer.remove(playerId)
        scoreboardShowAbilityStatusByPlayer.remove(playerId)
        inventoryQuickAccessByPlayer.remove(playerId)
        inventoryQuickAccessCastingEnabledByPlayer.remove(playerId)
    }

    @JvmStatic
    fun clear() {
        scoreboardLayoutModeByPlayer.clear()
        scoreboardShowAchievementPointsByPlayer.clear()
        scoreboardShowQuestPointsByPlayer.clear()
        scoreboardShowAutomationPointsByPlayer.clear()
        scoreboardShowSacrificePointsByPlayer.clear()
        scoreboardShowPrestigeStatsByPlayer.clear()
        scoreboardShowQuestProgressByPlayer.clear()
        scoreboardShowAbilityStatusByPlayer.clear()
        inventoryQuickAccessByPlayer.clear()
        inventoryQuickAccessCastingEnabledByPlayer.clear()
    }

    @JvmStatic
    fun clearPersistedKeys(dataConfig: FileConfiguration?) {
        if (dataConfig == null) {
            return
        }
        dataConfig.set("scoreboardLayoutMode", null)
        dataConfig.set("scoreboardShowAchievementPoints", null)
        dataConfig.set("scoreboardShowQuestPoints", null)
        dataConfig.set("scoreboardShowAutomationPoints", null)
        dataConfig.set("scoreboardShowSacrificePoints", null)
        dataConfig.set("scoreboardShowPrestigeStats", null)
        dataConfig.set("scoreboardShowQuestProgress", null)
        dataConfig.set("scoreboardShowAbilityStatus", null)
        dataConfig.set("inventoryQuickAccess", null)
        dataConfig.set("inventoryQuickAccessCastingEnabled", null)
    }

    @JvmStatic
    fun saveTo(dataConfig: FileConfiguration?) {
        if (dataConfig == null) {
            return
        }
        for ((playerId, layoutMode) in scoreboardLayoutModeByPlayer) {
            dataConfig.set("scoreboardLayoutMode.$playerId", layoutMode.coerceIn(0, 1))
        }
        saveBooleanMap(dataConfig, "scoreboardShowAchievementPoints", scoreboardShowAchievementPointsByPlayer)
        saveBooleanMap(dataConfig, "scoreboardShowQuestPoints", scoreboardShowQuestPointsByPlayer)
        saveBooleanMap(dataConfig, "scoreboardShowAutomationPoints", scoreboardShowAutomationPointsByPlayer)
        saveBooleanMap(dataConfig, "scoreboardShowSacrificePoints", scoreboardShowSacrificePointsByPlayer)
        saveBooleanMap(dataConfig, "scoreboardShowPrestigeStats", scoreboardShowPrestigeStatsByPlayer)
        saveBooleanMap(dataConfig, "scoreboardShowQuestProgress", scoreboardShowQuestProgressByPlayer)
        saveBooleanMap(dataConfig, "scoreboardShowAbilityStatus", scoreboardShowAbilityStatusByPlayer)
        for ((playerId, actions) in inventoryQuickAccessByPlayer) {
            if (actions.isNotEmpty()) {
                dataConfig.set("inventoryQuickAccess.$playerId", ArrayList(actions))
            }
        }
        for ((playerId, enabled) in inventoryQuickAccessCastingEnabledByPlayer) {
            if (!enabled) {
                dataConfig.set("inventoryQuickAccessCastingEnabled.$playerId", false)
            }
        }
    }

    @JvmStatic
    fun loadFrom(dataConfig: FileConfiguration?, maxItems: Int, isValidAction: Predicate<String>) {
        if (dataConfig == null) {
            return
        }
        loadLayoutModes(dataConfig)
        loadBooleanMap(dataConfig, "scoreboardShowAchievementPoints", false, scoreboardShowAchievementPointsByPlayer)
        loadBooleanMap(dataConfig, "scoreboardShowQuestPoints", false, scoreboardShowQuestPointsByPlayer)
        loadBooleanMap(dataConfig, "scoreboardShowAutomationPoints", false, scoreboardShowAutomationPointsByPlayer)
        loadBooleanMap(dataConfig, "scoreboardShowSacrificePoints", false, scoreboardShowSacrificePointsByPlayer)
        loadBooleanMap(dataConfig, "scoreboardShowPrestigeStats", true, scoreboardShowPrestigeStatsByPlayer)
        loadBooleanMap(dataConfig, "scoreboardShowQuestProgress", true, scoreboardShowQuestProgressByPlayer)
        loadBooleanMap(dataConfig, "scoreboardShowAbilityStatus", true, scoreboardShowAbilityStatusByPlayer)
        dataConfig.getConfigurationSection("inventoryQuickAccess")?.getKeys(false)?.forEach { key ->
            try {
                val playerId = UUID.fromString(key)
                setInventoryQuickAccessActions(
                    playerId,
                    dataConfig.getStringList("inventoryQuickAccess.$key"),
                    maxItems,
                    isValidAction
                )
            } catch (_: IllegalArgumentException) {
                // Ignore invalid UUIDs.
            }
        }
        dataConfig.getConfigurationSection("inventoryQuickAccessCastingEnabled")?.getKeys(false)?.forEach { key ->
            try {
                val playerId = UUID.fromString(key)
                val enabled = dataConfig.getBoolean("inventoryQuickAccessCastingEnabled.$key", true)
                setInventoryQuickAccessCastingEnabled(playerId, enabled)
            } catch (_: IllegalArgumentException) {
                // Ignore invalid UUIDs.
            }
        }
    }

    private fun normalizeQuickAccessActions(
        actions: Collection<String>?,
        maxItems: Int,
        isValidAction: Predicate<String>
    ): List<String> {
        if (actions.isNullOrEmpty() || maxItems <= 0) {
            return emptyList()
        }
        val normalized = ArrayList<String>()
        for (actionId in actions) {
            if (actionId.isBlank() || !isValidAction.test(actionId) || normalized.contains(actionId)) {
                continue
            }
            normalized.add(actionId)
            if (normalized.size >= maxItems) {
                break
            }
        }
        return normalized
    }

    private fun saveBooleanMap(
        dataConfig: FileConfiguration,
        path: String,
        values: Map<UUID, Boolean>
    ) {
        for ((playerId, value) in values) {
            dataConfig.set("$path.$playerId", value)
        }
    }

    private fun loadLayoutModes(dataConfig: FileConfiguration) {
        dataConfig.getConfigurationSection("scoreboardLayoutMode")?.getKeys(false)?.forEach { key ->
            try {
                val playerId = UUID.fromString(key)
                setScoreboardLayoutMode(playerId, dataConfig.getInt("scoreboardLayoutMode.$key", 0))
            } catch (_: IllegalArgumentException) {
                // Ignore invalid UUIDs.
            }
        }
    }

    private fun loadBooleanMap(
        dataConfig: FileConfiguration,
        path: String,
        defaultValue: Boolean,
        target: MutableMap<UUID, Boolean>
    ) {
        dataConfig.getConfigurationSection(path)?.getKeys(false)?.forEach { key ->
            try {
                target[UUID.fromString(key)] = dataConfig.getBoolean("$path.$key", defaultValue)
            } catch (_: IllegalArgumentException) {
                // Ignore invalid UUIDs.
            }
        }
    }
}