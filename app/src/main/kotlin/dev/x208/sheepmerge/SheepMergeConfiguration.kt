package dev.x208.sheepmerge

import org.bukkit.configuration.file.FileConfiguration

class SheepMergeConfiguration private constructor(configuration: FileConfiguration?) {

    val schedulerFastTickInterval: Long = getLong(configuration, "scheduling.fastTickInterval", 2L, 1L)
    val schedulerNormalTickInterval: Long = getLong(configuration, "scheduling.normalTickInterval", 20L, 1L)
    val schedulerReminderTickInterval: Long = getLong(configuration, "scheduling.reminderTickInterval", 20L, 1L)
    val schedulerTipIntervalTicks: Long = getLong(configuration, "scheduling.tipIntervalTicks", 1200L, 20L)

    val farmTeleportX: Double = getDouble(configuration, "farm.teleport.x", 0.5)
    val farmTeleportY: Double = getDouble(configuration, "farm.teleport.y", 101.0)
    val farmTeleportZ: Double = getDouble(configuration, "farm.teleport.z", 0.5)

    val mergeReminderDelayMs: Long = getLong(configuration, "gameplay.reminders.merge.delayMs", 30_000L, 1L)
    val mergeReminderRepeatMs: Long = getLong(configuration, "gameplay.reminders.merge.repeatMs", 60_000L, 1L)
    val tutorialReminderDelayMs: Long = getLong(configuration, "gameplay.reminders.tutorial.delayMs", 120_000L, 1L)
    val tutorialReminderRepeatMs: Long = getLong(configuration, "gameplay.reminders.tutorial.repeatMs", 60_000L, 1L)
    val tutorialTaskTitleRepeatMs: Long = getLong(configuration, "gameplay.reminders.tutorialTaskTitle.repeatMs", 12_000L, 1L)
    val tutorialStatusFeedRepeatMs: Long = getLong(configuration, "gameplay.reminders.tutorialStatusFeed.repeatMs", 12_000L, 1L)
    val tutorialFocusNotificationCooldownMs: Long = getLong(configuration, "gameplay.reminders.tutorialFocus.cooldownMs", 8_000L, 0L)
    val tutorialMergePointsReminderRepeatMs: Long = getLong(configuration, "gameplay.reminders.tutorialMergePoints.repeatMs", 15_000L, 1L)

    val upgradeLimitBaseCost: Int = getInt(configuration, "gameplay.upgrades.limit.baseCost", 16, 1)
    val upgradeEggSpeedBaseCost: Int = getInt(configuration, "gameplay.upgrades.eggSpeed.baseCost", 20, 1)
    val upgradeWoolRegenBaseCost: Int = getInt(configuration, "gameplay.upgrades.woolRegen.baseCost", 24, 1)
    val upgradeHigherTierChanceBaseCost: Int = getInt(configuration, "gameplay.upgrades.higherTierChance.baseCost", 28, 1)

    val questShearsTarget: Int = getInt(configuration, "gameplay.quests.targets.shears", 20, 1)
    val questSpawnsTarget: Int = getInt(configuration, "gameplay.quests.targets.spawns", 12, 1)
    val questMergesTarget: Int = getInt(configuration, "gameplay.quests.targets.merges", 8, 1)
    val questShearsReward: Int = getInt(configuration, "gameplay.quests.rewards.shears", 8, 1)
    val questSpawnsReward: Int = getInt(configuration, "gameplay.quests.rewards.spawns", 5, 1)
    val questMergesReward: Int = getInt(configuration, "gameplay.quests.rewards.merges", 7, 1)

    val abilityLuckyBurstBaseCost: Int = getInt(configuration, "gameplay.abilities.costs.luckyBurst", 8, 1)
    val abilityWoolRushBaseCost: Int = getInt(configuration, "gameplay.abilities.costs.woolRush", 10, 1)
    val abilityJackpotShearsBaseCost: Int = getInt(configuration, "gameplay.abilities.costs.jackpotShears", 15, 1)
    val abilityAutoMergeBaseCost: Int = getInt(configuration, "gameplay.abilities.costs.autoMerge", 18, 1)
    val abilityAutoShearBaseCost: Int = getInt(configuration, "gameplay.abilities.costs.autoShear", 12, 1)

    val abilityLuckyBurstBaseDurationMs: Long = getLong(configuration, "gameplay.abilities.durations.luckyBurstMs", 180_000L, 1L)
    val abilityWoolRushBaseDurationMs: Long = getLong(configuration, "gameplay.abilities.durations.woolRushMs", 240_000L, 1L)
    val abilityJackpotShearsBaseDurationMs: Long = getLong(configuration, "gameplay.abilities.durations.jackpotShearsMs", 120_000L, 1L)
    val abilityAutoMergeBaseDurationMs: Long = getLong(configuration, "gameplay.abilities.durations.autoMergeMs", 90_000L, 1L)
    val abilityAutoShearBaseDurationMs: Long = getLong(configuration, "gameplay.abilities.durations.autoShearMs", 90_000L, 1L)

    val questUpgradeDurationBaseCost: Int = getInt(configuration, "gameplay.questUpgrades.durationBaseCost", 12, 1)
    val questUpgradePowerBaseCost: Int = getInt(configuration, "gameplay.questUpgrades.powerBaseCost", 15, 1)

    val shearShopBaseCost: Int = getInt(configuration, "gameplay.shearShop.baseCost", 20, 1)
    val shearWoolSaveBaseCost: Int = getInt(configuration, "gameplay.shearShop.woolSaveBaseCost", 30, 1)
    val shearTierBoostBaseCost: Int = getInt(configuration, "gameplay.shearShop.tierBoostBaseCost", 45, 1)

    val comboDecayBaseCost: Int = getInt(configuration, "gameplay.combo.decayBaseCost", 75, 1)
    val comboGainBaseCost: Int = getInt(configuration, "gameplay.combo.gainBaseCost", 90, 1)
    val comboMaxBasePrestigeCost: Int = getInt(configuration, "gameplay.combo.maxBasePrestigeCost", 3, 1)

    val automationAutoBuyBaseCost: Int = getInt(configuration, "gameplay.automation.costs.autoBuy", 10, 1)
    val automationAutoAbilityBaseCost: Int = getInt(configuration, "gameplay.automation.costs.autoAbility", 14, 1)
    val automationSlowAutoMergeBaseCost: Int = getInt(configuration, "gameplay.automation.costs.slowAutoMerge", 16, 1)
    val automationSlowAutoShearBaseCost: Int = getInt(configuration, "gameplay.automation.costs.slowAutoShear", 12, 1)
    val automationAutoSpawnBaseCost: Int = getInt(configuration, "gameplay.automation.costs.autoSpawn", 10, 1)
    val automationPointIntervalMs: Long = getLong(configuration, "gameplay.automation.intervals.pointGainMs", 60_000L, 1L)
    val automationAutoBuyIntervalMs: Long = getLong(configuration, "gameplay.automation.intervals.autoBuyMs", 5_000L, 1L)
    val automationAutoAbilityIntervalMs: Long = getLong(configuration, "gameplay.automation.intervals.autoAbilityMs", 5_000L, 1L)
    val automationSlowAutoMergeIntervalMs: Long = getLong(configuration, "gameplay.automation.intervals.slowAutoMergeMs", 3_000L, 1L)
    val automationSlowAutoShearIntervalMs: Long = getLong(configuration, "gameplay.automation.intervals.slowAutoShearMs", 3_000L, 1L)
    val automationAutoSpawnBaseIntervalMs: Long = getLong(configuration, "gameplay.automation.intervals.autoSpawnBaseMs", 10_000L, 0L)
    val automationAutoSpawnIntervalStepMs: Long = getLong(configuration, "gameplay.automation.intervals.autoSpawnReductionMsPerLevel", 1_000L, 1L)
    val automationAutoSpawnMinIntervalMs: Long = getLong(configuration, "gameplay.automation.intervals.autoSpawnMinMs", 0L, 0L)
    val automationConditionMinPointsReserve: Long = getLong(configuration, "gameplay.automation.conditions.minPointsReserve", 0L, 0L)
    val automationConditionMinQuestPoints: Int = getInt(configuration, "gameplay.automation.conditions.minQuestPoints", 0, 0)
    val automationConditionMinSheepForMerge: Int = getInt(configuration, "gameplay.automation.conditions.minSheepForMerge", 2, 2)
    val automationConditionMinReadySheepForShear: Int = getInt(configuration, "gameplay.automation.conditions.minReadySheepForShear", 1, 1)

    val startingPlayerPoints: Long = getLong(configuration, "gameplay.starting.points", 1_000L, 0L)
    val tutorialShearTarget: Int = getInt(configuration, "gameplay.tutorial.targets.shears", 3, 1)
    val tutorialSpawnTarget: Int = getInt(configuration, "gameplay.tutorial.targets.spawns", 3, 1)
    val tutorialMergeTarget: Int = getInt(configuration, "gameplay.tutorial.targets.merges", 1, 1)
    val tutorialMenuSectionTarget: Int = getInt(configuration, "gameplay.tutorial.targets.menuSections", 8, 1)
    val prestigeLevelBaseCost: Int = getInt(configuration, "gameplay.prestige.levelBaseCost", 500, 1)

    companion object {
        private var instance: SheepMergeConfiguration? = null

        @JvmStatic
        fun initialize(plugin: SheepMergePlugin?) {
            if (plugin == null) {
                return
            }
            instance = SheepMergeConfiguration(plugin.config)
        }

        @JvmStatic
        fun get(): SheepMergeConfiguration? {
            return instance
        }

        private fun getLong(configuration: FileConfiguration?, path: String, defaultValue: Long, minValue: Long): Long {
            if (configuration == null) {
                return defaultValue
            }
            return configuration.getLong(path, defaultValue).coerceAtLeast(minValue)
        }

        private fun getInt(configuration: FileConfiguration?, path: String, defaultValue: Int, minValue: Int): Int {
            if (configuration == null) {
                return defaultValue
            }
            return configuration.getInt(path, defaultValue).coerceAtLeast(minValue)
        }

        private fun getDouble(configuration: FileConfiguration?, path: String, defaultValue: Double): Double {
            if (configuration == null) {
                return defaultValue
            }
            return configuration.getDouble(path, defaultValue)
        }
    }
}
