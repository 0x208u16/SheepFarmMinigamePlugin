package dev.x208.sheepmerge

import org.bukkit.entity.Player
import java.math.BigInteger

object SheepEconomyRuntime {
    private const val REGULAR_POINTS_UPGRADE_COST_MULTIPLIER = 6
    private const val LIMIT_UPGRADE_STEP = 5
    private const val SHEAR_WOOL_SAVE_CHANCE_PER_LEVEL = 5
    private const val SHEAR_TIER_BOOST_CHANCE_PER_LEVEL = 1
    private const val SHEAR_WOOL_SAVE_CHANCE_CAP = 50
    private const val SHEAR_TIER_BOOST_CHANCE_CAP = 75
    private const val SHEAR_WOOL_SAVE_MAX_LEVEL = SHEAR_WOOL_SAVE_CHANCE_CAP / SHEAR_WOOL_SAVE_CHANCE_PER_LEVEL
    private const val SHEAR_TIER_BOOST_MAX_LEVEL = 25

    @JvmStatic
    fun getPlayerLimit(player: Player?, baseLimit: Int): Int {
        if (player == null) return baseLimit
        return getOwnerLimit(player.uniqueId, baseLimit)
    }

    @JvmStatic
    fun getOwnerLimit(playerId: java.util.UUID?, baseLimit: Int): Int {
        if (playerId == null) return baseLimit
        val maximum = SheepMergeManager.economyMaxSheepLimit(playerId)
        return (baseLimit + SheepEconomyState.getExtraLimit(playerId).coerceAtLeast(0)).coerceAtMost(maximum)
    }

    @JvmStatic fun getEggSpeedLevel(player: Player?): Int = player?.let { SheepEconomyState.getEggSpeedLevel(it.uniqueId) } ?: 0
    @JvmStatic fun getWoolRegenLevel(player: Player?): Int =
        player?.let { SheepEconomyState.getWoolRegenLevel(it.uniqueId).coerceAtLeast(0) } ?: 0
    @JvmStatic fun getHigherTierChanceLevel(player: Player?): Int =
        player?.let { SheepEconomyState.getHigherTierChanceLevel(it.uniqueId) } ?: 0

    @JvmStatic
    fun getEggIntervalSeconds(player: Player?, baseInterval: Int, minimumInterval: Int): Int {
        if (player == null) return baseInterval
        return (baseInterval - SheepEconomyState.getEggSpeedLevel(player.uniqueId)).coerceAtLeast(minimumInterval)
    }

    @JvmStatic
    fun getEggSpeedMaxLevel(playerId: java.util.UUID?, baseMaxLevel: Int, capBonusPerLevel: Int, hardCap: Int): Int {
        if (playerId == null) return 0
        val computed = baseMaxLevel.toLong() +
            SheepMergeManager.economyPrestigeHigherMaxLevel(playerId).toLong() * capBonusPerLevel
        return computed.coerceAtMost(hardCap.toLong()).toInt()
    }

    @JvmStatic
    fun getWoolRegenMaxLevel(playerId: java.util.UUID?, baseMaxLevel: Int, capBonusPerLevel: Int): Int {
        if (playerId == null) return 0
        val computed = baseMaxLevel.toLong() +
            SheepMergeManager.economyPrestigeHigherMaxLevel(playerId).toLong() * capBonusPerLevel
        return computed.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }

    @JvmStatic
    fun getHigherTierChanceMaxLevel(
        playerId: java.util.UUID?,
        baseMaxLevel: Int,
        capBonusPerLevel: Int,
        softCap: Int,
        hardCap: Int,
    ): Int {
        if (playerId == null) return 0
        val computed = baseMaxLevel.toLong() +
            SheepMergeManager.economyPrestigeHigherMaxLevel(playerId).toLong() * capBonusPerLevel
        return computed.coerceAtMost(softCap.toLong()).coerceAtMost(hardCap.toLong()).toInt()
    }

    @JvmStatic
    fun getHigherTierChancePercent(playerId: java.util.UUID?, abilityBonus: Int, baseCap: Int): Int {
        if (playerId == null) return 0
        val base = (SheepEconomyState.getHigherTierChanceLevel(playerId) * 5).coerceAtMost(baseCap)
        return (base + abilityBonus.coerceAtLeast(0)).coerceAtMost(100)
    }

    @JvmStatic
    fun getPoints(player: Player?, startingPoints: BigInteger): BigInteger =
        player?.let { SheepEconomyState.getPoints(it.uniqueId, startingPoints) } ?: BigInteger.ZERO

    @JvmStatic
    fun getPointsLong(player: Player?, startingPoints: BigInteger): Long {
        val points = getPoints(player, startingPoints)
        return when {
            points.signum() <= 0 -> 0L
            points > BigInteger.valueOf(Long.MAX_VALUE) -> Long.MAX_VALUE
            else -> points.toLong()
        }
    }

    @JvmStatic
    fun addPoints(player: Player?, points: BigInteger?, startingPoints: BigInteger, multiplier: Int) {
        if (player == null || points == null || points.signum() <= 0) return
        val adjusted = points.multiply(BigInteger.valueOf(multiplier.coerceAtLeast(1).toLong()))
        SheepEconomyState.setPoints(player.uniqueId, getPoints(player, startingPoints).add(adjusted))
        SheepMergeManager.economyAfterPointsAdded(player, adjusted)
    }

    @JvmStatic
    fun trySpendPoints(player: Player?, points: BigInteger?, startingPoints: BigInteger): Boolean {
        if (player == null || points == null || points.signum() <= 0) return false
        val current = getPoints(player, startingPoints)
        if (current < points) return false
        SheepEconomyState.setPoints(player.uniqueId, current.subtract(points))
        SheepMergeManager.economyAfterPointsChanged()
        return true
    }

    @JvmStatic
    fun adminGivePoints(player: Player?, amount: Long, startingPoints: BigInteger) {
        if (player == null || amount == 0L) return
        val next = getPoints(player, startingPoints).add(BigInteger.valueOf(amount)).max(BigInteger.ZERO)
        SheepEconomyState.setPoints(player.uniqueId, next)
        SheepMergeManager.economyAfterPointsChanged()
    }

    @JvmStatic
    fun adminSetPoints(player: Player?, amount: Long) {
        if (player == null) return
        SheepEconomyState.setPoints(player.uniqueId, BigInteger.valueOf(amount.coerceAtLeast(0L)))
        SheepMergeManager.economyAfterPointsChanged()
    }

    @JvmStatic
    fun doubledUpgradeCost(baseCost: Int, level: Int): BigInteger {
        if (baseCost <= 0) return BigInteger.ZERO
        return BigInteger.valueOf(baseCost.toLong()).shiftLeft(level.coerceAtLeast(0))
    }

    @JvmStatic
    fun scaleRegularUpgradeBaseCost(baseCost: Int): Int {
        var scaled = baseCost.toLong().coerceAtLeast(1L) * REGULAR_POINTS_UPGRADE_COST_MULTIPLIER
        if (scaled and 1L != 0L) scaled++
        return scaled.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }

    @JvmStatic
    fun getLimitUpgradeCost(player: Player?, baseCost: Int): BigInteger =
        doubledUpgradeCost(scaleRegularUpgradeBaseCost(baseCost), getLimitUpgradeLevel(player))

    @JvmStatic
    fun getLimitUpgradeLevel(player: Player?): Int {
        if (player == null) return 0
        val extra = SheepEconomyState.getExtraLimit(player.uniqueId).coerceAtLeast(0)
        val maxExtra = SheepMergeManager.economyMaxSheepLimit(player.uniqueId) - SheepMergeManager.economyBaseSheepLimit()
        return extra.coerceAtMost(maxExtra) / LIMIT_UPGRADE_STEP
    }

    @JvmStatic
    fun upgradeLimit(player: Player?, baseCost: Int): Boolean {
        if (player == null) return false
        val maxLimit = SheepMergeManager.economyMaxSheepLimit(player.uniqueId)
        if (SheepMergeManager.getPlayerLimit(player) >= maxLimit) return false
        val cost = getLimitUpgradeCost(player, baseCost)
        if (!spendUpgradePoints(player, cost)) return false
        val maxExtra = maxLimit - SheepMergeManager.economyBaseSheepLimit()
        val current = SheepEconomyState.getExtraLimit(player.uniqueId).coerceAtLeast(0)
        SheepEconomyState.setExtraLimit(player.uniqueId, (current + LIMIT_UPGRADE_STEP).coerceAtMost(maxExtra))
        SheepMergeManager.economySaveData()
        return true
    }

    @JvmStatic
    fun getRegularUpgradeCost(baseCost: Int, level: Int): BigInteger =
        doubledUpgradeCost(scaleRegularUpgradeBaseCost(baseCost), level)

    @JvmStatic
    fun upgradeEggSpeed(player: Player?, baseCost: Int): Boolean {
        if (player == null) return false
        val current = SheepMergeManager.getEggSpeedLevel(player)
        if (current >= SheepMergeManager.getEggSpeedMaxLevel(player)) return false
        if (!spendUpgradePoints(player, getRegularUpgradeCost(baseCost, current))) return false
        SheepEconomyState.setEggSpeedLevel(player.uniqueId, current + 1)
        SheepMergeManager.economyResetEggTimer(player)
        SheepMergeManager.economySaveData()
        return true
    }

    @JvmStatic
    fun upgradeWoolRegen(player: Player?, baseCost: Int): Boolean {
        if (player == null) return false
        val current = SheepMergeManager.getWoolRegenLevel(player)
        if (current >= SheepMergeManager.getWoolRegenMaxLevel(player)) return false
        if (!spendUpgradePoints(player, getRegularUpgradeCost(baseCost, current))) return false
        val next = current + 1
        SheepEconomyState.setWoolRegenLevel(player.uniqueId, next)
        SheepMergeManager.economyApplyWoolRegenReduction(player, current, next)
        SheepMergeManager.economySaveData()
        return true
    }

    @JvmStatic
    fun upgradeHigherTierChance(player: Player?, baseCost: Int): Boolean {
        if (player == null) return false
        val current = SheepMergeManager.getHigherTierChanceLevel(player)
        if (current >= SheepMergeManager.getHigherTierChanceMaxLevel(player)) return false
        if (!spendUpgradePoints(player, getRegularUpgradeCost(baseCost, current))) return false
        SheepEconomyState.setHigherTierChanceLevel(player.uniqueId, current + 1)
        SheepMergeManager.economySaveData()
        return true
    }

    @JvmStatic fun getShearShopLevel(player: Player?): Int = player?.let { SheepUpgradeState.getShearShopLevel(it.uniqueId) } ?: 0
    @JvmStatic fun getShearWoolSaveLevel(player: Player?): Int = player?.let { SheepUpgradeState.getShearWoolSaveLevel(it.uniqueId) } ?: 0
    @JvmStatic fun getShearTierBoostLevel(player: Player?): Int = player?.let { SheepUpgradeState.getShearTierBoostLevel(it.uniqueId) } ?: 0
    @JvmStatic fun getShearPointGainUpgradeLevel(player: Player?): Int = (getShearShopLevel(player) + 1).coerceAtLeast(1)
    @JvmStatic fun getShearPointMultiplier(player: Player?): Int = getShearPointGainUpgradeLevel(player)
    @JvmStatic fun getShearWoolSaveChancePercent(player: Player?): Int =
        (getShearWoolSaveLevel(player) * SHEAR_WOOL_SAVE_CHANCE_PER_LEVEL).coerceAtMost(SHEAR_WOOL_SAVE_CHANCE_CAP)
    @JvmStatic fun getShearTierBoostChancePercent(player: Player?): Int =
        (getShearTierBoostLevel(player) * SHEAR_TIER_BOOST_CHANCE_PER_LEVEL).coerceAtMost(SHEAR_TIER_BOOST_CHANCE_CAP)

    @JvmStatic
    fun getShearUpgradeCost(player: Player?, baseCost: Int): BigInteger =
        getRegularUpgradeCost(baseCost, getShearShopLevel(player))

    @JvmStatic
    fun getShearWoolSaveUpgradeCost(player: Player?, baseCost: Int): BigInteger =
        getRegularUpgradeCost(baseCost, getShearWoolSaveLevel(player))

    @JvmStatic
    fun getShearTierBoostUpgradeCost(player: Player?, baseCost: Int): BigInteger =
        getRegularUpgradeCost(baseCost, getShearTierBoostLevel(player))

    @JvmStatic
    fun upgradeShearShop(player: Player?, baseCost: Int): Boolean {
        if (player == null || !spendUpgradePoints(player, getShearUpgradeCost(player, baseCost))) return false
        SheepUpgradeState.setShearShopLevel(player.uniqueId, getShearShopLevel(player) + 1)
        SheepMergeManager.economySaveData()
        return true
    }

    @JvmStatic
    fun upgradeShearWoolSave(player: Player?, baseCost: Int): Boolean {
        if (player == null) return false
        val current = getShearWoolSaveLevel(player)
        if (current >= SHEAR_WOOL_SAVE_MAX_LEVEL || !spendUpgradePoints(player, getShearWoolSaveUpgradeCost(player, baseCost))) return false
        SheepUpgradeState.setShearWoolSaveLevel(player.uniqueId, current + 1)
        SheepMergeManager.economySaveData()
        return true
    }

    @JvmStatic
    fun upgradeShearTierBoost(player: Player?, baseCost: Int): Boolean {
        if (player == null) return false
        val current = getShearTierBoostLevel(player)
        if (current >= SHEAR_TIER_BOOST_MAX_LEVEL || !spendUpgradePoints(player, getShearTierBoostUpgradeCost(player, baseCost))) return false
        SheepUpgradeState.setShearTierBoostLevel(player.uniqueId, current + 1)
        SheepMergeManager.economySaveData()
        return true
    }

    private fun spendUpgradePoints(player: Player, cost: BigInteger): Boolean {
        if (!SheepMergeManager.economyCanSpendUpgradePointsDuringTutorial(player, cost)) return false
        return trySpendPoints(player, cost, SheepMergeManager.economyStartingPoints())
    }
}
