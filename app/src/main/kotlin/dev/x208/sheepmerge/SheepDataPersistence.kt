package dev.x208.sheepmerge

import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.function.Predicate

object SheepDataPersistence {

    @JvmStatic
    fun saveData(
        plugin: SheepMergePlugin,
        dataFile: File,
        currentConfig: FileConfiguration?
    ): FileConfiguration? {
        if (!plugin.dataFolder.exists() && !plugin.dataFolder.mkdirs()) {
            return currentConfig
        }
        val dataConfig = currentConfig ?: YamlConfiguration.loadConfiguration(dataFile)
        try {
            clearPersistedKeys(dataConfig)
            saveState(dataConfig)
            savePendingInventories(dataConfig)
            dataConfig.save(dataFile)
        } catch (exception: IOException) {
            plugin.logger.warning("Unable to save sheep merge scores: ${exception.message}")
        }
        return dataConfig
    }

    @JvmStatic
    fun loadData(plugin: SheepMergePlugin, dataFile: File): FileConfiguration {
        val dataConfig = YamlConfiguration.loadConfiguration(dataFile)
        if (!dataFile.exists()) {
            return dataConfig
        }
        loadState(dataConfig)
        loadPendingInventories(dataConfig)
        return dataConfig
    }

    private fun clearPersistedKeys(dataConfig: FileConfiguration) {
        SheepEconomyState.clearPersistedKeys(dataConfig)
        SheepPrestigeState.clearPersistedKeys(dataConfig)
        dataConfig.set("prestigeExpandFarm", null)
        SheepUpgradeState.clearPersistedKeys(dataConfig)
        SheepVisitAccessState.clearPersistedKeys(dataConfig)
        SheepEffectPreferences.clearPersistedKeys(dataConfig)
        SheepQuestState.clearPersistedKeys(dataConfig)
        dataConfig.set("automationPoints", null)
        dataConfig.set("automationAutoBuy", null)
        dataConfig.set("automationAutoAbility", null)
        dataConfig.set("automationSlowAutoMerge", null)
        dataConfig.set("automationSlowAutoShear", null)
        dataConfig.set("automationAutoSpawn", null)
        dataConfig.set("automationAutoPrestige", null)
        dataConfig.set("automationAutoBuyEnabled", null)
        dataConfig.set("automationAutoAbilityEnabled", null)
        dataConfig.set("automationSlowAutoMergeEnabled", null)
        dataConfig.set("automationSlowAutoShearEnabled", null)
        dataConfig.set("automationAutoSpawnEnabled", null)
        dataConfig.set("automationAutoPrestigeEnabled", null)
        SheepUiPreferences.clearPersistedKeys(dataConfig)
        dataConfig.set("liveUpdate", null)
        dataConfig.set("dataSchemaVersion", null)
        SheepSacrificeProgression.clearPersistedKeys(dataConfig)
        SheepRebirthRuntime.clearPersistedKeys(dataConfig)
        SheepLifetimeProgressState.clearPersistedKeys(dataConfig)
        SheepAchievementState.clearPersistedKeys(dataConfig)
        SheepSnapshotState.clearPersistedKeys(dataConfig)
        dataConfig.set("pendingInventory", null)
    }

    private fun saveState(dataConfig: FileConfiguration) {
        SheepEconomyState.saveTo(dataConfig)
        SheepPrestigeState.saveTo(dataConfig)
        SheepUpgradeState.saveTo(dataConfig)
        SheepTutorialState.saveTo(dataConfig)
        SheepVisitAccessState.saveTo(dataConfig)
        SheepEffectPreferences.saveTo(dataConfig)
        SheepQuestState.saveTo(dataConfig)
        SheepComboState.saveTo(dataConfig)
        SheepAutomationState.saveTo(dataConfig)
        SheepUiPreferences.saveTo(dataConfig)
        SheepSacrificeProgression.saveTo(dataConfig)
        SheepRebirthRuntime.saveTo(dataConfig)
        SheepLifetimeProgressState.saveTo(dataConfig)
        SheepAchievementState.saveTo(dataConfig)
        dataConfig.set("liveUpdate.enabled", SheepLiveUpdateState.isLiveUpdateEnabled())
        dataConfig.set("liveUpdate.stagedVersion", SheepLiveUpdateState.getStagedLiveUpdateVersion())
        dataConfig.set("liveUpdate.lastStatus", SheepLiveUpdateState.getLastLiveUpdateStatus())
        dataConfig.set("liveUpdate.lastCheckAt", SheepLiveUpdateState.getLastLiveUpdateCheckAt().coerceAtLeast(0L))
        dataConfig.set("dataSchemaVersion", SheepLiveUpdateState.getDataSchemaVersion().coerceAtLeast(0))
        SheepSnapshotState.saveTo(dataConfig)
    }

    private fun savePendingInventories(dataConfig: FileConfiguration) {
        for ((playerId, snapshot) in SheepRuntimeUiState.savedInventoriesInternal()) {
            val basePath = "pendingInventory.$playerId"
            dataConfig.set(basePath + ".contents", InventoryDataUtils.serializeInventoryList(snapshot.contents()))
            dataConfig.set(basePath + ".armor", InventoryDataUtils.serializeInventoryList(snapshot.armor()))
            dataConfig.set(basePath + ".offhand", snapshot.offhand()?.clone())
        }
    }

    private fun loadState(dataConfig: FileConfiguration) {
        SheepEconomyState.loadFrom(dataConfig)
        SheepPrestigeState.loadFrom(
            dataConfig,
            SheepMergeManager.persistencePrestigeDoublePointsMaxLevel(),
            SheepMergeManager.persistenceRainbowSheepLevel()
        )
        SheepSnapshotState.loadFrom(dataConfig)
        SheepUpgradeState.loadFrom(dataConfig, SheepTier.WHITE.level, SheepMergeManager.persistenceRainbowSheepLevel())
        SheepTutorialState.loadFrom(dataConfig)
        SheepVisitAccessState.loadFrom(dataConfig)
        SheepEffectPreferences.loadFrom(dataConfig)
        SheepQuestState.loadFrom(dataConfig)
        SheepComboState.loadFrom(
            dataConfig,
            SheepMergeManager.persistenceComboDecayMaxLevel(),
            SheepMergeManager.persistenceComboGainMaxLevel()
        )
        SheepAutomationState.loadFrom(
            dataConfig,
            SheepMergeManager.persistenceAutomationAutoBuyMaxLevel(),
            SheepMergeManager.persistenceAutomationAutoAbilityMaxLevel(),
            SheepMergeManager.persistenceAutomationSlowAutoMergeMaxLevel(),
            SheepMergeManager.persistenceAutomationSlowAutoShearMaxLevel(),
            SheepMergeManager.persistenceAutomationAutoSpawnMaxLevel(),
            SheepMergeManager.persistenceAutomationAutoPrestigeMaxLevel()
        )
        SheepUiPreferences.loadFrom(
            dataConfig,
            SheepMergeManager.persistenceQuickAccessMaxItems(),
            Predicate(SheepMergeManager::persistenceIsValidQuickAccessAction)
        )
        SheepSacrificeProgression.loadFrom(dataConfig)
        SheepRebirthRuntime.loadFrom(dataConfig)
        SheepLifetimeProgressState.loadFrom(dataConfig)
        SheepAchievementState.loadFrom(
            dataConfig,
            Predicate(SheepMergeManager::persistenceIsValidAchievementId),
            Predicate(SheepMergeManager::persistenceIsValidAchievementMilestoneId)
        )
        SheepLiveUpdateState.loadPersistedState(
            !dataConfig.contains("liveUpdate.enabled") || dataConfig.getBoolean("liveUpdate.enabled", true),
            dataConfig.getString("liveUpdate.stagedVersion", ""),
            dataConfig.getString("liveUpdate.lastStatus", "Not checked yet."),
            dataConfig.getLong("liveUpdate.lastCheckAt", 0L),
            dataConfig.getInt("dataSchemaVersion", 0)
        )
        val playersToClamp = HashSet<UUID>()
        playersToClamp.addAll(SheepEconomyState.getUpgradeTrackedPlayerIds())
        playersToClamp.addAll(SheepPrestigeState.getHigherMaxTrackedPlayerIds())
        playersToClamp.addAll(SheepSacrificeProgression.getUnlockTrackedPlayerIds())
        playersToClamp.forEach(SheepMergeManager::persistenceClampUpgradeLevelsToCurrentCaps)
    }

    private fun loadPendingInventories(dataConfig: FileConfiguration) {
        dataConfig.getConfigurationSection("pendingInventory")?.getKeys(false)?.forEach { key ->
            try {
                val playerId = UUID.fromString(key)
                val basePath = "pendingInventory.$key"
                val contents = InventoryDataUtils.deserializeInventoryList(dataConfig.getList(basePath + ".contents"))
                val armor = InventoryDataUtils.deserializeInventoryList(dataConfig.getList(basePath + ".armor"))
                val offhand = dataConfig.getItemStack(basePath + ".offhand")
                SheepRuntimeUiState.savedInventoriesInternal()[playerId] =
                    InventoryDataUtils.Snapshot(contents, armor, offhand?.clone())
            } catch (_: IllegalArgumentException) {
                // Ignore invalid UUIDs.
            }
        }
    }
}