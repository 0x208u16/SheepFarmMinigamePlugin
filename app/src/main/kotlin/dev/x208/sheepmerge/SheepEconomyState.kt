package dev.x208.sheepmerge

import org.bukkit.configuration.file.FileConfiguration
import java.math.BigInteger
import java.util.UUID

object SheepEconomyState {
    private const val POINTS_KEY = "points"
    private const val EXTRA_LIMIT_KEY = "extraLimit"
    private const val EGG_SPEED_KEY = "eggSpeed"
    private const val WOOL_REGEN_KEY = "woolRegen"
    private const val HIGHER_TIER_CHANCE_KEY = "higherTierChance"

    private val pointsByPlayer: MutableMap<UUID, BigInteger> = HashMap()
    private val extraLimitByPlayer: MutableMap<UUID, Int> = HashMap()
    private val eggSpeedLevelByPlayer: MutableMap<UUID, Int> = HashMap()
    private val woolRegenLevelByPlayer: MutableMap<UUID, Int> = HashMap()
    private val higherTierChanceLevelByPlayer: MutableMap<UUID, Int> = HashMap()

    @JvmStatic fun getPoints(playerId: UUID?, defaultPoints: BigInteger): BigInteger =
        playerId?.let { pointsByPlayer[it] } ?: defaultPoints
    @JvmStatic fun setPoints(playerId: UUID, points: BigInteger) { pointsByPlayer[playerId] = points }
    @JvmStatic fun removePoints(playerId: UUID): Boolean = pointsByPlayer.remove(playerId) != null
    @JvmStatic fun getPointsSnapshot(): Map<UUID, BigInteger> = HashMap(pointsByPlayer)
    @JvmStatic fun getPointsTrackedPlayerIds(): Set<UUID> = HashSet(pointsByPlayer.keys)

    @JvmStatic fun getExtraLimit(playerId: UUID?): Int = playerId?.let { extraLimitByPlayer[it] } ?: 0
    @JvmStatic fun setExtraLimit(playerId: UUID, level: Int) { extraLimitByPlayer[playerId] = level }
    @JvmStatic fun removeExtraLimit(playerId: UUID): Boolean = extraLimitByPlayer.remove(playerId) != null
    @JvmStatic fun getEggSpeedLevel(playerId: UUID?): Int = playerId?.let { eggSpeedLevelByPlayer[it] } ?: 0
    @JvmStatic fun setEggSpeedLevel(playerId: UUID, level: Int) { eggSpeedLevelByPlayer[playerId] = level }
    @JvmStatic fun removeEggSpeedLevel(playerId: UUID): Boolean = eggSpeedLevelByPlayer.remove(playerId) != null
    @JvmStatic fun getWoolRegenLevel(playerId: UUID?): Int = playerId?.let { woolRegenLevelByPlayer[it] } ?: 0
    @JvmStatic fun setWoolRegenLevel(playerId: UUID, level: Int) { woolRegenLevelByPlayer[playerId] = level }
    @JvmStatic fun removeWoolRegenLevel(playerId: UUID): Boolean = woolRegenLevelByPlayer.remove(playerId) != null
    @JvmStatic fun getHigherTierChanceLevel(playerId: UUID?): Int = playerId?.let { higherTierChanceLevelByPlayer[it] } ?: 0
    @JvmStatic fun setHigherTierChanceLevel(playerId: UUID, level: Int) { higherTierChanceLevelByPlayer[playerId] = level }
    @JvmStatic fun removeHigherTierChanceLevel(playerId: UUID): Boolean = higherTierChanceLevelByPlayer.remove(playerId) != null

    @JvmStatic
    fun getUpgradeTrackedPlayerIds(): Set<UUID> = HashSet<UUID>().apply {
        addAll(extraLimitByPlayer.keys)
        addAll(eggSpeedLevelByPlayer.keys)
        addAll(woolRegenLevelByPlayer.keys)
        addAll(higherTierChanceLevelByPlayer.keys)
    }

    @JvmStatic
    fun resetRegularUpgrades(playerId: UUID?) {
        if (playerId == null) return
        extraLimitByPlayer.remove(playerId)
        eggSpeedLevelByPlayer.remove(playerId)
        woolRegenLevelByPlayer.remove(playerId)
        higherTierChanceLevelByPlayer.remove(playerId)
    }

    @JvmStatic
    fun resetAdminPlayer(playerId: UUID?) {
        if (playerId == null) return
        pointsByPlayer.remove(playerId)
        resetRegularUpgrades(playerId)
    }

    @JvmStatic
    fun clear() {
        pointsByPlayer.clear()
        extraLimitByPlayer.clear()
        eggSpeedLevelByPlayer.clear()
        woolRegenLevelByPlayer.clear()
        higherTierChanceLevelByPlayer.clear()
    }

    @JvmStatic
    fun clearPersistedKeys(dataConfig: FileConfiguration) {
        dataConfig.set(POINTS_KEY, null)
        dataConfig.set(EXTRA_LIMIT_KEY, null)
        dataConfig.set(EGG_SPEED_KEY, null)
        dataConfig.set(WOOL_REGEN_KEY, null)
        dataConfig.set(HIGHER_TIER_CHANCE_KEY, null)
    }

    @JvmStatic
    fun saveTo(dataConfig: FileConfiguration) {
        for ((playerId, points) in pointsByPlayer) {
            dataConfig.set("$POINTS_KEY.$playerId", points.toString())
        }
        saveIntMap(dataConfig, EXTRA_LIMIT_KEY, extraLimitByPlayer)
        saveIntMap(dataConfig, EGG_SPEED_KEY, eggSpeedLevelByPlayer)
        saveIntMap(dataConfig, WOOL_REGEN_KEY, woolRegenLevelByPlayer)
        saveIntMap(dataConfig, HIGHER_TIER_CHANCE_KEY, higherTierChanceLevelByPlayer)
    }

    @JvmStatic
    fun loadFrom(dataConfig: FileConfiguration) {
        val pointsSection = dataConfig.getConfigurationSection(POINTS_KEY)
        if (pointsSection != null) {
            for (playerKey in pointsSection.getKeys(false)) {
                try {
                    val playerId = UUID.fromString(playerKey)
                    val path = "$POINTS_KEY.$playerKey"
                    val raw = dataConfig.getString(path, null)
                    val parsed = if (!raw.isNullOrBlank()) {
                        BigInteger(raw.trim())
                    } else {
                        BigInteger.valueOf(dataConfig.getLong(path, 0L).coerceAtLeast(0L))
                    }
                    pointsByPlayer[playerId] = parsed.max(BigInteger.ZERO)
                } catch (_: IllegalArgumentException) {
                    // Preserve legacy behavior by ignoring invalid UUIDs and point values.
                }
            }
        }
        loadIntMap(dataConfig, EXTRA_LIMIT_KEY, extraLimitByPlayer)
        loadIntMap(dataConfig, EGG_SPEED_KEY, eggSpeedLevelByPlayer)
        loadIntMap(dataConfig, WOOL_REGEN_KEY, woolRegenLevelByPlayer)
        loadIntMap(dataConfig, HIGHER_TIER_CHANCE_KEY, higherTierChanceLevelByPlayer)
    }

    private fun saveIntMap(dataConfig: FileConfiguration, key: String, values: Map<UUID, Int>) {
        for ((playerId, value) in values) dataConfig.set("$key.$playerId", value)
    }

    private fun loadIntMap(dataConfig: FileConfiguration, key: String, destination: MutableMap<UUID, Int>) {
        val section = dataConfig.getConfigurationSection(key) ?: return
        for (playerKey in section.getKeys(false)) {
            val playerId = runCatching { UUID.fromString(playerKey) }.getOrNull() ?: continue
            destination[playerId] = dataConfig.getInt("$key.$playerKey", 0)
        }
    }
}