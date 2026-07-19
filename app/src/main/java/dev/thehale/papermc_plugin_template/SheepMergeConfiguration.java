package dev.thehale.papermc_plugin_template;

import org.bukkit.configuration.file.FileConfiguration;

public final class SheepMergeConfiguration {

    private static SheepMergeConfiguration instance;

    private final long schedulerFastTickInterval;
    private final long schedulerNormalTickInterval;
    private final long schedulerReminderTickInterval;
    private final long schedulerTipIntervalTicks;

    private final double farmTeleportX;
    private final double farmTeleportY;
    private final double farmTeleportZ;

    private final long mergeReminderDelayMs;
    private final long mergeReminderRepeatMs;
    private final long tutorialReminderDelayMs;
    private final long tutorialReminderRepeatMs;
    private final long tutorialTaskTitleRepeatMs;
    private final long tutorialStatusFeedRepeatMs;
    private final long tutorialFocusNotificationCooldownMs;
    private final long tutorialMergePointsReminderRepeatMs;

    private final int upgradeLimitBaseCost;
    private final int upgradeEggSpeedBaseCost;
    private final int upgradeWoolRegenBaseCost;
    private final int upgradeHigherTierChanceBaseCost;

    private final int questShearsTarget;
    private final int questSpawnsTarget;
    private final int questMergesTarget;
    private final int questShearsReward;
    private final int questSpawnsReward;
    private final int questMergesReward;

    private final int abilityLuckyBurstBaseCost;
    private final int abilityWoolRushBaseCost;
    private final int abilityJackpotShearsBaseCost;
    private final int abilityAutoMergeBaseCost;
    private final int abilityAutoShearBaseCost;

    private final long abilityLuckyBurstBaseDurationMs;
    private final long abilityWoolRushBaseDurationMs;
    private final long abilityJackpotShearsBaseDurationMs;
    private final long abilityAutoMergeBaseDurationMs;
    private final long abilityAutoShearBaseDurationMs;

    private final int questUpgradeDurationBaseCost;
    private final int questUpgradePowerBaseCost;

    private final int shearShopBaseCost;
    private final int shearWoolSaveBaseCost;
    private final int shearTierBoostBaseCost;

    private final int comboDecayBaseCost;
    private final int comboGainBaseCost;
    private final int comboMaxBasePrestigeCost;

    private final int automationAutoBuyBaseCost;
    private final int automationAutoAbilityBaseCost;
    private final int automationSlowAutoMergeBaseCost;
    private final int automationSlowAutoShearBaseCost;
    private final int automationAutoSpawnBaseCost;
    private final long automationPointIntervalMs;
    private final long automationAutoBuyIntervalMs;
    private final long automationAutoAbilityIntervalMs;
    private final long automationSlowAutoMergeIntervalMs;
    private final long automationSlowAutoShearIntervalMs;
    private final long automationAutoSpawnBaseIntervalMs;
    private final long automationAutoSpawnIntervalStepMs;
    private final long automationAutoSpawnMinIntervalMs;
    private final long automationConditionMinPointsReserve;
    private final int automationConditionMinQuestPoints;
    private final int automationConditionMinSheepForMerge;
    private final int automationConditionMinReadySheepForShear;

    private final long startingPlayerPoints;
    private final int tutorialShearTarget;
    private final int tutorialSpawnTarget;
    private final int tutorialMergeTarget;
    private final int tutorialMenuSectionTarget;
    private final int prestigeLevelBaseCost;

    private SheepMergeConfiguration(FileConfiguration configuration) {
        schedulerFastTickInterval = getLong(configuration, "scheduling.fastTickInterval", 2L, 1L);
        schedulerNormalTickInterval = getLong(configuration, "scheduling.normalTickInterval", 20L, 1L);
        schedulerReminderTickInterval = getLong(configuration, "scheduling.reminderTickInterval", 20L, 1L);
        schedulerTipIntervalTicks = getLong(configuration, "scheduling.tipIntervalTicks", 1200L, 20L);

        farmTeleportX = getDouble(configuration, "farm.teleport.x", 0.5D);
        farmTeleportY = getDouble(configuration, "farm.teleport.y", 101.0D);
        farmTeleportZ = getDouble(configuration, "farm.teleport.z", 0.5D);

        mergeReminderDelayMs = getLong(configuration, "gameplay.reminders.merge.delayMs", 30_000L, 1L);
        mergeReminderRepeatMs = getLong(configuration, "gameplay.reminders.merge.repeatMs", 60_000L, 1L);
        tutorialReminderDelayMs = getLong(configuration, "gameplay.reminders.tutorial.delayMs", 120_000L, 1L);
        tutorialReminderRepeatMs = getLong(configuration, "gameplay.reminders.tutorial.repeatMs", 60_000L, 1L);
        tutorialTaskTitleRepeatMs = getLong(configuration, "gameplay.reminders.tutorialTaskTitle.repeatMs", 12_000L,
                1L);
        tutorialStatusFeedRepeatMs = getLong(configuration, "gameplay.reminders.tutorialStatusFeed.repeatMs", 12_000L,
                1L);
        tutorialFocusNotificationCooldownMs = getLong(configuration,
                "gameplay.reminders.tutorialFocus.cooldownMs", 5_000L, 0L);
        tutorialMergePointsReminderRepeatMs = getLong(configuration,
                "gameplay.reminders.tutorialMergePoints.repeatMs", 15_000L, 1L);

        upgradeLimitBaseCost = getInt(configuration, "gameplay.upgrades.limit.baseCost", 16, 1);
        upgradeEggSpeedBaseCost = getInt(configuration, "gameplay.upgrades.eggSpeed.baseCost", 20, 1);
        upgradeWoolRegenBaseCost = getInt(configuration, "gameplay.upgrades.woolRegen.baseCost", 24, 1);
        upgradeHigherTierChanceBaseCost = getInt(configuration, "gameplay.upgrades.higherTierChance.baseCost", 28, 1);

        questShearsTarget = getInt(configuration, "gameplay.quests.targets.shears", 20, 1);
        questSpawnsTarget = getInt(configuration, "gameplay.quests.targets.spawns", 12, 1);
        questMergesTarget = getInt(configuration, "gameplay.quests.targets.merges", 8, 1);
        questShearsReward = getInt(configuration, "gameplay.quests.rewards.shears", 8, 1);
        questSpawnsReward = getInt(configuration, "gameplay.quests.rewards.spawns", 5, 1);
        questMergesReward = getInt(configuration, "gameplay.quests.rewards.merges", 7, 1);

        abilityLuckyBurstBaseCost = getInt(configuration, "gameplay.abilities.costs.luckyBurst", 8, 1);
        abilityWoolRushBaseCost = getInt(configuration, "gameplay.abilities.costs.woolRush", 10, 1);
        abilityJackpotShearsBaseCost = getInt(configuration, "gameplay.abilities.costs.jackpotShears", 15, 1);
        abilityAutoMergeBaseCost = getInt(configuration, "gameplay.abilities.costs.autoMerge", 18, 1);
        abilityAutoShearBaseCost = getInt(configuration, "gameplay.abilities.costs.autoShear", 12, 1);

        abilityLuckyBurstBaseDurationMs = getLong(configuration, "gameplay.abilities.durations.luckyBurstMs", 180_000L,
                1L);
        abilityWoolRushBaseDurationMs = getLong(configuration, "gameplay.abilities.durations.woolRushMs", 240_000L,
                1L);
        abilityJackpotShearsBaseDurationMs = getLong(configuration, "gameplay.abilities.durations.jackpotShearsMs",
                120_000L, 1L);
        abilityAutoMergeBaseDurationMs = getLong(configuration, "gameplay.abilities.durations.autoMergeMs", 90_000L,
                1L);
        abilityAutoShearBaseDurationMs = getLong(configuration, "gameplay.abilities.durations.autoShearMs", 90_000L,
                1L);

        questUpgradeDurationBaseCost = getInt(configuration, "gameplay.questUpgrades.durationBaseCost", 12, 1);
        questUpgradePowerBaseCost = getInt(configuration, "gameplay.questUpgrades.powerBaseCost", 15, 1);

        shearShopBaseCost = getInt(configuration, "gameplay.shearShop.baseCost", 20, 1);
        shearWoolSaveBaseCost = getInt(configuration, "gameplay.shearShop.woolSaveBaseCost", 30, 1);
        shearTierBoostBaseCost = getInt(configuration, "gameplay.shearShop.tierBoostBaseCost", 45, 1);

        comboDecayBaseCost = getInt(configuration, "gameplay.combo.decayBaseCost", 75, 1);
        comboGainBaseCost = getInt(configuration, "gameplay.combo.gainBaseCost", 90, 1);
        comboMaxBasePrestigeCost = getInt(configuration, "gameplay.combo.maxBasePrestigeCost", 3, 1);

        automationAutoBuyBaseCost = getInt(configuration, "gameplay.automation.costs.autoBuy", 10, 1);
        automationAutoAbilityBaseCost = getInt(configuration, "gameplay.automation.costs.autoAbility", 14, 1);
        automationSlowAutoMergeBaseCost = getInt(configuration, "gameplay.automation.costs.slowAutoMerge", 16, 1);
        automationSlowAutoShearBaseCost = getInt(configuration, "gameplay.automation.costs.slowAutoShear", 12, 1);
        automationAutoSpawnBaseCost = getInt(configuration, "gameplay.automation.costs.autoSpawn", 20, 1);
        automationPointIntervalMs = getLong(configuration, "gameplay.automation.intervals.pointGainMs", 60_000L, 1L);
        automationAutoBuyIntervalMs = getLong(configuration, "gameplay.automation.intervals.autoBuyMs", 5_000L, 1L);
        automationAutoAbilityIntervalMs = getLong(configuration, "gameplay.automation.intervals.autoAbilityMs", 5_000L,
                1L);
        automationSlowAutoMergeIntervalMs = getLong(configuration, "gameplay.automation.intervals.slowAutoMergeMs",
                3_000L, 1L);
        automationSlowAutoShearIntervalMs = getLong(configuration, "gameplay.automation.intervals.slowAutoShearMs",
                3_000L, 1L);
        automationAutoSpawnBaseIntervalMs = getLong(configuration, "gameplay.automation.intervals.autoSpawnBaseMs",
                10_000L, 0L);
        automationAutoSpawnIntervalStepMs = getLong(configuration,
                "gameplay.automation.intervals.autoSpawnReductionMsPerLevel", 1_000L, 1L);
        automationAutoSpawnMinIntervalMs = getLong(configuration, "gameplay.automation.intervals.autoSpawnMinMs", 0L,
                0L);
        automationConditionMinPointsReserve = getLong(configuration, "gameplay.automation.conditions.minPointsReserve",
                0L, 0L);
        automationConditionMinQuestPoints = getInt(configuration, "gameplay.automation.conditions.minQuestPoints", 0,
                0);
        automationConditionMinSheepForMerge = getInt(configuration, "gameplay.automation.conditions.minSheepForMerge",
                2, 2);
        automationConditionMinReadySheepForShear = getInt(configuration,
                "gameplay.automation.conditions.minReadySheepForShear", 1, 1);

        startingPlayerPoints = getLong(configuration, "gameplay.starting.points", 1_000L, 0L);
        tutorialShearTarget = getInt(configuration, "gameplay.tutorial.targets.shears", 3, 1);
        tutorialSpawnTarget = getInt(configuration, "gameplay.tutorial.targets.spawns", 3, 1);
        tutorialMergeTarget = getInt(configuration, "gameplay.tutorial.targets.merges", 1, 1);
        tutorialMenuSectionTarget = getInt(configuration, "gameplay.tutorial.targets.menuSections", 8, 1);
        prestigeLevelBaseCost = getInt(configuration, "gameplay.prestige.levelBaseCost", 500, 1);
    }

    public static void initialize(SheepMergePlugin plugin) {
        if (plugin == null) {
            return;
        }
        instance = new SheepMergeConfiguration(plugin.getConfig());
    }

    public static SheepMergeConfiguration get() {
        return instance;
    }

    public long getSchedulerFastTickInterval() {
        return schedulerFastTickInterval;
    }

    public long getSchedulerNormalTickInterval() {
        return schedulerNormalTickInterval;
    }

    public long getSchedulerReminderTickInterval() {
        return schedulerReminderTickInterval;
    }

    public long getSchedulerTipIntervalTicks() {
        return schedulerTipIntervalTicks;
    }

    public double getFarmTeleportX() {
        return farmTeleportX;
    }

    public double getFarmTeleportY() {
        return farmTeleportY;
    }

    public double getFarmTeleportZ() {
        return farmTeleportZ;
    }

    public long getMergeReminderDelayMs() {
        return mergeReminderDelayMs;
    }

    public long getMergeReminderRepeatMs() {
        return mergeReminderRepeatMs;
    }

    public long getTutorialReminderDelayMs() {
        return tutorialReminderDelayMs;
    }

    public long getTutorialReminderRepeatMs() {
        return tutorialReminderRepeatMs;
    }

    public long getTutorialTaskTitleRepeatMs() {
        return tutorialTaskTitleRepeatMs;
    }

    public long getTutorialStatusFeedRepeatMs() {
        return tutorialStatusFeedRepeatMs;
    }

    public long getTutorialFocusNotificationCooldownMs() {
        return tutorialFocusNotificationCooldownMs;
    }

    public long getTutorialMergePointsReminderRepeatMs() {
        return tutorialMergePointsReminderRepeatMs;
    }

    public int getUpgradeLimitBaseCost() {
        return upgradeLimitBaseCost;
    }

    public int getUpgradeEggSpeedBaseCost() {
        return upgradeEggSpeedBaseCost;
    }

    public int getUpgradeWoolRegenBaseCost() {
        return upgradeWoolRegenBaseCost;
    }

    public int getUpgradeHigherTierChanceBaseCost() {
        return upgradeHigherTierChanceBaseCost;
    }

    public int getQuestShearsTarget() {
        return questShearsTarget;
    }

    public int getQuestSpawnsTarget() {
        return questSpawnsTarget;
    }

    public int getQuestMergesTarget() {
        return questMergesTarget;
    }

    public int getQuestShearsReward() {
        return questShearsReward;
    }

    public int getQuestSpawnsReward() {
        return questSpawnsReward;
    }

    public int getQuestMergesReward() {
        return questMergesReward;
    }

    public int getAbilityLuckyBurstBaseCost() {
        return abilityLuckyBurstBaseCost;
    }

    public int getAbilityWoolRushBaseCost() {
        return abilityWoolRushBaseCost;
    }

    public int getAbilityJackpotShearsBaseCost() {
        return abilityJackpotShearsBaseCost;
    }

    public int getAbilityAutoMergeBaseCost() {
        return abilityAutoMergeBaseCost;
    }

    public int getAbilityAutoShearBaseCost() {
        return abilityAutoShearBaseCost;
    }

    public long getAbilityLuckyBurstBaseDurationMs() {
        return abilityLuckyBurstBaseDurationMs;
    }

    public long getAbilityWoolRushBaseDurationMs() {
        return abilityWoolRushBaseDurationMs;
    }

    public long getAbilityJackpotShearsBaseDurationMs() {
        return abilityJackpotShearsBaseDurationMs;
    }

    public long getAbilityAutoMergeBaseDurationMs() {
        return abilityAutoMergeBaseDurationMs;
    }

    public long getAbilityAutoShearBaseDurationMs() {
        return abilityAutoShearBaseDurationMs;
    }

    public int getQuestUpgradeDurationBaseCost() {
        return questUpgradeDurationBaseCost;
    }

    public int getQuestUpgradePowerBaseCost() {
        return questUpgradePowerBaseCost;
    }

    public int getShearShopBaseCost() {
        return shearShopBaseCost;
    }

    public int getShearWoolSaveBaseCost() {
        return shearWoolSaveBaseCost;
    }

    public int getShearTierBoostBaseCost() {
        return shearTierBoostBaseCost;
    }

    public int getComboDecayBaseCost() {
        return comboDecayBaseCost;
    }

    public int getComboGainBaseCost() {
        return comboGainBaseCost;
    }

    public int getComboMaxBasePrestigeCost() {
        return comboMaxBasePrestigeCost;
    }

    public int getAutomationAutoBuyBaseCost() {
        return automationAutoBuyBaseCost;
    }

    public int getAutomationAutoAbilityBaseCost() {
        return automationAutoAbilityBaseCost;
    }

    public int getAutomationSlowAutoMergeBaseCost() {
        return automationSlowAutoMergeBaseCost;
    }

    public int getAutomationSlowAutoShearBaseCost() {
        return automationSlowAutoShearBaseCost;
    }

    public int getAutomationAutoSpawnBaseCost() {
        return automationAutoSpawnBaseCost;
    }

    public long getAutomationPointIntervalMs() {
        return automationPointIntervalMs;
    }

    public long getAutomationAutoBuyIntervalMs() {
        return automationAutoBuyIntervalMs;
    }

    public long getAutomationAutoAbilityIntervalMs() {
        return automationAutoAbilityIntervalMs;
    }

    public long getAutomationSlowAutoMergeIntervalMs() {
        return automationSlowAutoMergeIntervalMs;
    }

    public long getAutomationSlowAutoShearIntervalMs() {
        return automationSlowAutoShearIntervalMs;
    }

    public long getAutomationAutoSpawnBaseIntervalMs() {
        return automationAutoSpawnBaseIntervalMs;
    }

    public long getAutomationAutoSpawnIntervalStepMs() {
        return automationAutoSpawnIntervalStepMs;
    }

    public long getAutomationAutoSpawnMinIntervalMs() {
        return automationAutoSpawnMinIntervalMs;
    }

    public long getAutomationConditionMinPointsReserve() {
        return automationConditionMinPointsReserve;
    }

    public int getAutomationConditionMinQuestPoints() {
        return automationConditionMinQuestPoints;
    }

    public int getAutomationConditionMinSheepForMerge() {
        return automationConditionMinSheepForMerge;
    }

    public int getAutomationConditionMinReadySheepForShear() {
        return automationConditionMinReadySheepForShear;
    }

    public long getStartingPlayerPoints() {
        return startingPlayerPoints;
    }

    public int getTutorialShearTarget() {
        return tutorialShearTarget;
    }

    public int getTutorialSpawnTarget() {
        return tutorialSpawnTarget;
    }

    public int getTutorialMergeTarget() {
        return tutorialMergeTarget;
    }

    public int getTutorialMenuSectionTarget() {
        return tutorialMenuSectionTarget;
    }

    public int getPrestigeLevelBaseCost() {
        return prestigeLevelBaseCost;
    }

    private static long getLong(FileConfiguration configuration, String path, long defaultValue, long minValue) {
        if (configuration == null) {
            return defaultValue;
        }
        return Math.max(minValue, configuration.getLong(path, defaultValue));
    }

    private static int getInt(FileConfiguration configuration, String path, int defaultValue, int minValue) {
        if (configuration == null) {
            return defaultValue;
        }
        return Math.max(minValue, configuration.getInt(path, defaultValue));
    }

    private static double getDouble(FileConfiguration configuration, String path, double defaultValue) {
        if (configuration == null) {
            return defaultValue;
        }
        return configuration.getDouble(path, defaultValue);
    }
}
