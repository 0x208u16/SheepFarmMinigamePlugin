package dev.x208.sheepmerge

import org.bukkit.entity.Player
import java.math.BigInteger
import kotlin.math.max
import kotlin.math.min

object SheepAutomationRuntime {
    enum class AutoBuyUpgrade {
        SHEEP_LIMIT, EGG_SPEED, WOOL_REGEN, HIGHER_TIER_CHANCE, COMBO_DECAY, COMBO_GAIN,
        SHEAR_WOOL_SAVE, SHEAR_TIER_BOOST, SHEAR_VALUE,
    }

    private const val SINGLE_LEVEL_MAX = 1
    private const val AUTO_ABILITY_MAX_LEVEL = 3
    private const val AUTO_BUY_MAX_LEVEL = 5
    private const val SLOW_AUTO_MERGE_MAX_LEVEL = 3
    private const val SLOW_AUTO_SHEAR_MAX_LEVEL = 3
    private const val AUTO_SPAWN_MAX_LEVEL = 10
    private const val AUTO_BUY_MAX_PURCHASES_PER_TICK = 512
    private const val MIN_INTERVAL_MS = 1_000L
    private const val AUTO_PRESTIGE_INTERVAL_MS = 5_000L

    private var autoBuyBaseCost = 10
    private var autoAbilityBaseCost = 14
    private var slowAutoMergeBaseCost = 16
    private var slowAutoShearBaseCost = 12
    private var autoSpawnBaseCost = 5
    private var autoPrestigeBaseCost = 64
    private var pointIntervalMs = 60_000L
    private var autoBuyIntervalMs = 5_000L
    private var autoAbilityIntervalMs = 5_000L
    private var slowAutoMergeIntervalMs = 3_000L
    private var slowAutoShearIntervalMs = 3_000L
    private var autoSpawnBaseIntervalMs = 10_000L
    private var autoSpawnIntervalStepMs = 1_000L
    private var autoSpawnMinIntervalMs = 0L
    private var conditionMinPointsReserve = 0L
    private var conditionMinQuestPoints = 0
    private var conditionMinSheepForMerge = 2
    private var conditionMinReadySheepForShear = 1

    @JvmStatic
    fun configure(
        autoBuyBaseCost: Int, autoAbilityBaseCost: Int, slowAutoMergeBaseCost: Int,
        slowAutoShearBaseCost: Int, autoSpawnBaseCost: Int, pointIntervalMs: Long,
        autoBuyIntervalMs: Long, autoAbilityIntervalMs: Long, slowAutoMergeIntervalMs: Long,
        slowAutoShearIntervalMs: Long, autoSpawnBaseIntervalMs: Long, autoSpawnIntervalStepMs: Long,
        autoSpawnMinIntervalMs: Long, conditionMinPointsReserve: Long, conditionMinQuestPoints: Int,
        conditionMinSheepForMerge: Int, conditionMinReadySheepForShear: Int,
    ) {
        this.autoBuyBaseCost = autoBuyBaseCost
        this.autoAbilityBaseCost = autoAbilityBaseCost
        this.slowAutoMergeBaseCost = slowAutoMergeBaseCost
        this.slowAutoShearBaseCost = slowAutoShearBaseCost
        this.autoSpawnBaseCost = autoSpawnBaseCost
        this.pointIntervalMs = pointIntervalMs
        this.autoBuyIntervalMs = autoBuyIntervalMs
        this.autoAbilityIntervalMs = autoAbilityIntervalMs
        this.slowAutoMergeIntervalMs = slowAutoMergeIntervalMs
        this.slowAutoShearIntervalMs = slowAutoShearIntervalMs
        this.autoSpawnBaseIntervalMs = autoSpawnBaseIntervalMs
        this.autoSpawnIntervalStepMs = autoSpawnIntervalStepMs
        this.autoSpawnMinIntervalMs = autoSpawnMinIntervalMs
        this.conditionMinPointsReserve = conditionMinPointsReserve
        this.conditionMinQuestPoints = conditionMinQuestPoints
        this.conditionMinSheepForMerge = conditionMinSheepForMerge
        this.conditionMinReadySheepForShear = conditionMinReadySheepForShear
    }

    @JvmStatic fun tickSystems(player: Player, now: Long) {
        tickAutoBuy(player, now)
        tickAutoAbility(player, now)
        tickSlowMerge(player, now)
        tickSlowShear(player, now)
        tickAutoPrestige(player, now)
    }

    @JvmStatic fun tickAutoSpawnRealtime(player: Player?) {
        if (player == null || !player.isOnline) return
        tickAutoSpawn(player, System.currentTimeMillis())
    }

    @JvmStatic fun tickPlaytimePoints(player: Player?) {
        if (player == null || pointIntervalMs <= 0L) return
        val playerId = player.uniqueId
        val now = System.currentTimeMillis()
        val nextAt = SheepAutomationState.getNextPointAt(playerId)
        if (nextAt <= 0L) {
            SheepAutomationState.setNextPointAt(playerId, now + pointIntervalMs)
            return
        }
        if (now < nextAt) return
        SheepAutomationState.setPoints(playerId, min(Int.MAX_VALUE.toLong(), getPoints(player).toLong() + 1L).toInt())
        SheepAutomationState.setNextPointAt(playerId, now + pointIntervalMs)
        SheepMergeManager.automationSaveData()
    }

    private fun tickAutoBuy(player: Player, now: Long) {
        val level = getAutoBuyUpgradeLevel(player)
        if (level <= 0 || !isAutoBuyEnabled(player)) return
        if (level < AUTO_BUY_MAX_LEVEL) {
            val interval = getAutoBuyIntervalMs(player)
            if (interval <= 0L || now < SheepAutomationState.getNextAutoBuyAt(player.uniqueId)) return
            SheepAutomationState.setNextAutoBuyAt(player.uniqueId, now + interval)
        } else {
            SheepAutomationState.setNextAutoBuyAt(player.uniqueId, 0L)
        }
        if (!canRun(player, false)) return
        if (level >= AUTO_BUY_MAX_LEVEL) {
            repeat(AUTO_BUY_MAX_PURCHASES_PER_TICK) { if (!tryAutoBuyOneUpgrade(player)) return }
        } else {
            tryAutoBuyOneUpgrade(player)
        }
    }

    private fun tryAutoBuyOneUpgrade(player: Player): Boolean {
        val availablePoints = SheepMergeManager.automationPlayerPoints(player)
        if (availablePoints.signum() <= 0) return false
        var selected: AutoBuyUpgrade? = null
        var cheapest: BigInteger? = null
        for (upgrade in AutoBuyUpgrade.entries) {
            val cost = SheepMergeManager.automationAutoBuyCost(player, upgrade, availablePoints) ?: continue
            if (cheapest == null || cost < cheapest) {
                selected = upgrade
                cheapest = cost
            }
        }
        return selected != null && SheepMergeManager.automationBuyUpgrade(player, selected)
    }

    private fun tickAutoAbility(player: Player, now: Long) {
        val level = getAutoAbilityUpgradeLevel(player)
        if (level <= 0 || !isAutoAbilityEnabled(player)) return
        if (level < AUTO_ABILITY_MAX_LEVEL) {
            if (autoAbilityIntervalMs <= 0L || now < SheepAutomationState.getNextAutoAbilityAt(player.uniqueId)) return
            SheepAutomationState.setNextAutoAbilityAt(player.uniqueId, now + autoAbilityIntervalMs)
        } else {
            SheepAutomationState.setNextAutoAbilityAt(player.uniqueId, 0L)
        }
        if (canRun(player, true)) SheepQuestRuntime.tryAutoActivateAbility(player, level >= 2)
    }

    private fun tickSlowMerge(player: Player, now: Long) {
        val interval = getSlowAutoMergeIntervalMs(player)
        if (getSlowAutoMergeUpgradeLevel(player) <= 0 || interval <= 0L || !isSlowAutoMergeEnabled(player)) return
        if (now < SheepAutomationState.getNextSlowMergeAt(player.uniqueId)) return
        SheepAutomationState.setNextSlowMergeAt(player.uniqueId, now + interval)
        if (canRun(player, false) && SheepMergeManager.automationHasMergeCandidates(player, conditionMinSheepForMerge)) {
            SheepQuestRuntime.tryAutoMergeOnce(player)
        }
    }

    private fun tickSlowShear(player: Player, now: Long) {
        val interval = getSlowAutoShearIntervalMs(player)
        if (getSlowAutoShearUpgradeLevel(player) <= 0 || interval <= 0L || !isSlowAutoShearEnabled(player)) return
        if (now < SheepAutomationState.getNextSlowShearAt(player.uniqueId)) return
        SheepAutomationState.setNextSlowShearAt(player.uniqueId, now + interval)
        if (canRun(player, false) && SheepMergeManager.automationReadySheepCount(player) >= conditionMinReadySheepForShear) {
            SheepQuestRuntime.tryAutoShearOnce(player)
        }
    }

    private fun tickAutoSpawn(player: Player, now: Long) {
        if (getAutoSpawnUpgradeLevel(player) <= 0 || !isAutoSpawnEnabled(player)) return
        val interval = getAutoSpawnIntervalMs(player)
        if (interval > 0L && now < SheepAutomationState.getNextAutoSpawnAt(player.uniqueId)) return
        SheepAutomationState.setNextAutoSpawnAt(player.uniqueId, if (interval > 0L) now + interval else now)
        if (!canRun(player, false) || SheepMergeManager.automationWorldAtLimit(player)) return
        if (SheepMergeManager.automationSpawnSheepFromSky(player)) SheepMergeManager.automationRecordSpawn(player)
    }

    private fun tickAutoPrestige(player: Player, now: Long) {
        if (getAutoPrestigeUpgradeLevel(player) <= 0 || !isAutoPrestigeEnabled(player)) return
        if (now < SheepAutomationState.getNextAutoPrestigeAt(player.uniqueId)) return
        SheepAutomationState.setNextAutoPrestigeAt(player.uniqueId, now + AUTO_PRESTIGE_INTERVAL_MS)
        if (canRun(player, false)) SheepMergeManager.automationPrestige(player)
    }

    private fun canRun(player: Player, requiresQuestPoints: Boolean): Boolean {
        if (!SheepMergeManager.automationCanUseOwnedFarm(player)) return false
        if (SheepMergeManager.automationPlayerPoints(player) < BigInteger.valueOf(conditionMinPointsReserve)) return false
        return !requiresQuestPoints || SheepQuestRuntime.getPoints(player) >= conditionMinQuestPoints
    }

    @JvmStatic fun getPoints(player: Player?) = player?.let { SheepAutomationState.getPoints(it.uniqueId) } ?: 0
    @JvmStatic fun getAutoBuyUpgradeLevel(player: Player?) = level(player, SheepAutomationState::getAutoBuyUpgrade, AUTO_BUY_MAX_LEVEL)
    @JvmStatic fun getAutoAbilityUpgradeLevel(player: Player?) = level(player, SheepAutomationState::getAutoAbilityUpgrade, AUTO_ABILITY_MAX_LEVEL)
    @JvmStatic fun getSlowAutoMergeUpgradeLevel(player: Player?) = level(player, SheepAutomationState::getSlowAutoMergeUpgrade, SLOW_AUTO_MERGE_MAX_LEVEL)
    @JvmStatic fun getSlowAutoShearUpgradeLevel(player: Player?) = level(player, SheepAutomationState::getSlowAutoShearUpgrade, SLOW_AUTO_SHEAR_MAX_LEVEL)
    @JvmStatic fun getAutoSpawnUpgradeLevel(player: Player?) = level(player, SheepAutomationState::getAutoSpawnUpgrade, AUTO_SPAWN_MAX_LEVEL)
    @JvmStatic fun getAutoPrestigeUpgradeLevel(player: Player?) = level(player, SheepAutomationState::getAutoPrestigeUpgrade, SINGLE_LEVEL_MAX)

    private fun level(player: Player?, getter: (java.util.UUID?) -> Int, maximum: Int) =
        player?.let { min(maximum, max(0, getter(it.uniqueId))) } ?: 0

    @JvmStatic fun isAutoBuyEnabled(player: Player?) = player != null && SheepAutomationState.isAutoBuyEnabled(player.uniqueId)
    @JvmStatic fun isAutoAbilityEnabled(player: Player?) = player != null && SheepAutomationState.isAutoAbilityEnabled(player.uniqueId)
    @JvmStatic fun isSlowAutoMergeEnabled(player: Player?) = player != null && SheepAutomationState.isSlowAutoMergeEnabled(player.uniqueId)
    @JvmStatic fun isSlowAutoShearEnabled(player: Player?) = player != null && SheepAutomationState.isSlowAutoShearEnabled(player.uniqueId)
    @JvmStatic fun isAutoSpawnEnabled(player: Player?) = player != null && SheepAutomationState.isAutoSpawnEnabled(player.uniqueId)
    @JvmStatic fun isAutoPrestigeEnabled(player: Player?) = player != null && SheepAutomationState.isAutoPrestigeEnabled(player.uniqueId)

    @JvmStatic fun getUnlockedCount(player: Player?) = if (player == null) 0 else listOf(
        getAutoBuyUpgradeLevel(player), getAutoAbilityUpgradeLevel(player), getSlowAutoMergeUpgradeLevel(player),
        getSlowAutoShearUpgradeLevel(player), getAutoSpawnUpgradeLevel(player), getAutoPrestigeUpgradeLevel(player),
    ).count { it > 0 }

    @JvmStatic fun setAllEnabled(player: Player?, enabled: Boolean): Int {
        if (player == null) return 0
        val playerId = player.uniqueId
        var changed = 0
        fun setIfUnlocked(level: Int, current: Boolean, setter: (Boolean) -> Unit) {
            if (level > 0 && current != enabled) { setter(enabled); changed++ }
        }
        setIfUnlocked(getAutoBuyUpgradeLevel(player), SheepAutomationState.isAutoBuyEnabled(playerId)) { SheepAutomationState.setAutoBuyEnabled(playerId, it) }
        setIfUnlocked(getAutoAbilityUpgradeLevel(player), SheepAutomationState.isAutoAbilityEnabled(playerId)) { SheepAutomationState.setAutoAbilityEnabled(playerId, it) }
        setIfUnlocked(getSlowAutoMergeUpgradeLevel(player), SheepAutomationState.isSlowAutoMergeEnabled(playerId)) { SheepAutomationState.setSlowAutoMergeEnabled(playerId, it) }
        setIfUnlocked(getSlowAutoShearUpgradeLevel(player), SheepAutomationState.isSlowAutoShearEnabled(playerId)) { SheepAutomationState.setSlowAutoShearEnabled(playerId, it) }
        setIfUnlocked(getAutoSpawnUpgradeLevel(player), SheepAutomationState.isAutoSpawnEnabled(playerId)) { SheepAutomationState.setAutoSpawnEnabled(playerId, it) }
        setIfUnlocked(getAutoPrestigeUpgradeLevel(player), SheepAutomationState.isAutoPrestigeEnabled(playerId)) { SheepAutomationState.setAutoPrestigeEnabled(playerId, it) }
        if (changed > 0) SheepMergeManager.automationSaveData()
        return changed
    }

    @JvmStatic fun toggleAutoBuy(player: Player) = toggle { SheepAutomationState.toggleAutoBuyEnabled(player.uniqueId) }
    @JvmStatic fun toggleAutoAbility(player: Player) = toggle { SheepAutomationState.toggleAutoAbilityEnabled(player.uniqueId) }
    @JvmStatic fun toggleSlowAutoMerge(player: Player) = toggle { SheepAutomationState.toggleSlowAutoMergeEnabled(player.uniqueId) }
    @JvmStatic fun toggleSlowAutoShear(player: Player) = toggle { SheepAutomationState.toggleSlowAutoShearEnabled(player.uniqueId) }
    @JvmStatic fun toggleAutoSpawn(player: Player) = toggle { SheepAutomationState.toggleAutoSpawnEnabled(player.uniqueId) }
    @JvmStatic fun toggleAutoPrestige(player: Player) = toggle { SheepAutomationState.toggleAutoPrestigeEnabled(player.uniqueId) }
    private fun toggle(action: () -> Boolean): Boolean = action().also { SheepMergeManager.automationSaveData() }

    @JvmStatic fun getAutoBuyMaxLevel() = AUTO_BUY_MAX_LEVEL
    @JvmStatic fun getAutoAbilityMaxLevel() = AUTO_ABILITY_MAX_LEVEL
    @JvmStatic fun getSlowAutoMergeMaxLevel() = SLOW_AUTO_MERGE_MAX_LEVEL
    @JvmStatic fun getSlowAutoShearMaxLevel() = SLOW_AUTO_SHEAR_MAX_LEVEL
    @JvmStatic fun getAutoSpawnMaxLevel() = AUTO_SPAWN_MAX_LEVEL
    @JvmStatic fun getPointIntervalMs() = pointIntervalMs
    @JvmStatic fun getAutoAbilityIntervalMs() = autoAbilityIntervalMs
    @JvmStatic fun getAutoPrestigeIntervalMs() = AUTO_PRESTIGE_INTERVAL_MS

    @JvmStatic fun getAutoBuyIntervalMs(player: Player?) = scaledInterval(getAutoBuyUpgradeLevel(player), AUTO_BUY_MAX_LEVEL, autoBuyIntervalMs, 0L)
    @JvmStatic fun getSlowAutoMergeIntervalMs(player: Player?) = scaledInterval(getSlowAutoMergeUpgradeLevel(player), SLOW_AUTO_MERGE_MAX_LEVEL, slowAutoMergeIntervalMs, MIN_INTERVAL_MS)
    @JvmStatic fun getSlowAutoShearIntervalMs(player: Player?) = scaledInterval(getSlowAutoShearUpgradeLevel(player), SLOW_AUTO_SHEAR_MAX_LEVEL, slowAutoShearIntervalMs, MIN_INTERVAL_MS)
    private fun scaledInterval(level: Int, maximum: Int, base: Long, maximumLevelInterval: Long): Long {
        if (level <= 0) return base
        if (level >= maximum) return maximumLevelInterval
        val step = max(1L, (base - MIN_INTERVAL_MS) / max(1, maximum - 1))
        return max(MIN_INTERVAL_MS, base - (level - 1L) * step)
    }

    @JvmStatic fun getAutoSpawnIntervalMs(player: Player?): Long {
        val level = getAutoSpawnUpgradeLevel(player)
        if (level <= 0) return autoSpawnBaseIntervalMs
        if (level >= AUTO_SPAWN_MAX_LEVEL) return 0L
        return max(max(0L, autoSpawnMinIntervalMs), autoSpawnBaseIntervalMs - level.toLong() * max(1L, autoSpawnIntervalStepMs))
    }

    @JvmStatic fun getAutoBuyUpgradeCost(player: Player?) = upgradeCost(autoBuyBaseCost, getAutoBuyUpgradeLevel(player), AUTO_BUY_MAX_LEVEL)
    @JvmStatic fun getAutoAbilityUpgradeCost(player: Player?) = upgradeCost(autoAbilityBaseCost, getAutoAbilityUpgradeLevel(player), AUTO_ABILITY_MAX_LEVEL)
    @JvmStatic fun getSlowAutoMergeUpgradeCost(player: Player?) = upgradeCost(slowAutoMergeBaseCost, getSlowAutoMergeUpgradeLevel(player), SLOW_AUTO_MERGE_MAX_LEVEL)
    @JvmStatic fun getSlowAutoShearUpgradeCost(player: Player?) = upgradeCost(slowAutoShearBaseCost, getSlowAutoShearUpgradeLevel(player), SLOW_AUTO_SHEAR_MAX_LEVEL)
    @JvmStatic fun getAutoSpawnUpgradeCost(player: Player?) = upgradeCost(autoSpawnBaseCost, getAutoSpawnUpgradeLevel(player), AUTO_SPAWN_MAX_LEVEL)
    @JvmStatic fun getAutoPrestigeUpgradeCost(player: Player?) = if (getAutoPrestigeUpgradeLevel(player) > 0) 0 else autoPrestigeBaseCost
    private fun upgradeCost(base: Int, level: Int, maximum: Int): Int {
        if (level >= maximum) return 0
        var cost = max(1, base).toLong()
        repeat(max(0, level)) { cost = min(Int.MAX_VALUE.toLong(), cost * 2L) }
        return cost.toInt()
    }

    @JvmStatic fun upgradeAutoBuy(player: Player?) = upgrade(player, getAutoBuyUpgradeLevel(player), AUTO_BUY_MAX_LEVEL, getAutoBuyUpgradeCost(player)) { SheepAutomationState.setAutoBuyUpgrade(it.uniqueId, getAutoBuyUpgradeLevel(it) + 1) }
    @JvmStatic fun upgradeAutoAbility(player: Player?) = upgrade(player, getAutoAbilityUpgradeLevel(player), AUTO_ABILITY_MAX_LEVEL, getAutoAbilityUpgradeCost(player)) { SheepAutomationState.setAutoAbilityUpgrade(it.uniqueId, getAutoAbilityUpgradeLevel(it) + 1); SheepAutomationState.setNextAutoAbilityAt(it.uniqueId, 0L) }
    @JvmStatic fun upgradeSlowAutoMerge(player: Player?) = upgrade(player, getSlowAutoMergeUpgradeLevel(player), SLOW_AUTO_MERGE_MAX_LEVEL, getSlowAutoMergeUpgradeCost(player)) { SheepAutomationState.setSlowAutoMergeUpgrade(it.uniqueId, getSlowAutoMergeUpgradeLevel(it) + 1); SheepAutomationState.setNextSlowMergeAt(it.uniqueId, 0L) }
    @JvmStatic fun upgradeSlowAutoShear(player: Player?) = upgrade(player, getSlowAutoShearUpgradeLevel(player), SLOW_AUTO_SHEAR_MAX_LEVEL, getSlowAutoShearUpgradeCost(player)) { SheepAutomationState.setSlowAutoShearUpgrade(it.uniqueId, getSlowAutoShearUpgradeLevel(it) + 1); SheepAutomationState.setNextSlowShearAt(it.uniqueId, 0L) }
    @JvmStatic fun upgradeAutoSpawn(player: Player?) = upgrade(player, getAutoSpawnUpgradeLevel(player), AUTO_SPAWN_MAX_LEVEL, getAutoSpawnUpgradeCost(player)) { SheepAutomationState.setAutoSpawnUpgrade(it.uniqueId, getAutoSpawnUpgradeLevel(it) + 1); SheepAutomationState.setNextAutoSpawnAt(it.uniqueId, 0L) }
    @JvmStatic fun upgradeAutoPrestige(player: Player?) = upgrade(player, getAutoPrestigeUpgradeLevel(player), SINGLE_LEVEL_MAX, getAutoPrestigeUpgradeCost(player)) { SheepAutomationState.setAutoPrestigeUpgrade(it.uniqueId, 1); SheepAutomationState.setNextAutoPrestigeAt(it.uniqueId, 0L) }

    private fun upgrade(player: Player?, level: Int, maximum: Int, cost: Int, apply: (Player) -> Unit): Boolean {
        if (player == null || level >= maximum || !trySpendPoints(player, cost)) return false
        apply(player)
        SheepMergeManager.automationSaveData()
        return true
    }

    private fun trySpendPoints(player: Player, amount: Int): Boolean {
        if (amount <= 0 || getPoints(player) < amount) return false
        SheepAutomationState.setPoints(player.uniqueId, getPoints(player) - amount)
        SheepMergeManager.automationSaveData()
        return true
    }
}