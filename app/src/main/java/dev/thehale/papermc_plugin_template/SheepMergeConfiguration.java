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

    private static long getLong(FileConfiguration configuration, String path, long defaultValue, long minValue) {
        if (configuration == null) {
            return defaultValue;
        }
        return Math.max(minValue, configuration.getLong(path, defaultValue));
    }

    private static double getDouble(FileConfiguration configuration, String path, double defaultValue) {
        if (configuration == null) {
            return defaultValue;
        }
        return configuration.getDouble(path, defaultValue);
    }
}
