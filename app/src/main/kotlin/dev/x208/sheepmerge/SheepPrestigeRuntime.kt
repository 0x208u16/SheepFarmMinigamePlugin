package dev.x208.sheepmerge

import org.bukkit.entity.Player
import java.math.BigInteger

object SheepPrestigeRuntime {
    private const val MAX_LEVEL = Int.MAX_VALUE
    private const val DOUBLE_POINTS_MAX_LEVEL = 20
    private const val DOUBLE_POINTS_BASE_COST = 1
    private const val HIGHER_MAX_BASE_COST = 2
    private const val START_EGGS_BASE_COST = 1
    private const val EGG_CAP_BASE_COST = 2
    private const val BASE_SPAWN_TIER_BASE_COST = 10
    private const val QUEST_REWARD_BASE_COST = 4
    private const val REFUND_COOLDOWN_MS = 30L * 60L * 1000L

    @JvmStatic fun getLevel(player: Player?): Int = player?.let { SheepPrestigeState.getLevel(it.uniqueId) } ?: 0
    @JvmStatic fun getPoints(player: Player?): Int = player?.let { SheepPrestigeState.getPoints(it.uniqueId) } ?: 0
    @JvmStatic fun getMaxLevel(): Int = MAX_LEVEL
    @JvmStatic fun getDoublePointsLevel(player: Player?): Int =
        player?.let { SheepPrestigeState.getDoublePointsChance(it.uniqueId).coerceAtLeast(0).coerceAtMost(DOUBLE_POINTS_MAX_LEVEL) } ?: 0
    @JvmStatic fun getHigherMaxLevel(player: Player?): Int =
        player?.let { SheepPrestigeState.getHigherMaxLevel(it.uniqueId).coerceAtLeast(0) } ?: 0
    @JvmStatic fun getStartEggsLevel(player: Player?): Int = player?.let { SheepPrestigeState.getStartEggs(it.uniqueId) } ?: 0
    @JvmStatic fun getEggCapLevel(player: Player?): Int = player?.let { SheepPrestigeState.getEggCap(it.uniqueId) } ?: 0
    @JvmStatic fun getBaseSpawnTierLevel(player: Player?, maximumTier: Int): Int =
        player?.let { SheepPrestigeState.getBaseSpawnTier(it.uniqueId).coerceAtMost(maximumTier) } ?: 0
    @JvmStatic fun getQuestRewardLevel(player: Player?): Int = player?.let { SheepPrestigeState.getQuestReward(it.uniqueId) } ?: 0
    @JvmStatic fun getDoublePointsChancePercent(player: Player?): Int = (getDoublePointsLevel(player) * 5).coerceAtMost(100)

    @JvmStatic
    fun getCostForLevel(level: Int, baseCost: Int): BigInteger {
        if (baseCost <= 0) return BigInteger.ZERO
        return BigInteger.valueOf(baseCost.toLong()).shiftLeft(level.coerceAtLeast(0).coerceAtMost(Int.MAX_VALUE - 2))
    }

    @JvmStatic fun getCost(player: Player?, baseCost: Int): BigInteger = getCostForLevel(getLevel(player), baseCost)

    @JvmStatic
    fun getAffordableLevels(player: Player?, baseCost: Int): Int {
        if (player == null) return 0
        var level = getLevel(player)
        var points = SheepMergeManager.getPlayerPointsBig(player)
        var gained = 0
        while (level < Int.MAX_VALUE) {
            val cost = getCostForLevel(level, baseCost)
            if (points < cost) break
            points = points.subtract(cost)
            level++
            gained++
        }
        return gained
    }

    @JvmStatic
    fun getTotalCost(currentLevel: Int, levelsToBuy: Int, baseCost: Int): BigInteger {
        var total = BigInteger.ZERO
        repeat(levelsToBuy.coerceAtLeast(0)) { offset -> total = total.add(getCostForLevel(currentLevel + offset, baseCost)) }
        return total
    }

    @JvmStatic
    fun getPointsReward(currentLevel: Int, levelsToBuy: Int): Int {
        var reward = 0L
        for (offset in 1..levelsToBuy.coerceAtLeast(0)) {
            reward += currentLevel.toLong() + offset
            if (reward >= Int.MAX_VALUE) return Int.MAX_VALUE
        }
        return reward.toInt()
    }

    @JvmStatic
    fun prestige(player: Player?, baseCost: Int): Int {
        if (player == null) return 0
        val current = getLevel(player)
        val affordable = getAffordableLevels(player, baseCost)
        if (affordable <= 0) return 0
        val totalCost = getTotalCost(current, affordable, baseCost)
        if (totalCost.signum() <= 0 || !SheepMergeManager.trySpendPoints(player, totalCost)) return 0

        val playerId = player.uniqueId
        SheepPrestigeState.setTotalLevelsEarned(
            playerId,
            addSaturated(SheepPrestigeState.getTotalLevelsEarned(playerId), affordable),
        )
        SheepPrestigeState.setLevel(playerId, current + affordable)
        SheepPrestigeState.setPoints(playerId, addSaturated(getPoints(player), getPointsReward(current, affordable)))
        SheepMergeManager.prestigeClearReminder(player)
        SheepMergeManager.prestigeRunResetEffects(player, false)
        SheepMergeManager.prestigeSaveData()
        SheepMergeManager.prestigeEvaluateAchievements(player)
        SheepMergeManager.prestigeMarkTutorialComplete(player)
        return affordable
    }

    @JvmStatic
    fun runResetEffects(player: Player?, forRebirth: Boolean) {
        if (player == null) return
        val playerId = player.uniqueId
        val keepPoints = SheepMergeManager.prestigeKeepsPoints(playerId)
        val keepSheep = SheepMergeManager.prestigeKeepsSheep(playerId)

        if (!keepSheep) {
            val sacrificeGained = SheepMergeManager.prestigeRemoveSheepAndGetSacrifice(player)
            if (sacrificeGained.signum() > 0) {
                SheepMergeManager.prestigeAddSacrificePoints(playerId, sacrificeGained)
            }
        }
        if (!keepPoints) {
            SheepEconomyState.setPoints(playerId, BigInteger.ZERO)
            SheepMergeManager.prestigeRefreshTopPoints()
        }
        if (!SheepMergeManager.prestigeKeepsRegularUpgrades(playerId) || forRebirth) {
            SheepEconomyState.resetRegularUpgrades(playerId)
        }
        if (!SheepMergeManager.prestigeKeepsShearUpgrades(playerId) || forRebirth) {
            SheepUpgradeState.resetShearUpgrades(playerId)
        }
        if (!SheepMergeManager.prestigeKeepsComboUpgrades(playerId) || forRebirth) {
            SheepMergeManager.prestigeResetComboUpgrades(playerId)
        }
        SheepMergeManager.prestigeClearMergeReminder(player)
        SheepMergeManager.prestigeClearEggRuntime(playerId)
        SheepMergeManager.prestigeClearComboRuntime(player)
    }

    @JvmStatic
    fun linearUpgradeCost(baseCost: Int, level: Int): BigInteger {
        if (baseCost <= 0) return BigInteger.ZERO
        return BigInteger.valueOf(baseCost.toLong()).multiply(BigInteger.valueOf(level.coerceAtLeast(0).toLong() + 1L))
    }

    @JvmStatic fun upgradeCost(baseCost: Int, level: Int): Int = toIntClamped(linearUpgradeCost(baseCost, level))
    @JvmStatic fun getDoublePointsCost(player: Player?): Int =
        if (getDoublePointsLevel(player) >= DOUBLE_POINTS_MAX_LEVEL) 0 else upgradeCost(DOUBLE_POINTS_BASE_COST, getDoublePointsLevel(player))
    @JvmStatic fun getHigherMaxCost(player: Player?): Int = upgradeCost(HIGHER_MAX_BASE_COST, getHigherMaxLevel(player))
    @JvmStatic fun getStartEggsCost(player: Player?): Int = upgradeCost(START_EGGS_BASE_COST, getStartEggsLevel(player))
    @JvmStatic fun getEggCapCost(player: Player?): Int = upgradeCost(EGG_CAP_BASE_COST, getEggCapLevel(player))
    @JvmStatic fun getBaseSpawnTierCost(player: Player?, maximumTier: Int): Int =
        upgradeCost(BASE_SPAWN_TIER_BASE_COST, getBaseSpawnTierLevel(player, maximumTier))
    @JvmStatic fun getQuestRewardCost(player: Player?): Int = upgradeCost(QUEST_REWARD_BASE_COST, getQuestRewardLevel(player))

    @JvmStatic
    fun trySpendPoints(player: Player?, points: Int): Boolean {
        if (player == null || points <= 0 || getPoints(player) < points) return false
        SheepPrestigeState.setPoints(player.uniqueId, getPoints(player) - points)
        SheepMergeManager.prestigeSaveData()
        return true
    }

    @JvmStatic
    fun upgradeDoublePoints(player: Player?): Boolean {
        if (player == null || getDoublePointsLevel(player) >= DOUBLE_POINTS_MAX_LEVEL) return false
        if (!trySpendPoints(player, getDoublePointsCost(player))) return false
        SheepPrestigeState.setDoublePointsChance(player.uniqueId, getDoublePointsLevel(player) + 1)
        SheepMergeManager.prestigeSaveData()
        return true
    }

    @JvmStatic
    fun upgradeHigherMax(player: Player?): Boolean {
        if (player == null) return false
        val current = getHigherMaxLevel(player)
        if (current >= Int.MAX_VALUE || !trySpendPoints(player, getHigherMaxCost(player))) return false
        SheepPrestigeState.setHigherMaxLevel(player.uniqueId, current + 1)
        SheepMergeManager.prestigeSaveData()
        return true
    }

    @JvmStatic
    fun upgradeStartEggs(player: Player?): Boolean {
        if (player == null || !trySpendPoints(player, getStartEggsCost(player))) return false
        val oldBonus = SheepMergeManager.getStartEggsBonus(player)
        SheepPrestigeState.setStartEggs(player.uniqueId, getStartEggsLevel(player) + 1)
        val gainedEggs = (SheepMergeManager.getStartEggsBonus(player) - oldBonus).coerceAtLeast(0)
        if (gainedEggs > 0) SheepMergeManager.prestigeAddEggs(player, gainedEggs)
        SheepMergeManager.prestigeSaveData()
        return true
    }

    @JvmStatic
    fun upgradeEggCap(player: Player?): Boolean {
        if (player == null || !trySpendPoints(player, getEggCapCost(player))) return false
        SheepPrestigeState.setEggCap(player.uniqueId, getEggCapLevel(player) + 1)
        SheepMergeManager.prestigeSaveData()
        return true
    }

    @JvmStatic
    fun upgradeBaseSpawnTier(player: Player?, maximumTier: Int): Boolean {
        if (player == null) return false
        val current = getBaseSpawnTierLevel(player, maximumTier)
        if (current >= maximumTier || !trySpendPoints(player, getBaseSpawnTierCost(player, maximumTier))) return false
        SheepPrestigeState.setBaseSpawnTier(player.uniqueId, current + 1)
        SheepMergeManager.prestigeUpgradeSheepBelowMinimum(player)
        SheepMergeManager.prestigeSaveData()
        return true
    }

    @JvmStatic
    fun upgradeQuestReward(player: Player?): Boolean {
        if (player == null) return false
        val current = getQuestRewardLevel(player)
        if (!trySpendPoints(player, getQuestRewardCost(player))) return false
        SheepPrestigeState.setQuestReward(player.uniqueId, current + 1)
        SheepMergeManager.prestigeSaveData()
        return true
    }

    @JvmStatic
    fun refundRemainingMs(player: Player?): Long {
        if (player == null) return 0L
        return (SheepPrestigeState.getNextRefundTimestamp(player.uniqueId) - System.currentTimeMillis()).coerceAtLeast(0L)
    }

    @JvmStatic
    fun linearSpentCost(baseCost: Int, currentLevel: Int): Int {
        if (baseCost <= 0 || currentLevel <= 0) return 0
        var total = BigInteger.ZERO
        repeat(currentLevel) { level ->
            total = total.add(linearUpgradeCost(baseCost, level))
            if (total >= BigInteger.valueOf(Int.MAX_VALUE.toLong())) return Int.MAX_VALUE
        }
        return total.toInt()
    }

    @JvmStatic
    fun getRefundAmount(player: Player?, comboBaseCost: Int, comboLevel: Int, maximumTier: Int): Int {
        if (player == null) return 0
        var total = 0L
        total += linearSpentCost(DOUBLE_POINTS_BASE_COST, getDoublePointsLevel(player))
        total += linearSpentCost(HIGHER_MAX_BASE_COST, getHigherMaxLevel(player))
        total += linearSpentCost(START_EGGS_BASE_COST, getStartEggsLevel(player))
        total += linearSpentCost(EGG_CAP_BASE_COST, getEggCapLevel(player))
        total += linearSpentCost(BASE_SPAWN_TIER_BASE_COST, getBaseSpawnTierLevel(player, maximumTier))
        total += linearSpentCost(QUEST_REWARD_BASE_COST, getQuestRewardLevel(player))
        total += linearSpentCost(comboBaseCost, comboLevel)
        return total.coerceAtMost(Int.MAX_VALUE.toLong()).coerceAtLeast(0L).toInt()
    }

    @JvmStatic
    fun tryRefund(player: Player?, comboBaseCost: Int, comboLevel: Int, maximumTier: Int): Boolean {
        if (player == null) return false
        val now = System.currentTimeMillis()
        if (now < SheepPrestigeState.getNextRefundTimestamp(player.uniqueId)) return false
        val refund = getRefundAmount(player, comboBaseCost, comboLevel, maximumTier)
        if (refund <= 0) return false
        SheepPrestigeState.setPoints(player.uniqueId, addSaturated(getPoints(player), refund))
        SheepMergeManager.prestigeResetUpgrades(player.uniqueId, false)
        SheepPrestigeState.setNextRefundTimestamp(player.uniqueId, now + REFUND_COOLDOWN_MS)
        SheepMergeManager.prestigeSaveData()
        return true
    }

    @JvmStatic
    fun adminSetLevel(
        player: Player?,
        targetLevel: Int,
        comboBaseCost: Int,
        comboLevel: Int,
        maximumTier: Int,
    ): Boolean {
        if (player == null || targetLevel < 0) return false
        val playerId = player.uniqueId
        val totalEarnedPoints = totalPointsForLevel(targetLevel)
        var availablePoints = totalEarnedPoints - getRefundAmount(player, comboBaseCost, comboLevel, maximumTier)
        SheepPrestigeState.setLevel(playerId, targetLevel)
        SheepMergeManager.prestigeClearReminder(player)
        if (availablePoints < 0) {
            SheepMergeManager.prestigeResetUpgrades(playerId, true)
            availablePoints = totalEarnedPoints
        }
        SheepPrestigeState.setPoints(playerId, availablePoints.coerceAtLeast(0))
        SheepMergeManager.prestigeSaveData()
        return true
    }

    @JvmStatic
    fun totalPointsForLevel(level: Int): Int {
        if (level <= 0) return 0
        val total = level.toLong() * (level + 1L) / 2L
        return total.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }

    private fun addSaturated(current: Int, delta: Int): Int =
        (current.toLong() + delta.coerceAtLeast(0).toLong()).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

    private fun toIntClamped(value: BigInteger): Int = when {
        value.signum() <= 0 -> 0
        value > BigInteger.valueOf(Int.MAX_VALUE.toLong()) -> Int.MAX_VALUE
        else -> value.toInt()
    }
}