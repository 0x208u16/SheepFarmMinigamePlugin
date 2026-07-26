package dev.x208.sheepmerge

import org.bukkit.configuration.file.FileConfiguration
import java.util.UUID

object SheepQuestState {
    private val questPointsByPlayer = HashMap<UUID, Int>()
    private val nextQuestResetTimestampByPlayer = HashMap<UUID, Long>()
    private val questShearsByPlayer = HashMap<UUID, Int>()
    private val questSpawnsByPlayer = HashMap<UUID, Int>()
    private val questMergesByPlayer = HashMap<UUID, Int>()
    private val questShearsCompleteByPlayer = HashMap<UUID, Boolean>()
    private val questSpawnsCompleteByPlayer = HashMap<UUID, Boolean>()
    private val questMergesCompleteByPlayer = HashMap<UUID, Boolean>()
    private val questUpgradeDurationByPlayer = HashMap<UUID, Int>()
    private val questUpgradePowerByPlayer = HashMap<UUID, Int>()
    private val activeLuckyBurstUntilByPlayer = HashMap<UUID, Long>()
    private val activeLuckyBurstUsesByPlayer = HashMap<UUID, Int>()
    private val luckyBurstEnabledByPlayer = HashMap<UUID, Boolean>()
    private val activeWoolRushUntilByPlayer = HashMap<UUID, Long>()
    private val activeJackpotShearsUntilByPlayer = HashMap<UUID, Long>()
    private val activeAutoMergeUntilByPlayer = HashMap<UUID, Long>()
    private val activeAutoMergeUsesByPlayer = HashMap<UUID, Int>()
    private val autoMergeEnabledByPlayer = HashMap<UUID, Boolean>()
    private val pausedLuckyBurstRemainingMsByPlayer = HashMap<UUID, Long>()
    private val pausedWoolRushRemainingMsByPlayer = HashMap<UUID, Long>()
    private val pausedJackpotShearsRemainingMsByPlayer = HashMap<UUID, Long>()
    private val pausedAutoMergeRemainingMsByPlayer = HashMap<UUID, Long>()
    private val nextAutoMergeAtByPlayer = HashMap<UUID, Long>()
    private val activeAutoShearUntilByPlayer = HashMap<UUID, Long>()
    private val activeAutoShearUsesByPlayer = HashMap<UUID, Int>()
    private val autoShearEnabledByPlayer = HashMap<UUID, Boolean>()
    private val pausedAutoShearRemainingMsByPlayer = HashMap<UUID, Long>()
    private val nextAutoShearAtByPlayer = HashMap<UUID, Long>()
    private val lastTierBoostSoundTimestampByPlayer = HashMap<UUID, Long>()
    private val lastAbilityAuraSoundTimestampByPlayer = HashMap<UUID, Long>()

    @JvmStatic fun questPoints() = questPointsByPlayer
    @JvmStatic fun nextQuestResetTimestamps() = nextQuestResetTimestampByPlayer
    @JvmStatic fun questShears() = questShearsByPlayer
    @JvmStatic fun questSpawns() = questSpawnsByPlayer
    @JvmStatic fun questMerges() = questMergesByPlayer
    @JvmStatic fun questShearsComplete() = questShearsCompleteByPlayer
    @JvmStatic fun questSpawnsComplete() = questSpawnsCompleteByPlayer
    @JvmStatic fun questMergesComplete() = questMergesCompleteByPlayer
    @JvmStatic fun questUpgradeDurations() = questUpgradeDurationByPlayer
    @JvmStatic fun questUpgradePowers() = questUpgradePowerByPlayer
    @JvmStatic fun activeLuckyBurstUntil() = activeLuckyBurstUntilByPlayer
    @JvmStatic fun activeLuckyBurstUses() = activeLuckyBurstUsesByPlayer
    @JvmStatic fun luckyBurstEnabled() = luckyBurstEnabledByPlayer
    @JvmStatic fun activeWoolRushUntil() = activeWoolRushUntilByPlayer
    @JvmStatic fun activeJackpotShearsUntil() = activeJackpotShearsUntilByPlayer
    @JvmStatic fun activeAutoMergeUntil() = activeAutoMergeUntilByPlayer
    @JvmStatic fun activeAutoMergeUses() = activeAutoMergeUsesByPlayer
    @JvmStatic fun autoMergeEnabled() = autoMergeEnabledByPlayer
    @JvmStatic fun pausedLuckyBurstRemaining() = pausedLuckyBurstRemainingMsByPlayer
    @JvmStatic fun pausedWoolRushRemaining() = pausedWoolRushRemainingMsByPlayer
    @JvmStatic fun pausedJackpotShearsRemaining() = pausedJackpotShearsRemainingMsByPlayer
    @JvmStatic fun pausedAutoMergeRemaining() = pausedAutoMergeRemainingMsByPlayer
    @JvmStatic fun nextAutoMergeAt() = nextAutoMergeAtByPlayer
    @JvmStatic fun activeAutoShearUntil() = activeAutoShearUntilByPlayer
    @JvmStatic fun activeAutoShearUses() = activeAutoShearUsesByPlayer
    @JvmStatic fun autoShearEnabled() = autoShearEnabledByPlayer
    @JvmStatic fun pausedAutoShearRemaining() = pausedAutoShearRemainingMsByPlayer
    @JvmStatic fun nextAutoShearAt() = nextAutoShearAtByPlayer
    @JvmStatic fun lastTierBoostSoundTimestamps() = lastTierBoostSoundTimestampByPlayer
    @JvmStatic fun lastAbilityAuraSoundTimestamps() = lastAbilityAuraSoundTimestampByPlayer

    @JvmStatic
    fun resetPlayer(playerId: UUID) {
        allMaps().forEach { it.remove(playerId) }
    }

    @JvmStatic
    fun clear() {
        allMaps().forEach { it.clear() }
    }

    @JvmStatic
    fun clearPersistedKeys(configuration: FileConfiguration) {
        persistedKeys.forEach { configuration.set(it, null) }
    }

    @JvmStatic
    fun saveTo(configuration: FileConfiguration) {
        saveMap(configuration, "questPoints", questPointsByPlayer)
        saveMap(configuration, "questReset", nextQuestResetTimestampByPlayer)
        saveMap(configuration, "questUpgradeDuration", questUpgradeDurationByPlayer)
        saveMap(configuration, "questUpgradePower", questUpgradePowerByPlayer)
        saveMap(configuration, "activeLuckyBurstUntil", activeLuckyBurstUntilByPlayer)
        saveMap(configuration, "activeLuckyBurstUses", activeLuckyBurstUsesByPlayer)
        saveMap(configuration, "luckyBurstEnabled", luckyBurstEnabledByPlayer)
        saveMap(configuration, "activeWoolRushUntil", activeWoolRushUntilByPlayer)
        saveMap(configuration, "activeJackpotShearsUntil", activeJackpotShearsUntilByPlayer)
        saveMap(configuration, "activeAutoMergeUntil", activeAutoMergeUntilByPlayer)
        saveMap(configuration, "activeAutoMergeUses", activeAutoMergeUsesByPlayer)
        saveMap(configuration, "autoMergeEnabled", autoMergeEnabledByPlayer)
        saveMap(configuration, "activeAutoShearUntil", activeAutoShearUntilByPlayer)
        saveMap(configuration, "activeAutoShearUses", activeAutoShearUsesByPlayer)
        saveMap(configuration, "autoShearEnabled", autoShearEnabledByPlayer)
        saveMap(configuration, "pausedLuckyBurstRemaining", pausedLuckyBurstRemainingMsByPlayer)
        saveMap(configuration, "pausedWoolRushRemaining", pausedWoolRushRemainingMsByPlayer)
        saveMap(configuration, "pausedJackpotShearsRemaining", pausedJackpotShearsRemainingMsByPlayer)
        saveMap(configuration, "pausedAutoMergeRemaining", pausedAutoMergeRemainingMsByPlayer)
        saveMap(configuration, "pausedAutoShearRemaining", pausedAutoShearRemainingMsByPlayer)
    }

    @JvmStatic
    fun loadFrom(configuration: FileConfiguration) {
        loadIntMap(configuration, "questPoints", questPointsByPlayer)
        loadLongMap(configuration, "questReset", nextQuestResetTimestampByPlayer)
        loadIntMap(configuration, "questUpgradeDuration", questUpgradeDurationByPlayer)
        loadIntMap(configuration, "questUpgradePower", questUpgradePowerByPlayer)
        loadLongMap(configuration, "activeLuckyBurstUntil", activeLuckyBurstUntilByPlayer)
        loadIntMap(configuration, "activeLuckyBurstUses", activeLuckyBurstUsesByPlayer, clampToZero = true)
        loadBooleanMap(configuration, "luckyBurstEnabled", luckyBurstEnabledByPlayer, true)
        loadLongMap(configuration, "activeWoolRushUntil", activeWoolRushUntilByPlayer)
        loadLongMap(configuration, "activeJackpotShearsUntil", activeJackpotShearsUntilByPlayer)
        loadLongMap(configuration, "activeAutoMergeUntil", activeAutoMergeUntilByPlayer)
        loadIntMap(configuration, "activeAutoMergeUses", activeAutoMergeUsesByPlayer, clampToZero = true)
        loadBooleanMap(configuration, "autoMergeEnabled", autoMergeEnabledByPlayer, true)
        loadLongMap(configuration, "activeAutoShearUntil", activeAutoShearUntilByPlayer)
        loadIntMap(configuration, "activeAutoShearUses", activeAutoShearUsesByPlayer, clampToZero = true)
        loadBooleanMap(configuration, "autoShearEnabled", autoShearEnabledByPlayer, true)
        loadLongMap(configuration, "pausedLuckyBurstRemaining", pausedLuckyBurstRemainingMsByPlayer)
        loadLongMap(configuration, "pausedWoolRushRemaining", pausedWoolRushRemainingMsByPlayer)
        loadLongMap(configuration, "pausedJackpotShearsRemaining", pausedJackpotShearsRemainingMsByPlayer)
        loadLongMap(configuration, "pausedAutoMergeRemaining", pausedAutoMergeRemainingMsByPlayer)
        loadLongMap(configuration, "pausedAutoShearRemaining", pausedAutoShearRemainingMsByPlayer)
    }

    private fun allMaps(): List<MutableMap<UUID, *>> = listOf(
        questPointsByPlayer, nextQuestResetTimestampByPlayer, questShearsByPlayer, questSpawnsByPlayer,
        questMergesByPlayer, questShearsCompleteByPlayer, questSpawnsCompleteByPlayer,
        questMergesCompleteByPlayer, questUpgradeDurationByPlayer, questUpgradePowerByPlayer,
        activeLuckyBurstUntilByPlayer, activeLuckyBurstUsesByPlayer, luckyBurstEnabledByPlayer,
        activeWoolRushUntilByPlayer, activeJackpotShearsUntilByPlayer, activeAutoMergeUntilByPlayer,
        activeAutoMergeUsesByPlayer, autoMergeEnabledByPlayer, pausedLuckyBurstRemainingMsByPlayer,
        pausedWoolRushRemainingMsByPlayer, pausedJackpotShearsRemainingMsByPlayer,
        pausedAutoMergeRemainingMsByPlayer, nextAutoMergeAtByPlayer, activeAutoShearUntilByPlayer,
        activeAutoShearUsesByPlayer, autoShearEnabledByPlayer, pausedAutoShearRemainingMsByPlayer,
        nextAutoShearAtByPlayer, lastTierBoostSoundTimestampByPlayer, lastAbilityAuraSoundTimestampByPlayer,
    )

    private fun saveMap(configuration: FileConfiguration, key: String, values: Map<UUID, *>) {
        values.forEach { (playerId, value) -> configuration.set("$key.$playerId", value) }
    }

    private fun loadIntMap(
        configuration: FileConfiguration,
        key: String,
        destination: MutableMap<UUID, Int>,
        clampToZero: Boolean = false,
    ) {
        loadKeys(configuration, key) { playerId, path ->
            val value = configuration.getInt(path, 0)
            destination[playerId] = if (clampToZero) value.coerceAtLeast(0) else value
        }
    }

    private fun loadLongMap(configuration: FileConfiguration, key: String, destination: MutableMap<UUID, Long>) {
        loadKeys(configuration, key) { playerId, path ->
            destination[playerId] = configuration.getLong(path, 0L).coerceAtLeast(0L)
        }
    }

    private fun loadBooleanMap(
        configuration: FileConfiguration,
        key: String,
        destination: MutableMap<UUID, Boolean>,
        default: Boolean,
    ) {
        loadKeys(configuration, key) { playerId, path ->
            destination[playerId] = configuration.getBoolean(path, default)
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
        "questPoints", "questReset", "questUpgradeDuration", "questUpgradePower",
        "activeLuckyBurstUntil", "activeLuckyBurstUses", "luckyBurstEnabled", "activeWoolRushUntil",
        "activeJackpotShearsUntil", "activeAutoMergeUntil", "activeAutoMergeUses", "autoMergeEnabled",
        "activeAutoShearUntil", "activeAutoShearUses", "autoShearEnabled", "pausedLuckyBurstRemaining",
        "pausedWoolRushRemaining", "pausedJackpotShearsRemaining", "pausedAutoMergeRemaining",
        "pausedAutoShearRemaining",
    )
}