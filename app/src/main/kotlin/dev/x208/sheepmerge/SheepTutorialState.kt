package dev.x208.sheepmerge

import org.bukkit.configuration.file.FileConfiguration
import java.util.UUID

object SheepTutorialState {
    enum class Section {
        UPGRADE_OPENED,
        QUEST_OPENED,
        QUEST_UPGRADES_OPENED,
        PRESTIGE_OPENED,
        ABILITY_USED,
        SHEAR_UPGRADED,
        REGULAR_UPGRADES_BOUGHT,
        PRESTIGED_ONCE,
        SHEAR_SHOP_OPENED,
    }

    private val tutorialCompletedByPlayer = HashMap<UUID, Boolean>()
    private val tutorialShearsByPlayer = HashMap<UUID, Int>()
    private val tutorialSpawnsByPlayer = HashMap<UUID, Int>()
    private val tutorialMergesByPlayer = HashMap<UUID, Int>()
    private val tutorialUpgradeOpenedByPlayer = HashMap<UUID, Boolean>()
    private val tutorialQuestOpenedByPlayer = HashMap<UUID, Boolean>()
    private val tutorialQuestUpgradesOpenedByPlayer = HashMap<UUID, Boolean>()
    private val tutorialPrestigeOpenedByPlayer = HashMap<UUID, Boolean>()
    private val tutorialAbilityUsedByPlayer = HashMap<UUID, Boolean>()
    private val tutorialShearUpgradedByPlayer = HashMap<UUID, Boolean>()
    private val tutorialRegularUpgradesBoughtByPlayer = HashMap<UUID, Boolean>()
    private val tutorialShearTaskRewardGrantedByPlayer = HashMap<UUID, Boolean>()
    private val tutorialPrestigePrepRewardGrantedByPlayer = HashMap<UUID, Boolean>()
    private val tutorialPrestigedOnceByPlayer = HashMap<UUID, Boolean>()
    private val tutorialShearShopOpenedByPlayer = HashMap<UUID, Boolean>()
    private val tutorialBypassedByPlayer = HashMap<UUID, Boolean>()
    private val tutorialStartedAtByPlayer = HashMap<UUID, Long>()
    private val lastTutorialReminderTimestampByPlayer = HashMap<UUID, Long>()
    private val lastTutorialTaskTitleTimestampByPlayer = HashMap<UUID, Long>()
    private val lastTutorialTaskTitleStepByPlayer = HashMap<UUID, String>()
    private val lastTutorialStatusFeedTimestampByPlayer = HashMap<UUID, Long>()
    private val lastTutorialProgressFeedLineByPlayer = HashMap<UUID, String>()
    private val lastTutorialStepFeedLineByPlayer = HashMap<UUID, String>()
    private val lastTutorialFocusNotificationTimestampByPlayer = HashMap<UUID, Long>()
    private val lastTutorialMergePointsReminderTimestampByPlayer = HashMap<UUID, Long>()

    @JvmStatic
    fun isCompleted(playerId: UUID): Boolean = tutorialCompletedByPlayer.getOrDefault(playerId, false)

    @JvmStatic
    fun setCompleted(playerId: UUID, completed: Boolean) {
        tutorialCompletedByPlayer[playerId] = completed
    }

    @JvmStatic
    fun isBypassed(playerId: UUID): Boolean = tutorialBypassedByPlayer.getOrDefault(playerId, false)

    @JvmStatic
    fun setBypassed(playerId: UUID, bypassed: Boolean) {
        tutorialBypassedByPlayer[playerId] = bypassed
    }

    @JvmStatic
    fun completedPlayerIds(): Set<UUID> = HashSet(tutorialCompletedByPlayer.keys)

    @JvmStatic
    fun bypassedPlayerIds(): Set<UUID> = HashSet(tutorialBypassedByPlayer.keys)

    @JvmStatic
    fun getShears(playerId: UUID): Int = tutorialShearsByPlayer.getOrDefault(playerId, 0)

    @JvmStatic
    fun setShears(playerId: UUID, count: Int) {
        tutorialShearsByPlayer[playerId] = count
    }

    @JvmStatic
    fun getSpawns(playerId: UUID): Int = tutorialSpawnsByPlayer.getOrDefault(playerId, 0)

    @JvmStatic
    fun setSpawns(playerId: UUID, count: Int) {
        tutorialSpawnsByPlayer[playerId] = count
    }

    @JvmStatic
    fun getMerges(playerId: UUID): Int = tutorialMergesByPlayer.getOrDefault(playerId, 0)

    @JvmStatic
    fun setMerges(playerId: UUID, count: Int) {
        tutorialMergesByPlayer[playerId] = count
    }

    @JvmStatic
    fun isSectionComplete(playerId: UUID, section: Section): Boolean =
        sectionMap(section).getOrDefault(playerId, false)

    @JvmStatic
    fun setSectionComplete(playerId: UUID, section: Section) {
        sectionMap(section)[playerId] = true
    }

    @JvmStatic
    fun isShearTaskRewardGranted(playerId: UUID): Boolean =
        tutorialShearTaskRewardGrantedByPlayer.getOrDefault(playerId, false)

    @JvmStatic
    fun setShearTaskRewardGranted(playerId: UUID) {
        tutorialShearTaskRewardGrantedByPlayer[playerId] = true
    }

    @JvmStatic
    fun isPrestigePrepRewardGranted(playerId: UUID): Boolean =
        tutorialPrestigePrepRewardGrantedByPlayer.getOrDefault(playerId, false)

    @JvmStatic
    fun setPrestigePrepRewardGranted(playerId: UUID) {
        tutorialPrestigePrepRewardGrantedByPlayer[playerId] = true
    }

    @JvmStatic
    fun ensureStartedAt(playerId: UUID, timestamp: Long): Long =
        tutorialStartedAtByPlayer.getOrPut(playerId) { timestamp }

    @JvmStatic
    fun setStartedAt(playerId: UUID, timestamp: Long) {
        tutorialStartedAtByPlayer[playerId] = timestamp
    }

    @JvmStatic
    fun getLastReminderTimestamp(playerId: UUID): Long =
        lastTutorialReminderTimestampByPlayer.getOrDefault(playerId, 0L)

    @JvmStatic
    fun setLastReminderTimestamp(playerId: UUID, timestamp: Long) {
        lastTutorialReminderTimestampByPlayer[playerId] = timestamp
    }

    @JvmStatic
    fun clearLastReminderTimestamp(playerId: UUID) {
        lastTutorialReminderTimestampByPlayer.remove(playerId)
    }

    @JvmStatic
    fun getLastTaskTitleTimestamp(playerId: UUID): Long =
        lastTutorialTaskTitleTimestampByPlayer.getOrDefault(playerId, 0L)

    @JvmStatic
    fun setLastTaskTitleTimestamp(playerId: UUID, timestamp: Long) {
        lastTutorialTaskTitleTimestampByPlayer[playerId] = timestamp
    }

    @JvmStatic
    fun getLastTaskTitleStep(playerId: UUID): String? = lastTutorialTaskTitleStepByPlayer[playerId]

    @JvmStatic
    fun setLastTaskTitleStep(playerId: UUID, step: String) {
        lastTutorialTaskTitleStepByPlayer[playerId] = step
    }

    @JvmStatic
    fun getLastStatusFeedTimestamp(playerId: UUID): Long =
        lastTutorialStatusFeedTimestampByPlayer.getOrDefault(playerId, 0L)

    @JvmStatic
    fun setLastStatusFeedTimestamp(playerId: UUID, timestamp: Long) {
        lastTutorialStatusFeedTimestampByPlayer[playerId] = timestamp
    }

    @JvmStatic
    fun getLastProgressFeedLine(playerId: UUID): String? = lastTutorialProgressFeedLineByPlayer[playerId]

    @JvmStatic
    fun setLastProgressFeedLine(playerId: UUID, line: String) {
        lastTutorialProgressFeedLineByPlayer[playerId] = line
    }

    @JvmStatic
    fun getLastStepFeedLine(playerId: UUID): String? = lastTutorialStepFeedLineByPlayer[playerId]

    @JvmStatic
    fun setLastStepFeedLine(playerId: UUID, line: String) {
        lastTutorialStepFeedLineByPlayer[playerId] = line
    }

    @JvmStatic
    fun getLastFocusNotificationTimestamp(playerId: UUID): Long =
        lastTutorialFocusNotificationTimestampByPlayer.getOrDefault(playerId, 0L)

    @JvmStatic
    fun setLastFocusNotificationTimestamp(playerId: UUID, timestamp: Long) {
        lastTutorialFocusNotificationTimestampByPlayer[playerId] = timestamp
    }

    @JvmStatic
    fun getLastMergePointsReminderTimestamp(playerId: UUID): Long =
        lastTutorialMergePointsReminderTimestampByPlayer.getOrDefault(playerId, 0L)

    @JvmStatic
    fun setLastMergePointsReminderTimestamp(playerId: UUID, timestamp: Long) {
        lastTutorialMergePointsReminderTimestampByPlayer[playerId] = timestamp
    }

    @JvmStatic
    fun clearRuntimeState(playerId: UUID) {
        tutorialStartedAtByPlayer.remove(playerId)
        lastTutorialReminderTimestampByPlayer.remove(playerId)
        lastTutorialTaskTitleTimestampByPlayer.remove(playerId)
        lastTutorialTaskTitleStepByPlayer.remove(playerId)
        lastTutorialStatusFeedTimestampByPlayer.remove(playerId)
        lastTutorialProgressFeedLineByPlayer.remove(playerId)
        lastTutorialStepFeedLineByPlayer.remove(playerId)
        lastTutorialFocusNotificationTimestampByPlayer.remove(playerId)
        lastTutorialMergePointsReminderTimestampByPlayer.remove(playerId)
    }

    @JvmStatic
    fun resetPlayer(playerId: UUID) {
        tutorialCompletedByPlayer.remove(playerId)
        tutorialShearsByPlayer.remove(playerId)
        tutorialSpawnsByPlayer.remove(playerId)
        tutorialMergesByPlayer.remove(playerId)
        tutorialUpgradeOpenedByPlayer.remove(playerId)
        tutorialQuestOpenedByPlayer.remove(playerId)
        tutorialQuestUpgradesOpenedByPlayer.remove(playerId)
        tutorialPrestigeOpenedByPlayer.remove(playerId)
        tutorialAbilityUsedByPlayer.remove(playerId)
        tutorialShearUpgradedByPlayer.remove(playerId)
        tutorialRegularUpgradesBoughtByPlayer.remove(playerId)
        tutorialShearTaskRewardGrantedByPlayer.remove(playerId)
        tutorialPrestigePrepRewardGrantedByPlayer.remove(playerId)
        tutorialPrestigedOnceByPlayer.remove(playerId)
        tutorialShearShopOpenedByPlayer.remove(playerId)
        tutorialBypassedByPlayer.remove(playerId)
        clearRuntimeState(playerId)
    }

    @JvmStatic
    fun clear() {
        tutorialCompletedByPlayer.clear()
        tutorialShearsByPlayer.clear()
        tutorialSpawnsByPlayer.clear()
        tutorialMergesByPlayer.clear()
        tutorialUpgradeOpenedByPlayer.clear()
        tutorialQuestOpenedByPlayer.clear()
        tutorialQuestUpgradesOpenedByPlayer.clear()
        tutorialPrestigeOpenedByPlayer.clear()
        tutorialAbilityUsedByPlayer.clear()
        tutorialShearUpgradedByPlayer.clear()
        tutorialRegularUpgradesBoughtByPlayer.clear()
        tutorialShearTaskRewardGrantedByPlayer.clear()
        tutorialPrestigePrepRewardGrantedByPlayer.clear()
        tutorialPrestigedOnceByPlayer.clear()
        tutorialShearShopOpenedByPlayer.clear()
        tutorialBypassedByPlayer.clear()
        tutorialStartedAtByPlayer.clear()
        lastTutorialReminderTimestampByPlayer.clear()
        lastTutorialTaskTitleTimestampByPlayer.clear()
        lastTutorialTaskTitleStepByPlayer.clear()
        lastTutorialStatusFeedTimestampByPlayer.clear()
        lastTutorialProgressFeedLineByPlayer.clear()
        lastTutorialStepFeedLineByPlayer.clear()
        lastTutorialFocusNotificationTimestampByPlayer.clear()
        lastTutorialMergePointsReminderTimestampByPlayer.clear()
    }

    @JvmStatic
    fun saveTo(configuration: FileConfiguration) {
        clearPersistedKeys(configuration)
        saveMap(configuration, "tutorialCompleted", tutorialCompletedByPlayer)
        saveMap(configuration, "tutorialBypassed", tutorialBypassedByPlayer)
        saveMap(configuration, "tutorialShears", tutorialShearsByPlayer)
        saveMap(configuration, "tutorialSpawns", tutorialSpawnsByPlayer)
        saveMap(configuration, "tutorialMerges", tutorialMergesByPlayer)
        saveMap(configuration, "tutorialUpgradeOpened", tutorialUpgradeOpenedByPlayer)
        saveMap(configuration, "tutorialQuestOpened", tutorialQuestOpenedByPlayer)
        saveMap(configuration, "tutorialQuestUpgradesOpened", tutorialQuestUpgradesOpenedByPlayer)
        saveMap(configuration, "tutorialPrestigeOpened", tutorialPrestigeOpenedByPlayer)
        saveMap(configuration, "tutorialAbilityUsed", tutorialAbilityUsedByPlayer)
        saveMap(configuration, "tutorialShearUpgraded", tutorialShearUpgradedByPlayer)
        saveMap(configuration, "tutorialRegularUpgradesBought", tutorialRegularUpgradesBoughtByPlayer)
        saveMap(configuration, "tutorialShearTaskRewardGranted", tutorialShearTaskRewardGrantedByPlayer)
        saveMap(configuration, "tutorialPrestigePrepRewardGranted", tutorialPrestigePrepRewardGrantedByPlayer)
        saveMap(configuration, "tutorialPrestigedOnce", tutorialPrestigedOnceByPlayer)
        saveMap(configuration, "tutorialShearShopOpened", tutorialShearShopOpenedByPlayer)
    }

    @JvmStatic
    fun loadFrom(configuration: FileConfiguration) {
        loadBooleanMap(configuration, "tutorialCompleted", tutorialCompletedByPlayer)
        loadBooleanMap(configuration, "tutorialBypassed", tutorialBypassedByPlayer)
        loadIntMap(configuration, "tutorialShears", tutorialShearsByPlayer)
        loadIntMap(configuration, "tutorialSpawns", tutorialSpawnsByPlayer)
        loadIntMap(configuration, "tutorialMerges", tutorialMergesByPlayer)
        loadBooleanMap(configuration, "tutorialUpgradeOpened", tutorialUpgradeOpenedByPlayer)
        loadBooleanMap(configuration, "tutorialQuestOpened", tutorialQuestOpenedByPlayer)
        loadBooleanMap(configuration, "tutorialQuestUpgradesOpened", tutorialQuestUpgradesOpenedByPlayer)
        loadBooleanMap(configuration, "tutorialPrestigeOpened", tutorialPrestigeOpenedByPlayer)
        loadBooleanMap(configuration, "tutorialAbilityUsed", tutorialAbilityUsedByPlayer)
        loadBooleanMap(configuration, "tutorialShearUpgraded", tutorialShearUpgradedByPlayer)
        loadBooleanMap(configuration, "tutorialRegularUpgradesBought", tutorialRegularUpgradesBoughtByPlayer)
        loadBooleanMap(configuration, "tutorialShearTaskRewardGranted", tutorialShearTaskRewardGrantedByPlayer)
        loadBooleanMap(configuration, "tutorialPrestigePrepRewardGranted", tutorialPrestigePrepRewardGrantedByPlayer)
        loadBooleanMap(configuration, "tutorialPrestigedOnce", tutorialPrestigedOnceByPlayer)
        loadBooleanMap(configuration, "tutorialShearShopOpened", tutorialShearShopOpenedByPlayer)
    }

    private fun sectionMap(section: Section): MutableMap<UUID, Boolean> = when (section) {
        Section.UPGRADE_OPENED -> tutorialUpgradeOpenedByPlayer
        Section.QUEST_OPENED -> tutorialQuestOpenedByPlayer
        Section.QUEST_UPGRADES_OPENED -> tutorialQuestUpgradesOpenedByPlayer
        Section.PRESTIGE_OPENED -> tutorialPrestigeOpenedByPlayer
        Section.ABILITY_USED -> tutorialAbilityUsedByPlayer
        Section.SHEAR_UPGRADED -> tutorialShearUpgradedByPlayer
        Section.REGULAR_UPGRADES_BOUGHT -> tutorialRegularUpgradesBoughtByPlayer
        Section.PRESTIGED_ONCE -> tutorialPrestigedOnceByPlayer
        Section.SHEAR_SHOP_OPENED -> tutorialShearShopOpenedByPlayer
    }

    private fun clearPersistedKeys(configuration: FileConfiguration) {
        KEYS_CLEARED_BEFORE_SAVE.forEach { configuration.set(it, null) }
    }

    private fun saveMap(configuration: FileConfiguration, path: String, values: Map<UUID, *>) {
        values.forEach { (playerId, value) -> configuration.set("$path.$playerId", value) }
    }

    private fun loadBooleanMap(
        configuration: FileConfiguration,
        path: String,
        destination: MutableMap<UUID, Boolean>,
    ) {
        configuration.getConfigurationSection(path)?.getKeys(false)?.forEach { key ->
            parsePlayerId(key)?.let { destination[it] = configuration.getBoolean("$path.$key", false) }
        }
    }

    private fun loadIntMap(
        configuration: FileConfiguration,
        path: String,
        destination: MutableMap<UUID, Int>,
    ) {
        configuration.getConfigurationSection(path)?.getKeys(false)?.forEach { key ->
            parsePlayerId(key)?.let { destination[it] = configuration.getInt("$path.$key", 0) }
        }
    }

    private fun parsePlayerId(value: String): UUID? = try {
        UUID.fromString(value)
    } catch (_: IllegalArgumentException) {
        null
    }

    private val KEYS_CLEARED_BEFORE_SAVE = listOf(
        "tutorialCompleted",
        "tutorialBypassed",
        "tutorialShears",
        "tutorialSpawns",
        "tutorialMerges",
        "tutorialUpgradeOpened",
        "tutorialQuestOpened",
        "tutorialQuestUpgradesOpened",
        "tutorialPrestigeOpened",
        "tutorialAbilityUsed",
        "tutorialShearUpgraded",
        "tutorialRegularUpgradesBought",
        "tutorialPrestigedOnce",
        "tutorialShearShopOpened",
    )
}