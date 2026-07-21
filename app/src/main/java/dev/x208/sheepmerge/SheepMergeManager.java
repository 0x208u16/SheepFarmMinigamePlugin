package dev.x208.sheepmerge;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.nio.charset.StandardCharsets;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.util.Vector;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public final class SheepMergeManager {

    private static final Map<UUID, BigInteger> pointsByPlayer = new HashMap<>();
    private static final Map<UUID, Integer> extraLimitByPlayer = new HashMap<>();
    private static final Map<UUID, Integer> eggSpeedLevelByPlayer = new HashMap<>();
    private static final Map<UUID, Integer> woolRegenLevelByPlayer = new HashMap<>();
    private static final Map<UUID, Integer> higherTierChanceLevelByPlayer = new HashMap<>();
    private static final Map<UUID, Integer> prestigeLevelByPlayer = new HashMap<>();
    private static final Map<UUID, Integer> prestigePointsByPlayer = new HashMap<>();
    private static final Map<UUID, Integer> prestigeDoublePointsChanceByPlayer = new HashMap<>();
    private static final Map<UUID, Integer> prestigeHigherMaxLevelByPlayer = new HashMap<>();
    private static final Map<UUID, Integer> prestigeStartEggsByPlayer = new HashMap<>();
    private static final Map<UUID, Integer> prestigeEggCapByPlayer = new HashMap<>();
    private static final Map<UUID, Integer> prestigeBaseSpawnTierByPlayer = new HashMap<>();
    private static final Map<UUID, Integer> prestigeQuestRewardByPlayer = new HashMap<>();
    private static final Map<UUID, Integer> highestAnnouncedTierByPlayer = new HashMap<>();
    private static final Map<UUID, Integer> highestAnnouncedRainbowTierByPlayer = new HashMap<>();
    private static final Map<UUID, Integer> shearShopLevelByPlayer = new HashMap<>();
    private static final Map<UUID, Integer> shearWoolSaveLevelByPlayer = new HashMap<>();
    private static final Map<UUID, Integer> shearTierBoostLevelByPlayer = new HashMap<>();
    private static final Map<UUID, Boolean> tutorialCompletedByPlayer = new HashMap<>();
    private static final Map<UUID, Integer> tutorialShearsByPlayer = new HashMap<>();
    private static final Map<UUID, Integer> tutorialSpawnsByPlayer = new HashMap<>();
    private static final Map<UUID, Integer> tutorialMergesByPlayer = new HashMap<>();
    private static final Map<UUID, Boolean> tutorialUpgradeOpenedByPlayer = new HashMap<>();
    private static final Map<UUID, Boolean> tutorialQuestOpenedByPlayer = new HashMap<>();
    private static final Map<UUID, Boolean> tutorialQuestUpgradesOpenedByPlayer = new HashMap<>();
    private static final Map<UUID, Boolean> tutorialPrestigeOpenedByPlayer = new HashMap<>();
    private static final Map<UUID, Boolean> tutorialAbilityUsedByPlayer = new HashMap<>();
    private static final Map<UUID, Boolean> tutorialShearUpgradedByPlayer = new HashMap<>();
    private static final Map<UUID, Boolean> tutorialRegularUpgradesBoughtByPlayer = new HashMap<>();
    private static final Map<UUID, Boolean> tutorialShearTaskRewardGrantedByPlayer = new HashMap<>();
    private static final Map<UUID, Boolean> tutorialPrestigePrepRewardGrantedByPlayer = new HashMap<>();
    private static final Map<UUID, Boolean> tutorialPrestigedOnceByPlayer = new HashMap<>();
    private static final Map<UUID, Boolean> tutorialShearShopOpenedByPlayer = new HashMap<>();
    private static final Map<UUID, Boolean> tutorialBypassedByPlayer = new HashMap<>();
    private static final Map<UUID, Long> tutorialStartedAtByPlayer = new HashMap<>();
    private static final Map<UUID, Long> lastTutorialReminderTimestampByPlayer = new HashMap<>();
    private static final Map<UUID, Long> lastTutorialTaskTitleTimestampByPlayer = new HashMap<>();
    private static final Map<UUID, String> lastTutorialTaskTitleStepByPlayer = new HashMap<>();
    private static final Map<UUID, Long> lastTutorialStatusFeedTimestampByPlayer = new HashMap<>();
    private static final Map<UUID, String> lastTutorialProgressFeedLineByPlayer = new HashMap<>();
    private static final Map<UUID, String> lastTutorialStepFeedLineByPlayer = new HashMap<>();
    private static final Map<UUID, Long> lastTutorialFocusNotificationTimestampByPlayer = new HashMap<>();
    private static final Map<UUID, Long> lastTutorialMergePointsReminderTimestampByPlayer = new HashMap<>();
    private static final Map<UUID, Integer> questPointsByPlayer = new HashMap<>();
    private static final Map<UUID, List<SheepSnapshot>> savedFarmSheepByPlayer = new HashMap<>();
    private static final Map<UUID, List<SheepSnapshot>> savedTutorialSheepByPlayer = new HashMap<>();
    private static final Map<UUID, Long> nextQuestResetTimestampByPlayer = new HashMap<>();
    private static final Map<UUID, Integer> questShearsByPlayer = new HashMap<>();
    private static final Map<UUID, Integer> questSpawnsByPlayer = new HashMap<>();
    private static final Map<UUID, Integer> questMergesByPlayer = new HashMap<>();
    private static final Map<UUID, Boolean> questShearsCompleteByPlayer = new HashMap<>();
    private static final Map<UUID, Boolean> questSpawnsCompleteByPlayer = new HashMap<>();
    private static final Map<UUID, Boolean> questMergesCompleteByPlayer = new HashMap<>();
    private static final Map<UUID, Integer> questUpgradeDurationByPlayer = new HashMap<>();
    private static final Map<UUID, Integer> questUpgradePowerByPlayer = new HashMap<>();
    private static final Map<UUID, Long> activeLuckyBurstUntilByPlayer = new HashMap<>();
    private static final Map<UUID, Integer> activeLuckyBurstUsesByPlayer = new HashMap<>();
    private static final Map<UUID, Boolean> luckyBurstEnabledByPlayer = new HashMap<>();
    private static final Map<UUID, Long> activeWoolRushUntilByPlayer = new HashMap<>();
    private static final Map<UUID, Long> activeJackpotShearsUntilByPlayer = new HashMap<>();
    private static final Map<UUID, Long> activeAutoMergeUntilByPlayer = new HashMap<>();
    private static final Map<UUID, Integer> activeAutoMergeUsesByPlayer = new HashMap<>();
    private static final Map<UUID, Boolean> autoMergeEnabledByPlayer = new HashMap<>();
    private static final Map<UUID, Long> pausedLuckyBurstRemainingMsByPlayer = new HashMap<>();
    private static final Map<UUID, Long> pausedWoolRushRemainingMsByPlayer = new HashMap<>();
    private static final Map<UUID, Long> pausedJackpotShearsRemainingMsByPlayer = new HashMap<>();
    private static final Map<UUID, Long> pausedAutoMergeRemainingMsByPlayer = new HashMap<>();
    private static final Map<UUID, Long> nextAutoMergeAtByPlayer = new HashMap<>();
    private static final Map<UUID, Long> activeAutoShearUntilByPlayer = new HashMap<>();
    private static final Map<UUID, Integer> activeAutoShearUsesByPlayer = new HashMap<>();
    private static final Map<UUID, Boolean> autoShearEnabledByPlayer = new HashMap<>();
    private static final Map<UUID, Long> pausedAutoShearRemainingMsByPlayer = new HashMap<>();
    private static final Map<UUID, Long> nextAutoShearAtByPlayer = new HashMap<>();
    private static final Map<UUID, Long> lastAbilityAuraSoundTimestampByPlayer = new HashMap<>();
    private static final Map<UUID, Long> lastPrestigeReminderTimestampByPlayer = new HashMap<>();
    private static final Map<UUID, Boolean> prestigeTitleReminderShownByPlayer = new HashMap<>();
    private static final Map<UUID, Long> nextPrestigeRefundTimestampByPlayer = new HashMap<>();
    private static final Map<UUID, Long> lastMergeTimestampByPlayer = new HashMap<>();
    private static final Map<UUID, Long> lastMergeReminderTimestampByPlayer = new HashMap<>();
    private static final Map<UUID, Boolean> mergeTitleReminderShownByPlayer = new HashMap<>();
    private static final Map<UUID, Double> comboScoreByPlayer = new HashMap<>();
    private static final Map<UUID, Long> comboLastUpdateTimestampByPlayer = new HashMap<>();
    private static final Map<UUID, Integer> comboDecayUpgradeByPlayer = new HashMap<>();
    private static final Map<UUID, Integer> comboMaxUpgradeByPlayer = new HashMap<>();
    private static final Map<UUID, Integer> comboGainUpgradeByPlayer = new HashMap<>();
    private static final Map<UUID, Integer> automationPointsByPlayer = new HashMap<>();
    private static final Map<UUID, Integer> automationAutoBuyUpgradeByPlayer = new HashMap<>();
    private static final Map<UUID, Integer> automationAutoAbilityUpgradeByPlayer = new HashMap<>();
    private static final Map<UUID, Integer> automationSlowAutoMergeUpgradeByPlayer = new HashMap<>();
    private static final Map<UUID, Integer> automationSlowAutoShearUpgradeByPlayer = new HashMap<>();
    private static final Map<UUID, Integer> automationAutoSpawnUpgradeByPlayer = new HashMap<>();
    private static final Map<UUID, Integer> automationAutoPrestigeUpgradeByPlayer = new HashMap<>();
    private static final Map<UUID, Boolean> automationAutoBuyEnabledByPlayer = new HashMap<>();
    private static final Map<UUID, Boolean> automationAutoAbilityEnabledByPlayer = new HashMap<>();
    private static final Map<UUID, Boolean> automationSlowAutoMergeEnabledByPlayer = new HashMap<>();
    private static final Map<UUID, Boolean> automationSlowAutoShearEnabledByPlayer = new HashMap<>();
    private static final Map<UUID, Boolean> automationAutoSpawnEnabledByPlayer = new HashMap<>();
    private static final Map<UUID, Boolean> automationAutoPrestigeEnabledByPlayer = new HashMap<>();
    private static final Map<UUID, Long> nextAutomationPointAtByPlayer = new HashMap<>();
    private static final Map<UUID, Long> nextAutomationAutoBuyAtByPlayer = new HashMap<>();
    private static final Map<UUID, Long> nextAutomationAutoAbilityAtByPlayer = new HashMap<>();
    private static final Map<UUID, Long> nextAutomationSlowMergeAtByPlayer = new HashMap<>();
    private static final Map<UUID, Long> nextAutomationSlowShearAtByPlayer = new HashMap<>();
    private static final Map<UUID, Long> nextAutomationAutoSpawnAtByPlayer = new HashMap<>();
    private static final Map<UUID, Long> nextAutomationAutoPrestigeAtByPlayer = new HashMap<>();
    private static final Map<UUID, Long> pointsOverlayExpiresAtByPlayer = new HashMap<>();
    private static final Map<UUID, Integer> lastPointsOverlayByPlayer = new HashMap<>();
    private static final Map<UUID, BossBar> comboBossBarByPlayer = new HashMap<>();
    private static final Map<UUID, BossBar> visitFarmBossBarByPlayer = new HashMap<>();
    private static final Map<UUID, Sheep> carriedSheepByPlayer = new HashMap<>();
    private static final Map<UUID, Long> sheepRescueStartByEntity = new HashMap<>();
    private static final Map<UUID, org.bukkit.Location> sheepRescueOriginByEntity = new HashMap<>();
    private static final Map<UUID, Long> sheepRescueNextCorrectionAtByEntity = new HashMap<>();
    private static final Map<UUID, InventoryDataUtils.Snapshot> savedInventories = new HashMap<>();
    private static final Map<UUID, Scoreboard> savedScoreboards = new HashMap<>();
    private static final Map<UUID, Integer> liveSheepCountByWorld = new HashMap<>();
    private static final Map<UUID, Boolean> farmVisitEnabledByPlayer = new HashMap<>();
    private static final Map<UUID, BigInteger> sacrificePointsByPlayer = new HashMap<>();
    private static final Map<UUID, Integer> sacrificeUnlocksBoughtByPlayer = new HashMap<>();
    private static final Map<UUID, Integer> scoreboardLayoutModeByPlayer = new HashMap<>();
    private static final Map<UUID, Boolean> scoreboardShowQuestPointsByPlayer = new HashMap<>();
    private static final Map<UUID, Boolean> scoreboardShowAutomationPointsByPlayer = new HashMap<>();
    private static final Map<UUID, Boolean> scoreboardShowSacrificePointsByPlayer = new HashMap<>();
    private static final Map<UUID, Boolean> scoreboardShowQuestProgressByPlayer = new HashMap<>();
    private static final Map<UUID, Boolean> scoreboardShowAbilityStatusByPlayer = new HashMap<>();
    private static final Map<UUID, Long> lastSpawnLimitWarningTimestampByPlayer = new HashMap<>();
    private static final Map<UUID, Long> lastOutOfEggWarningTimestampByPlayer = new HashMap<>();
    private static final String TOP_POINTS_DISPLAY_WORLD_KEY = "topPointsDisplay.world";
    private static final String TOP_POINTS_DISPLAY_X_KEY = "topPointsDisplay.x";
    private static final String TOP_POINTS_DISPLAY_Y_KEY = "topPointsDisplay.y";
    private static final String TOP_POINTS_DISPLAY_Z_KEY = "topPointsDisplay.z";
    private static final String TOP_POINTS_DISPLAY_YAW_KEY = "topPointsDisplay.yaw";
    private static final String TOP_POINTS_DISPLAY_PITCH_KEY = "topPointsDisplay.pitch";
    private static final Pattern OWNER_ID_PATTERN = Pattern.compile("^sheepfarm_([0-9a-fA-F]{32})$");
    private static final Pattern TUTORIAL_OWNER_ID_PATTERN = Pattern.compile("^sheeptutorial_([0-9a-fA-F]{32})$");
    private static final Random RANDOM = new Random();
    private static final int BASE_SHEEP_LIMIT = 10;
    private static final int MAX_SHEEP_LIMIT = 50;
    private static final int SACRIFICE_UNLOCK_MAX_SHEEP_LIMIT = 100;
    private static final int LIMIT_UPGRADE_STEP = 5;
    private static int LIMIT_UPGRADE_COST = 16;
    private static final int BASE_EGG_INTERVAL_SECONDS = 10;
    private static final int MIN_EGG_INTERVAL_SECONDS = 2;
    private static final int MIN_EGG_INTERVAL_SECONDS_WITH_SACRIFICE = 1;
    private static final int EGG_SPEED_MAX_LEVEL = BASE_EGG_INTERVAL_SECONDS - MIN_EGG_INTERVAL_SECONDS;
    private static final int EGG_SPEED_BASE_MAX_LEVEL = Math.max(1, EGG_SPEED_MAX_LEVEL / 2);
    private static int EGG_SPEED_UPGRADE_BASE_COST = 20;
    private static int WOOL_REGEN_UPGRADE_BASE_COST = 24;
    private static final int WOOL_REGEN_MAX_LEVEL = 8;
    private static final int WOOL_REGEN_BASE_MAX_LEVEL = Math.max(1, WOOL_REGEN_MAX_LEVEL / 2);
    private static int HIGHER_TIER_CHANCE_UPGRADE_BASE_COST = 28;
    private static final int HIGHER_TIER_CHANCE_MAX_LEVEL = 10;
    private static final int HIGHER_TIER_CHANCE_BASE_MAX_LEVEL = Math.max(1, HIGHER_TIER_CHANCE_MAX_LEVEL / 2);
    private static final int HIGHER_TIER_CHANCE_HARD_MAX_LEVEL = 10;
    private static final int PRESTIGE_CAP_BONUS_PER_LEVEL = 2;
    private static final int HIGHER_TIER_CHANCE_BASE_CAP_PERCENT = 50;
    private static final int QUEST_LUCKY_BURST_SPAWN_CHANCE_BONUS_PERCENT = 50;
    private static final int PRESTIGE_DOUBLE_POINTS_BASE_COST = 1;
    private static final int PRESTIGE_DOUBLE_POINTS_MAX_LEVEL = 20;
    private static final int PRESTIGE_HIGHER_MAX_LEVEL_BASE_COST = 2;
    private static final int PRESTIGE_START_EGGS_BASE_COST = 1;
    private static final int PRESTIGE_EGG_CAP_BASE_COST = 2;
    private static final int PRESTIGE_BASE_SPAWN_TIER_BASE_COST = 10;
    private static final int PRESTIGE_QUEST_REWARD_BASE_COST = 4;
    private static int QUEST_SHEARS_TARGET = 20;
    private static int QUEST_SPAWNS_TARGET = 12;
    private static int QUEST_MERGES_TARGET = 8;
    private static int QUEST_SHEARS_REWARD = 8;
    private static int QUEST_SPAWNS_REWARD = 5;
    private static int QUEST_MERGES_REWARD = 7;
    private static final long BASE_QUEST_RESET_MS = 15L * 60L * 1000L;
    private static final long MIN_QUEST_RESET_MS = 5L * 60L * 1000L;
    private static final long QUEST_RESET_REDUCTION_PER_PRESTIGE_MS = 60L * 1000L;
    private static int QUEST_LUCKY_BURST_BASE_COST = 8;
    private static int QUEST_WOOL_RUSH_BASE_COST = 10;
    private static int QUEST_JACKPOT_SHEARS_BASE_COST = 15;
    private static int QUEST_AUTO_MERGE_BASE_COST = 18;
    private static int QUEST_AUTO_SHEAR_BASE_COST = 12;
    private static long QUEST_LUCKY_BURST_BASE_DURATION_MS = 3L * 60L * 1000L;
    private static long QUEST_WOOL_RUSH_BASE_DURATION_MS = 4L * 60L * 1000L;
    private static long QUEST_JACKPOT_SHEARS_BASE_DURATION_MS = 2L * 60L * 1000L;
    private static long QUEST_AUTO_MERGE_BASE_DURATION_MS = 90L * 1000L;
    private static long QUEST_AUTO_SHEAR_BASE_DURATION_MS = 90L * 1000L;
    private static final long ABILITY_AURA_SOUND_INTERVAL_MS = 15_000L;
    private static final long QUEST_AUTO_MERGE_INTERVAL_MS = 1000L;
    private static final long QUEST_AUTO_SHEAR_INTERVAL_MS = 1000L;
    private static int QUEST_UPGRADE_DURATION_BASE_COST = 12;
    private static int QUEST_UPGRADE_POWER_BASE_COST = 15;
    private static final int BASE_EGG_CAP = 10;
    private static final int PRESTIGE_EGG_CAP_STEP = 10;
    private static final int PRESTIGE_MAX_LEVEL = Integer.MAX_VALUE;
    private static final int PRESTIGE_QUEST_REWARD_MAX_LEVEL = 36;
    private static final double PRESTIGE_QUEST_REWARD_BONUS_PER_LEVEL = 0.25D;
    private static final long PRESTIGE_REFUND_COOLDOWN_MS = 30L * 60L * 1000L;
    private static final String FARM_BUILD_WORLD_NAME = "sheepfarm_build";
    private static final double FARM_CENTER_X = 0.5D;
    private static final double FARM_CENTER_Z = 0.5D;
    private static final int FARM_MIN_XZ = -4;
    private static final int FARM_MAX_XZ = 5;
    private static final int FARM_RADIUS = Math.max(Math.abs(FARM_MIN_XZ), Math.abs(FARM_MAX_XZ));
    private static final int FARM_BASE_Y = 100;
    private static final int FARM_MIN_Y = FARM_BASE_Y - 1;
    private static final int FARM_MAX_Y = FARM_BASE_Y + 4;
    private static final long SHEEP_RESCUE_TIMEOUT_MS = 10_000L;
    private static final long SHEEP_RESCUE_PATH_DURATION_MS = 1_350L;
    private static final long SHEEP_RESCUE_CORRECTION_INTERVAL_MS = 80L;
    private static final double SHEEP_RESCUE_POSITION_CORRECTION_DISTANCE = 0.42D;
    private static final double SHEEP_RESCUE_HORIZONTAL_VELOCITY = 0.62D;
    private static final double SHEEP_RESCUE_UPWARD_VELOCITY = 0.75D;
    private static final double SHEEP_RESCUE_DOWNWARD_VELOCITY = -0.85D;
    private static final double SHEEP_RESCUE_ARCH_HEIGHT_BASE = 1.20D;
    private static final double SHEEP_RESCUE_ARCH_HEIGHT_PER_BLOCK = 0.14D;
    private static final double SHEEP_RESCUE_ARCH_HEIGHT_MAX = 3.20D;
    private static final double SHEEP_RESCUE_EDGE_MARGIN = 0.60D;
    private static final double SHEEP_FALL_TRIGGER_EDGE_MARGIN = 0.25D;
    private static final double SHEEP_FALL_TRIGGER_Y = FARM_BASE_Y + 1.05D;
    private static final long RAINBOW_ANIMATION_STEP_MS = 220L;
    private static final org.bukkit.DyeColor[] RAINBOW_ANIMATION_COLORS = {
            org.bukkit.DyeColor.RED,
            org.bukkit.DyeColor.ORANGE,
            org.bukkit.DyeColor.YELLOW,
            org.bukkit.DyeColor.LIME,
            org.bukkit.DyeColor.LIGHT_BLUE,
            org.bukkit.DyeColor.BLUE,
            org.bukkit.DyeColor.PURPLE,
            org.bukkit.DyeColor.MAGENTA,
            org.bukkit.DyeColor.PINK
    };
    private static final int FARM_UPGRADE_COMMAND_SLOT = 8;
    private static int SHEAR_SHOP_BASE_COST = 20;
    private static int SHEAR_WOOL_SAVE_BASE_COST = 30;
    private static int SHEAR_TIER_BOOST_BASE_COST = 45;
    private static final int SHEAR_WOOL_SAVE_CHANCE_PER_LEVEL = 5;
    private static final int SHEAR_TIER_BOOST_CHANCE_PER_LEVEL = 1;
    private static final int SHEAR_WOOL_SAVE_CHANCE_CAP = 50;
    private static final int SHEAR_TIER_BOOST_CHANCE_CAP = 75;
    private static final int SHEAR_WOOL_SAVE_MAX_LEVEL = SHEAR_WOOL_SAVE_CHANCE_CAP / SHEAR_WOOL_SAVE_CHANCE_PER_LEVEL;
    private static final int SHEAR_TIER_BOOST_MAX_LEVEL = 25;
    private static final long SPAWN_LIMIT_WARNING_COOLDOWN_MS = 5_000L;
    private static final long OUT_OF_EGGS_WARNING_COOLDOWN_MS = 2_500L;
    private static long MERGE_REMINDER_DELAY_MS = 30_000L;
    private static long MERGE_REMINDER_REPEAT_MS = 60_000L;
    private static long TUTORIAL_REMINDER_DELAY_MS = 2L * 60L * 1000L;
    private static long TUTORIAL_REMINDER_REPEAT_MS = 60_000L;
    private static long TUTORIAL_TASK_TITLE_REPEAT_MS = 12_000L;
    private static long TUTORIAL_STATUS_FEED_REPEAT_MS = 12_000L;
    private static long TUTORIAL_FOCUS_NOTIFICATION_COOLDOWN_MS = 8_000L;
    private static long TUTORIAL_MERGE_POINTS_REMINDER_REPEAT_MS = 15_000L;
    private static final int WOOL_REGEN_PERCENT_DISPLAY_DECIMALS = 6;
    private static final int WOOL_REGEN_FACTOR_DISPLAY_DECIMALS = 10;
    private static final double WOOL_REGEN_MAX_REDUCTION_PERCENT = 99.999999D;
    private static final double WOOL_REGEN_COOLDOWN_REDUCTION_PER_LEVEL_PERCENT = 25.0D;
    private static final double WOOL_REGEN_PER_LEVEL_MULTIPLIER = 1.0D
            - (WOOL_REGEN_COOLDOWN_REDUCTION_PER_LEVEL_PERCENT / 100.0D);
    private static final long RANDOM_EVENT_ROLL_INTERVAL_MS = 60_000L;
    private static final int RANDOM_EVENT_TRIGGER_CHANCE_DENOMINATOR = 10;
    private static final long SHEEP_RAIN_EVENT_DURATION_MS = 60_000L;
    private static final long SHEEP_RAIN_MIN_INTERVAL_MS = 1_000L;
    private static final long SHEEP_RAIN_MAX_INTERVAL_MS = 3_000L;
    private static final int SHEEP_RAIN_SPAWN_HEIGHT = 12;
    private static final double SHEEP_RAIN_HORIZONTAL_PADDING = 1.5D;
    private static final long COMBO_FRENZY_EVENT_DURATION_MS = 60_000L;
    private static final double COMBO_FRENZY_MULTIPLIER = 10.0D;
    private static final long POINTS_OVERLAY_DISPLAY_DURATION_MS = 1_400L;
    private static final double BASE_COMBO_DECAY_PER_SECOND = 1.3D;
    private static final double COMBO_DECAY_HIGH_LEVEL_SCALING = 2.2D;
    private static final double COMBO_BASE_MAX_SCORE = 100.0D;
    private static final double COMBO_MAX_SCORE_PER_LEVEL = 50.0D;
    private static final int COMBO_DECAY_MAX_LEVEL = 20;
    private static final int COMBO_MAX_MAX_LEVEL = 20;
    private static final int COMBO_GAIN_MAX_LEVEL = 20;
    private static int COMBO_DECAY_BASE_COST = 75;
    private static int COMBO_GAIN_BASE_COST = 90;
    private static int COMBO_MAX_BASE_PRESTIGE_COST = 3;
    private static int AUTOMATION_AUTO_BUY_BASE_COST = 10;
    private static int AUTOMATION_AUTO_ABILITY_BASE_COST = 14;
    private static int AUTOMATION_SLOW_AUTO_MERGE_BASE_COST = 16;
    private static int AUTOMATION_SLOW_AUTO_SHEAR_BASE_COST = 12;
    private static int AUTOMATION_AUTO_SPAWN_BASE_COST = 10;
    private static final int AUTOMATION_SINGLE_LEVEL_MAX = 1;
    private static final int AUTOMATION_AUTO_BUY_MAX_LEVEL = 5;
    private static final int AUTOMATION_SLOW_AUTO_MERGE_MAX_LEVEL = 3;
    private static final int AUTOMATION_SLOW_AUTO_SHEAR_MAX_LEVEL = 3;
    private static final int AUTOMATION_AUTO_SPAWN_MAX_LEVEL = 10;
    private static int AUTOMATION_AUTO_PRESTIGE_BASE_COST = 64;
    private static long AUTOMATION_POINT_INTERVAL_MS = 60_000L;
    private static long AUTOMATION_AUTO_BUY_INTERVAL_MS = 5_000L;
    private static long AUTOMATION_AUTO_ABILITY_INTERVAL_MS = 5_000L;
    private static long AUTOMATION_SLOW_AUTO_MERGE_INTERVAL_MS = 3_000L;
    private static long AUTOMATION_SLOW_AUTO_SHEAR_INTERVAL_MS = 3_000L;
    private static final long AUTOMATION_MIN_INTERVAL_MS = 1_000L;
    private static long AUTOMATION_AUTO_SPAWN_BASE_INTERVAL_MS = 10_000L;
    private static long AUTOMATION_AUTO_SPAWN_INTERVAL_STEP_MS = 1_000L;
    private static long AUTOMATION_AUTO_SPAWN_MIN_INTERVAL_MS = 0L;
    private static long AUTOMATION_AUTO_PRESTIGE_INTERVAL_MS = 5_000L;
    private static long AUTOMATION_CONDITION_MIN_POINTS_RESERVE = 0L;
    private static int AUTOMATION_CONDITION_MIN_QUEST_POINTS = 0;
    private static int AUTOMATION_CONDITION_MIN_SHEEP_FOR_MERGE = 2;
    private static int AUTOMATION_CONDITION_MIN_READY_SHEEP_FOR_SHEAR = 1;
    private static final double COMBO_GAIN_PERCENT_PER_LEVEL = 10.0D;
    private static final double COMBO_POINT_MULTIPLIER_PER_SCORE = 0.015D;
    private static final long BACKUP_AUTOMATIC_PERMANENT_INTERVAL_MS = 7L * 24L * 60L * 60L * 1000L;
    private static final long BACKUP_AUTOMATIC_BUFFER_INTERVAL_MS = 24L * 60L * 60L * 1000L;
    private static final int BACKUP_AUTOMATIC_BUFFER_MAX_FILES = 7;
    private static final long BACKUP_AUTOMATIC_ROLLING_INTERVAL_TICKS = 20L * 60L * 60L;
    private static final String BACKUP_DIR_NAME = "backups";
    private static final String BACKUP_INDEX_FILE_NAME = "backup-index.yml";
    private static final String BACKUP_ROLLING_FILE_NAME = "rolling-auto-latest.zip";
    private static final String BACKUP_INDEX_LAST_PERMANENT_AT_KEY = "lastPermanentAt";
    private static final String BACKUP_INDEX_LAST_BUFFER_AT_KEY = "lastBufferAt";
    private static final String BACKUP_INDEX_MARKED_FOR_DELETION_KEY = "markedForDeletion";
    private static final String BACKUP_BUFFER_FILE_PREFIX = "buffer-24h-";
    private static final long BACKUP_SOFT_DELETE_GRACE_MS = 24L * 60L * 60L * 1000L;
    private static final DateTimeFormatter BACKUP_TIMESTAMP_FORMATTER = DateTimeFormatter
            .ofPattern("yyyyMMdd-HHmmss")
            .withZone(ZoneOffset.UTC);
    private static long STARTING_PLAYER_POINTS = 1_000L;
    private static int TUTORIAL_SHEAR_TARGET = 3;
    private static int TUTORIAL_SPAWN_TARGET = 3;
    private static int TUTORIAL_MERGE_TARGET = 1;
    private static int TUTORIAL_MENU_SECTION_TARGET = 8;
    private static int PRESTIGE_LEVEL_BASE_COST = 500;
    public static final String UPGRADE_MENU_TITLE = "Sheep Merge Upgrades";
    public static final String PRESTIGE_MENU_TITLE = "Prestige Upgrades";
    public static final String QUEST_MENU_TITLE = "Quest Abilities";
    public static final String QUEST_UPGRADES_MENU_TITLE = "Quest Upgrades";
    public static final String SHOP_MENU_TITLE = "Shear Shop";
    public static final String COMBO_SHOP_MENU_TITLE = "Combo Upgrades";
    public static final String AUTOMATION_MENU_TITLE = "Automation Upgrades";
    public static final String SACRIFICE_MENU_TITLE = "Sacrifice Unlocks";
    public static final String SCOREBOARD_MENU_TITLE = "Scoreboard Settings";
    public static final int LIMIT_UPGRADE_SLOT = 10;
    public static final int EGG_SPEED_UPGRADE_SLOT = 12;
    public static final int WOOL_REGEN_UPGRADE_SLOT = 14;
    public static final int HIGHER_TIER_CHANCE_UPGRADE_SLOT = 16;
    public static final int PRESTIGE_MENU_OPEN_SLOT = 22;
    public static final int QUEST_MENU_OPEN_SLOT = 20;
    public static final int SHOP_MENU_OPEN_SLOT = 24;
    public static final int COMBO_MENU_OPEN_SLOT = 18;
    public static final int AUTOMATION_MENU_OPEN_SLOT = 26;
    public static final int SACRIFICE_MENU_OPEN_SLOT = 8;
    public static final int PRESTIGE_UPGRADE_SLOT = 10;
    public static final int PRESTIGE_DOUBLE_POINTS_SLOT = 12;
    public static final int PRESTIGE_HIGHER_MAX_LEVEL_SLOT = 14;
    public static final int PRESTIGE_START_EGGS_SLOT = 16;
    public static final int PRESTIGE_EGG_CAP_SLOT = 18;
    public static final int PRESTIGE_BASE_SPAWN_TIER_SLOT = 20;
    public static final int PRESTIGE_QUEST_REWARD_SLOT = 22;
    public static final int PRESTIGE_REFUND_SLOT = 24;
    public static final int PRESTIGE_BACK_TO_UPGRADES_SLOT = 26;
    public static final int QUEST_ABILITY_LUCKY_BURST_SLOT = 10;
    public static final int QUEST_ABILITY_WOOL_RUSH_SLOT = 13;
    public static final int QUEST_ABILITY_JACKPOT_SHEARS_SLOT = 16;
    public static final int QUEST_ABILITY_AUTO_MERGE_SLOT = 19;
    public static final int QUEST_ABILITY_AUTO_SHEAR_SLOT = 22;
    public static final int QUEST_OPEN_UPGRADES_SLOT = 24;
    public static final int QUEST_BACK_TO_UPGRADES_SLOT = 26;
    public static final int QUEST_BOARD_SLOT = 4;
    public static final int QUEST_UPGRADE_DURATION_SLOT = 11;
    public static final int QUEST_UPGRADE_POWER_SLOT = 15;
    public static final int QUEST_UPGRADE_BACK_SLOT = 26;
    public static final int SHOP_SHEAR_KEEP_WOOL_SLOT = 11;
    public static final int SHOP_SHEAR_SLOT = 13;
    public static final int SHOP_SHEAR_TIER_BOOST_SLOT = 15;
    public static final int SHOP_BACK_TO_UPGRADES_SLOT = 26;
    public static final int COMBO_DECAY_SLOT = 10;
    public static final int COMBO_MAX_SLOT = 13;
    public static final int COMBO_GAIN_SLOT = 16;
    public static final int COMBO_BACK_TO_UPGRADES_SLOT = 26;
    public static final int AUTOMATION_AUTO_BUY_SLOT = 10;
    public static final int AUTOMATION_AUTO_ABILITY_SLOT = 12;
    public static final int AUTOMATION_SLOW_AUTO_MERGE_SLOT = 14;
    public static final int AUTOMATION_AUTO_PRESTIGE_SLOT = 15;
    public static final int AUTOMATION_SLOW_AUTO_SHEAR_SLOT = 16;
    public static final int AUTOMATION_AUTO_SPAWN_SLOT = 11;
    public static final int AUTOMATION_AUTO_BUY_TOGGLE_SLOT = 19;
    public static final int AUTOMATION_AUTO_ABILITY_TOGGLE_SLOT = 21;
    public static final int AUTOMATION_SLOW_AUTO_MERGE_TOGGLE_SLOT = 23;
    public static final int AUTOMATION_AUTO_PRESTIGE_TOGGLE_SLOT = 24;
    public static final int AUTOMATION_SLOW_AUTO_SHEAR_TOGGLE_SLOT = 25;
    public static final int AUTOMATION_AUTO_SPAWN_TOGGLE_SLOT = 20;
    public static final int AUTOMATION_ENABLE_ALL_SLOT = 13;
    public static final int AUTOMATION_DISABLE_ALL_SLOT = 22;
    public static final int AUTOMATION_BACK_TO_UPGRADES_SLOT = 26;
    public static final int SACRIFICE_POINTS_SLOT = 4;
    public static final int SACRIFICE_ALL_SHEEP_SLOT = 10;
    public static final int SACRIFICE_UNLOCK_REGULAR_RESETS_SLOT = 11;
    public static final int SACRIFICE_UNLOCK_COMBO_RESETS_SLOT = 12;
    public static final int SACRIFICE_UNLOCK_SHEAR_RESETS_SLOT = 13;
    public static final int SACRIFICE_UNLOCK_EGG_COOLDOWN_SLOT = 14;
    public static final int SACRIFICE_UNLOCK_MAX_SHEEP_SLOT = 15;
    public static final int SACRIFICE_REFUND_SLOT = 24;
    public static final int SACRIFICE_BACK_TO_UPGRADES_SLOT = 26;
    public static final int SCOREBOARD_LAYOUT_SLOT = 10;
    public static final int SCOREBOARD_QUEST_POINTS_SLOT = 12;
    public static final int SCOREBOARD_AUTOMATION_POINTS_SLOT = 14;
    public static final int SCOREBOARD_SACRIFICE_POINTS_SLOT = 16;
    public static final int SCOREBOARD_QUEST_PROGRESS_SLOT = 20;
    public static final int SCOREBOARD_ABILITIES_SLOT = 22;
    public static final int SCOREBOARD_BACK_SLOT = 26;

    private static final int SACRIFICE_UNLOCK_NO_REGULAR_RESETS = 1;
    private static final int SACRIFICE_UNLOCK_NO_COMBO_RESETS = 2;
    private static final int SACRIFICE_UNLOCK_NO_SHEAR_RESETS = 3;
    private static final int SACRIFICE_UNLOCK_EGG_COOLDOWN_TO_1S = 4;
    private static final int SACRIFICE_UNLOCK_MAX_SHEEP_100 = 5;
    private static final int SACRIFICE_UNLOCK_MAX = SACRIFICE_UNLOCK_MAX_SHEEP_100;
    private static final BigInteger SACRIFICE_UNLOCK_COST_MULTIPLIER = BigInteger.valueOf(1000L);
    private static final int FARM_EGG_ITEM_SLOT = 7;

    private static SheepMergePlugin plugin;
    private static final SheepEggModule EGG_MODULE = new SheepEggModule();
    private static FileConfiguration dataConfig;
    private static File dataFile;
    private static FileConfiguration farmLayoutConfig;
    private static File farmLayoutFile;
    private static long nextRandomEventRollAtMs = 0L;
    private static long sheepRainEventEndsAtMs = 0L;
    private static long nextSheepRainSpawnAtMs = 0L;
    private static long comboFrenzyEventEndsAtMs = 0L;
    private static BossBar sheepRainBossBar;
    private static int lastGameplayTipIndex = -1;
    private static boolean farmCommitInProgress = false;

    private static final class SheepSnapshot {
        private final int tierLevel;
        private final double x;
        private final double y;
        private final double z;
        private final boolean sheared;
        private final long nextEatAt;
        private final int mergedCount;

        private SheepSnapshot(int tierLevel, double x, double y, double z, boolean sheared, long nextEatAt,
                int mergedCount) {
            this.tierLevel = tierLevel;
            this.x = x;
            this.y = y;
            this.z = z;
            this.sheared = sheared;
            this.nextEatAt = nextEatAt;
            this.mergedCount = Math.max(1, mergedCount);
        }
    }

    private static final List<String> GAMEPLAY_TIPS = List.of(
            "&7Use &e/sheepmerge &7to jump to your farm. Use it again while visiting to return home.",
            "&7Your upgrade item is the &bNether Star &7in hotbar slot 9. Right-click it to open upgrades.",
            "&7Eggs are shown as your XP level. The XP bar shows time until the next egg.",
            "&7Spawn eggs are in hotbar slot 8. No eggs? Wait for the timer or raise egg speed.",
            "&7Merge faster: sneak-right-click a sheep to carry it, then right-click a same-tier sheep.",
            "&7Shearing and merging together are your main point income. Keep both loops active.",
            "&7Rainbow sheep can merge with matching rainbow tier to push rainbow tiers higher forever.",
            "&7Shear Shop boosts shear value and adds procs like Wool Keeper and Tier Booster.",
            "&7Quest objectives reset over time. Finish them to earn quest points for active abilities.",
            "&7Quest Upgrades increase ability duration and lower ability costs.",
            "&7Combo score multiplies your gains. Keep merging to avoid decay and maintain high value.",
            "&7Combo Upgrades improve decay, gain, and max combo cap.",
            "&7Prestige resets normal progress and grants prestige points for permanent account upgrades.",
            "&7Prestige upgrades can raise egg cap, base spawn tier, and several maximum upgrade caps.",
            "&7Prestige refund lets you respec prestige upgrades after cooldown.",
            "&7Automation points are earned over playtime. Spend them in Automation Upgrades.",
            "&7Automation tracks start disabled. Buy and toggle each track on when you are ready.",
            "&7Auto Spawn uses eggs and can be upgraded down to near-instant checks.",
            "&7Auto Prestige can run automatically once unlocked and toggled on.",
            "&7Use &e/sheepmerge visit <player> &7to visit open farms and &e/sheepmerge visit -toggle &7to manage access.",
            "&7Use &e/sheepmerge status &7to quickly check your points, quests, combo, and prestige progress.",
            "&7Admins: &e/sheepmerge backup list/load/delete/recover &7manage compressed backups safely.");

    private SheepMergeManager() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void initialize(SheepMergePlugin plugin) {
        SheepMergeManager.plugin = plugin;
        dataFile = new File(plugin.getDataFolder(), "scores.yml");
        farmLayoutFile = new File(plugin.getDataFolder(), "farm-layout.yml");
        loadData();
        loadFarmLayout();
    }

    public static void applyConfiguration(SheepMergeConfiguration configuration) {
        if (configuration == null) {
            return;
        }
        LIMIT_UPGRADE_COST = configuration.getUpgradeLimitBaseCost();
        EGG_SPEED_UPGRADE_BASE_COST = configuration.getUpgradeEggSpeedBaseCost();
        WOOL_REGEN_UPGRADE_BASE_COST = configuration.getUpgradeWoolRegenBaseCost();
        HIGHER_TIER_CHANCE_UPGRADE_BASE_COST = configuration.getUpgradeHigherTierChanceBaseCost();
        QUEST_SHEARS_TARGET = configuration.getQuestShearsTarget();
        QUEST_SPAWNS_TARGET = configuration.getQuestSpawnsTarget();
        QUEST_MERGES_TARGET = configuration.getQuestMergesTarget();
        QUEST_SHEARS_REWARD = configuration.getQuestShearsReward();
        QUEST_SPAWNS_REWARD = configuration.getQuestSpawnsReward();
        QUEST_MERGES_REWARD = configuration.getQuestMergesReward();
        QUEST_LUCKY_BURST_BASE_COST = configuration.getAbilityLuckyBurstBaseCost();
        QUEST_WOOL_RUSH_BASE_COST = configuration.getAbilityWoolRushBaseCost();
        QUEST_JACKPOT_SHEARS_BASE_COST = configuration.getAbilityJackpotShearsBaseCost();
        QUEST_AUTO_MERGE_BASE_COST = configuration.getAbilityAutoMergeBaseCost();
        QUEST_AUTO_SHEAR_BASE_COST = configuration.getAbilityAutoShearBaseCost();
        QUEST_LUCKY_BURST_BASE_DURATION_MS = configuration.getAbilityLuckyBurstBaseDurationMs();
        QUEST_WOOL_RUSH_BASE_DURATION_MS = configuration.getAbilityWoolRushBaseDurationMs();
        QUEST_JACKPOT_SHEARS_BASE_DURATION_MS = configuration.getAbilityJackpotShearsBaseDurationMs();
        QUEST_AUTO_MERGE_BASE_DURATION_MS = configuration.getAbilityAutoMergeBaseDurationMs();
        QUEST_AUTO_SHEAR_BASE_DURATION_MS = configuration.getAbilityAutoShearBaseDurationMs();
        QUEST_UPGRADE_DURATION_BASE_COST = configuration.getQuestUpgradeDurationBaseCost();
        QUEST_UPGRADE_POWER_BASE_COST = configuration.getQuestUpgradePowerBaseCost();
        SHEAR_SHOP_BASE_COST = configuration.getShearShopBaseCost();
        SHEAR_WOOL_SAVE_BASE_COST = configuration.getShearWoolSaveBaseCost();
        SHEAR_TIER_BOOST_BASE_COST = configuration.getShearTierBoostBaseCost();
        COMBO_DECAY_BASE_COST = configuration.getComboDecayBaseCost();
        COMBO_GAIN_BASE_COST = configuration.getComboGainBaseCost();
        COMBO_MAX_BASE_PRESTIGE_COST = configuration.getComboMaxBasePrestigeCost();
        AUTOMATION_AUTO_BUY_BASE_COST = configuration.getAutomationAutoBuyBaseCost();
        AUTOMATION_AUTO_ABILITY_BASE_COST = configuration.getAutomationAutoAbilityBaseCost();
        AUTOMATION_SLOW_AUTO_MERGE_BASE_COST = configuration.getAutomationSlowAutoMergeBaseCost();
        AUTOMATION_SLOW_AUTO_SHEAR_BASE_COST = configuration.getAutomationSlowAutoShearBaseCost();
        AUTOMATION_AUTO_SPAWN_BASE_COST = Math.max(1, configuration.getAutomationAutoSpawnBaseCost() / 2);
        AUTOMATION_POINT_INTERVAL_MS = configuration.getAutomationPointIntervalMs();
        AUTOMATION_AUTO_BUY_INTERVAL_MS = configuration.getAutomationAutoBuyIntervalMs();
        AUTOMATION_AUTO_ABILITY_INTERVAL_MS = configuration.getAutomationAutoAbilityIntervalMs();
        AUTOMATION_SLOW_AUTO_MERGE_INTERVAL_MS = configuration.getAutomationSlowAutoMergeIntervalMs();
        AUTOMATION_SLOW_AUTO_SHEAR_INTERVAL_MS = configuration.getAutomationSlowAutoShearIntervalMs();
        AUTOMATION_AUTO_SPAWN_BASE_INTERVAL_MS = configuration.getAutomationAutoSpawnBaseIntervalMs();
        AUTOMATION_AUTO_SPAWN_INTERVAL_STEP_MS = configuration.getAutomationAutoSpawnIntervalStepMs();
        AUTOMATION_AUTO_SPAWN_MIN_INTERVAL_MS = configuration.getAutomationAutoSpawnMinIntervalMs();
        AUTOMATION_CONDITION_MIN_POINTS_RESERVE = configuration.getAutomationConditionMinPointsReserve();
        AUTOMATION_CONDITION_MIN_QUEST_POINTS = configuration.getAutomationConditionMinQuestPoints();
        AUTOMATION_CONDITION_MIN_SHEEP_FOR_MERGE = configuration.getAutomationConditionMinSheepForMerge();
        AUTOMATION_CONDITION_MIN_READY_SHEEP_FOR_SHEAR = configuration.getAutomationConditionMinReadySheepForShear();
        STARTING_PLAYER_POINTS = configuration.getStartingPlayerPoints();
        TUTORIAL_SHEAR_TARGET = configuration.getTutorialShearTarget();
        TUTORIAL_SPAWN_TARGET = configuration.getTutorialSpawnTarget();
        TUTORIAL_MERGE_TARGET = configuration.getTutorialMergeTarget();
        TUTORIAL_MENU_SECTION_TARGET = configuration.getTutorialMenuSectionTarget();
        PRESTIGE_LEVEL_BASE_COST = configuration.getPrestigeLevelBaseCost();
        MERGE_REMINDER_DELAY_MS = configuration.getMergeReminderDelayMs();
        MERGE_REMINDER_REPEAT_MS = configuration.getMergeReminderRepeatMs();
        TUTORIAL_REMINDER_DELAY_MS = configuration.getTutorialReminderDelayMs();
        TUTORIAL_REMINDER_REPEAT_MS = configuration.getTutorialReminderRepeatMs();
        TUTORIAL_TASK_TITLE_REPEAT_MS = configuration.getTutorialTaskTitleRepeatMs();
        TUTORIAL_STATUS_FEED_REPEAT_MS = configuration.getTutorialStatusFeedRepeatMs();
        TUTORIAL_FOCUS_NOTIFICATION_COOLDOWN_MS = configuration.getTutorialFocusNotificationCooldownMs();
        TUTORIAL_MERGE_POINTS_REMINDER_REPEAT_MS = configuration.getTutorialMergePointsReminderRepeatMs();
    }

    public static int getFarmRadius() {
        return FARM_RADIUS;
    }

    public static int getFarmBaseY() {
        return FARM_BASE_Y;
    }

    public static boolean hasSavedFarmLayout() {
        if (farmLayoutConfig == null) {
            return false;
        }
        if (farmLayoutConfig.isConfigurationSection("chunks")
                && !farmLayoutConfig.getConfigurationSection("chunks").getKeys(false).isEmpty()) {
            return true;
        }
        return farmLayoutConfig.isConfigurationSection("blocks")
                && !farmLayoutConfig.getConfigurationSection("blocks").getKeys(false).isEmpty();
    }

    public static String getFarmBuildWorldName() {
        return FARM_BUILD_WORLD_NAME;
    }

    public static boolean isFarmBuildWorld(World world) {
        return world != null && FARM_BUILD_WORLD_NAME.equals(world.getName());
    }

    public static boolean saveSharedFarmLayoutFromWorld(World sourceWorld) {
        if (sourceWorld == null || (!isSheepFarmWorld(sourceWorld) && !isFarmBuildWorld(sourceWorld))) {
            return false;
        }
        if (farmLayoutConfig == null) {
            farmLayoutConfig = new YamlConfiguration();
        }
        farmLayoutConfig.set("version", 2);
        farmLayoutConfig.set("world.minY", sourceWorld.getMinHeight());
        farmLayoutConfig.set("world.maxY", sourceWorld.getMaxHeight());
        farmLayoutConfig.set("world.name", sourceWorld.getName());
        farmLayoutConfig.set("world.savedAt", System.currentTimeMillis());
        farmLayoutConfig.set("chunks", null);
        farmLayoutConfig.set("blocks", null);

        int minY = sourceWorld.getMinHeight();
        int maxY = sourceWorld.getMaxHeight();
        if (minY >= maxY) {
            return false;
        }
        for (org.bukkit.Chunk chunk : sourceWorld.getLoadedChunks()) {
            int chunkX = chunk.getX();
            int chunkZ = chunk.getZ();

            String chunkPath = "chunks." + chunkKeyFor(chunkX, chunkZ);
            farmLayoutConfig.set(chunkPath + ".x", chunkX);
            farmLayoutConfig.set(chunkPath + ".z", chunkZ);

            List<String> palette = new ArrayList<>();
            Map<String, Integer> paletteIndices = new HashMap<>();
            StringBuilder encodedRuns = new StringBuilder();
            int previousPaletteIndex = -1;
            int runLength = 0;

            for (int y = minY; y < maxY; y++) {
                for (int localX = 0; localX < 16; localX++) {
                    for (int localZ = 0; localZ < 16; localZ++) {
                        int worldX = (chunkX << 4) + localX;
                        int worldZ = (chunkZ << 4) + localZ;
                        String serializedBlockData = sourceWorld.getBlockAt(worldX, y, worldZ)
                                .getBlockData()
                                .getAsString();

                        Integer paletteIndex = paletteIndices.get(serializedBlockData);
                        if (paletteIndex == null) {
                            paletteIndex = palette.size();
                            palette.add(serializedBlockData);
                            paletteIndices.put(serializedBlockData, paletteIndex);
                        }

                        if (paletteIndex == previousPaletteIndex) {
                            runLength++;
                        } else {
                            appendChunkRun(encodedRuns, previousPaletteIndex, runLength);
                            previousPaletteIndex = paletteIndex;
                            runLength = 1;
                        }
                    }
                }
            }

            appendChunkRun(encodedRuns, previousPaletteIndex, runLength);
            farmLayoutConfig.set(chunkPath + ".format", "rle-v1");
            farmLayoutConfig.set(chunkPath + ".palette", palette);
            farmLayoutConfig.set(chunkPath + ".data", encodedRuns.toString());
            farmLayoutConfig.set(chunkPath + ".height", maxY - minY);
        }
        return saveFarmLayout();
    }

    public static int applySharedFarmLayoutToAllFarmWorlds() {
        if (plugin == null) {
            return 0;
        }
        int updated = 0;
        for (World world : plugin.getServer().getWorlds()) {
            if (!isSheepFarmWorld(world)) {
                continue;
            }
            applyFarmLayout(world);
            updated++;
        }
        return updated;
    }

    public static void applyFarmLayout(World world) {
        if (world == null) {
            return;
        }
        if (hasSavedFarmLayout()) {
            applySavedFarmLayout(world);
        } else {
            applyDefaultFarmLayout(world);
        }
    }

    private static void applySavedFarmLayout(World world) {
        if (farmLayoutConfig != null && farmLayoutConfig.isConfigurationSection("chunks")) {
            applySavedChunkLayout(world);
            return;
        }

        for (int x = FARM_MIN_XZ; x <= FARM_MAX_XZ; x++) {
            for (int y = FARM_MIN_Y; y <= FARM_MAX_Y; y++) {
                for (int z = FARM_MIN_XZ; z <= FARM_MAX_XZ; z++) {
                    String serialized = farmLayoutConfig.getString("blocks." + keyFor(x, y, z));
                    BlockData data = (serialized == null || serialized.isBlank())
                            ? Bukkit.createBlockData(getDefaultFarmMaterialAt(x, y, z))
                            : parseBlockData(serialized);
                    world.getBlockAt(x, y, z).setBlockData(data, true);
                }
            }
        }
    }

    private static void applySavedChunkLayout(World world) {
        if (world == null || farmLayoutConfig == null || !farmLayoutConfig.isConfigurationSection("chunks")) {
            return;
        }

        org.bukkit.configuration.ConfigurationSection chunksSection = farmLayoutConfig
                .getConfigurationSection("chunks");
        if (chunksSection == null || chunksSection.getKeys(false).isEmpty()) {
            return;
        }

        int minY = Math.max(world.getMinHeight(), farmLayoutConfig.getInt("world.minY", world.getMinHeight()));
        int maxY = Math.min(world.getMaxHeight(), farmLayoutConfig.getInt("world.maxY", world.getMaxHeight()));
        if (minY >= maxY) {
            return;
        }

        for (String chunkKey : chunksSection.getKeys(false)) {
            String chunkPath = "chunks." + chunkKey;
            int chunkX = farmLayoutConfig.getInt(chunkPath + ".x", Integer.MIN_VALUE);
            int chunkZ = farmLayoutConfig.getInt(chunkPath + ".z", Integer.MIN_VALUE);
            if (chunkX == Integer.MIN_VALUE || chunkZ == Integer.MIN_VALUE) {
                continue;
            }
            List<String> palette = farmLayoutConfig.getStringList(chunkPath + ".palette");
            String encodedRuns = farmLayoutConfig.getString(chunkPath + ".data", "");
            if (!palette.isEmpty() && encodedRuns != null && !encodedRuns.isBlank()) {
                int totalBlocks = (maxY - minY) * 16 * 16;
                int blockIndex = 0;
                String[] tokens = encodedRuns.split(";");
                for (String token : tokens) {
                    if (token == null || token.isBlank() || blockIndex >= totalBlocks) {
                        continue;
                    }

                    int separator = token.indexOf('*');
                    String paletteIndexRaw = separator >= 0 ? token.substring(0, separator) : token;
                    String runLengthRaw = separator >= 0 ? token.substring(separator + 1) : "1";

                    int paletteIndex = parseChunkEncodedNumber(paletteIndexRaw, -1);
                    int runLength = parseChunkEncodedNumber(runLengthRaw, 1);
                    if (paletteIndex < 0 || paletteIndex >= palette.size() || runLength <= 0) {
                        continue;
                    }

                    String serialized = palette.get(paletteIndex);
                    BlockData data = (serialized == null || serialized.isBlank())
                            ? Bukkit.createBlockData(Material.AIR)
                            : parseBlockData(serialized);

                    for (int i = 0; i < runLength && blockIndex < totalBlocks; i++) {
                        int yOffset = blockIndex / (16 * 16);
                        int withinLayer = blockIndex % (16 * 16);
                        int localX = withinLayer / 16;
                        int localZ = withinLayer % 16;
                        int worldX = (chunkX << 4) + localX;
                        int worldZ = (chunkZ << 4) + localZ;
                        int y = minY + yOffset;
                        world.getBlockAt(worldX, y, worldZ).setBlockData(data, true);
                        blockIndex++;
                    }
                }
                continue;
            }

            int blockIndex = 0;
            for (int y = minY; y < maxY; y++) {
                for (int localX = 0; localX < 16; localX++) {
                    for (int localZ = 0; localZ < 16; localZ++) {
                        int worldX = (chunkX << 4) + localX;
                        int worldZ = (chunkZ << 4) + localZ;
                        String serialized = farmLayoutConfig.getString(chunkPath + ".blocks." + blockIndex);
                        BlockData data = (serialized == null || serialized.isBlank())
                                ? Bukkit.createBlockData(Material.AIR)
                                : parseBlockData(serialized);
                        world.getBlockAt(worldX, y, worldZ).setBlockData(data, true);
                        blockIndex++;
                    }
                }
            }
        }
    }

    private static void applyDefaultFarmLayout(World world) {
        for (int x = FARM_MIN_XZ; x <= FARM_MAX_XZ; x++) {
            for (int y = FARM_MIN_Y; y <= FARM_MAX_Y; y++) {
                for (int z = FARM_MIN_XZ; z <= FARM_MAX_XZ; z++) {
                    Material material = getDefaultFarmMaterialAt(x, y, z);
                    world.getBlockAt(x, y, z).setBlockData(Bukkit.createBlockData(material), true);
                }
            }
        }
    }

    public static void saveSheepSnapshotForWorld(World world) {
        if (world == null || !isSheepFarmWorld(world)) {
            return;
        }
        UUID ownerId = getOwnerId(world);
        if (ownerId == null) {
            return;
        }
        getSavedSheepSnapshots(world).put(ownerId, captureSheepSnapshots(world));
    }

    public static void restoreSavedSheepForWorld(World world) {
        if (world == null || !isSheepFarmWorld(world)) {
            return;
        }
        UUID ownerId = getOwnerId(world);
        if (ownerId == null) {
            return;
        }
        List<SheepSnapshot> snapshots = getSavedSheepSnapshots(world).get(ownerId);
        if (snapshots == null || snapshots.isEmpty()) {
            refreshLiveSheepCount(world);
            return;
        }

        long now = System.currentTimeMillis();
        for (SheepSnapshot snapshot : snapshots) {
            if (snapshot == null) {
                continue;
            }
            Sheep sheep = world.spawn(new Location(world, snapshot.x, snapshot.y, snapshot.z), Sheep.class);
            SheepTier tier = SheepTier.byLevel(snapshot.tierLevel);
            setSheepTier(sheep, tier);
            setRainbowTier(sheep, snapshot.mergedCount);
            sheep.setAdult();
            if (snapshot.sheared && snapshot.nextEatAt > now) {
                sheep.setSheared(true);
                setNextEatTimestamp(sheep, snapshot.nextEatAt);
            } else {
                sheep.setSheared(false);
                setNextEatTimestamp(sheep, 0L);
            }
            updateSheepName(sheep);
        }
        refreshLiveSheepCount(world);
    }

    public static void rebuildFarmWorld(World world) {
        if (world == null || !isSheepFarmWorld(world)) {
            return;
        }
        clearSheepEntities(world);
        applyFarmLayout(world);
        restoreSavedSheepForWorld(world);
        world.save();
    }

    public static int commitFarmBuildWorldToLoadedFarms() {
        if (plugin == null || farmCommitInProgress) {
            return 0;
        }
        World buildWorld = Bukkit.getWorld(FARM_BUILD_WORLD_NAME);
        if (buildWorld == null || !isFarmBuildWorld(buildWorld) || !saveSharedFarmLayoutFromWorld(buildWorld)) {
            return 0;
        }

        List<World> farmWorlds = new ArrayList<>();
        for (World world : plugin.getServer().getWorlds()) {
            if (isSheepFarmWorld(world)) {
                farmWorlds.add(world);
            }
        }
        if (farmWorlds.isEmpty()) {
            saveData();
            return 0;
        }

        World fallbackWorld = plugin.getServer().getWorlds().isEmpty() ? null : plugin.getServer().getWorlds().get(0);
        farmCommitInProgress = true;
        processFarmCommitBatch(farmWorlds, fallbackWorld, null, 0, 0);
        return farmWorlds.size();
    }

    public static boolean isFarmBuildCommitInProgress() {
        return farmCommitInProgress;
    }

    public static int startCommitFarmBuildWorldToLoadedFarms(Player initiator) {
        if (plugin == null || farmCommitInProgress) {
            return 0;
        }

        World buildWorld = Bukkit.getWorld(FARM_BUILD_WORLD_NAME);
        if (buildWorld == null || !isFarmBuildWorld(buildWorld) || !saveSharedFarmLayoutFromWorld(buildWorld)) {
            return 0;
        }

        List<World> farmWorlds = new ArrayList<>();
        for (World world : plugin.getServer().getWorlds()) {
            if (isSheepFarmWorld(world)) {
                farmWorlds.add(world);
            }
        }
        if (farmWorlds.isEmpty()) {
            saveData();
            return 0;
        }

        World fallbackWorld = plugin.getServer().getWorlds().isEmpty() ? null : plugin.getServer().getWorlds().get(0);
        farmCommitInProgress = true;
        processFarmCommitBatch(farmWorlds, fallbackWorld, initiator, 0, 0);
        return farmWorlds.size();
    }

    private static void processFarmCommitBatch(List<World> farmWorlds, World fallbackWorld, Player initiator, int index,
            int updatedCount) {
        if (plugin == null) {
            farmCommitInProgress = false;
            return;
        }

        if (index >= farmWorlds.size()) {
            saveData();
            farmCommitInProgress = false;
            if (initiator != null && initiator.isOnline()) {
                initiator.sendMessage(action("Committed the shared farm build world into " + updatedCount
                        + " loaded farm world(s)."));
            }
            return;
        }

        World world = farmWorlds.get(index);
        if (world != null) {
            teleportPlayersOutOfWorld(world, fallbackWorld);
            saveSheepSnapshotForWorld(world);
            rebuildFarmWorld(world);
            updatedCount++;
        }

        final int nextIndex = index + 1;
        final int nextUpdatedCount = updatedCount;
        plugin.getServer().getScheduler().runTaskLater(plugin,
                () -> processFarmCommitBatch(farmWorlds, fallbackWorld, initiator, nextIndex, nextUpdatedCount),
                1L);
    }

    public static void saveBuildWorldIfIdle() {
        World buildWorld = Bukkit.getWorld(FARM_BUILD_WORLD_NAME);
        if (buildWorld == null) {
            return;
        }
        for (Player player : buildWorld.getPlayers()) {
            if (player != null && player.isOp()) {
                return;
            }
        }
        buildWorld.save();
    }

    private static void clearSheepEntities(World world) {
        if (world == null) {
            return;
        }
        for (Sheep sheep : world.getEntitiesByClass(Sheep.class)) {
            if (sheep == null || !sheep.isValid()) {
                continue;
            }
            UUID sheepId = sheep.getUniqueId();
            sheep.remove();
            clearSheepRescueState(sheepId);
        }
        refreshLiveSheepCount(world);
    }

    private static List<SheepSnapshot> captureSheepSnapshots(World world) {
        List<SheepSnapshot> snapshots = new ArrayList<>();
        if (world == null) {
            return snapshots;
        }
        for (Sheep sheep : world.getEntitiesByClass(Sheep.class)) {
            if (sheep == null || !sheep.isValid() || sheep.isDead()) {
                continue;
            }
            SheepTier tier = getSheepTier(sheep);
            Location location = sheep.getLocation();
            snapshots.add(new SheepSnapshot(
                    tier == null ? SheepTier.WHITE.getLevel() : tier.getLevel(),
                    location.getX(),
                    location.getY(),
                    location.getZ(),
                    sheep.isSheared(),
                    getNextEatTimestamp(sheep),
                    getRainbowTier(sheep)));
        }
        return snapshots;
    }

    private static Map<UUID, List<SheepSnapshot>> getSavedSheepSnapshots(World world) {
        return isTutorialWorld(world) ? savedTutorialSheepByPlayer : savedFarmSheepByPlayer;
    }

    private static void teleportPlayersOutOfWorld(World world, World fallbackWorld) {
        if (world == null || fallbackWorld == null) {
            return;
        }
        Location fallbackSpawn = fallbackWorld.getSpawnLocation().clone().add(0.5D, 0.0D, 0.5D);
        for (Player online : world.getPlayers()) {
            if (online == null) {
                continue;
            }
            online.teleport(fallbackSpawn);
            online.sendMessage(hint("Your farm is being refreshed. Use /sheepmerge to return once it finishes."));
        }
    }

    private static Material getDefaultFarmMaterialAt(int x, int y, int z) {
        if (y == FARM_MIN_Y) {
            return Material.DIRT;
        }
        if (y == FARM_BASE_Y) {
            return Material.GRASS_BLOCK;
        }
        if (y == FARM_BASE_Y + 1
                && (x == FARM_MIN_XZ || x == FARM_MAX_XZ || z == FARM_MIN_XZ || z == FARM_MAX_XZ)) {
            return Material.OAK_FENCE;
        }
        return Material.AIR;
    }

    private static Material parseMaterial(String materialName) {
        if (materialName == null || materialName.isBlank()) {
            return Material.AIR;
        }
        Material material = Material.matchMaterial(materialName);
        return material == null ? Material.AIR : material;
    }

    private static BlockData parseBlockData(String serializedData) {
        if (serializedData == null || serializedData.isBlank()) {
            return Bukkit.createBlockData(Material.AIR);
        }
        try {
            return Bukkit.createBlockData(serializedData);
        } catch (IllegalArgumentException ignored) {
            return Bukkit.createBlockData(parseMaterial(serializedData));
        }
    }

    private static String keyFor(int x, int y, int z) {
        return x + "," + y + "," + z;
    }

    private static String chunkKeyFor(int chunkX, int chunkZ) {
        return chunkX + "," + chunkZ;
    }

    private static void appendChunkRun(StringBuilder encodedRuns, int paletteIndex, int runLength) {
        if (encodedRuns == null || paletteIndex < 0 || runLength <= 0) {
            return;
        }
        if (encodedRuns.length() > 0) {
            encodedRuns.append(';');
        }
        encodedRuns.append(Integer.toString(paletteIndex, 36));
        if (runLength > 1) {
            encodedRuns.append('*').append(Integer.toString(runLength, 36));
        }
    }

    private static int parseChunkEncodedNumber(String rawValue, int fallback) {
        if (rawValue == null || rawValue.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(rawValue, 36);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static void loadFarmLayout() {
        if (plugin == null || farmLayoutFile == null) {
            return;
        }
        farmLayoutConfig = YamlConfiguration.loadConfiguration(farmLayoutFile);
    }

    private static boolean saveFarmLayout() {
        if (plugin == null || farmLayoutFile == null || farmLayoutConfig == null) {
            return false;
        }
        try {
            if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
                return false;
            }
            farmLayoutConfig.save(farmLayoutFile);
            return true;
        } catch (IOException exception) {
            plugin.getLogger().warning("Unable to save farm layout: " + exception.getMessage());
            return false;
        }
    }

    public static NamespacedKey getTierKey() {
        return new NamespacedKey(plugin, "sheep-tier");
    }

    private static NamespacedKey getTopPointsDisplayKey() {
        return new NamespacedKey(plugin, "top-points-display");
    }

    private static NamespacedKey getNextEatKey() {
        return new NamespacedKey(plugin, "sheep-next-eat");
    }

    private static NamespacedKey getRainbowTierKey() {
        return new NamespacedKey(plugin, "rainbow-tier");
    }

    private static NamespacedKey getLegacyRainbowMergedCountKey() {
        return new NamespacedKey(plugin, "rainbow-merged-count");
    }

    public static boolean isSheepFarmWorld(World world) {
        return world != null
                && (world.getName().startsWith("sheepfarm_") || world.getName().startsWith("sheeptutorial_"));
    }

    public static UUID getFarmOwnerId(World world) {
        return getOwnerId(world);
    }

    public static boolean isFarmOwner(Player player, World world) {
        if (player == null || world == null) {
            return false;
        }
        UUID ownerId = getOwnerId(world);
        return ownerId != null && ownerId.equals(player.getUniqueId());
    }

    public static void updateVisitFarmBossBar(Player player) {
        if (player == null) {
            return;
        }

        World world = player.getWorld();
        if (!isSheepFarmWorld(world) || isFarmOwner(player, world)) {
            clearVisitFarmBossBar(player);
            return;
        }

        UUID playerId = player.getUniqueId();
        BossBar bar = visitFarmBossBarByPlayer.get(playerId);
        if (bar == null) {
            bar = Bukkit.createBossBar("Visiting Farm", BarColor.BLUE, BarStyle.SOLID);
            visitFarmBossBarByPlayer.put(playerId, bar);
        }

        UUID ownerId = getFarmOwnerId(world);
        String ownerName = null;
        if (ownerId != null) {
            Player ownerPlayer = Bukkit.getPlayer(ownerId);
            ownerName = ownerPlayer == null ? Bukkit.getOfflinePlayer(ownerId).getName() : ownerPlayer.getName();
        }
        if (ownerName == null || ownerName.isBlank()) {
            ownerName = "another player's farm";
        }

        bar.setTitle(color("&e" + ownerName + " &7| &fUse /sheepmerge to return"));
        bar.setProgress(1.0D);
        bar.setVisible(true);
        bar.addPlayer(player);
    }

    public static void clearVisitFarmBossBar(Player player) {
        if (player == null) {
            return;
        }
        BossBar bar = visitFarmBossBarByPlayer.remove(player.getUniqueId());
        if (bar == null) {
            return;
        }
        bar.removeAll();
        bar.setVisible(false);
    }

    public static boolean isFarmVisitable(UUID ownerId) {
        if (ownerId == null) {
            return false;
        }
        return farmVisitEnabledByPlayer.getOrDefault(ownerId, true);
    }

    public static boolean toggleFarmVisitable(Player owner) {
        if (owner == null) {
            return false;
        }
        UUID ownerId = owner.getUniqueId();
        boolean next = !farmVisitEnabledByPlayer.getOrDefault(ownerId, true);
        farmVisitEnabledByPlayer.put(ownerId, next);
        saveData();
        return next;
    }

    public static boolean shouldNotifySpawnLimit(Player player) {
        if (player == null) {
            return false;
        }
        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();
        long last = lastSpawnLimitWarningTimestampByPlayer.getOrDefault(playerId, 0L);
        if (now - last < SPAWN_LIMIT_WARNING_COOLDOWN_MS) {
            return false;
        }
        lastSpawnLimitWarningTimestampByPlayer.put(playerId, now);
        return true;
    }

    public static boolean shouldNotifyOutOfEggs(Player player) {
        if (player == null) {
            return false;
        }
        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();
        long last = lastOutOfEggWarningTimestampByPlayer.getOrDefault(playerId, 0L);
        if (now - last < OUT_OF_EGGS_WARNING_COOLDOWN_MS) {
            return false;
        }
        lastOutOfEggWarningTimestampByPlayer.put(playerId, now);
        return true;
    }

    public static boolean isTutorialWorld(World world) {
        return world != null && world.getName().startsWith("sheeptutorial_");
    }

    private static boolean isOwnedSheepFarmWorld(World world) {
        return world != null && world.getName().startsWith("sheepfarm_");
    }

    public static String getTutorialWorldName(UUID playerId) {
        return "sheeptutorial_" + playerId.toString().replace("-", "");
    }

    public static boolean isTutorialCompleted(Player player) {
        return player != null && tutorialCompletedByPlayer.getOrDefault(player.getUniqueId(), false);
    }

    public static boolean hasUnlockedFarm(Player player) {
        if (player == null) {
            return false;
        }
        UUID playerId = player.getUniqueId();
        return hasUnlockedFarm(playerId);
    }

    public static boolean hasUnlockedFarm(UUID playerId) {
        if (playerId == null) {
            return false;
        }
        return tutorialCompletedByPlayer.getOrDefault(playerId, false)
                || tutorialBypassedByPlayer.getOrDefault(playerId, false);
    }

    private static void clearTutorialRuntimeState(UUID playerId) {
        if (playerId == null) {
            return;
        }
        tutorialStartedAtByPlayer.remove(playerId);
        lastTutorialReminderTimestampByPlayer.remove(playerId);
        lastTutorialTaskTitleTimestampByPlayer.remove(playerId);
        lastTutorialTaskTitleStepByPlayer.remove(playerId);
        lastTutorialStatusFeedTimestampByPlayer.remove(playerId);
        lastTutorialProgressFeedLineByPlayer.remove(playerId);
        lastTutorialStepFeedLineByPlayer.remove(playerId);
        lastTutorialFocusNotificationTimestampByPlayer.remove(playerId);
        lastTutorialMergePointsReminderTimestampByPlayer.remove(playerId);
    }

    private static void resetTutorialProgress(UUID playerId) {
        if (playerId == null) {
            return;
        }
        savedTutorialSheepByPlayer.remove(playerId);
        tutorialCompletedByPlayer.remove(playerId);
        tutorialBypassedByPlayer.remove(playerId);
        tutorialShearsByPlayer.remove(playerId);
        tutorialSpawnsByPlayer.remove(playerId);
        tutorialMergesByPlayer.remove(playerId);
        tutorialUpgradeOpenedByPlayer.remove(playerId);
        tutorialQuestOpenedByPlayer.remove(playerId);
        tutorialQuestUpgradesOpenedByPlayer.remove(playerId);
        tutorialPrestigeOpenedByPlayer.remove(playerId);
        tutorialAbilityUsedByPlayer.remove(playerId);
        tutorialShearUpgradedByPlayer.remove(playerId);
        tutorialRegularUpgradesBoughtByPlayer.remove(playerId);
        tutorialPrestigedOnceByPlayer.remove(playerId);
        tutorialShearShopOpenedByPlayer.remove(playerId);
        clearTutorialRuntimeState(playerId);
    }

    public static int getPrestigeLevel(Player player) {
        return player == null ? 0 : prestigeLevelByPlayer.getOrDefault(player.getUniqueId(), 0);
    }

    public static int getPrestigePoints(Player player) {
        return player == null ? 0 : prestigePointsByPlayer.getOrDefault(player.getUniqueId(), 0);
    }

    public static int getPrestigeMaxLevel() {
        return PRESTIGE_MAX_LEVEL;
    }

    public static String formatPoints(long points) {
        return formatPoints(BigInteger.valueOf(points));
    }

    public static String formatPoints(BigInteger points) {
        BigInteger safe = points == null ? BigInteger.ZERO : points;
        boolean negative = safe.signum() < 0;
        BigInteger value = negative ? safe.negate() : safe;
        String[] suffixes = { "", "K", "M", "B", "T", "Q", "Qi", "Sx", "Sp", "Oc", "No", "Dc" };
        int suffixIndex = 0;
        while (value.compareTo(BigInteger.valueOf(10_000L)) >= 0 && suffixIndex < suffixes.length - 1) {
            value = value.divide(BigInteger.valueOf(1000L));
            suffixIndex++;
        }
        return (negative ? "-" : "") + value + suffixes[suffixIndex];
    }

    public static int getQuestPoints(Player player) {
        return player == null ? 0 : questPointsByPlayer.getOrDefault(player.getUniqueId(), 10);
    }

    private static void addQuestPoints(Player player, int amount) {
        if (player == null || amount <= 0) {
            return;
        }
        UUID playerId = player.getUniqueId();
        questPointsByPlayer.put(playerId, addSaturated(getQuestPoints(player), amount));
        saveData();
    }

    private static boolean trySpendQuestPoints(Player player, int amount) {
        if (player == null || amount <= 0) {
            return false;
        }
        int current = getQuestPoints(player);
        if (current < amount) {
            return false;
        }
        questPointsByPlayer.put(player.getUniqueId(), current - amount);
        saveData();
        return true;
    }

    private static long getQuestResetIntervalMs(Player player) {
        int prestige = getPrestigeLevel(player);
        long interval = BASE_QUEST_RESET_MS - (prestige * QUEST_RESET_REDUCTION_PER_PRESTIGE_MS);
        return Math.max(MIN_QUEST_RESET_MS, interval);
    }

    public static void tickQuestSystem(Player player) {
        if (player == null || !isSheepFarmWorld(player.getWorld())) {
            return;
        }
        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();
        long nextReset = nextQuestResetTimestampByPlayer.getOrDefault(playerId, 0L);
        if (nextReset <= 0L) {
            nextQuestResetTimestampByPlayer.put(playerId, now + getQuestResetIntervalMs(player));
            return;
        }
        if (now < nextReset) {
            return;
        }

        questShearsByPlayer.put(playerId, 0);
        questSpawnsByPlayer.put(playerId, 0);
        questMergesByPlayer.put(playerId, 0);
        questShearsCompleteByPlayer.put(playerId, false);
        questSpawnsCompleteByPlayer.put(playerId, false);
        questMergesCompleteByPlayer.put(playerId, false);
        nextQuestResetTimestampByPlayer.put(playerId, now + getQuestResetIntervalMs(player));
        player.sendTitle(color("&eNew quests"), color("&7Quest board refreshed"), 10, 40, 10);
    }

    public static void tickTutorialReminder(Player player) {
        if (player == null) {
            return;
        }

        UUID playerId = player.getUniqueId();
        if (!isTutorialInProgress(player) || !isInTutorialWorld(player)) {
            clearTutorialRuntimeState(playerId);
            return;
        }

        markTutorialRegularUpgradesIfComplete(player);
        if (getShearShopLevel(player) > 0) {
            markTutorialShearUpgraded(player);
        }
        if (getPrestigeLevel(player) > 0) {
            markTutorialPrestigedOnce(player);
        }

        long now = System.currentTimeMillis();
        tickTutorialTaskTitle(player, now);
        maybeSendTutorialMergePointsReminder(player, now);
        tutorialStartedAtByPlayer.putIfAbsent(playerId, now);
        long startedAt = tutorialStartedAtByPlayer.getOrDefault(playerId, now);
        if (now - startedAt < TUTORIAL_REMINDER_DELAY_MS) {
            return;
        }

        long lastReminder = lastTutorialReminderTimestampByPlayer.getOrDefault(playerId, 0L);
        if (now - lastReminder < TUTORIAL_REMINDER_REPEAT_MS) {
            return;
        }

        lastTutorialReminderTimestampByPlayer.put(playerId, now);
        player.sendMessage(warning("Finish the tutorial to unlock your farm."));
        player.sendMessage(hint("Next: " + getTutorialNextStepLine(player)));
    }

    private static void tickTutorialTaskTitle(Player player, long now) {
        if (player == null) {
            return;
        }
        UUID playerId = player.getUniqueId();
        String titleStep = getTutorialNextStepLine(player);

        String previousStep = lastTutorialTaskTitleStepByPlayer.get(playerId);
        long lastShownAt = lastTutorialTaskTitleTimestampByPlayer.getOrDefault(playerId, 0L);
        boolean stepChanged = !titleStep.equals(previousStep);
        if (!stepChanged && now - lastShownAt < TUTORIAL_TASK_TITLE_REPEAT_MS) {
            return;
        }

        sendTutorialTitle(player, "&eTutorial Step", "&f" + titleStep);
        lastTutorialTaskTitleTimestampByPlayer.put(playerId, now);
        lastTutorialTaskTitleStepByPlayer.put(playerId, titleStep);
    }

    private static void sendTutorialTitle(Player player, String title, String subtitle) {
        if (player == null) {
            return;
        }
        int stayTicks = getReadableTutorialTitleStayTicks(subtitle);
        player.sendTitle(color(title), color(subtitle), 0, stayTicks, 10);
    }

    private static int getReadableTutorialTitleStayTicks(String subtitle) {
        String plain = ChatColor.stripColor(color(subtitle));
        int length = plain == null ? 0 : plain.trim().length();
        int minStayTicks = 35;
        int maxStayTicks = 120;
        int extraTicks = (int) Math.ceil(length * 1.5D);
        return Math.max(minStayTicks, Math.min(maxStayTicks, minStayTicks + extraTicks));
    }

    public static void recordQuestShear(Player player) {
        updateQuestProgress(player, questShearsByPlayer, questShearsCompleteByPlayer, QUEST_SHEARS_TARGET,
                QUEST_SHEARS_REWARD, "Shearing quest complete", Sound.ENTITY_PLAYER_LEVELUP);
    }

    public static void recordQuestSpawn(Player player) {
        updateQuestProgress(player, questSpawnsByPlayer, questSpawnsCompleteByPlayer, QUEST_SPAWNS_TARGET,
                QUEST_SPAWNS_REWARD, "Spawning quest complete", Sound.ENTITY_PLAYER_LEVELUP);
    }

    public static void recordQuestMerge(Player player) {
        updateQuestProgress(player, questMergesByPlayer, questMergesCompleteByPlayer, QUEST_MERGES_TARGET,
                QUEST_MERGES_REWARD, "Merging quest complete", Sound.ENTITY_PLAYER_LEVELUP);
    }

    private static void updateQuestProgress(Player player, Map<UUID, Integer> progress,
            Map<UUID, Boolean> completed, int target, int reward, String completionText, Sound rewardSound) {
        if (player == null || !isSheepFarmWorld(player.getWorld())) {
            return;
        }
        UUID playerId = player.getUniqueId();
        if (completed.getOrDefault(playerId, false)) {
            return;
        }
        int value = progress.getOrDefault(playerId, 0) + 1;
        progress.put(playerId, value);
        if (value < target) {
            return;
        }
        completed.put(playerId, true);
        int boostedReward = Math.max(1, (int) Math.round(reward * getPrestigeQuestRewardMultiplier(player)));
        addQuestPoints(player, boostedReward);
        player.sendMessage(action(completionText + ": +" + formatPoints(boostedReward) + " quest points"));
        playSound(player, rewardSound, 1.0f, 1.1f);
        player.getWorld().spawnParticle(org.bukkit.Particle.VILLAGER_HAPPY,
                player.getLocation().add(0, 1.0, 0), 14, 0.35, 0.4, 0.35, 0.02);
        if (areAllQuestsCompleted(playerId)) {
            player.sendTitle(color("&aAll Quests Complete"), color("&7Nice cycle. New quests on reset."), 10, 45, 10);
            playSound(player, Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.9f, 1.2f);
            player.sendMessage(action("All current quests are completed."));
        }
    }

    private static boolean areAllQuestsCompleted(UUID playerId) {
        if (playerId == null) {
            return false;
        }
        return questShearsCompleteByPlayer.getOrDefault(playerId, false)
                && questSpawnsCompleteByPlayer.getOrDefault(playerId, false)
                && questMergesCompleteByPlayer.getOrDefault(playerId, false);
    }

    public static void tickActiveAbilities(Player player) {
        if (player == null || !isSheepFarmWorld(player.getWorld())) {
            return;
        }
        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();

        tickAbilityVisual(player, playerId, now, activeWoolRushUntilByPlayer, org.bukkit.Particle.CLOUD,
                "Wool Rush ended");
        tickAbilityVisual(player, playerId, now, activeJackpotShearsUntilByPlayer, org.bukkit.Particle.CRIT,
                "Jackpot Shears ended");
        emitAbilityAura(player, playerId, now);
        tickAutoMergeAbility(player, playerId, now);
        tickAutoShearAbility(player, playerId, now);
        tickAutomationSystems(player, playerId, now);
        updatePointsScoreboard(player);
    }

    private static void tickAutomationSystems(Player player, UUID playerId, long now) {
        tickAutomationAutoBuy(player, playerId, now);
        tickAutomationAutoAbility(player, playerId, now);
        tickAutomationSlowMerge(player, playerId, now);
        tickAutomationSlowShear(player, playerId, now);
        tickAutomationAutoSpawn(player, playerId, now);
        tickAutomationAutoPrestige(player, playerId, now);
    }

    public static void tickAutomationPlaytimePoints(Player player) {
        if (player == null) {
            return;
        }
        tickAutomationPointGain(player, player.getUniqueId(), System.currentTimeMillis());
    }

    private static void tickAutomationPointGain(Player player, UUID playerId, long now) {
        if (AUTOMATION_POINT_INTERVAL_MS <= 0L) {
            return;
        }
        long nextAt = nextAutomationPointAtByPlayer.getOrDefault(playerId, 0L);
        if (nextAt <= 0L) {
            nextAutomationPointAtByPlayer.put(playerId, now + AUTOMATION_POINT_INTERVAL_MS);
            return;
        }
        if (now < nextAt) {
            return;
        }
        automationPointsByPlayer.put(playerId, addSaturated(getAutomationPoints(player), 1));
        nextAutomationPointAtByPlayer.put(playerId, now + AUTOMATION_POINT_INTERVAL_MS);
        saveData();
    }

    private static void tickAutomationAutoBuy(Player player, UUID playerId, long now) {
        long interval = getAutomationAutoBuyIntervalMs(player);
        if (getAutomationAutoBuyUpgradeLevel(player) <= 0 || interval <= 0L
                || !isAutomationAutoBuyEnabled(player)) {
            return;
        }
        long nextAt = nextAutomationAutoBuyAtByPlayer.getOrDefault(playerId, 0L);
        if (now < nextAt) {
            return;
        }
        nextAutomationAutoBuyAtByPlayer.put(playerId, now + interval);
        if (!canAutomationRun(player, false)) {
            return;
        }
        tryAutoBuyOneUpgrade(player);
    }

    private static void tickAutomationAutoAbility(Player player, UUID playerId, long now) {
        if (getAutomationAutoAbilityUpgradeLevel(player) <= 0 || AUTOMATION_AUTO_ABILITY_INTERVAL_MS <= 0L
                || !isAutomationAutoAbilityEnabled(player)) {
            return;
        }
        long nextAt = nextAutomationAutoAbilityAtByPlayer.getOrDefault(playerId, 0L);
        if (now < nextAt) {
            return;
        }
        nextAutomationAutoAbilityAtByPlayer.put(playerId, now + AUTOMATION_AUTO_ABILITY_INTERVAL_MS);
        if (!canAutomationRun(player, true)) {
            return;
        }
        tryAutoActivateAbility(player);
    }

    private static void tickAutomationSlowMerge(Player player, UUID playerId, long now) {
        long interval = getAutomationSlowAutoMergeIntervalMs(player);
        if (getAutomationSlowAutoMergeUpgradeLevel(player) <= 0 || interval <= 0L
                || !isAutomationSlowAutoMergeEnabled(player)) {
            return;
        }
        long nextAt = nextAutomationSlowMergeAtByPlayer.getOrDefault(playerId, 0L);
        if (now < nextAt) {
            return;
        }
        nextAutomationSlowMergeAtByPlayer.put(playerId, now + interval);
        if (!canAutomationRun(player, false) || !hasMergeCandidates(player)) {
            return;
        }
        tryAutoMergeOnce(player);
    }

    private static void tickAutomationSlowShear(Player player, UUID playerId, long now) {
        long interval = getAutomationSlowAutoShearIntervalMs(player);
        if (getAutomationSlowAutoShearUpgradeLevel(player) <= 0 || interval <= 0L
                || !isAutomationSlowAutoShearEnabled(player)) {
            return;
        }
        long nextAt = nextAutomationSlowShearAtByPlayer.getOrDefault(playerId, 0L);
        if (now < nextAt) {
            return;
        }
        nextAutomationSlowShearAtByPlayer.put(playerId, now + interval);
        if (!canAutomationRun(player, false)
                || countReadySheep(player) < AUTOMATION_CONDITION_MIN_READY_SHEEP_FOR_SHEAR) {
            return;
        }
        tryAutoShearOnce(player);
    }

    private static void tickAutomationAutoSpawn(Player player, UUID playerId, long now) {
        if (getAutomationAutoSpawnUpgradeLevel(player) <= 0 || !isAutomationAutoSpawnEnabled(player)) {
            return;
        }
        long interval = getAutomationAutoSpawnIntervalMs(player);
        long nextAt = nextAutomationAutoSpawnAtByPlayer.getOrDefault(playerId, 0L);
        if (interval > 0L && now < nextAt) {
            return;
        }
        if (interval > 0L) {
            nextAutomationAutoSpawnAtByPlayer.put(playerId, now + interval);
        } else {
            nextAutomationAutoSpawnAtByPlayer.put(playerId, now);
        }
        if (!canAutomationRun(player, false)) {
            return;
        }
        if (player.getWorld() == null || !isSheepFarmWorld(player.getWorld())
                || !isFarmOwner(player, player.getWorld())) {
            return;
        }
        if (isWorldAtLimit(player.getWorld())) {
            return;
        }
        Location spawnLocation = player.getLocation().clone();
        if (spawnLocation.getY() < FARM_BASE_Y + 1.0D) {
            spawnLocation.setY(FARM_BASE_Y + 1.0D);
        }
        if (spawnSheepFromEgg(player, spawnLocation)) {
            recordQuestSpawn(player);
            recordTutorialSpawn(player);
        }
    }

    private static void tickAutomationAutoPrestige(Player player, UUID playerId, long now) {
        if (getAutomationAutoPrestigeUpgradeLevel(player) <= 0 || !isAutomationAutoPrestigeEnabled(player)
                || AUTOMATION_AUTO_PRESTIGE_INTERVAL_MS <= 0L) {
            return;
        }
        long nextAt = nextAutomationAutoPrestigeAtByPlayer.getOrDefault(playerId, 0L);
        if (now < nextAt) {
            return;
        }
        nextAutomationAutoPrestigeAtByPlayer.put(playerId, now + AUTOMATION_AUTO_PRESTIGE_INTERVAL_MS);
        if (!canAutomationRun(player, false)) {
            return;
        }
        if (player.getWorld() == null || !isSheepFarmWorld(player.getWorld())
                || !isFarmOwner(player, player.getWorld())) {
            return;
        }
        prestige(player);
    }

    private static boolean canAutomationRun(Player player, boolean requiresQuestPoints) {
        if (player == null || !isSheepFarmWorld(player.getWorld()) || !isFarmOwner(player, player.getWorld())) {
            return false;
        }
        if (getPlayerPointsBig(player).compareTo(BigInteger.valueOf(AUTOMATION_CONDITION_MIN_POINTS_RESERVE)) < 0) {
            return false;
        }
        return !requiresQuestPoints || getQuestPoints(player) >= AUTOMATION_CONDITION_MIN_QUEST_POINTS;
    }

    private static boolean hasMergeCandidates(Player player) {
        if (player == null || player.getWorld() == null) {
            return false;
        }
        Map<String, Integer> byTier = new HashMap<>();
        for (Sheep sheep : player.getWorld().getEntitiesByClass(Sheep.class)) {
            if (sheep == null || !sheep.isValid() || sheep.isDead() || sheep.isInsideVehicle()) {
                continue;
            }
            SheepTier tier = getSheepTier(sheep);
            if (tier == null) {
                continue;
            }
            String key = tier == SheepTier.RAINBOW
                    ? (tier.getLevel() + ":" + getRainbowTier(sheep))
                    : String.valueOf(tier.getLevel());
            int count = byTier.getOrDefault(key, 0) + 1;
            if (count >= AUTOMATION_CONDITION_MIN_SHEEP_FOR_MERGE) {
                return true;
            }
            byTier.put(key, count);
        }
        return false;
    }

    private static int countReadySheep(Player player) {
        if (player == null || player.getWorld() == null) {
            return 0;
        }
        int ready = 0;
        long now = System.currentTimeMillis();
        for (Sheep sheep : player.getWorld().getEntitiesByClass(Sheep.class)) {
            if (sheep == null || !sheep.isValid() || sheep.isDead() || sheep.isSheared() || !sheep.isAdult()) {
                continue;
            }
            if (getNextEatTimestamp(sheep) <= now) {
                ready++;
            }
        }
        return ready;
    }

    private static boolean tryAutoBuyOneUpgrade(Player player) {
        if (player == null) {
            return false;
        }
        if (getPlayerLimit(player) < MAX_SHEEP_LIMIT && upgradeLimit(player)) {
            return true;
        }
        if (getEggSpeedLevel(player) < getEggSpeedMaxLevel(player) && upgradeEggSpeed(player)) {
            return true;
        }
        if (getWoolRegenLevel(player) < getWoolRegenMaxLevel(player) && upgradeWoolRegen(player)) {
            return true;
        }
        if (getHigherTierChanceLevel(player) < getHigherTierChanceMaxLevel(player) && upgradeHigherTierChance(player)) {
            return true;
        }
        if (getComboDecayUpgradeLevel(player) < COMBO_DECAY_MAX_LEVEL && upgradeComboDecay(player)) {
            return true;
        }
        if (getComboGainUpgradeLevel(player) < COMBO_GAIN_MAX_LEVEL && upgradeComboGain(player)) {
            return true;
        }
        if (getShearWoolSaveLevel(player) < SHEAR_WOOL_SAVE_MAX_LEVEL && upgradeShearWoolSave(player)) {
            return true;
        }
        if (getShearTierBoostLevel(player) < SHEAR_TIER_BOOST_MAX_LEVEL && upgradeShearTierBoost(player)) {
            return true;
        }
        return upgradeShearShop(player);
    }

    private static boolean tryAutoActivateAbility(Player player) {
        if (player == null) {
            return false;
        }
        UUID playerId = player.getUniqueId();
        if (getCountAbilityRemainingUses(activeAutoShearUsesByPlayer, playerId) <= 0
                && activateCountQuestAbility(player,
                        activeAutoShearUsesByPlayer,
                        autoShearEnabledByPlayer,
                        getQuestAutoShearCost(player),
                        getAbilityUseCount(player, QUEST_AUTO_SHEAR_BASE_DURATION_MS),
                        Sound.ENTITY_SHEEP_SHEAR,
                        org.bukkit.Particle.WAX_OFF)) {
            nextAutoShearAtByPlayer.put(playerId, 0L);
            return true;
        }
        if (getCountAbilityRemainingUses(activeAutoMergeUsesByPlayer, playerId) <= 0
                && activateCountQuestAbility(player,
                        activeAutoMergeUsesByPlayer,
                        autoMergeEnabledByPlayer,
                        getQuestAutoMergeCost(player),
                        getAbilityUseCount(player, QUEST_AUTO_MERGE_BASE_DURATION_MS),
                        Sound.BLOCK_PISTON_EXTEND,
                        org.bukkit.Particle.ENCHANTMENT_TABLE)) {
            nextAutoMergeAtByPlayer.put(playerId, 0L);
            return true;
        }
        if (!isAbilityActive(activeJackpotShearsUntilByPlayer, playerId)
                && activateQuestAbility(player, activeJackpotShearsUntilByPlayer, getQuestJackpotCost(player),
                        getAbilityDurationMs(player, QUEST_JACKPOT_SHEARS_BASE_DURATION_MS),
                        Sound.ENTITY_PLAYER_LEVELUP, org.bukkit.Particle.CRIT)) {
            return true;
        }
        if (!isAbilityActive(activeWoolRushUntilByPlayer, playerId)
                && activateQuestAbility(player, activeWoolRushUntilByPlayer, getQuestWoolRushCost(player),
                        getAbilityDurationMs(player, QUEST_WOOL_RUSH_BASE_DURATION_MS),
                        Sound.ENTITY_ENDER_DRAGON_FLAP, org.bukkit.Particle.CLOUD)) {
            return true;
        }
        if (getCountAbilityRemainingUses(activeLuckyBurstUsesByPlayer, playerId) <= 0
                && activateCountQuestAbility(player,
                        activeLuckyBurstUsesByPlayer,
                        luckyBurstEnabledByPlayer,
                        getQuestLuckyBurstCost(player),
                        getAbilityUseCount(player, QUEST_LUCKY_BURST_BASE_DURATION_MS),
                        Sound.BLOCK_BEACON_POWER_SELECT,
                        org.bukkit.Particle.END_ROD)) {
            return true;
        }
        return false;
    }

    public static void tickRandomFarmEvents() {
        if (plugin == null) {
            return;
        }

        long now = System.currentTimeMillis();
        if (sheepRainEventEndsAtMs > now) {
            tickSheepRainEvent(now);
        } else if (sheepRainEventEndsAtMs > 0L) {
            endSheepRainEvent();
        }

        if (comboFrenzyEventEndsAtMs > now) {
            // Frenzy countdown is rendered through each player's combo boss bar.
        } else if (comboFrenzyEventEndsAtMs > 0L) {
            endComboFrenzyEvent();
        }

        if (nextRandomEventRollAtMs <= 0L) {
            nextRandomEventRollAtMs = now + RANDOM_EVENT_ROLL_INTERVAL_MS;
            return;
        }
        if (now < nextRandomEventRollAtMs) {
            return;
        }

        nextRandomEventRollAtMs = now + RANDOM_EVENT_ROLL_INTERVAL_MS;
        if (RANDOM.nextInt(RANDOM_EVENT_TRIGGER_CHANCE_DENOMINATOR) == 0) {
            startSheepRainEvent(now);
        }
        if (RANDOM.nextInt(RANDOM_EVENT_TRIGGER_CHANCE_DENOMINATOR) == 0) {
            startComboFrenzyEvent(now);
        }
    }

    public static boolean triggerSheepStormEvent() {
        if (plugin == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (sheepRainEventEndsAtMs > now) {
            return false;
        }
        nextRandomEventRollAtMs = now + RANDOM_EVENT_ROLL_INTERVAL_MS;
        startSheepRainEvent(now);
        return true;
    }

    public static boolean isSheepStormActive() {
        return sheepRainEventEndsAtMs > System.currentTimeMillis();
    }

    public static boolean triggerComboFrenzyEvent() {
        if (plugin == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (comboFrenzyEventEndsAtMs > now) {
            return false;
        }
        nextRandomEventRollAtMs = now + RANDOM_EVENT_ROLL_INTERVAL_MS;
        startComboFrenzyEvent(now);
        return true;
    }

    private static void startComboFrenzyEvent(long now) {
        comboFrenzyEventEndsAtMs = now + COMBO_FRENZY_EVENT_DURATION_MS;
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            if (!isSheepFarmWorld(online.getWorld())) {
                continue;
            }
            online.sendMessage(action("Random Event: Combo Frenzy started (10x combo gain)."));
            playSound(online, Sound.ENTITY_PLAYER_LEVELUP, 0.9f, 1.6f);
        }
    }

    private static void endComboFrenzyEvent() {
        comboFrenzyEventEndsAtMs = 0L;
        if (plugin == null) {
            return;
        }
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            if (!isSheepFarmWorld(online.getWorld())) {
                continue;
            }
            online.sendMessage(hint("Random Event: Combo Frenzy ended."));
            playSound(online, Sound.BLOCK_BEACON_DEACTIVATE, 0.7f, 1.25f);
        }
    }

    public static void broadcastRandomGameplayTip() {
        if (plugin == null || plugin.getServer() == null || plugin.getServer().getOnlinePlayers().isEmpty()) {
            return;
        }

        String tip = getNextGameplayTip();
        String message = color("&8[&6SheepMerge Tip&8] &f" + tip);
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (!isSheepFarmWorld(player.getWorld())) {
                continue;
            }
            player.sendMessage(message);
        }
    }

    private static String getNextGameplayTip() {
        if (GAMEPLAY_TIPS.isEmpty()) {
            return "&7Keep merging sheep and upgrading your farm.";
        }
        if (GAMEPLAY_TIPS.size() == 1) {
            return GAMEPLAY_TIPS.get(0);
        }

        int nextIndex = RANDOM.nextInt(GAMEPLAY_TIPS.size());
        while (nextIndex == lastGameplayTipIndex) {
            nextIndex = RANDOM.nextInt(GAMEPLAY_TIPS.size());
        }
        lastGameplayTipIndex = nextIndex;
        return GAMEPLAY_TIPS.get(nextIndex);
    }

    private static void startSheepRainEvent(long now) {
        sheepRainEventEndsAtMs = now + SHEEP_RAIN_EVENT_DURATION_MS;
        nextSheepRainSpawnAtMs = now;

        if (sheepRainBossBar == null) {
            sheepRainBossBar = Bukkit.createBossBar("Sheep Storm", BarColor.WHITE, BarStyle.SEGMENTED_10);
        }
        sheepRainBossBar.setVisible(true);

        for (Player online : plugin.getServer().getOnlinePlayers()) {
            if (!isSheepFarmWorld(online.getWorld())) {
                sheepRainBossBar.removePlayer(online);
                continue;
            }
            sheepRainBossBar.addPlayer(online);
            online.sendMessage(action("Random Event: Sheep Storm started."));
            playSound(online, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5f, 1.6f);
        }
        for (World world : plugin.getServer().getWorlds()) {
            if (!isSheepFarmWorld(world)) {
                continue;
            }
            world.setStorm(true);
            world.setThundering(true);
            world.setWeatherDuration((int) (SHEEP_RAIN_EVENT_DURATION_MS / 50L) + 40);
            world.setThunderDuration((int) (SHEEP_RAIN_EVENT_DURATION_MS / 50L) + 40);
        }
        updateSheepRainBossBar(now);
    }

    private static void endSheepRainEvent() {
        sheepRainEventEndsAtMs = 0L;
        nextSheepRainSpawnAtMs = 0L;
        if (sheepRainBossBar != null) {
            sheepRainBossBar.removeAll();
            sheepRainBossBar.setVisible(false);
        }
        if (plugin == null) {
            return;
        }
        for (World world : plugin.getServer().getWorlds()) {
            if (!isSheepFarmWorld(world)) {
                continue;
            }
            world.setThundering(false);
            world.setStorm(false);
            world.setWeatherDuration(0);
            world.setThunderDuration(0);
        }
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            if (!isSheepFarmWorld(online.getWorld())) {
                continue;
            }
            online.sendMessage(hint("Random Event: Sheep Storm ended."));
            playSound(online, Sound.BLOCK_BEACON_DEACTIVATE, 0.7f, 1.2f);
        }
    }

    private static void tickSheepRainEvent(long now) {
        updateSheepRainBossBar(now);
        if (now < nextSheepRainSpawnAtMs) {
            return;
        }

        for (World world : plugin.getServer().getWorlds()) {
            if (!isSheepFarmWorld(world) || isWorldAtLimit(world)) {
                continue;
            }
            spawnRainSheep(world);
        }
        nextSheepRainSpawnAtMs = now + getRandomSheepRainIntervalMs();
    }

    private static void updateSheepRainBossBar(long now) {
        if (sheepRainBossBar == null) {
            return;
        }

        long remaining = Math.max(0L, sheepRainEventEndsAtMs - now);
        double progress = Math.max(0.0D, Math.min(1.0D, remaining / (double) SHEEP_RAIN_EVENT_DURATION_MS));
        sheepRainBossBar.setProgress(progress);
        sheepRainBossBar.setTitle(color("&fSheep Storm &7- &e" + formatDuration(remaining) + " left"));

        if (plugin == null) {
            return;
        }
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            if (isSheepFarmWorld(online.getWorld())) {
                sheepRainBossBar.addPlayer(online);
            } else {
                sheepRainBossBar.removePlayer(online);
            }
        }
    }

    private static long getRandomSheepRainIntervalMs() {
        long range = SHEEP_RAIN_MAX_INTERVAL_MS - SHEEP_RAIN_MIN_INTERVAL_MS;
        if (range <= 0L) {
            return SHEEP_RAIN_MIN_INTERVAL_MS;
        }
        return SHEEP_RAIN_MIN_INTERVAL_MS + RANDOM.nextInt((int) range + 1);
    }

    private static void spawnRainSheep(World world) {
        double min = FARM_MIN_XZ + SHEEP_RAIN_HORIZONTAL_PADDING;
        double max = FARM_MAX_XZ - SHEEP_RAIN_HORIZONTAL_PADDING;
        double x = min + RANDOM.nextDouble() * Math.max(0.01D, max - min);
        double z = min + RANDOM.nextDouble() * Math.max(0.01D, max - min);
        double y = FARM_BASE_Y + SHEEP_RAIN_SPAWN_HEIGHT;
        Location spawnLocation = new Location(world, x, y, z);

        Sheep sheep = world.spawn(spawnLocation, Sheep.class);
        setSheepTier(sheep, rollSpawnTier(world));
        sheep.setVelocity(new Vector(0.0D, -0.1D, 0.0D));

        world.spawnParticle(org.bukkit.Particle.CLOUD,
                spawnLocation.clone().add(0.0D, 0.4D, 0.0D),
                14,
                0.2D,
                0.4D,
                0.2D,
                0.02D);
        world.playSound(spawnLocation, Sound.ENTITY_SHEEP_AMBIENT, 0.7f, 1.5f);
    }

    private static void tickAutoMergeAbility(Player player, UUID playerId, long now) {
        if (!isCountAbilityActive(activeAutoMergeUsesByPlayer, autoMergeEnabledByPlayer, playerId)) {
            nextAutoMergeAtByPlayer.remove(playerId);
        }
    }

    private static void tickAutoShearAbility(Player player, UUID playerId, long now) {
        if (!isCountAbilityActive(activeAutoShearUsesByPlayer, autoShearEnabledByPlayer, playerId)) {
            nextAutoShearAtByPlayer.remove(playerId);
            return;
        }

        long nextAutoShearAt = nextAutoShearAtByPlayer.getOrDefault(playerId, 0L);
        if (now < nextAutoShearAt) {
            return;
        }

        nextAutoShearAtByPlayer.put(playerId, now + 100L);
        if (tryAutoShearLookTarget(player)) {
            consumeCountAbilityUse(activeAutoShearUsesByPlayer, playerId);
        }
    }

    private static boolean tryAutoShearLookTarget(Player player) {
        if (player == null || player.getWorld() == null || !isSheepFarmWorld(player.getWorld())) {
            return false;
        }
        org.bukkit.util.RayTraceResult result = player.getWorld().rayTraceEntities(
                player.getEyeLocation(),
                player.getLocation().getDirection(),
                6.0D,
                0.22D,
                entity -> entity instanceof Sheep);
        if (result == null || !(result.getHitEntity() instanceof Sheep target)) {
            return false;
        }
        return shearSheepForPlayer(player, target);
    }

    private static boolean tryAutoMergeOnce(Player player) {
        if (player == null || player.getWorld() == null || !isSheepFarmWorld(player.getWorld())) {
            return false;
        }
        if (!isFarmOwner(player, player.getWorld())) {
            return false;
        }

        World world = player.getWorld();
        Map<String, Sheep> firstByTier = new HashMap<>();
        for (Sheep sheep : world.getEntitiesByClass(Sheep.class)) {
            if (sheep == null || !sheep.isValid() || sheep.isDead()) {
                continue;
            }
            if (sheep.isInsideVehicle()) {
                continue;
            }

            SheepTier tier = getSheepTier(sheep);
            if (tier == null) {
                continue;
            }

            int rainbowTier = tier == SheepTier.RAINBOW ? getRainbowTier(sheep) : 0;
            String mergeKey = tier == SheepTier.RAINBOW ? (tier.getLevel() + ":" + rainbowTier)
                    : String.valueOf(tier.getLevel());
            Sheep first = firstByTier.putIfAbsent(mergeKey, sheep);
            if (first == null || !first.isValid() || first.getUniqueId().equals(sheep.getUniqueId())) {
                continue;
            }
            return mergeSheepPair(player, first, sheep, false);
        }
        return false;
    }

    public static boolean tryAutoMergeOnPickup(Player player, Sheep pickedSheep) {
        if (player == null || pickedSheep == null || !pickedSheep.isValid()) {
            return false;
        }
        UUID playerId = player.getUniqueId();
        if (!isCountAbilityActive(activeAutoMergeUsesByPlayer, autoMergeEnabledByPlayer, playerId)) {
            return false;
        }
        if (!isFarmOwner(player, pickedSheep.getWorld())) {
            return false;
        }

        SheepTier tier = getSheepTier(pickedSheep);
        if (tier == null) {
            return false;
        }
        if (countMatchingMergeCandidates(pickedSheep.getWorld(), pickedSheep, tier) < 2) {
            return false;
        }
        Sheep mergePartner = findMatchingMergePartner(pickedSheep.getWorld(), pickedSheep, tier);
        if (mergePartner == null) {
            return false;
        }
        if (!mergeSheepPair(player, pickedSheep, mergePartner, false)) {
            return false;
        }
        consumeCountAbilityUse(activeAutoMergeUsesByPlayer, playerId);
        return true;
    }

    private static Sheep findMatchingMergePartner(World world, Sheep source, SheepTier tier) {
        if (world == null || source == null || tier == null) {
            return null;
        }
        int sourceRainbowTier = tier == SheepTier.RAINBOW ? getRainbowTier(source) : 0;
        for (Sheep candidate : world.getEntitiesByClass(Sheep.class)) {
            if (candidate == null || !candidate.isValid() || candidate.isDead()) {
                continue;
            }
            if (candidate.getUniqueId().equals(source.getUniqueId())) {
                continue;
            }
            SheepTier candidateTier = getSheepTier(candidate);
            if (candidateTier != tier) {
                continue;
            }
            if (tier == SheepTier.RAINBOW && getRainbowTier(candidate) != sourceRainbowTier) {
                continue;
            }
            return candidate;
        }
        return null;
    }

    private static int countMatchingMergeCandidates(World world, Sheep source, SheepTier tier) {
        if (world == null || source == null || tier == null) {
            return 0;
        }
        int sourceRainbowTier = tier == SheepTier.RAINBOW ? getRainbowTier(source) : 0;
        int count = 0;
        for (Sheep candidate : world.getEntitiesByClass(Sheep.class)) {
            if (candidate == null || !candidate.isValid() || candidate.isDead()) {
                continue;
            }
            SheepTier candidateTier = getSheepTier(candidate);
            if (candidateTier != tier) {
                continue;
            }
            if (tier == SheepTier.RAINBOW && getRainbowTier(candidate) != sourceRainbowTier) {
                continue;
            }
            count++;
            if (count >= 2) {
                return count;
            }
        }
        return count;
    }

    private static boolean mergeSheepPair(Player player, Sheep first, Sheep second, boolean recordTutorial) {
        if (player == null || first == null || second == null || !first.isValid() || !second.isValid()) {
            return false;
        }
        SheepTier tier = getSheepTier(first);
        SheepTier otherTier = getSheepTier(second);
        if (tier == null || otherTier != tier) {
            return false;
        }

        int sourceRainbowTier = tier == SheepTier.RAINBOW ? getRainbowTier(first) : 0;
        int otherRainbowTier = tier == SheepTier.RAINBOW ? getRainbowTier(second) : 0;
        if (tier == SheepTier.RAINBOW && sourceRainbowTier != otherRainbowTier) {
            return false;
        }

        World world = first.getWorld();
        if (world == null || !world.equals(second.getWorld())) {
            return false;
        }

        SheepTier mergedTier = tier.hasNext() ? tier.next() : SheepTier.RAINBOW;
        int woolReadyCount = (!first.isSheared() ? 1 : 0) + (!second.isSheared() ? 1 : 0);
        long combinedWoolRegenMs = getCombinedRemainingWoolRegenMs(first, second);
        Location spawnLocation = second.getLocation().clone();

        first.remove();
        second.remove();

        Sheep mergedSheep = world.spawn(spawnLocation, Sheep.class);
        setSheepTier(mergedSheep, mergedTier);
        if (mergedTier == SheepTier.RAINBOW && !tier.hasNext()) {
            setRainbowTier(mergedSheep, sourceRainbowTier + 1);
        }
        initializeMergedSheepAfterMerge(mergedSheep, mergedTier, combinedWoolRegenMs);
        mergedSheep.setVelocity(new Vector(0.0D, 0.18D, 0.0D));
        world.spawnParticle(org.bukkit.Particle.VILLAGER_HAPPY,
                spawnLocation.clone().add(0.0D, 0.5D, 0.0D),
                10,
                0.25D,
                0.25D,
                0.25D,
                0.02D);

        int mergedRainbowTier = mergedTier == SheepTier.RAINBOW ? getRainbowTier(mergedSheep) : 0;
        if (shouldAnnounceTierUnlock(player, mergedTier, mergedRainbowTier)) {
            announceTierUnlock(player, mergedTier, mergedRainbowTier);
            markTierUnlockAnnounced(player, mergedTier, mergedRainbowTier);
        }
        recordSheepMerge(player, tier, woolReadyCount);
        recordQuestMerge(player);
        if (recordTutorial) {
            recordTutorialMerge(player);
        }
        return true;
    }

    private static boolean tryAutoShearOnce(Player player) {
        if (player == null || player.getWorld() == null || !isSheepFarmWorld(player.getWorld())) {
            return false;
        }
        if (!isFarmOwner(player, player.getWorld())) {
            return false;
        }

        Sheep carriedSheep = getPickedUpSheep(player);
        UUID carriedId = carriedSheep == null ? null : carriedSheep.getUniqueId();
        long now = System.currentTimeMillis();
        Sheep bestCandidate = null;
        int bestTierWeight = -1;
        for (Sheep sheep : player.getWorld().getEntitiesByClass(Sheep.class)) {
            if (sheep == null || !sheep.isValid() || sheep.isDead() || sheep.isSheared()) {
                continue;
            }
            if (!sheep.isAdult()) {
                continue;
            }
            if (getNextEatTimestamp(sheep) > now) {
                sheep.setSheared(true);
                updateSheepName(sheep);
                continue;
            }
            if (carriedId != null && carriedId.equals(sheep.getUniqueId())) {
                continue;
            }

            SheepTier tier = getSheepTier(sheep);
            if (tier == null) {
                continue;
            }
            int tierWeight = tier.getLevel() * 10_000 + (tier == SheepTier.RAINBOW ? getRainbowTier(sheep) : 0);
            if (bestCandidate == null || tierWeight > bestTierWeight) {
                bestCandidate = sheep;
                bestTierWeight = tierWeight;
            }
        }
        return shearSheepForPlayer(player, bestCandidate);
    }

    static long getRemainingWoolRegenMs(Sheep sheep) {
        if (sheep == null || !sheep.isValid() || !sheep.isSheared()) {
            return 0L;
        }
        return Math.max(0L, getNextEatTimestamp(sheep) - System.currentTimeMillis());
    }

    static long getCombinedRemainingWoolRegenMs(Sheep first, Sheep second) {
        long firstRemaining = getRemainingWoolRegenMs(first);
        long secondRemaining = getRemainingWoolRegenMs(second);
        return (firstRemaining + secondRemaining) / 2L;
    }

    public static void initializeMergedSheepAfterMerge(Sheep sheep, SheepTier tier, long remainingWoolRegenMs) {
        if (sheep == null || tier == null) {
            return;
        }
        sheep.setAI(true);
        sheep.setGravity(true);
        if (remainingWoolRegenMs > 0L) {
            sheep.setSheared(true);
            setNextEatTimestamp(sheep, System.currentTimeMillis() + remainingWoolRegenMs);
        } else {
            sheep.setSheared(false);
            setNextEatTimestamp(sheep, 0L);
        }
        updateSheepName(sheep);
    }

    private static void tickAbilityVisual(Player player, UUID playerId, long now, Map<UUID, Long> activeUntil,
            org.bukkit.Particle particle, String endedText) {
        long until = activeUntil.getOrDefault(playerId, 0L);
        if (until <= 0L) {
            return;
        }
        if (now >= until) {
            activeUntil.remove(playerId);
            player.sendMessage(hint(endedText));
            playSound(player, Sound.BLOCK_BEACON_DEACTIVATE, 0.6f, 1.6f);
            return;
        }
        player.getWorld().spawnParticle(particle, player.getLocation().add(0, 1.0, 0), 5, 0.25, 0.35, 0.25, 0.01);
    }

    private static void emitAbilityAura(Player player, UUID playerId, long now) {
        boolean hasActiveAbility = false;
        if (isCountAbilityActive(activeLuckyBurstUsesByPlayer, luckyBurstEnabledByPlayer, playerId)) {
            hasActiveAbility = true;
            player.getWorld().spawnParticle(org.bukkit.Particle.TOTEM,
                    player.getLocation().add(0.0D, 1.1D, 0.0D),
                    2,
                    0.18D,
                    0.28D,
                    0.18D,
                    0.0D);
        }

        if (isAbilityActive(activeWoolRushUntilByPlayer, playerId)) {
            hasActiveAbility = true;
            player.getWorld().spawnParticle(org.bukkit.Particle.SPORE_BLOSSOM_AIR,
                    player.getLocation().add(0.0D, 0.9D, 0.0D),
                    4,
                    0.22D,
                    0.26D,
                    0.22D,
                    0.01D);
        }

        if (isAbilityActive(activeJackpotShearsUntilByPlayer, playerId)) {
            hasActiveAbility = true;
            player.getWorld().spawnParticle(org.bukkit.Particle.FIREWORKS_SPARK,
                    player.getLocation().add(0.0D, 1.25D, 0.0D),
                    3,
                    0.25D,
                    0.35D,
                    0.25D,
                    0.01D);
        }

        if (isCountAbilityActive(activeAutoMergeUsesByPlayer, autoMergeEnabledByPlayer, playerId)) {
            hasActiveAbility = true;
            player.getWorld().spawnParticle(org.bukkit.Particle.WAX_ON,
                    player.getLocation().add(0.0D, 1.0D, 0.0D),
                    5,
                    0.22D,
                    0.28D,
                    0.22D,
                    0.02D);
        }

        if (isCountAbilityActive(activeAutoShearUsesByPlayer, autoShearEnabledByPlayer, playerId)) {
            hasActiveAbility = true;
            player.getWorld().spawnParticle(org.bukkit.Particle.WAX_OFF,
                    player.getLocation().add(0.0D, 1.0D, 0.0D),
                    5,
                    0.22D,
                    0.28D,
                    0.22D,
                    0.02D);
        }

        if (!hasActiveAbility) {
            return;
        }

        long lastSoundAt = lastAbilityAuraSoundTimestampByPlayer.getOrDefault(playerId, 0L);
        if (now - lastSoundAt < ABILITY_AURA_SOUND_INTERVAL_MS) {
            return;
        }

        lastAbilityAuraSoundTimestampByPlayer.put(playerId, now);
        Sound[] gentleAuraSounds = {
                Sound.BLOCK_NOTE_BLOCK_CHIME,
                Sound.BLOCK_NOTE_BLOCK_HARP,
                Sound.BLOCK_AMETHYST_BLOCK_CHIME
        };
        playSound(player, gentleAuraSounds[RANDOM.nextInt(gentleAuraSounds.length)], 0.16f, 1.0f);
    }

    public static int getShearShopLevel(Player player) {
        return player == null ? 0 : shearShopLevelByPlayer.getOrDefault(player.getUniqueId(), 0);
    }

    public static int getShearWoolSaveLevel(Player player) {
        return player == null ? 0 : shearWoolSaveLevelByPlayer.getOrDefault(player.getUniqueId(), 0);
    }

    public static int getShearTierBoostLevel(Player player) {
        return player == null ? 0 : shearTierBoostLevelByPlayer.getOrDefault(player.getUniqueId(), 0);
    }

    public static int getShearFlatBonus(Player player) {
        return 0;
    }

    public static int getShearPointMultiplier(Player player) {
        int level = getShearShopLevel(player);
        return level + 1;
    }

    public static BigInteger getShearUpgradeCost(Player player) {
        return getDoubledUpgradeCostBig(SHEAR_SHOP_BASE_COST, getShearShopLevel(player));
    }

    public static int getShearWoolSaveChancePercent(Player player) {
        return Math.min(SHEAR_WOOL_SAVE_CHANCE_CAP, getShearWoolSaveLevel(player) * SHEAR_WOOL_SAVE_CHANCE_PER_LEVEL);
    }

    public static int getShearTierBoostChancePercent(Player player) {
        return Math.min(SHEAR_TIER_BOOST_CHANCE_CAP,
                getShearTierBoostLevel(player) * SHEAR_TIER_BOOST_CHANCE_PER_LEVEL);
    }

    public static BigInteger getShearWoolSaveUpgradeCost(Player player) {
        return getDoubledUpgradeCostBig(SHEAR_WOOL_SAVE_BASE_COST, getShearWoolSaveLevel(player));
    }

    public static BigInteger getShearTierBoostUpgradeCost(Player player) {
        return getDoubledUpgradeCostBig(SHEAR_TIER_BOOST_BASE_COST, getShearTierBoostLevel(player));
    }

    public static int getComboDecayUpgradeLevel(Player player) {
        return player == null ? 0 : comboDecayUpgradeByPlayer.getOrDefault(player.getUniqueId(), 0);
    }

    public static int getComboMaxUpgradeLevel(Player player) {
        return player == null ? 0 : comboMaxUpgradeByPlayer.getOrDefault(player.getUniqueId(), 0);
    }

    public static int getComboGainUpgradeLevel(Player player) {
        return player == null ? 0 : comboGainUpgradeByPlayer.getOrDefault(player.getUniqueId(), 0);
    }

    public static int getAutomationPoints(Player player) {
        return player == null ? 0 : automationPointsByPlayer.getOrDefault(player.getUniqueId(), 0);
    }

    public static int getAutomationAutoBuyUpgradeLevel(Player player) {
        return player == null ? 0
                : Math.min(AUTOMATION_AUTO_BUY_MAX_LEVEL,
                        Math.max(0, automationAutoBuyUpgradeByPlayer.getOrDefault(player.getUniqueId(), 0)));
    }

    public static int getAutomationAutoAbilityUpgradeLevel(Player player) {
        return player == null ? 0
                : Math.min(AUTOMATION_SINGLE_LEVEL_MAX,
                        Math.max(0, automationAutoAbilityUpgradeByPlayer.getOrDefault(player.getUniqueId(), 0)));
    }

    public static int getAutomationSlowAutoMergeUpgradeLevel(Player player) {
        return player == null ? 0
                : Math.min(AUTOMATION_SLOW_AUTO_MERGE_MAX_LEVEL,
                        Math.max(0, automationSlowAutoMergeUpgradeByPlayer.getOrDefault(player.getUniqueId(), 0)));
    }

    public static int getAutomationSlowAutoShearUpgradeLevel(Player player) {
        return player == null ? 0
                : Math.min(AUTOMATION_SLOW_AUTO_SHEAR_MAX_LEVEL,
                        Math.max(0, automationSlowAutoShearUpgradeByPlayer.getOrDefault(player.getUniqueId(), 0)));
    }

    private static long getAutomationAutoBuyIntervalMs(Player player) {
        int level = getAutomationAutoBuyUpgradeLevel(player);
        if (level <= 0) {
            return AUTOMATION_AUTO_BUY_INTERVAL_MS;
        }
        if (level >= AUTOMATION_AUTO_BUY_MAX_LEVEL) {
            return AUTOMATION_MIN_INTERVAL_MS;
        }
        long step = Math.max(1L, (AUTOMATION_AUTO_BUY_INTERVAL_MS - AUTOMATION_MIN_INTERVAL_MS)
                / Math.max(1, AUTOMATION_AUTO_BUY_MAX_LEVEL - 1));
        return Math.max(AUTOMATION_MIN_INTERVAL_MS, AUTOMATION_AUTO_BUY_INTERVAL_MS - ((long) (level - 1) * step));
    }

    private static long getAutomationSlowAutoMergeIntervalMs(Player player) {
        int level = getAutomationSlowAutoMergeUpgradeLevel(player);
        if (level <= 0) {
            return AUTOMATION_SLOW_AUTO_MERGE_INTERVAL_MS;
        }
        if (level >= AUTOMATION_SLOW_AUTO_MERGE_MAX_LEVEL) {
            return AUTOMATION_MIN_INTERVAL_MS;
        }
        long step = Math.max(1L, (AUTOMATION_SLOW_AUTO_MERGE_INTERVAL_MS - AUTOMATION_MIN_INTERVAL_MS)
                / Math.max(1, AUTOMATION_SLOW_AUTO_MERGE_MAX_LEVEL - 1));
        return Math.max(AUTOMATION_MIN_INTERVAL_MS,
                AUTOMATION_SLOW_AUTO_MERGE_INTERVAL_MS - ((long) (level - 1) * step));
    }

    private static long getAutomationSlowAutoShearIntervalMs(Player player) {
        int level = getAutomationSlowAutoShearUpgradeLevel(player);
        if (level <= 0) {
            return AUTOMATION_SLOW_AUTO_SHEAR_INTERVAL_MS;
        }
        if (level >= AUTOMATION_SLOW_AUTO_SHEAR_MAX_LEVEL) {
            return AUTOMATION_MIN_INTERVAL_MS;
        }
        long step = Math.max(1L, (AUTOMATION_SLOW_AUTO_SHEAR_INTERVAL_MS - AUTOMATION_MIN_INTERVAL_MS)
                / Math.max(1, AUTOMATION_SLOW_AUTO_SHEAR_MAX_LEVEL - 1));
        return Math.max(AUTOMATION_MIN_INTERVAL_MS,
                AUTOMATION_SLOW_AUTO_SHEAR_INTERVAL_MS - ((long) (level - 1) * step));
    }

    public static int getAutomationAutoSpawnUpgradeLevel(Player player) {
        return player == null ? 0
                : Math.min(AUTOMATION_AUTO_SPAWN_MAX_LEVEL,
                        Math.max(0, automationAutoSpawnUpgradeByPlayer.getOrDefault(player.getUniqueId(), 0)));
    }

    public static int getAutomationAutoPrestigeUpgradeLevel(Player player) {
        return player == null ? 0
                : Math.min(AUTOMATION_SINGLE_LEVEL_MAX,
                        Math.max(0, automationAutoPrestigeUpgradeByPlayer.getOrDefault(player.getUniqueId(), 0)));
    }

    public static BigInteger getSacrificePoints(Player player) {
        if (player == null) {
            return BigInteger.ZERO;
        }
        return getSacrificePoints(player.getUniqueId());
    }

    private static BigInteger getSacrificePoints(UUID playerId) {
        if (playerId == null) {
            return BigInteger.ZERO;
        }
        return sacrificePointsByPlayer.getOrDefault(playerId, BigInteger.ZERO).max(BigInteger.ZERO);
    }

    public static int getSacrificeUnlocksBought(Player player) {
        if (player == null) {
            return 0;
        }
        return getSacrificeUnlocksBought(player.getUniqueId());
    }

    private static int getSacrificeUnlocksBought(UUID playerId) {
        if (playerId == null) {
            return 0;
        }
        return Math.max(0, Math.min(SACRIFICE_UNLOCK_MAX,
                sacrificeUnlocksBoughtByPlayer.getOrDefault(playerId, 0)));
    }

    private static boolean hasSacrificeUnlock(Player player, int unlockId) {
        return player != null && hasSacrificeUnlock(player.getUniqueId(), unlockId);
    }

    private static boolean hasSacrificeUnlock(UUID playerId, int unlockId) {
        if (playerId == null || unlockId <= 0) {
            return false;
        }
        return getSacrificeUnlocksBought(playerId) >= unlockId;
    }

    private static BigInteger getSacrificeUnlockCost(UUID playerId) {
        int bought = getSacrificeUnlocksBought(playerId);
        return SACRIFICE_UNLOCK_COST_MULTIPLIER.pow(Math.max(0, bought));
    }

    private static BigInteger getSacrificeUnlockCost(Player player) {
        return player == null ? BigInteger.ONE : getSacrificeUnlockCost(player.getUniqueId());
    }

    private static void addSacrificePoints(UUID playerId, BigInteger amount) {
        if (playerId == null || amount == null || amount.signum() <= 0) {
            return;
        }
        sacrificePointsByPlayer.put(playerId, getSacrificePoints(playerId).add(amount));
    }

    private static BigInteger getSpentSacrificePoints(int unlocksBought) {
        int clamped = Math.max(0, Math.min(SACRIFICE_UNLOCK_MAX, unlocksBought));
        BigInteger total = BigInteger.ZERO;
        for (int i = 0; i < clamped; i++) {
            total = total.add(SACRIFICE_UNLOCK_COST_MULTIPLIER.pow(i));
        }
        return total;
    }

    private static BigInteger getSacrificeValueForSheep(Sheep sheep) {
        if (sheep == null || !sheep.isValid() || sheep.isDead()) {
            return BigInteger.ZERO;
        }
        SheepTier tier = getSheepTier(sheep);
        if (tier == null) {
            return BigInteger.ZERO;
        }
        int effectiveTier = Math.max(0, tier.getLevel());
        if (tier == SheepTier.RAINBOW) {
            effectiveTier = Math.max(0, SheepTier.RAINBOW.getLevel() + getRainbowTier(sheep) - 1);
        }
        return BigInteger.TWO.pow(effectiveTier);
    }

    private static BigInteger sacrificeAllSheepForPlayer(Player player) {
        if (player == null || player.getWorld() == null || !isSheepFarmWorld(player.getWorld())) {
            return BigInteger.ZERO;
        }
        if (!isFarmOwner(player, player.getWorld())) {
            return BigInteger.ZERO;
        }
        BigInteger gained = BigInteger.ZERO;
        World world = player.getWorld();
        for (Sheep sheep : world.getEntitiesByClass(Sheep.class)) {
            gained = gained.add(getSacrificeValueForSheep(sheep));
            if (sheep != null && sheep.isValid()) {
                sheep.remove();
            }
        }
        refreshLiveSheepCount(world);
        if (gained.signum() > 0) {
            addSacrificePoints(player.getUniqueId(), gained);
            saveData();
        }
        return gained;
    }

    private static boolean tryBuyNextSacrificeUnlock(Player player) {
        if (player == null) {
            return false;
        }
        UUID playerId = player.getUniqueId();
        int current = getSacrificeUnlocksBought(playerId);
        if (current >= SACRIFICE_UNLOCK_MAX) {
            return false;
        }
        BigInteger cost = getSacrificeUnlockCost(playerId);
        BigInteger points = getSacrificePoints(playerId);
        if (points.compareTo(cost) < 0) {
            return false;
        }
        sacrificePointsByPlayer.put(playerId, points.subtract(cost));
        sacrificeUnlocksBoughtByPlayer.put(playerId, current + 1);
        saveData();
        return true;
    }

    private static boolean refundSacrificeUnlocks(Player player) {
        if (player == null) {
            return false;
        }
        UUID playerId = player.getUniqueId();
        int current = getSacrificeUnlocksBought(playerId);
        if (current <= 0) {
            return false;
        }
        BigInteger refund = getSpentSacrificePoints(current);
        sacrificePointsByPlayer.put(playerId, getSacrificePoints(playerId).add(refund));
        sacrificeUnlocksBoughtByPlayer.remove(playerId);
        saveData();
        return true;
    }

    public static boolean isAutomationAutoBuyEnabled(Player player) {
        return player != null && automationAutoBuyEnabledByPlayer.getOrDefault(player.getUniqueId(), false);
    }

    public static boolean isAutomationAutoAbilityEnabled(Player player) {
        return player != null && automationAutoAbilityEnabledByPlayer.getOrDefault(player.getUniqueId(), false);
    }

    public static boolean isAutomationSlowAutoMergeEnabled(Player player) {
        return player != null && automationSlowAutoMergeEnabledByPlayer.getOrDefault(player.getUniqueId(), false);
    }

    public static boolean isAutomationSlowAutoShearEnabled(Player player) {
        return player != null && automationSlowAutoShearEnabledByPlayer.getOrDefault(player.getUniqueId(), false);
    }

    public static boolean isAutomationAutoSpawnEnabled(Player player) {
        return player != null && automationAutoSpawnEnabledByPlayer.getOrDefault(player.getUniqueId(), false);
    }

    public static boolean isAutomationAutoPrestigeEnabled(Player player) {
        return player != null && automationAutoPrestigeEnabledByPlayer.getOrDefault(player.getUniqueId(), false);
    }

    private static boolean toggleAutomationEnabled(Player player, Map<UUID, Boolean> enabledMap) {
        if (player == null || enabledMap == null) {
            return false;
        }
        UUID playerId = player.getUniqueId();
        boolean next = !enabledMap.getOrDefault(playerId, false);
        enabledMap.put(playerId, next);
        saveData();
        return next;
    }

    private static int getUnlockedAutomationCount(Player player) {
        if (player == null) {
            return 0;
        }
        int count = 0;
        if (getAutomationAutoBuyUpgradeLevel(player) > 0) {
            count++;
        }
        if (getAutomationAutoAbilityUpgradeLevel(player) > 0) {
            count++;
        }
        if (getAutomationSlowAutoMergeUpgradeLevel(player) > 0) {
            count++;
        }
        if (getAutomationSlowAutoShearUpgradeLevel(player) > 0) {
            count++;
        }
        if (getAutomationAutoSpawnUpgradeLevel(player) > 0) {
            count++;
        }
        if (getAutomationAutoPrestigeUpgradeLevel(player) > 0) {
            count++;
        }
        return count;
    }

    private static int setAllAutomationsEnabled(Player player, boolean enabled) {
        if (player == null) {
            return 0;
        }
        UUID playerId = player.getUniqueId();
        int changed = 0;

        if (getAutomationAutoBuyUpgradeLevel(player) > 0
                && automationAutoBuyEnabledByPlayer.getOrDefault(playerId, false) != enabled) {
            automationAutoBuyEnabledByPlayer.put(playerId, enabled);
            changed++;
        }
        if (getAutomationAutoAbilityUpgradeLevel(player) > 0
                && automationAutoAbilityEnabledByPlayer.getOrDefault(playerId, false) != enabled) {
            automationAutoAbilityEnabledByPlayer.put(playerId, enabled);
            changed++;
        }
        if (getAutomationSlowAutoMergeUpgradeLevel(player) > 0
                && automationSlowAutoMergeEnabledByPlayer.getOrDefault(playerId, false) != enabled) {
            automationSlowAutoMergeEnabledByPlayer.put(playerId, enabled);
            changed++;
        }
        if (getAutomationSlowAutoShearUpgradeLevel(player) > 0
                && automationSlowAutoShearEnabledByPlayer.getOrDefault(playerId, false) != enabled) {
            automationSlowAutoShearEnabledByPlayer.put(playerId, enabled);
            changed++;
        }
        if (getAutomationAutoSpawnUpgradeLevel(player) > 0
                && automationAutoSpawnEnabledByPlayer.getOrDefault(playerId, false) != enabled) {
            automationAutoSpawnEnabledByPlayer.put(playerId, enabled);
            changed++;
        }
        if (getAutomationAutoPrestigeUpgradeLevel(player) > 0
                && automationAutoPrestigeEnabledByPlayer.getOrDefault(playerId, false) != enabled) {
            automationAutoPrestigeEnabledByPlayer.put(playerId, enabled);
            changed++;
        }

        if (changed > 0) {
            saveData();
        }
        return changed;
    }

    private static BigInteger getComboDecayUpgradeCost(Player player) {
        return getDoubledUpgradeCostBig(COMBO_DECAY_BASE_COST, getComboDecayUpgradeLevel(player));
    }

    private static BigInteger getComboGainUpgradeCost(Player player) {
        return getDoubledUpgradeCostBig(COMBO_GAIN_BASE_COST, getComboGainUpgradeLevel(player));
    }

    private static int getComboMaxUpgradePrestigeCost(Player player) {
        return getPrestigeUpgradeCost(COMBO_MAX_BASE_PRESTIGE_COST, getComboMaxUpgradeLevel(player));
    }

    private static int getAutomationAutoBuyUpgradeCost(Player player) {
        if (getAutomationAutoBuyUpgradeLevel(player) >= AUTOMATION_AUTO_BUY_MAX_LEVEL) {
            return 0;
        }
        return getDoubledUpgradeCost(AUTOMATION_AUTO_BUY_BASE_COST, getAutomationAutoBuyUpgradeLevel(player));
    }

    private static int getAutomationAutoAbilityUpgradeCost(Player player) {
        if (getAutomationAutoAbilityUpgradeLevel(player) >= AUTOMATION_SINGLE_LEVEL_MAX) {
            return 0;
        }
        return getDoubledUpgradeCost(AUTOMATION_AUTO_ABILITY_BASE_COST, getAutomationAutoAbilityUpgradeLevel(player));
    }

    private static int getAutomationSlowAutoMergeUpgradeCost(Player player) {
        if (getAutomationSlowAutoMergeUpgradeLevel(player) >= AUTOMATION_SLOW_AUTO_MERGE_MAX_LEVEL) {
            return 0;
        }
        return getDoubledUpgradeCost(AUTOMATION_SLOW_AUTO_MERGE_BASE_COST,
                getAutomationSlowAutoMergeUpgradeLevel(player));
    }

    private static int getAutomationSlowAutoShearUpgradeCost(Player player) {
        if (getAutomationSlowAutoShearUpgradeLevel(player) >= AUTOMATION_SLOW_AUTO_SHEAR_MAX_LEVEL) {
            return 0;
        }
        return getDoubledUpgradeCost(AUTOMATION_SLOW_AUTO_SHEAR_BASE_COST,
                getAutomationSlowAutoShearUpgradeLevel(player));
    }

    private static int getAutomationAutoSpawnUpgradeCost(Player player) {
        if (getAutomationAutoSpawnUpgradeLevel(player) >= AUTOMATION_AUTO_SPAWN_MAX_LEVEL) {
            return 0;
        }
        return getDoubledUpgradeCost(AUTOMATION_AUTO_SPAWN_BASE_COST, getAutomationAutoSpawnUpgradeLevel(player));
    }

    private static int getAutomationAutoPrestigeUpgradeCost(Player player) {
        return getAutomationAutoPrestigeUpgradeLevel(player) > 0 ? 0 : AUTOMATION_AUTO_PRESTIGE_BASE_COST;
    }

    private static long getAutomationAutoSpawnIntervalMs(Player player) {
        int level = getAutomationAutoSpawnUpgradeLevel(player);
        if (level <= 0) {
            return AUTOMATION_AUTO_SPAWN_BASE_INTERVAL_MS;
        }
        if (level >= AUTOMATION_AUTO_SPAWN_MAX_LEVEL) {
            return 0L;
        }
        long reduced = AUTOMATION_AUTO_SPAWN_BASE_INTERVAL_MS
                - (long) level * Math.max(1L, AUTOMATION_AUTO_SPAWN_INTERVAL_STEP_MS);
        return Math.max(Math.max(0L, AUTOMATION_AUTO_SPAWN_MIN_INTERVAL_MS), reduced);
    }

    private static boolean trySpendAutomationPoints(Player player, int amount) {
        if (player == null || amount <= 0) {
            return false;
        }
        int current = getAutomationPoints(player);
        if (current < amount) {
            return false;
        }
        automationPointsByPlayer.put(player.getUniqueId(), current - amount);
        saveData();
        return true;
    }

    private static boolean upgradeComboDecay(Player player) {
        if (player == null) {
            return false;
        }
        int currentLevel = getComboDecayUpgradeLevel(player);
        if (currentLevel >= COMBO_DECAY_MAX_LEVEL) {
            return false;
        }
        BigInteger cost = getComboDecayUpgradeCost(player);
        if (!canSpendUpgradePointsDuringTutorial(player, cost)) {
            return false;
        }
        if (!trySpendPoints(player, cost)) {
            return false;
        }
        comboDecayUpgradeByPlayer.put(player.getUniqueId(), currentLevel + 1);
        saveData();
        return true;
    }

    private static boolean upgradeComboMax(Player player) {
        if (player == null) {
            return false;
        }
        int currentLevel = getComboMaxUpgradeLevel(player);
        if (currentLevel >= COMBO_MAX_MAX_LEVEL) {
            return false;
        }
        int cost = getComboMaxUpgradePrestigeCost(player);
        if (!trySpendPrestigePoints(player, cost)) {
            return false;
        }
        comboMaxUpgradeByPlayer.put(player.getUniqueId(), currentLevel + 1);
        double score = Math.min(getComboMaxScore(player), getComboScore(player));
        comboScoreByPlayer.put(player.getUniqueId(), score);
        saveData();
        return true;
    }

    private static boolean upgradeComboGain(Player player) {
        if (player == null) {
            return false;
        }
        int currentLevel = getComboGainUpgradeLevel(player);
        if (currentLevel >= COMBO_GAIN_MAX_LEVEL) {
            return false;
        }
        BigInteger cost = getComboGainUpgradeCost(player);
        if (!canSpendUpgradePointsDuringTutorial(player, cost)) {
            return false;
        }
        if (!trySpendPoints(player, cost)) {
            return false;
        }
        comboGainUpgradeByPlayer.put(player.getUniqueId(), currentLevel + 1);
        saveData();
        return true;
    }

    private static boolean upgradeAutomationAutoBuy(Player player) {
        if (player == null || getAutomationAutoBuyUpgradeLevel(player) >= AUTOMATION_AUTO_BUY_MAX_LEVEL) {
            return false;
        }
        int cost = getAutomationAutoBuyUpgradeCost(player);
        if (!trySpendAutomationPoints(player, cost)) {
            return false;
        }
        automationAutoBuyUpgradeByPlayer.put(player.getUniqueId(), getAutomationAutoBuyUpgradeLevel(player) + 1);
        saveData();
        return true;
    }

    private static boolean upgradeAutomationAutoAbility(Player player) {
        if (player == null || getAutomationAutoAbilityUpgradeLevel(player) >= AUTOMATION_SINGLE_LEVEL_MAX) {
            return false;
        }
        int cost = getAutomationAutoAbilityUpgradeCost(player);
        if (!trySpendAutomationPoints(player, cost)) {
            return false;
        }
        automationAutoAbilityUpgradeByPlayer.put(player.getUniqueId(),
                getAutomationAutoAbilityUpgradeLevel(player) + 1);
        saveData();
        return true;
    }

    private static boolean upgradeAutomationSlowAutoMerge(Player player) {
        if (player == null || getAutomationSlowAutoMergeUpgradeLevel(player) >= AUTOMATION_SLOW_AUTO_MERGE_MAX_LEVEL) {
            return false;
        }
        int cost = getAutomationSlowAutoMergeUpgradeCost(player);
        if (!trySpendAutomationPoints(player, cost)) {
            return false;
        }
        automationSlowAutoMergeUpgradeByPlayer.put(player.getUniqueId(),
                getAutomationSlowAutoMergeUpgradeLevel(player) + 1);
        nextAutomationSlowMergeAtByPlayer.put(player.getUniqueId(), 0L);
        saveData();
        return true;
    }

    private static boolean upgradeAutomationSlowAutoShear(Player player) {
        if (player == null || getAutomationSlowAutoShearUpgradeLevel(player) >= AUTOMATION_SLOW_AUTO_SHEAR_MAX_LEVEL) {
            return false;
        }
        int cost = getAutomationSlowAutoShearUpgradeCost(player);
        if (!trySpendAutomationPoints(player, cost)) {
            return false;
        }
        automationSlowAutoShearUpgradeByPlayer.put(player.getUniqueId(),
                getAutomationSlowAutoShearUpgradeLevel(player) + 1);
        nextAutomationSlowShearAtByPlayer.put(player.getUniqueId(), 0L);
        saveData();
        return true;
    }

    private static boolean upgradeAutomationAutoSpawn(Player player) {
        if (player == null || getAutomationAutoSpawnUpgradeLevel(player) >= AUTOMATION_AUTO_SPAWN_MAX_LEVEL) {
            return false;
        }
        int cost = getAutomationAutoSpawnUpgradeCost(player);
        if (!trySpendAutomationPoints(player, cost)) {
            return false;
        }
        automationAutoSpawnUpgradeByPlayer.put(player.getUniqueId(), getAutomationAutoSpawnUpgradeLevel(player) + 1);
        nextAutomationAutoSpawnAtByPlayer.put(player.getUniqueId(), 0L);
        saveData();
        return true;
    }

    private static boolean upgradeAutomationAutoPrestige(Player player) {
        if (player == null || getAutomationAutoPrestigeUpgradeLevel(player) > 0) {
            return false;
        }
        int cost = getAutomationAutoPrestigeUpgradeCost(player);
        if (!trySpendAutomationPoints(player, cost)) {
            return false;
        }
        UUID playerId = player.getUniqueId();
        automationAutoPrestigeUpgradeByPlayer.put(playerId, 1);
        nextAutomationAutoPrestigeAtByPlayer.put(playerId, 0L);
        saveData();
        return true;
    }

    public static boolean upgradeShearShop(Player player) {
        if (player == null) {
            return false;
        }
        BigInteger cost = getShearUpgradeCost(player);
        if (!canSpendUpgradePointsDuringTutorial(player, cost)) {
            return false;
        }
        if (!trySpendPoints(player, cost)) {
            return false;
        }
        shearShopLevelByPlayer.put(player.getUniqueId(), getShearShopLevel(player) + 1);
        saveData();
        return true;
    }

    public static boolean upgradeShearWoolSave(Player player) {
        if (player == null) {
            return false;
        }
        int currentLevel = getShearWoolSaveLevel(player);
        if (currentLevel >= SHEAR_WOOL_SAVE_MAX_LEVEL) {
            return false;
        }
        BigInteger cost = getShearWoolSaveUpgradeCost(player);
        if (!canSpendUpgradePointsDuringTutorial(player, cost)) {
            return false;
        }
        if (!trySpendPoints(player, cost)) {
            return false;
        }
        shearWoolSaveLevelByPlayer.put(player.getUniqueId(), currentLevel + 1);
        saveData();
        return true;
    }

    public static boolean upgradeShearTierBoost(Player player) {
        if (player == null) {
            return false;
        }
        int currentLevel = getShearTierBoostLevel(player);
        if (currentLevel >= SHEAR_TIER_BOOST_MAX_LEVEL) {
            return false;
        }
        BigInteger cost = getShearTierBoostUpgradeCost(player);
        if (!canSpendUpgradePointsDuringTutorial(player, cost)) {
            return false;
        }
        if (!trySpendPoints(player, cost)) {
            return false;
        }
        shearTierBoostLevelByPlayer.put(player.getUniqueId(), currentLevel + 1);
        saveData();
        return true;
    }

    public static int prestige(Player player) {
        if (player == null) {
            return 0;
        }
        int current = getPrestigeLevel(player);

        int affordableLevels = getAffordablePrestigeLevels(player);
        if (affordableLevels <= 0) {
            return 0;
        }

        BigInteger totalCost = getTotalPrestigeCostForNextLevels(current, affordableLevels);
        if (totalCost.signum() <= 0 || !trySpendPoints(player, totalCost)) {
            return 0;
        }

        int nextPrestige = current + affordableLevels;
        int gainedPrestigePoints = getPrestigePointsRewardForNextLevels(current, affordableLevels);
        UUID playerId = player.getUniqueId();
        prestigeLevelByPlayer.put(playerId, nextPrestige);
        prestigePointsByPlayer.put(playerId, addSaturated(getPrestigePoints(player), gainedPrestigePoints));
        clearPrestigeReminder(player);

        BigInteger sacrificeGained = BigInteger.ZERO;
        World world = player.getWorld();
        if (isSheepFarmWorld(world) && isFarmOwner(player, world)) {
            for (Sheep sheep : world.getEntitiesByClass(Sheep.class)) {
                sacrificeGained = sacrificeGained.add(getSacrificeValueForSheep(sheep));
                if (sheep != null && sheep.isValid()) {
                    sheep.remove();
                }
            }
            refreshLiveSheepCount(world);
        }
        if (sacrificeGained.signum() > 0) {
            addSacrificePoints(playerId, sacrificeGained);
        }

        // Reset regular progression purchased with normal points.
        pointsByPlayer.put(playerId, BigInteger.ZERO);
        refreshTopPointsDisplays();
        if (!hasSacrificeUnlock(playerId, SACRIFICE_UNLOCK_NO_REGULAR_RESETS)) {
            extraLimitByPlayer.remove(playerId);
            eggSpeedLevelByPlayer.remove(playerId);
            woolRegenLevelByPlayer.remove(playerId);
            higherTierChanceLevelByPlayer.remove(playerId);
        }
        if (!hasSacrificeUnlock(playerId, SACRIFICE_UNLOCK_NO_SHEAR_RESETS)) {
            shearShopLevelByPlayer.remove(playerId);
            shearWoolSaveLevelByPlayer.remove(playerId);
            shearTierBoostLevelByPlayer.remove(playerId);
        }
        if (!hasSacrificeUnlock(playerId, SACRIFICE_UNLOCK_NO_COMBO_RESETS)) {
            comboDecayUpgradeByPlayer.remove(playerId);
            comboGainUpgradeByPlayer.remove(playerId);
        }
        clearMergeReminder(player);
        EGG_MODULE.clearRuntimeState(playerId);
        clearComboRuntime(player);

        saveData();
        markTutorialPrestigedOnce(player);
        return affordableLevels;
    }

    public static int getPrestigeCost(Player player) {
        return toIntClamped(getPrestigeCostBig(player));
    }

    private static BigInteger getPrestigeCostBig(Player player) {
        return getPrestigeCostForLevelBig(getPrestigeLevel(player));
    }

    private static BigInteger getPrestigeCostForLevelBig(int level) {
        int normalizedLevel = Math.max(0, level);
        if (PRESTIGE_LEVEL_BASE_COST <= 0) {
            return BigInteger.ZERO;
        }
        return BigInteger.valueOf(PRESTIGE_LEVEL_BASE_COST).shiftLeft(Math.min(Integer.MAX_VALUE - 2, normalizedLevel));
    }

    private static int getAffordablePrestigeLevels(Player player) {
        if (player == null) {
            return 0;
        }
        int level = getPrestigeLevel(player);
        BigInteger points = getPlayerPointsBig(player);
        int gained = 0;

        while (level < Integer.MAX_VALUE) {
            BigInteger cost = getPrestigeCostForLevelBig(level);
            if (points.compareTo(cost) < 0) {
                break;
            }
            points = points.subtract(cost);
            level++;
            gained++;
        }
        return gained;
    }

    private static BigInteger getTotalPrestigeCostForNextLevels(int currentLevel, int levelsToBuy) {
        BigInteger total = BigInteger.ZERO;
        int cappedLevels = Math.max(0, levelsToBuy);
        for (int i = 0; i < cappedLevels; i++) {
            total = total.add(getPrestigeCostForLevelBig(currentLevel + i));
        }
        return total;
    }

    private static int getPrestigePointsRewardForNextLevels(int currentLevel, int levelsToBuy) {
        long reward = 0L;
        int cappedLevels = Math.max(0, levelsToBuy);
        for (int i = 1; i <= cappedLevels; i++) {
            reward += currentLevel + (long) i;
            if (reward >= Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            }
        }
        return (int) reward;
    }

    public static int getUnlockedTierCap(World world) {
        UUID ownerId = getOwnerId(world);
        if (ownerId == null) {
            return SheepTier.WHITE.getLevel();
        }
        int prestigeLevel = prestigeLevelByPlayer.getOrDefault(ownerId, 0);
        int maxLevelBonus = prestigeHigherMaxLevelByPlayer.getOrDefault(ownerId, 0);
        int cap = 1 + prestigeLevel * 2 + maxLevelBonus * 2;
        return Math.min(SheepTier.RAINBOW.getLevel(), cap);
    }

    public static boolean canTierExistInWorld(World world, SheepTier tier) {
        if (tier == null) {
            return true;
        }
        return tier.getLevel() <= getUnlockedTierCap(world);
    }

    public static int getTutorialShearCount(Player player) {
        return player == null ? 0 : tutorialShearsByPlayer.getOrDefault(player.getUniqueId(), 0);
    }

    public static int getTutorialSpawnCount(Player player) {
        return player == null ? 0 : tutorialSpawnsByPlayer.getOrDefault(player.getUniqueId(), 0);
    }

    public static int getTutorialMergeCount(Player player) {
        return player == null ? 0 : tutorialMergesByPlayer.getOrDefault(player.getUniqueId(), 0);
    }

    private static boolean isTutorialInProgress(Player player) {
        return player != null && !hasUnlockedFarm(player);
    }

    private static boolean isInTutorialWorld(Player player) {
        return player != null && isTutorialWorld(player.getWorld());
    }

    private static int getTutorialSectionCount(Player player) {
        if (player == null) {
            return 0;
        }
        UUID playerId = player.getUniqueId();
        int count = 0;
        if (tutorialUpgradeOpenedByPlayer.getOrDefault(playerId, false)) {
            count++;
        }
        if (tutorialQuestOpenedByPlayer.getOrDefault(playerId, false)) {
            count++;
        }
        if (tutorialQuestUpgradesOpenedByPlayer.getOrDefault(playerId, false)) {
            count++;
        }
        if (tutorialPrestigeOpenedByPlayer.getOrDefault(playerId, false)) {
            count++;
        }
        if (tutorialAbilityUsedByPlayer.getOrDefault(playerId, false)) {
            count++;
        }
        if (tutorialShearUpgradedByPlayer.getOrDefault(playerId, false)) {
            count++;
        }
        if (tutorialRegularUpgradesBoughtByPlayer.getOrDefault(playerId, false)) {
            count++;
        }
        if (tutorialPrestigedOnceByPlayer.getOrDefault(playerId, false)) {
            count++;
        }
        return count;
    }

    private static String getTutorialNextStepLine(Player player) {
        if (player == null) {
            return "Enter your tutorial world";
        }
        if (getTutorialSpawnCount(player) < TUTORIAL_SPAWN_TARGET) {
            return "Spawn sheep (" + getTutorialSpawnCount(player) + "/" + TUTORIAL_SPAWN_TARGET + ")";
        }
        if (getTutorialShearCount(player) < TUTORIAL_SHEAR_TARGET) {
            return "Shear sheep (" + getTutorialShearCount(player) + "/" + TUTORIAL_SHEAR_TARGET
                    + ")";
        }
        if (getTutorialMergeCount(player) < TUTORIAL_MERGE_TARGET) {
            return "Merge same-tier sheep (SHIFT + RIGHT-CLICK) ("
                    + getTutorialMergeCount(player) + "/" + TUTORIAL_MERGE_TARGET + ")";
        }

        UUID playerId = player.getUniqueId();
        if (!tutorialUpgradeOpenedByPlayer.getOrDefault(playerId, false)) {
            return "Open Upgrades (Nether Star)";
        }
        if (!tutorialRegularUpgradesBoughtByPlayer.getOrDefault(playerId, false)) {
            return "Buy any regular upgrade";
        }
        if (!tutorialQuestOpenedByPlayer.getOrDefault(playerId, false)) {
            return "Open Quests from Upgrades";
        }
        if (!tutorialAbilityUsedByPlayer.getOrDefault(playerId, false)) {
            return "Activate any quest ability";
        }
        if (!tutorialQuestUpgradesOpenedByPlayer.getOrDefault(playerId, false)) {
            return "Open Quest Upgrades";
        }
        if (!tutorialShearUpgradedByPlayer.getOrDefault(playerId, false)) {
            return "Buy one Shear Shop upgrade";
        }
        if (!tutorialPrestigeOpenedByPlayer.getOrDefault(playerId, false)) {
            return "Open Prestige from Upgrades";
        }
        if (!tutorialPrestigedOnceByPlayer.getOrDefault(playerId, false)) {
            return "Use Prestige once";
        }
        return "Tutorial complete";
    }

    public static void startTutorial(Player player, boolean resetProgress) {
        if (player == null) {
            return;
        }
        UUID playerId = player.getUniqueId();
        if (resetProgress) {
            resetTutorialProgress(playerId);
            saveData();
        }

        tutorialStartedAtByPlayer.put(playerId, System.currentTimeMillis());
        lastTutorialReminderTimestampByPlayer.remove(playerId);

        if (!SheepFarmWorldCommand.teleportToTutorialWorld(player)) {
            player.sendMessage(warning("Unable to open your tutorial world right now."));
            return;
        }

        EGG_MODULE.addEggs(player, 10);

        sendTutorialTitle(player, "&eSheepMerge Tutorial", "&fFollow the steps to unlock your farm");
        player.sendMessage(hint("Step 1: Spawn " + TUTORIAL_SPAWN_TARGET + " sheep."));
        player.sendMessage(hint("Step 2: Shear " + TUTORIAL_SHEAR_TARGET + " sheep."));
        player.sendMessage(hint("Step 3: Merge " + TUTORIAL_MERGE_TARGET + " pair (SHIFT + RIGHT-CLICK)."));
        player.sendMessage(hint("Step 4: Menus -> Upgrades, Quests, Quest Upgrades, Shear Shop, Prestige."));
        player.sendMessage(accent("Tip: /sheepmerge status shows your current step."));
        sendTutorialStatusFeed(player);
    }

    private static void markTutorialSection(Player player, Map<UUID, Boolean> sectionMap, String message) {
        if (!isTutorialInProgress(player) || !isInTutorialWorld(player) || sectionMap == null) {
            return;
        }
        UUID playerId = player.getUniqueId();
        if (sectionMap.getOrDefault(playerId, false)) {
            return;
        }
        sectionMap.put(playerId, true);
        player.sendMessage(action(message));
        sendTutorialStatusFeed(player);
        maybeGrantTutorialPrestigePrepReward(player);
        checkTutorialCompletion(player);
    }

    public static void markTutorialUpgradeOpened(Player player) {
        markTutorialSection(player, tutorialUpgradeOpenedByPlayer, "Tutorial step done: Upgrades opened.");
    }

    public static void markTutorialQuestOpened(Player player) {
        markTutorialSection(player, tutorialQuestOpenedByPlayer, "Tutorial step done: Quests opened.");
    }

    public static void markTutorialPrestigeOpened(Player player) {
        markTutorialSection(player, tutorialPrestigeOpenedByPlayer, "Tutorial step done: Prestige opened.");
    }

    public static void markTutorialQuestUpgradesOpened(Player player) {
        markTutorialSection(player, tutorialQuestUpgradesOpenedByPlayer, "Tutorial step done: Quest Upgrades opened.");
    }

    public static void markTutorialAbilityUsed(Player player) {
        markTutorialSection(player, tutorialAbilityUsedByPlayer, "Tutorial step done: Ability used.");
    }

    public static void markTutorialShearUpgraded(Player player) {
        markTutorialSection(player, tutorialShearUpgradedByPlayer, "Tutorial step done: Shear upgrade bought.");
    }

    public static void markTutorialPrestigedOnce(Player player) {
        markTutorialSection(player, tutorialPrestigedOnceByPlayer, "Tutorial step done: Prestiged once.");
    }

    public static void markTutorialRegularUpgradesIfComplete(Player player) {
        if (!isTutorialInProgress(player) || !isInTutorialWorld(player)) {
            return;
        }
        if (getLimitUpgradeLevel(player) <= 0
                && getEggSpeedLevel(player) <= 0
                && getWoolRegenLevel(player) <= 0
                && getHigherTierChanceLevel(player) <= 0) {
            return;
        }
        markTutorialSection(
                player,
                tutorialRegularUpgradesBoughtByPlayer,
                "Tutorial step done: Regular upgrade bought.");
    }

    public static void markTutorialShearShopOpened(Player player) {
        markTutorialSection(player, tutorialShearShopOpenedByPlayer, "Tutorial step done: Shear Shop opened.");
    }

    public static void recordTutorialShear(Player player) {
        if (!isTutorialInProgress(player) || !isInTutorialWorld(player)) {
            return;
        }
        tutorialShearsByPlayer.put(player.getUniqueId(), getTutorialShearCount(player) + 1);
        sendTutorialStatusFeed(player);
        maybeGrantTutorialShearTaskReward(player);
        maybeGrantTutorialPrestigePrepReward(player);
        checkTutorialCompletion(player);
    }

    public static void recordTutorialSpawn(Player player) {
        if (!isTutorialInProgress(player) || !isInTutorialWorld(player)) {
            return;
        }
        tutorialSpawnsByPlayer.put(player.getUniqueId(), getTutorialSpawnCount(player) + 1);
        sendTutorialStatusFeed(player);
        maybeGrantTutorialShearTaskReward(player);
        maybeGrantTutorialPrestigePrepReward(player);
        checkTutorialCompletion(player);
    }

    public static void recordTutorialMerge(Player player) {
        if (!isTutorialInProgress(player) || !isInTutorialWorld(player)) {
            return;
        }
        tutorialMergesByPlayer.put(player.getUniqueId(), getTutorialMergeCount(player) + 1);
        sendTutorialStatusFeed(player);
        maybeGrantTutorialPrestigePrepReward(player);
        checkTutorialCompletion(player);
    }

    private static void maybeGrantTutorialShearTaskReward(Player player) {
        if (player == null) {
            return;
        }
        UUID playerId = player.getUniqueId();
        if (tutorialShearTaskRewardGrantedByPlayer.getOrDefault(playerId, false)) {
            return;
        }
        if (getTutorialSpawnCount(player) < TUTORIAL_SPAWN_TARGET
                || getTutorialShearCount(player) < TUTORIAL_SHEAR_TARGET) {
            return;
        }
        tutorialShearTaskRewardGrantedByPlayer.put(playerId, true);
        player.sendMessage(action("Tutorial milestone: spawn + shear goals complete."));
    }

    private static void maybeGrantTutorialPrestigePrepReward(Player player) {
        if (player == null) {
            return;
        }
        UUID playerId = player.getUniqueId();
        if (tutorialPrestigePrepRewardGrantedByPlayer.getOrDefault(playerId, false)
                || tutorialPrestigedOnceByPlayer.getOrDefault(playerId, false)) {
            return;
        }
        if (getTutorialShearCount(player) < TUTORIAL_SHEAR_TARGET
                || getTutorialSpawnCount(player) < TUTORIAL_SPAWN_TARGET
                || getTutorialMergeCount(player) < TUTORIAL_MERGE_TARGET
                || !tutorialRegularUpgradesBoughtByPlayer.getOrDefault(playerId, false)
                || !tutorialUpgradeOpenedByPlayer.getOrDefault(playerId, false)
                || !tutorialQuestOpenedByPlayer.getOrDefault(playerId, false)
                || !tutorialQuestUpgradesOpenedByPlayer.getOrDefault(playerId, false)
                || !tutorialPrestigeOpenedByPlayer.getOrDefault(playerId, false)
                || !tutorialAbilityUsedByPlayer.getOrDefault(playerId, false)
                || !tutorialShearUpgradedByPlayer.getOrDefault(playerId, false)) {
            return;
        }

        tutorialPrestigePrepRewardGrantedByPlayer.put(playerId, true);
        player.sendMessage(action("Tutorial milestone: prestige prep complete."));
    }

    private enum TutorialStep {
        SPAWN,
        SHEAR,
        MERGE,
        OPEN_UPGRADES,
        BUY_REGULAR_UPGRADE,
        OPEN_QUESTS,
        USE_ABILITY,
        OPEN_QUEST_UPGRADES,
        BUY_SHEAR_UPGRADE,
        OPEN_PRESTIGE,
        PRESTIGE_ONCE,
        COMPLETE
    }

    public enum TutorialAction {
        SPAWN_SHEEP,
        SHEAR_SHEEP,
        MERGE_SHEEP,
        OPEN_UPGRADE_COMMAND,
        OPEN_SHOP_COMMAND,
        OPEN_PRESTIGE_COMMAND,
        OTHER_COMMAND
    }

    private static TutorialStep getCurrentTutorialStep(Player player) {
        if (player == null) {
            return TutorialStep.COMPLETE;
        }
        if (getTutorialSpawnCount(player) < TUTORIAL_SPAWN_TARGET) {
            return TutorialStep.SPAWN;
        }
        if (getTutorialShearCount(player) < TUTORIAL_SHEAR_TARGET) {
            return TutorialStep.SHEAR;
        }
        if (getTutorialMergeCount(player) < TUTORIAL_MERGE_TARGET) {
            return TutorialStep.MERGE;
        }

        UUID playerId = player.getUniqueId();
        if (!tutorialUpgradeOpenedByPlayer.getOrDefault(playerId, false)) {
            return TutorialStep.OPEN_UPGRADES;
        }
        if (!tutorialRegularUpgradesBoughtByPlayer.getOrDefault(playerId, false)) {
            return TutorialStep.BUY_REGULAR_UPGRADE;
        }
        if (!tutorialQuestOpenedByPlayer.getOrDefault(playerId, false)) {
            return TutorialStep.OPEN_QUESTS;
        }
        if (!tutorialAbilityUsedByPlayer.getOrDefault(playerId, false)) {
            return TutorialStep.USE_ABILITY;
        }
        if (!tutorialQuestUpgradesOpenedByPlayer.getOrDefault(playerId, false)) {
            return TutorialStep.OPEN_QUEST_UPGRADES;
        }
        if (!tutorialShearUpgradedByPlayer.getOrDefault(playerId, false)) {
            return TutorialStep.BUY_SHEAR_UPGRADE;
        }
        if (!tutorialPrestigeOpenedByPlayer.getOrDefault(playerId, false)) {
            return TutorialStep.OPEN_PRESTIGE;
        }
        if (!tutorialPrestigedOnceByPlayer.getOrDefault(playerId, false)) {
            return TutorialStep.PRESTIGE_ONCE;
        }
        return TutorialStep.COMPLETE;
    }

    public static boolean shouldRestrictTutorialActions(Player player) {
        return player != null && isTutorialInProgress(player) && isInTutorialWorld(player);
    }

    private static String getCurrentTutorialTaskLabel(TutorialStep step) {
        return switch (step) {
            case SPAWN -> "Spawn sheep";
            case SHEAR -> "Shear sheep";
            case MERGE -> "Merge same-tier sheep";
            case OPEN_UPGRADES -> "Open Upgrades (Nether Star)";
            case BUY_REGULAR_UPGRADE -> "Buy one regular upgrade";
            case OPEN_QUESTS -> "Open Quests from Upgrades";
            case USE_ABILITY -> "Activate any quest ability";
            case OPEN_QUEST_UPGRADES -> "Open Quest Upgrades";
            case BUY_SHEAR_UPGRADE -> "Buy one Shear Shop upgrade";
            case OPEN_PRESTIGE -> "Open Prestige from Upgrades";
            case PRESTIGE_ONCE -> "Use Prestige once";
            case COMPLETE -> "Tutorial complete";
        };
    }

    private static BigInteger getTutorialStepRequiredPoints(Player player, TutorialStep step) {
        if (player == null || step == null) {
            return BigInteger.valueOf(-1L);
        }
        return switch (step) {
            case BUY_REGULAR_UPGRADE -> getMinimumRegularUpgradeCost(player);
            case BUY_SHEAR_UPGRADE -> getMinimumShearUpgradeCost(player);
            case PRESTIGE_ONCE -> getPrestigeCostBig(player);
            default -> BigInteger.valueOf(-1L);
        };
    }

    private static BigInteger getMinimumRegularUpgradeCost(Player player) {
        if (player == null) {
            return BigInteger.valueOf(-1L);
        }
        BigInteger minimum = null;
        if (getPlayerLimit(player) < MAX_SHEEP_LIMIT) {
            minimum = minPositive(minimum, getUpgradeCost(player));
        }
        if (getEggSpeedLevel(player) < getEggSpeedMaxLevel(player)) {
            minimum = minPositive(minimum, getEggSpeedUpgradeCost(player));
        }
        if (getWoolRegenLevel(player) < getWoolRegenMaxLevel(player)) {
            minimum = minPositive(minimum, getWoolRegenUpgradeCost(player));
        }
        if (getHigherTierChanceLevel(player) < getHigherTierChanceMaxLevel(player)) {
            minimum = minPositive(minimum, getHigherTierChanceUpgradeCost(player));
        }
        return minimum == null ? BigInteger.valueOf(-1L) : minimum;
    }

    private static BigInteger getMinimumShearUpgradeCost(Player player) {
        if (player == null) {
            return BigInteger.valueOf(-1L);
        }
        BigInteger minimum = null;
        minimum = minPositive(minimum, getShearUpgradeCost(player));
        if (getShearWoolSaveLevel(player) < SHEAR_WOOL_SAVE_MAX_LEVEL) {
            minimum = minPositive(minimum, getShearWoolSaveUpgradeCost(player));
        }
        if (getShearTierBoostLevel(player) < SHEAR_TIER_BOOST_MAX_LEVEL) {
            minimum = minPositive(minimum, getShearTierBoostUpgradeCost(player));
        }
        return minimum == null ? BigInteger.valueOf(-1L) : minimum;
    }

    private static void maybeSendTutorialMergePointsReminder(Player player, long now) {
        if (player == null) {
            return;
        }
        UUID playerId = player.getUniqueId();
        TutorialStep step = getCurrentTutorialStep(player);
        BigInteger requiredPoints = getTutorialStepRequiredPoints(player, step);
        if (requiredPoints == null || requiredPoints.signum() <= 0) {
            return;
        }

        BigInteger currentPoints = getPlayerPointsBig(player);
        if (currentPoints.compareTo(requiredPoints) >= 0) {
            return;
        }

        long lastReminder = lastTutorialMergePointsReminderTimestampByPlayer.getOrDefault(playerId, 0L);
        if (now - lastReminder < TUTORIAL_MERGE_POINTS_REMINDER_REPEAT_MS) {
            return;
        }

        BigInteger missing = requiredPoints.subtract(currentPoints).max(BigInteger.ZERO);
        String taskLabel = getCurrentTutorialTaskLabel(step);
        lastTutorialMergePointsReminderTimestampByPlayer.put(playerId, now);
        player.sendMessage(warning("Need " + formatPoints(requiredPoints) + " points for: " + taskLabel));
        player.sendMessage(hint("You are short " + formatPoints(missing) + ". Merge sheep to gain points fast."));
    }

    private static BigInteger minPositive(BigInteger current, BigInteger candidate) {
        if (candidate == null || candidate.signum() <= 0) {
            return current;
        }
        if (current == null || candidate.compareTo(current) < 0) {
            return candidate;
        }
        return current;
    }

    private static boolean isTutorialActionAllowed(TutorialStep step, TutorialAction action) {
        if (step == TutorialStep.COMPLETE) {
            return true;
        }
        return switch (action) {
            case SPAWN_SHEEP -> step == TutorialStep.SPAWN;
            case SHEAR_SHEEP -> step == TutorialStep.SHEAR;
            case MERGE_SHEEP -> step == TutorialStep.MERGE;
            case OPEN_UPGRADE_COMMAND -> step == TutorialStep.OPEN_UPGRADES
                    || step == TutorialStep.BUY_REGULAR_UPGRADE
                    || step == TutorialStep.OPEN_QUESTS
                    || step == TutorialStep.USE_ABILITY
                    || step == TutorialStep.OPEN_QUEST_UPGRADES
                    || step == TutorialStep.BUY_SHEAR_UPGRADE
                    || step == TutorialStep.OPEN_PRESTIGE
                    || step == TutorialStep.PRESTIGE_ONCE;
            case OPEN_SHOP_COMMAND -> step == TutorialStep.BUY_SHEAR_UPGRADE;
            case OPEN_PRESTIGE_COMMAND -> step == TutorialStep.OPEN_PRESTIGE || step == TutorialStep.PRESTIGE_ONCE;
            case OTHER_COMMAND -> false;
        };
    }

    public static boolean blockTutorialAction(Player player, TutorialAction action, String attemptedAction) {
        if (!shouldRestrictTutorialActions(player)) {
            return false;
        }
        TutorialStep step = getCurrentTutorialStep(player);
        if (isTutorialActionAllowed(step, action)) {
            return false;
        }
        notifyTutorialOffTask(player, attemptedAction, step);
        return false;
    }

    private static boolean blockTutorialMenuPurchase(Player player, TutorialStep requiredStep, String requiredAction) {
        if (!isTutorialInProgress(player) || !isInTutorialWorld(player)) {
            return false;
        }
        if (getCurrentTutorialStep(player) == requiredStep) {
            return false;
        }
        notifyTutorialOffTask(player, requiredAction, getCurrentTutorialStep(player));
        return false;
    }

    private static void notifyTutorialOffTask(Player player, String attemptedAction, TutorialStep step) {
        if (player == null) {
            return;
        }
        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();
        long lastShownAt = lastTutorialFocusNotificationTimestampByPlayer.getOrDefault(playerId, 0L);
        if (now - lastShownAt < TUTORIAL_FOCUS_NOTIFICATION_COOLDOWN_MS) {
            return;
        }

        lastTutorialFocusNotificationTimestampByPlayer.put(playerId, now);
        String currentTask = getCurrentTutorialTaskLabel(step);
        if (attemptedAction != null && !attemptedAction.isBlank()) {
            player.sendMessage(warning("Not now: " + attemptedAction));
        }
        player.sendMessage(hint("Do this now: " + currentTask));
        showOverlay(player, warning("Tutorial step: " + currentTask));
        sendTutorialTitle(player, "&6Tutorial Focus", "&f" + currentTask);
    }

    private static void sendTutorialStatusFeed(Player player) {
        if (player == null) {
            return;
        }
        UUID playerId = player.getUniqueId();
        String progressLine = getTutorialProgressLine(player);
        String stepLine = getTutorialNextStepLine(player);
        long now = System.currentTimeMillis();
        String previousProgressLine = lastTutorialProgressFeedLineByPlayer.get(playerId);
        String previousStepLine = lastTutorialStepFeedLineByPlayer.get(playerId);
        long lastSentAt = lastTutorialStatusFeedTimestampByPlayer.getOrDefault(playerId, 0L);
        boolean changed = !progressLine.equals(previousProgressLine) || !stepLine.equals(previousStepLine);
        if (!changed && now - lastSentAt < TUTORIAL_STATUS_FEED_REPEAT_MS) {
            return;
        }

        lastTutorialStatusFeedTimestampByPlayer.put(playerId, now);
        lastTutorialProgressFeedLineByPlayer.put(playerId, progressLine);
        lastTutorialStepFeedLineByPlayer.put(playerId, stepLine);
        player.sendMessage(hint("Step: " + stepLine));
        player.sendMessage(accent(progressLine));
    }

    public static String getTutorialProgressLine(Player player) {
        return "Spawn " + getTutorialSpawnCount(player) + "/" + TUTORIAL_SPAWN_TARGET
                + " | Shear " + getTutorialShearCount(player) + "/" + TUTORIAL_SHEAR_TARGET
                + " | Merge " + getTutorialMergeCount(player) + "/" + TUTORIAL_MERGE_TARGET
                + " | Menus " + getTutorialSectionCount(player) + "/" + TUTORIAL_MENU_SECTION_TARGET;
    }

    private static void checkTutorialCompletion(Player player) {
        if (player == null || hasUnlockedFarm(player)) {
            return;
        }
        if (getTutorialShearCount(player) >= TUTORIAL_SHEAR_TARGET
                && getTutorialSpawnCount(player) >= TUTORIAL_SPAWN_TARGET
                && getTutorialMergeCount(player) >= TUTORIAL_MERGE_TARGET
                && getTutorialSectionCount(player) >= TUTORIAL_MENU_SECTION_TARGET) {
            UUID playerId = player.getUniqueId();
            tutorialCompletedByPlayer.put(playerId, true);
            clearTutorialRuntimeState(playerId);
            migrateTutorialSheepToFarmWorld(playerId);
            boolean teleported = SheepFarmWorldCommand.teleportToFarmWorld(player);
            if (teleported) {
                sendTutorialTitle(player, "&aTutorial Complete", "&fWelcome to your sheep farm");
                player.sendMessage(action("Tutorial complete! You were sent to your sheep farm."));
            } else {
                sendTutorialTitle(player, "&aTutorial Complete", "&fRun /sheepmerge to go to your farm");
                player.sendMessage(action("Tutorial complete! Run /sheepmerge to go to your farm."));
            }
            saveData();
        }
    }

    private static void migrateTutorialSheepToFarmWorld(UUID playerId) {
        if (playerId == null) {
            return;
        }

        World tutorialWorld = Bukkit.getWorld(getTutorialWorldName(playerId));
        if (tutorialWorld == null) {
            return;
        }
        savedFarmSheepByPlayer.put(playerId, captureSheepSnapshots(tutorialWorld));
        savedTutorialSheepByPlayer.remove(playerId);
        World fallbackWorld = plugin == null || plugin.getServer().getWorlds().isEmpty()
                ? null
                : plugin.getServer().getWorlds().get(0);
        teleportPlayersOutOfWorld(tutorialWorld, fallbackWorld);
        clearSheepEntities(tutorialWorld);
        tutorialWorld.save();
        SheepFarmWorldCleanupListener.deleteWorldByName(getTutorialWorldName(playerId), false, false);
    }

    public static boolean adminResetPlayer(Player player) {
        if (player == null) {
            return false;
        }
        UUID id = player.getUniqueId();
        savedFarmSheepByPlayer.remove(id);
        savedTutorialSheepByPlayer.remove(id);
        pointsByPlayer.remove(id);
        refreshTopPointsDisplays();
        extraLimitByPlayer.remove(id);
        eggSpeedLevelByPlayer.remove(id);
        woolRegenLevelByPlayer.remove(id);
        higherTierChanceLevelByPlayer.remove(id);
        prestigeLevelByPlayer.remove(id);
        prestigePointsByPlayer.remove(id);
        prestigeDoublePointsChanceByPlayer.remove(id);
        prestigeHigherMaxLevelByPlayer.remove(id);
        prestigeStartEggsByPlayer.remove(id);
        prestigeEggCapByPlayer.remove(id);
        prestigeBaseSpawnTierByPlayer.remove(id);
        nextPrestigeRefundTimestampByPlayer.remove(id);
        highestAnnouncedTierByPlayer.remove(id);
        highestAnnouncedRainbowTierByPlayer.remove(id);
        lastPrestigeReminderTimestampByPlayer.remove(id);
        shearShopLevelByPlayer.remove(id);
        shearWoolSaveLevelByPlayer.remove(id);
        shearTierBoostLevelByPlayer.remove(id);
        resetTutorialProgress(id);
        tutorialShearTaskRewardGrantedByPlayer.remove(id);
        tutorialPrestigePrepRewardGrantedByPlayer.remove(id);
        farmVisitEnabledByPlayer.remove(id);
        lastOutOfEggWarningTimestampByPlayer.remove(id);
        questPointsByPlayer.remove(id);
        nextQuestResetTimestampByPlayer.remove(id);
        questShearsByPlayer.remove(id);
        questSpawnsByPlayer.remove(id);
        questMergesByPlayer.remove(id);
        questShearsCompleteByPlayer.remove(id);
        questSpawnsCompleteByPlayer.remove(id);
        questMergesCompleteByPlayer.remove(id);
        questUpgradeDurationByPlayer.remove(id);
        questUpgradePowerByPlayer.remove(id);
        activeLuckyBurstUntilByPlayer.remove(id);
        activeWoolRushUntilByPlayer.remove(id);
        activeJackpotShearsUntilByPlayer.remove(id);
        activeAutoMergeUntilByPlayer.remove(id);
        pausedLuckyBurstRemainingMsByPlayer.remove(id);
        pausedWoolRushRemainingMsByPlayer.remove(id);
        pausedJackpotShearsRemainingMsByPlayer.remove(id);
        pausedAutoMergeRemainingMsByPlayer.remove(id);
        nextAutoMergeAtByPlayer.remove(id);
        activeAutoShearUntilByPlayer.remove(id);
        pausedAutoShearRemainingMsByPlayer.remove(id);
        nextAutoShearAtByPlayer.remove(id);
        EGG_MODULE.clearRuntimeState(id);
        lastSpawnLimitWarningTimestampByPlayer.remove(id);
        comboScoreByPlayer.remove(id);
        comboLastUpdateTimestampByPlayer.remove(id);
        comboDecayUpgradeByPlayer.remove(id);
        comboMaxUpgradeByPlayer.remove(id);
        comboGainUpgradeByPlayer.remove(id);
        automationPointsByPlayer.remove(id);
        automationAutoBuyUpgradeByPlayer.remove(id);
        automationAutoAbilityUpgradeByPlayer.remove(id);
        automationSlowAutoMergeUpgradeByPlayer.remove(id);
        automationSlowAutoShearUpgradeByPlayer.remove(id);
        automationAutoSpawnUpgradeByPlayer.remove(id);
        automationAutoPrestigeUpgradeByPlayer.remove(id);
        automationAutoBuyEnabledByPlayer.remove(id);
        automationAutoAbilityEnabledByPlayer.remove(id);
        automationSlowAutoMergeEnabledByPlayer.remove(id);
        automationSlowAutoShearEnabledByPlayer.remove(id);
        automationAutoSpawnEnabledByPlayer.remove(id);
        automationAutoPrestigeEnabledByPlayer.remove(id);
        scoreboardLayoutModeByPlayer.remove(id);
        scoreboardShowQuestPointsByPlayer.remove(id);
        scoreboardShowAutomationPointsByPlayer.remove(id);
        scoreboardShowSacrificePointsByPlayer.remove(id);
        scoreboardShowQuestProgressByPlayer.remove(id);
        scoreboardShowAbilityStatusByPlayer.remove(id);
        sacrificePointsByPlayer.remove(id);
        sacrificeUnlocksBoughtByPlayer.remove(id);
        nextAutomationPointAtByPlayer.remove(id);
        nextAutomationAutoBuyAtByPlayer.remove(id);
        nextAutomationAutoAbilityAtByPlayer.remove(id);
        nextAutomationSlowMergeAtByPlayer.remove(id);
        nextAutomationSlowShearAtByPlayer.remove(id);
        nextAutomationAutoSpawnAtByPlayer.remove(id);
        nextAutomationAutoPrestigeAtByPlayer.remove(id);
        lastPointsOverlayByPlayer.remove(id);
        pointsOverlayExpiresAtByPlayer.remove(id);
        removeComboBossBar(id);
        carriedSheepByPlayer.remove(id);
        resetFarmWorldForPlayer(id);
        saveData();
        return true;
    }

    private static void resetFarmWorldForPlayer(UUID playerId) {
        if (playerId == null) {
            return;
        }
        resetFarmWorldByName(SheepFarmWorldCommand.getWorldName(playerId));
        resetFarmWorldByName(getTutorialWorldName(playerId));
    }

    private static void resetFarmWorldByName(String worldName) {
        if (worldName == null || worldName.isBlank()) {
            return;
        }
        World world = Bukkit.getWorld(worldName);
        if (world == null || !isSheepFarmWorld(world)) {
            return;
        }
        rebuildFarmWorld(world);
    }

    public static void adminGivePoints(Player player, long amount) {
        if (player == null || amount == 0) {
            return;
        }
        UUID id = player.getUniqueId();
        pointsByPlayer.put(id, getPlayerPointsBig(player).add(BigInteger.valueOf(amount)).max(BigInteger.ZERO));
        refreshTopPointsDisplays();
        saveData();
    }

    public static void adminSetPoints(Player player, long amount) {
        if (player == null) {
            return;
        }
        pointsByPlayer.put(player.getUniqueId(), BigInteger.valueOf(Math.max(0L, amount)));
        refreshTopPointsDisplays();
        saveData();
    }

    public static void adminGiveQuestPoints(Player player, int amount) {
        if (player == null || amount == 0) {
            return;
        }
        UUID id = player.getUniqueId();
        questPointsByPlayer.put(id, addSaturated(questPointsByPlayer.getOrDefault(id, 0), amount));
        saveData();
    }

    public static void adminGiveAutomationPoints(Player player, int amount) {
        if (player == null || amount == 0) {
            return;
        }
        UUID id = player.getUniqueId();
        automationPointsByPlayer.put(id, addSaturated(automationPointsByPlayer.getOrDefault(id, 0), amount));
        saveData();
    }

    public static void adminSetQuestPoints(Player player, int amount) {
        if (player == null) {
            return;
        }
        questPointsByPlayer.put(player.getUniqueId(), Math.max(0, amount));
        saveData();
    }

    public static boolean adminSetPrestigeLevel(Player player, int targetLevel) {
        if (player == null || targetLevel < 0) {
            return false;
        }

        UUID playerId = player.getUniqueId();
        int totalEarnedPoints = getTotalPrestigePointsForLevel(targetLevel);
        int spentPoints = getPrestigeRefundAmount(player);
        int availablePoints = totalEarnedPoints - spentPoints;

        prestigeLevelByPlayer.put(playerId, targetLevel);
        clearPrestigeReminder(player);

        if (availablePoints < 0) {
            resetPrestigeUpgrades(playerId, true);
            availablePoints = totalEarnedPoints;
        }

        prestigePointsByPlayer.put(playerId, Math.max(0, availablePoints));
        saveData();
        return true;
    }

    public static SheepTier getSheepTier(Sheep sheep) {
        if (sheep == null) {
            return SheepTier.WHITE;
        }
        var container = sheep.getPersistentDataContainer();
        Integer level = container.get(getTierKey(), PersistentDataType.INTEGER);
        return SheepTier.byLevel(level == null ? 0 : level);
    }

    public static void setSheepTier(Sheep sheep, SheepTier tier) {
        if (sheep == null || tier == null) {
            return;
        }
        sheep.setRemoveWhenFarAway(false);
        sheep.setPersistent(true);
        sheep.setColor(tier.getColor() == null ? org.bukkit.DyeColor.WHITE : tier.getColor());
        sheep.getPersistentDataContainer().set(getTierKey(), PersistentDataType.INTEGER, tier.getLevel());
        if (tier == SheepTier.RAINBOW) {
            setRainbowTier(sheep, Math.max(1, getRainbowTier(sheep)));
        } else {
            sheep.getPersistentDataContainer().remove(getRainbowTierKey());
            sheep.getPersistentDataContainer().remove(getLegacyRainbowMergedCountKey());
        }
        if (sheep.isSheared()) {
            setNextEatTimestamp(sheep, System.currentTimeMillis() + getEatCooldownSeconds(sheep, tier) * 1000L);
        } else {
            setNextEatTimestamp(sheep, 0L);
        }
        applyRainbowColorAnimation(sheep, tier);
        updateSheepName(sheep);
    }

    public static int getEatCooldownSeconds(SheepTier tier) {
        if (tier == null) {
            return 10;
        }
        return 10 * (1 << tier.getLevel());
    }

    public static int getEatCooldownSeconds(Sheep sheep, SheepTier tier) {
        int baseSeconds = getEatCooldownSeconds(tier);
        if (sheep == null || sheep.getWorld() == null) {
            return baseSeconds;
        }
        int regenLevel = getWoolRegenLevel(sheep.getWorld());
        double multiplier = Math.pow(WOOL_REGEN_PER_LEVEL_MULTIPLIER, regenLevel);
        UUID ownerId = getOwnerId(sheep.getWorld());
        if (isAbilityActive(activeWoolRushUntilByPlayer, ownerId)) {
            multiplier *= 0.1D;
        }
        return Math.max(1, (int) Math.ceil(baseSeconds * multiplier));
    }

    private static void applyWoolRushToShearedSheep(Player player) {
        if (player == null || player.getWorld() == null) {
            return;
        }
        long now = System.currentTimeMillis();
        for (Sheep sheep : player.getWorld().getEntitiesByClass(Sheep.class)) {
            if (sheep == null || !sheep.isValid() || sheep.isDead() || !sheep.isSheared()) {
                continue;
            }
            long nextEatAt = getNextEatTimestamp(sheep);
            if (nextEatAt <= now) {
                continue;
            }
            long remainingMs = nextEatAt - now;
            long reducedMs = Math.max(1L, (long) Math.ceil(remainingMs * 0.1D));
            setNextEatTimestamp(sheep, now + reducedMs);
            updateSheepName(sheep);
        }
    }

    public static void processSheepEatTimer(Sheep sheep) {
        if (sheep == null || !sheep.isValid() || sheep.getWorld() == null
                || !isSheepFarmWorld(sheep.getWorld())) {
            return;
        }

        applySheepRescueMotionIfNeeded(sheep);
        SheepTier tier = getSheepTier(sheep);
        if (tier == SheepTier.RAINBOW) {
            applyRainbowColorAnimation(sheep, tier);
        } else if (tier != null && tier.getColor() != null && sheep.getColor() != tier.getColor()) {
            sheep.setColor(tier.getColor());
        }

        long now = System.currentTimeMillis();
        long nextEat = getNextEatTimestamp(sheep);

        if (!sheep.isSheared()) {
            if (nextEat > now) {
                sheep.setSheared(true);
            } else if (nextEat > 0L) {
                setNextEatTimestamp(sheep, 0L);
            }
            updateSheepName(sheep);
            return;
        }

        if (now >= nextEat && nextEat > 0L) {
            promptSheepToEatGrass(sheep);
            return;
        }
        sheep.setSheared(true);
        sheep.setGravity(true);
        sheep.setAI(true);
        updateSheepName(sheep);
    }

    private static void promptSheepToEatGrass(Sheep sheep) {
        if (sheep == null || sheep.getWorld() == null) {
            return;
        }

        setNextEatTimestamp(sheep, 0L);
        sheep.setAI(true);
        sheep.setGravity(true);
        sheep.setVelocity(new Vector(0.0D, 0.0D, 0.0D));

        org.bukkit.Location mouth = sheep.getLocation().add(0.0D, 0.35D, 0.0D);
        sheep.getWorld().spawnParticle(org.bukkit.Particle.CLOUD,
                mouth,
                10,
                0.18D,
                0.08D,
                0.18D,
                0.01D);
        sheep.getWorld().playSound(mouth, Sound.ENTITY_SHEEP_AMBIENT, 0.75f, 1.05f);

        sheep.setSheared(false);
        updateSheepName(sheep);
    }

    private static boolean applySheepRescueMotionIfNeeded(Sheep sheep) {
        if (sheep == null || !sheep.isValid()) {
            return false;
        }

        UUID sheepId = sheep.getUniqueId();
        org.bukkit.Location location = sheep.getLocation();
        boolean rescueInProgress = sheepRescueStartByEntity.containsKey(sheepId);
        boolean shouldRescue = rescueInProgress
                ? !isSafelyOnPlatform(sheep, location)
                : (isOffPlatform(location) || isFallingOffPlatform(sheep, location));
        if (!shouldRescue) {
            clearSheepRescueState(sheepId);
            sheep.setCollidable(true);
            return false;
        }

        // Let rescued sheep phase through collisions while returning to center.
        sheep.setCollidable(false);
        sheep.setAI(true);
        sheep.setGravity(true);

        long now = System.currentTimeMillis();
        long started = sheepRescueStartByEntity.computeIfAbsent(sheepId, key -> now);
        sheepRescueOriginByEntity.computeIfAbsent(sheepId, key -> location.clone());
        sheepRescueNextCorrectionAtByEntity.putIfAbsent(sheepId, now);

        if (now - started >= SHEEP_RESCUE_TIMEOUT_MS) {
            teleportSheepToFarmCenter(sheep);
            clearSheepRescueState(sheepId);
            sheep.setCollidable(true);
            return false;
        }

        org.bukkit.Location origin = sheepRescueOriginByEntity.getOrDefault(sheepId, location.clone());
        org.bukkit.Location desired = getRescuePathTargetLocation(sheep, origin, started, now);
        Vector steeringVelocity = getRescueSteeringVelocity(location, desired);
        sheep.setVelocity(steeringVelocity);

        long nextCorrectionAt = sheepRescueNextCorrectionAtByEntity.getOrDefault(sheepId, now);
        if (now >= nextCorrectionAt) {
            if (location.distanceSquared(desired) >= SHEEP_RESCUE_POSITION_CORRECTION_DISTANCE
                    * SHEEP_RESCUE_POSITION_CORRECTION_DISTANCE) {
                sheep.teleport(desired);
            }
            sheepRescueNextCorrectionAtByEntity.put(sheepId, now + SHEEP_RESCUE_CORRECTION_INTERVAL_MS);
        }

        sheep.setFallDistance(0.0F);
        return true;
    }

    private static org.bukkit.Location getRescuePathTargetLocation(Sheep sheep, org.bukkit.Location origin,
            long started,
            long now) {
        org.bukkit.Location center = new org.bukkit.Location(
                sheep.getWorld(),
                FARM_CENTER_X,
                FARM_BASE_Y + 1.0D,
                FARM_CENTER_Z,
                sheep.getLocation().getYaw(),
                sheep.getLocation().getPitch());

        if (origin == null) {
            return center;
        }

        double progress = Math.min(1.0D, Math.max(0.0D, (now - started) / (double) SHEEP_RESCUE_PATH_DURATION_MS));
        double dx = center.getX() - origin.getX();
        double dz = center.getZ() - origin.getZ();
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        double archHeight = Math.min(
                SHEEP_RESCUE_ARCH_HEIGHT_MAX,
                SHEEP_RESCUE_ARCH_HEIGHT_BASE + horizontalDistance * SHEEP_RESCUE_ARCH_HEIGHT_PER_BLOCK);

        double x = lerp(origin.getX(), center.getX(), progress);
        double z = lerp(origin.getZ(), center.getZ(), progress);
        double linearY = lerp(origin.getY(), center.getY(), progress);
        double y = linearY + Math.sin(Math.PI * progress) * archHeight;
        return new org.bukkit.Location(sheep.getWorld(), x, y, z, center.getYaw(), center.getPitch());
    }

    private static Vector getRescueSteeringVelocity(org.bukkit.Location current, org.bukkit.Location target) {
        double correctionHorizonSeconds = SHEEP_RESCUE_CORRECTION_INTERVAL_MS / 1000.0D;
        Vector toTarget = target.toVector().subtract(current.toVector());
        Vector horizontal = new Vector(toTarget.getX(), 0.0D, toTarget.getZ());
        double horizontalLength = horizontal.length();
        if (horizontalLength > 0.0001D) {
            horizontal.normalize().multiply(Math.min(
                    SHEEP_RESCUE_HORIZONTAL_VELOCITY,
                    horizontalLength / Math.max(0.001D, correctionHorizonSeconds)));
        } else {
            horizontal.zero();
        }

        double yVelocity = toTarget.getY() / Math.max(0.001D, correctionHorizonSeconds);
        yVelocity = Math.max(SHEEP_RESCUE_DOWNWARD_VELOCITY, Math.min(SHEEP_RESCUE_UPWARD_VELOCITY, yVelocity));
        return new Vector(horizontal.getX(), yVelocity, horizontal.getZ());
    }

    private static double lerp(double start, double end, double progress) {
        return start + (end - start) * progress;
    }

    private static void clearSheepRescueState(UUID sheepId) {
        sheepRescueStartByEntity.remove(sheepId);
        sheepRescueOriginByEntity.remove(sheepId);
        sheepRescueNextCorrectionAtByEntity.remove(sheepId);
    }

    private static boolean isSafelyOnPlatform(Sheep sheep, org.bukkit.Location location) {
        if (sheep == null || location == null) {
            return false;
        }
        if (!sheep.isOnGround()) {
            return false;
        }
        if (location.getY() < FARM_BASE_Y - 0.05D) {
            return false;
        }
        return !isOutsideFarmHorizontalBounds(location, -0.20D);
    }

    private static void applyRainbowColorAnimation(Sheep sheep, SheepTier tier) {
        if (sheep == null || tier != SheepTier.RAINBOW || RAINBOW_ANIMATION_COLORS.length == 0) {
            return;
        }

        long now = System.currentTimeMillis();
        int index = (int) ((now / RAINBOW_ANIMATION_STEP_MS) % RAINBOW_ANIMATION_COLORS.length);
        sheep.setColor(RAINBOW_ANIMATION_COLORS[index]);
    }

    private static void teleportSheepToFarmCenter(Sheep sheep) {
        if (sheep == null || !sheep.isValid() || sheep.getWorld() == null) {
            return;
        }
        org.bukkit.Location target = new org.bukkit.Location(
                sheep.getWorld(),
                FARM_CENTER_X,
                FARM_BASE_Y + 1.0D,
                FARM_CENTER_Z,
                sheep.getLocation().getYaw(),
                sheep.getLocation().getPitch());
        sheep.teleport(target);
        sheep.setVelocity(new Vector(0.0D, 0.0D, 0.0D));
        sheep.setFallDistance(0.0F);
    }

    private static boolean isFallingOffPlatform(Sheep sheep, org.bukkit.Location location) {
        if (sheep == null || location == null) {
            return false;
        }
        if (location.getY() > SHEEP_FALL_TRIGGER_Y) {
            return false;
        }
        boolean nearEdge = isOutsideFarmHorizontalBounds(location, -SHEEP_FALL_TRIGGER_EDGE_MARGIN);
        if (!nearEdge) {
            return false;
        }
        return sheep.getVelocity().getY() < -0.02D;
    }

    private static boolean isOffPlatform(org.bukkit.Location location) {
        if (location == null) {
            return false;
        }
        if (location.getY() < FARM_BASE_Y - 0.05D) {
            return true;
        }
        return isOutsideFarmHorizontalBounds(location, SHEEP_RESCUE_EDGE_MARGIN);
    }

    private static boolean isOutsideFarmHorizontalBounds(org.bukkit.Location location, double edgeMargin) {
        if (location == null) {
            return false;
        }
        double minBound = FARM_MIN_XZ - 0.5D - edgeMargin;
        double maxBound = FARM_MAX_XZ + 0.5D + edgeMargin;
        return location.getX() < minBound
                || location.getX() > maxBound
                || location.getZ() < minBound
                || location.getZ() > maxBound;
    }

    public static long getNextEatTimestamp(Sheep sheep) {
        if (sheep == null) {
            return 0L;
        }
        Long value = sheep.getPersistentDataContainer().get(getNextEatKey(), PersistentDataType.LONG);
        return value == null ? 0L : value;
    }

    public static void setNextEatTimestamp(Sheep sheep, long timestamp) {
        if (sheep == null) {
            return;
        }
        sheep.getPersistentDataContainer().set(getNextEatKey(), PersistentDataType.LONG, timestamp);
    }

    public static void updateSheepName(Sheep sheep) {
        if (sheep == null) {
            return;
        }
        SheepTier tier = getSheepTier(sheep);
        String name = getTierDisplayNameWithColor(tier);
        if (tier == SheepTier.RAINBOW) {
            name += ChatColor.WHITE + " T" + formatPoints(getRainbowTier(sheep));
        }
        if (sheep.isSheared()) {
            long remainingSeconds = Math.max(0L,
                    (getNextEatTimestamp(sheep) - System.currentTimeMillis() + 999L) / 1000L);
            name += ChatColor.YELLOW + " [" + remainingSeconds + "s]";
        }
        sheep.setCustomName(name);
        sheep.setCustomNameVisible(true);
    }

    private static String getTierDisplayNameWithColor(SheepTier tier) {
        if (tier == null) {
            return ChatColor.WHITE + "White Sheep";
        }
        ChatColor color = switch (tier) {
            case WHITE, LIGHT_GRAY -> ChatColor.WHITE;
            case ORANGE -> ChatColor.GOLD;
            case MAGENTA, PINK -> ChatColor.LIGHT_PURPLE;
            case LIGHT_BLUE, CYAN -> ChatColor.AQUA;
            case YELLOW -> ChatColor.YELLOW;
            case LIME, GREEN -> ChatColor.GREEN;
            case GRAY -> ChatColor.DARK_GRAY;
            case PURPLE -> ChatColor.DARK_PURPLE;
            case BLUE -> ChatColor.BLUE;
            case BROWN -> ChatColor.GOLD;
            case RED -> ChatColor.RED;
            case BLACK -> ChatColor.BLACK;
            case RAINBOW -> ChatColor.LIGHT_PURPLE;
        };
        return color + tier.getDisplayName();
    }

    private static BigInteger getStartingPointsBig() {
        return BigInteger.valueOf(Math.max(0L, STARTING_PLAYER_POINTS));
    }

    public static BigInteger getPlayerPointsBig(Player player) {
        if (player == null) {
            return BigInteger.ZERO;
        }
        return pointsByPlayer.getOrDefault(player.getUniqueId(), getStartingPointsBig());
    }

    public static long getPlayerPoints(Player player) {
        BigInteger points = getPlayerPointsBig(player);
        if (points.signum() <= 0) {
            return 0L;
        }
        if (points.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0) {
            return Long.MAX_VALUE;
        }
        return points.longValue();
    }

    public static int calculateShearPoints(Player player, SheepTier tier) {
        BigInteger points = calculateShearPointsBig(player, tier, null);
        if (points.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0) {
            return Integer.MAX_VALUE;
        }
        return Math.max(1, points.intValue());
    }

    public static BigInteger calculateShearPointsBig(Player player, SheepTier tier, Sheep sheep) {
        BigInteger base;
        if (tier == SheepTier.RAINBOW) {
            int rainbowTier = sheep == null ? 1 : getRainbowTier(sheep);
            int effectiveLevel = Math.max(0, SheepTier.RAINBOW.getLevel() + rainbowTier - 1);
            base = BigInteger.valueOf(4L).pow(effectiveLevel);
        } else {
            int level = tier == null ? 0 : Math.max(0, tier.getLevel());
            base = BigInteger.valueOf(4L).pow(level);
        }
        BigInteger points = base.multiply(BigInteger.valueOf(Math.max(1, getShearPointMultiplier(player))));
        if (isAbilityActive(activeJackpotShearsUntilByPlayer, player == null ? null : player.getUniqueId())) {
            points = points.multiply(BigInteger.valueOf(2L + getQuestUpgradePowerLevel(player)));
        }
        if (RANDOM.nextInt(100) < getDoublePointsChancePercent(player)) {
            points = points.multiply(BigInteger.TWO);
        }
        return points.max(BigInteger.ONE);
    }

    public static int getRainbowTier(Sheep sheep) {
        if (sheep == null || getSheepTier(sheep) != SheepTier.RAINBOW) {
            return 1;
        }
        Integer value = sheep.getPersistentDataContainer().get(getRainbowTierKey(), PersistentDataType.INTEGER);
        if (value == null) {
            value = sheep.getPersistentDataContainer().get(getLegacyRainbowMergedCountKey(),
                    PersistentDataType.INTEGER);
        }
        return value == null ? 1 : Math.max(1, value);
    }

    public static void setRainbowTier(Sheep sheep, int tier) {
        if (sheep == null) {
            return;
        }
        sheep.getPersistentDataContainer().set(getRainbowTierKey(), PersistentDataType.INTEGER,
                Math.max(1, tier));
        sheep.getPersistentDataContainer().remove(getLegacyRainbowMergedCountKey());
        updateSheepName(sheep);
    }

    public static String formatRainbowTier(int tier) {
        return "T" + formatPoints(Math.max(1L, tier));
    }

    public static boolean shearSheepForPlayer(Player player, Sheep sheep) {
        if (player == null || sheep == null || sheep.getWorld() == null || !isSheepFarmWorld(sheep.getWorld())) {
            return false;
        }
        if (!sheep.isAdult()) {
            return false;
        }
        long now = System.currentTimeMillis();
        long nextEatAt = getNextEatTimestamp(sheep);
        if (nextEatAt > now) {
            sheep.setSheared(true);
            updateSheepName(sheep);
            return false;
        }
        if (sheep.isSheared()) {
            return false;
        }

        sheep.setSheared(true);
        sheep.setAI(true);
        SheepTier tier = getSheepTier(sheep);
        setNextEatTimestamp(sheep,
                System.currentTimeMillis() + getEatCooldownSeconds(sheep, tier) * 1000L);

        BigInteger points = calculateShearPointsBig(player, tier, sheep);
        addPoints(player, points);
        tryTriggerShearWoolSave(player, sheep);
        tryTriggerShearTierBoost(player, sheep);
        updateSheepName(sheep);
        recordQuestShear(player);
        recordTutorialShear(player);
        updatePointsScoreboard(player);
        return true;
    }

    public static int getWoolDropAmount(Player player) {
        return 1 + getShearShopLevel(player);
    }

    public static boolean tryTriggerShearWoolSave(Player player, Sheep sheep) {
        if (player == null || sheep == null) {
            return false;
        }
        int chance = getShearWoolSaveChancePercent(player);
        if (chance <= 0 || RANDOM.nextInt(100) >= chance) {
            return false;
        }

        sheep.setSheared(false);
        setNextEatTimestamp(sheep, 0L);
        updateSheepName(sheep);
        showOverlay(player, accent("Wool Keeper triggered: wool preserved"));
        playSound(player, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.9f, 1.35f);
        return true;
    }

    public static boolean tryTriggerShearTierBoost(Player player, Sheep sheep) {
        if (player == null || sheep == null) {
            return false;
        }
        int chance = getShearTierBoostChancePercent(player);
        if (chance <= 0 || RANDOM.nextInt(100) >= chance) {
            return false;
        }

        SheepTier currentTier = getSheepTier(sheep);
        if (currentTier == null || !currentTier.hasNext()) {
            return false;
        }

        SheepTier upgradedTier = currentTier.next();
        setSheepTier(sheep, upgradedTier);
        sheep.getWorld().spawnParticle(org.bukkit.Particle.VILLAGER_HAPPY,
                sheep.getLocation().add(0, 0.7, 0), 12, 0.25, 0.2, 0.25, 0.02);
        showOverlay(player, accent("Tier Booster triggered: " + currentTier.getDisplayName()
                + color(" &7-> ") + upgradedTier.getDisplayName()));
        playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.9f, 1.45f);
        return true;
    }

    public static String buildTopPointsText(int maxEntries) {
        StringBuilder builder = new StringBuilder("Top Sheep Merge Points");
        pointsByPlayer.entrySet().stream()
                .sorted((left, right) -> {
                    int pointsCompare = right.getValue().compareTo(left.getValue());
                    if (pointsCompare != 0) {
                        return pointsCompare;
                    }

                    String leftName = Bukkit.getOfflinePlayer(left.getKey()).getName();
                    String rightName = Bukkit.getOfflinePlayer(right.getKey()).getName();
                    String leftSafeName = leftName == null || leftName.isBlank()
                            ? left.getKey().toString().substring(0, 8)
                            : leftName;
                    String rightSafeName = rightName == null || rightName.isBlank()
                            ? right.getKey().toString().substring(0, 8)
                            : rightName;

                    int nameCompare = leftSafeName.compareToIgnoreCase(rightSafeName);
                    if (nameCompare != 0) {
                        return nameCompare;
                    }
                    return left.getKey().compareTo(right.getKey());
                })
                .limit(Math.max(1, maxEntries))
                .forEach(entry -> {
                    String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
                    if (name == null || name.isBlank()) {
                        name = entry.getKey().toString().substring(0, 8);
                    }
                    builder.append("\n").append(name).append(": ").append(formatPoints(entry.getValue()));
                });
        if (builder.toString().equals("Top Sheep Merge Points")) {
            builder.append("\nNo scores yet");
        }
        return builder.toString();
    }

    public static List<String> getTopPointsLines(int maxEntries) {
        List<String> lines = new ArrayList<>();
        final int limit = Math.max(1, maxEntries);
        List<Map.Entry<UUID, BigInteger>> entries = pointsByPlayer.entrySet().stream()
                .sorted((left, right) -> {
                    int pointsCompare = right.getValue().compareTo(left.getValue());
                    if (pointsCompare != 0) {
                        return pointsCompare;
                    }

                    String leftName = Bukkit.getOfflinePlayer(left.getKey()).getName();
                    String rightName = Bukkit.getOfflinePlayer(right.getKey()).getName();
                    String leftSafeName = leftName == null || leftName.isBlank()
                            ? left.getKey().toString().substring(0, 8)
                            : leftName;
                    String rightSafeName = rightName == null || rightName.isBlank()
                            ? right.getKey().toString().substring(0, 8)
                            : rightName;

                    int nameCompare = leftSafeName.compareToIgnoreCase(rightSafeName);
                    if (nameCompare != 0) {
                        return nameCompare;
                    }
                    return left.getKey().compareTo(right.getKey());
                })
                .limit(limit)
                .toList();

        int rank = 1;
        for (Map.Entry<UUID, BigInteger> entry : entries) {
            String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
            if (name == null || name.isBlank()) {
                name = entry.getKey().toString().substring(0, 8);
            }
            lines.add(rank + ". " + name + " - " + formatPoints(entry.getValue()));
            rank++;
        }
        if (lines.isEmpty()) {
            lines.add("No scores yet.");
        }
        return lines;
    }

    public static boolean spawnOrMoveTopPointsDisplay(Player player) {
        if (player == null || player.getWorld() == null) {
            return false;
        }

        return spawnOrMoveTopPointsDisplay(player.getLocation().clone().add(0, 2.2, 0));
    }

    public static boolean spawnOrMoveTopPointsDisplay(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }

        removeNearbyUnmarkedTopPointsTextDisplays(location);
        removeLegacyTopPointsArmorStands(location);
        saveTopPointsDisplayLocation(location);
        TextDisplay display = ensureTopPointsDisplay(location);
        configureTopPointsDisplay(display);
        refreshTopPointsDisplays();
        return true;
    }

    public static boolean removeTopPointsDisplay() {
        List<TextDisplay> displays = findTopPointsDisplays();
        if (displays.isEmpty() && !hasSavedTopPointsDisplayLocation()) {
            return false;
        }

        for (TextDisplay display : displays) {
            if (display != null) {
                display.remove();
            }
        }

        clearTopPointsDisplayLocation();
        saveData();
        return true;
    }

    public static void restoreTopPointsDisplayIfPossible() {
        Location savedLocation = getSavedTopPointsDisplayLocation();
        if (savedLocation == null || savedLocation.getWorld() == null) {
            return;
        }
        ensureTopPointsDisplay(savedLocation);
        refreshTopPointsDisplays();
    }

    public static void restoreTopPointsDisplayAfterRestart(World loadedWorld) {
        if (plugin == null || dataConfig == null) {
            return;
        }

        String savedWorldName = dataConfig.getString(TOP_POINTS_DISPLAY_WORLD_KEY, null);
        if (savedWorldName == null || savedWorldName.isBlank()) {
            return;
        }

        if (loadedWorld != null && !savedWorldName.equals(loadedWorld.getName())) {
            return;
        }

        World targetWorld = Bukkit.getWorld(savedWorldName);
        if (targetWorld == null) {
            return;
        }

        Location savedLocation = getSavedTopPointsDisplayLocation();
        if (savedLocation == null) {
            return;
        }

        removeTopPointsDisplaysAtSavedLocation(savedLocation);

        for (TextDisplay display : findTopPointsDisplays()) {
            if (display != null) {
                display.remove();
            }
        }

        TextDisplay restored = targetWorld.spawn(savedLocation, TextDisplay.class);
        restored.getPersistentDataContainer().set(getTopPointsDisplayKey(), PersistentDataType.BYTE, (byte) 1);
        configureTopPointsDisplay(restored);
        restored.setText(buildTopPointsText(10));
    }

    public static void reconcileTopPointsDisplayForChunk(World world, int chunkX, int chunkZ) {
        if (!isSavedTopPointsDisplayChunk(world, chunkX, chunkZ)) {
            return;
        }
        restoreTopPointsDisplayAfterRestart(world);
    }

    public static void reconcileTopPointsDisplayForLocation(Location location) {
        if (location == null || location.getWorld() == null) {
            return;
        }
        reconcileTopPointsDisplayForChunk(location.getWorld(), location.getChunk().getX(), location.getChunk().getZ());
    }

    private static boolean isSavedTopPointsDisplayChunk(World world, int chunkX, int chunkZ) {
        if (world == null || dataConfig == null) {
            return false;
        }

        String savedWorldName = dataConfig.getString(TOP_POINTS_DISPLAY_WORLD_KEY, null);
        if (savedWorldName == null || savedWorldName.isBlank() || !savedWorldName.equals(world.getName())) {
            return false;
        }

        double savedX = dataConfig.getDouble(TOP_POINTS_DISPLAY_X_KEY, 0.0D);
        double savedZ = dataConfig.getDouble(TOP_POINTS_DISPLAY_Z_KEY, 0.0D);
        int savedChunkX = (int) Math.floor(savedX / 16.0D);
        int savedChunkZ = (int) Math.floor(savedZ / 16.0D);
        return savedChunkX == chunkX && savedChunkZ == chunkZ;
    }

    private static void refreshTopPointsDisplays() {
        String topPointsText = buildTopPointsText(10);
        Location savedLocation = getSavedTopPointsDisplayLocation();
        if (savedLocation != null && savedLocation.getWorld() != null) {
            removeNearbyUnmarkedTopPointsTextDisplays(savedLocation);
        }
        List<TextDisplay> displays = findTopPointsDisplays();
        if (displays.isEmpty()) {
            TextDisplay restored = ensureTopPointsDisplay(savedLocation);
            if (restored != null) {
                displays.add(restored);
            }
        }
        for (TextDisplay display : displays) {
            configureTopPointsDisplay(display);
            display.setText(topPointsText);
        }
    }

    private static void configureTopPointsDisplay(TextDisplay display) {
        if (display == null) {
            return;
        }
        display.setBillboard(Display.Billboard.CENTER);
        display.setSeeThrough(true);
        display.setDefaultBackground(false);
        display.setShadowed(true);
        display.setLineWidth(260);
    }

    private static TextDisplay ensureTopPointsDisplay(Location location) {
        if (location == null || location.getWorld() == null) {
            return null;
        }

        List<TextDisplay> displays = findTopPointsDisplays();
        TextDisplay display = displays.isEmpty() ? null : displays.get(0);
        if (display == null) {
            display = location.getWorld().spawn(location, TextDisplay.class);
            display.getPersistentDataContainer().set(getTopPointsDisplayKey(), PersistentDataType.BYTE, (byte) 1);
        } else {
            display.teleport(location);
        }

        for (int index = 1; index < displays.size(); index++) {
            displays.get(index).remove();
        }

        return display;
    }

    private static List<TextDisplay> findTopPointsDisplays() {
        List<TextDisplay> displays = new ArrayList<>();
        if (plugin == null || plugin.getServer() == null) {
            return displays;
        }
        for (World world : plugin.getServer().getWorlds()) {
            for (TextDisplay display : world.getEntitiesByClass(TextDisplay.class)) {
                Byte marker = display.getPersistentDataContainer().get(getTopPointsDisplayKey(),
                        PersistentDataType.BYTE);
                if (marker != null && marker == (byte) 1) {
                    displays.add(display);
                }
            }
        }
        return displays;
    }

    private static void removeTopPointsDisplaysAtSavedLocation(Location savedLocation) {
        if (savedLocation == null || savedLocation.getWorld() == null) {
            return;
        }

        World world = savedLocation.getWorld();
        Chunk chunk = world.getChunkAt(savedLocation);
        if (!chunk.isLoaded()) {
            chunk.load();
        }

        for (TextDisplay display : world.getEntitiesByClass(TextDisplay.class)) {
            if (display == null || !display.isValid()) {
                continue;
            }

            Location displayLocation = display.getLocation();
            if (displayLocation == null || displayLocation.getWorld() == null
                    || !displayLocation.getWorld().equals(world)) {
                continue;
            }

            double dx = Math.abs(displayLocation.getX() - savedLocation.getX());
            double dy = Math.abs(displayLocation.getY() - savedLocation.getY());
            double dz = Math.abs(displayLocation.getZ() - savedLocation.getZ());
            boolean samePlacement = dx <= 1.25D && dy <= 6.0D && dz <= 1.25D;
            if (!samePlacement) {
                continue;
            }

            // At the saved spawn point, treat all text displays as stale leaderboard
            // artifacts.
            display.remove();
        }

        for (Entity entity : chunk.getEntities()) {
            if (!(entity instanceof TextDisplay display) || !display.isValid()) {
                continue;
            }

            Location displayLocation = display.getLocation();
            if (displayLocation == null || displayLocation.getWorld() == null
                    || !displayLocation.getWorld().equals(world)) {
                continue;
            }

            double dx = Math.abs(displayLocation.getX() - savedLocation.getX());
            double dy = Math.abs(displayLocation.getY() - savedLocation.getY());
            double dz = Math.abs(displayLocation.getZ() - savedLocation.getZ());
            boolean nearSavedPlacement = dx <= 1.25D && dy <= 6.0D && dz <= 1.25D;
            if (!nearSavedPlacement) {
                continue;
            }

            display.remove();
        }

        removeLegacyTopPointsArmorStands(savedLocation);
    }

    private static void removeNearbyUnmarkedTopPointsTextDisplays(Location location) {
        if (location == null || location.getWorld() == null) {
            return;
        }

        World world = location.getWorld();
        Collection<Entity> nearby = world.getNearbyEntities(location, 1.25D, 6.0D, 1.25D);
        for (Entity entity : nearby) {
            if (!(entity instanceof TextDisplay display) || !display.isValid()) {
                continue;
            }

            Byte marker = display.getPersistentDataContainer().get(getTopPointsDisplayKey(), PersistentDataType.BYTE);
            if (marker != null && marker == (byte) 1) {
                continue;
            }

            String text = display.getText();
            String stripped = text == null ? null : ChatColor.stripColor(text);
            if (stripped != null && stripped.toLowerCase(Locale.ROOT).contains("top sheep merge points")) {
                display.remove();
            }
        }
    }

    private static void removeLegacyTopPointsArmorStands(Location savedLocation) {
        if (savedLocation == null || savedLocation.getWorld() == null) {
            return;
        }

        World world = savedLocation.getWorld();
        Collection<Entity> nearby = world.getNearbyEntities(savedLocation, 1.25D, 6.0D, 1.25D);
        boolean hasLegacyHeader = false;
        for (Entity entity : nearby) {
            if (!(entity instanceof ArmorStand armorStand)) {
                continue;
            }
            String customName = armorStand.getCustomName();
            String stripped = customName == null ? null : ChatColor.stripColor(customName);
            if (stripped != null && stripped.toLowerCase(Locale.ROOT).contains("top sheep merge points")) {
                hasLegacyHeader = true;
                break;
            }
        }

        if (!hasLegacyHeader) {
            return;
        }

        for (Entity entity : nearby) {
            if (!(entity instanceof ArmorStand armorStand)) {
                continue;
            }
            if (armorStand.getCustomName() == null || armorStand.getCustomName().isBlank()) {
                continue;
            }
            armorStand.remove();
        }
    }

    private static void saveTopPointsDisplayLocation(Location location) {
        if (plugin == null || location == null || location.getWorld() == null) {
            return;
        }
        if (dataConfig == null) {
            dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        }
        dataConfig.set(TOP_POINTS_DISPLAY_WORLD_KEY, location.getWorld().getName());
        dataConfig.set(TOP_POINTS_DISPLAY_X_KEY, location.getX());
        dataConfig.set(TOP_POINTS_DISPLAY_Y_KEY, location.getY());
        dataConfig.set(TOP_POINTS_DISPLAY_Z_KEY, location.getZ());
        dataConfig.set(TOP_POINTS_DISPLAY_YAW_KEY, location.getYaw());
        dataConfig.set(TOP_POINTS_DISPLAY_PITCH_KEY, location.getPitch());
        saveData();
    }

    private static void clearTopPointsDisplayLocation() {
        if (dataConfig == null) {
            return;
        }
        dataConfig.set(TOP_POINTS_DISPLAY_WORLD_KEY, null);
        dataConfig.set(TOP_POINTS_DISPLAY_X_KEY, null);
        dataConfig.set(TOP_POINTS_DISPLAY_Y_KEY, null);
        dataConfig.set(TOP_POINTS_DISPLAY_Z_KEY, null);
        dataConfig.set(TOP_POINTS_DISPLAY_YAW_KEY, null);
        dataConfig.set(TOP_POINTS_DISPLAY_PITCH_KEY, null);
    }

    private static boolean hasSavedTopPointsDisplayLocation() {
        return dataConfig != null && dataConfig.contains(TOP_POINTS_DISPLAY_WORLD_KEY);
    }

    private static Location getSavedTopPointsDisplayLocation() {
        if (plugin == null || dataConfig == null) {
            return null;
        }
        String worldName = dataConfig.getString(TOP_POINTS_DISPLAY_WORLD_KEY, null);
        if (worldName == null || worldName.isBlank()) {
            return null;
        }
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }
        double x = dataConfig.getDouble(TOP_POINTS_DISPLAY_X_KEY, 0.0D);
        double y = dataConfig.getDouble(TOP_POINTS_DISPLAY_Y_KEY, 0.0D);
        double z = dataConfig.getDouble(TOP_POINTS_DISPLAY_Z_KEY, 0.0D);
        float yaw = (float) dataConfig.getDouble(TOP_POINTS_DISPLAY_YAW_KEY, 0.0D);
        float pitch = (float) dataConfig.getDouble(TOP_POINTS_DISPLAY_PITCH_KEY, 0.0D);
        return new Location(world, x, y, z, yaw, pitch);
    }

    public static synchronized File createBackup(boolean permanent, String trigger) {
        if (plugin == null) {
            return null;
        }

        captureLiveSheepSnapshotsForLoadedWorlds();
        saveData();
        if (hasSavedFarmLayout()) {
            saveFarmLayout();
        }

        File backupDir = new File(plugin.getDataFolder(), BACKUP_DIR_NAME);
        if (!backupDir.exists() && !backupDir.mkdirs()) {
            return null;
        }

        if (permanent) {
            String timestamp = BACKUP_TIMESTAMP_FORMATTER.format(Instant.now());
            String suffix = trigger == null || trigger.isBlank() ? "manual" : sanitizeBackupToken(trigger);
            File destination = new File(backupDir, "permanent-" + timestamp + "-" + suffix + ".zip");
            if (!writeBackupArchive(destination)) {
                return null;
            }
            markLastPermanentBackupNow();
            return destination;
        }

        File rolling = new File(backupDir, BACKUP_ROLLING_FILE_NAME);
        if (!writeBackupArchive(rolling)) {
            return null;
        }
        return rolling;
    }

    public static synchronized boolean maybeCreateAutomaticBackup(String trigger) {
        if (plugin == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        long lastPermanentAt = getLastPermanentBackupAt();
        long lastBufferAt = getLastBufferBackupAt();
        boolean duePermanent = now - lastPermanentAt >= BACKUP_AUTOMATIC_PERMANENT_INTERVAL_MS;
        if (duePermanent) {
            return createBackup(true, trigger == null ? "auto-weekly" : trigger + "-weekly") != null;
        }

        boolean dueBuffer = now - lastBufferAt >= BACKUP_AUTOMATIC_BUFFER_INTERVAL_MS;
        if (dueBuffer) {
            return createBufferBackup(trigger == null ? "auto-24h" : trigger + "-24h") != null;
        }

        return createBackup(false, trigger == null ? "auto" : trigger + "-rolling") != null;
    }

    public static long getAutomaticBackupIntervalTicks() {
        return BACKUP_AUTOMATIC_ROLLING_INTERVAL_TICKS;
    }

    public static synchronized File createManualBackup() {
        return createBackup(true, "manual");
    }

    private static synchronized File createBufferBackup(String trigger) {
        if (plugin == null) {
            return null;
        }

        captureLiveSheepSnapshotsForLoadedWorlds();
        saveData();
        if (hasSavedFarmLayout()) {
            saveFarmLayout();
        }

        File backupDir = new File(plugin.getDataFolder(), BACKUP_DIR_NAME);
        if (!backupDir.exists() && !backupDir.mkdirs()) {
            return null;
        }

        String timestamp = BACKUP_TIMESTAMP_FORMATTER.format(Instant.now());
        String suffix = trigger == null || trigger.isBlank() ? "auto-24h" : sanitizeBackupToken(trigger);
        File destination = new File(backupDir, BACKUP_BUFFER_FILE_PREFIX + timestamp + "-" + suffix + ".zip");
        if (!writeBackupArchive(destination)) {
            return null;
        }

        markLastBufferBackupNow();
        pruneBufferBackups(backupDir);
        return destination;
    }

    private static void captureLiveSheepSnapshotsForLoadedWorlds() {
        if (plugin == null || plugin.getServer() == null) {
            return;
        }

        for (World world : plugin.getServer().getWorlds()) {
            if (!isSheepFarmWorld(world)) {
                continue;
            }
            saveSheepSnapshotForWorld(world);
        }
    }

    private static void pruneBufferBackups(File backupDir) {
        if (backupDir == null || !backupDir.exists() || !backupDir.isDirectory()) {
            return;
        }

        File[] bufferFiles = backupDir
                .listFiles(file -> file != null && file.isFile() && file.getName().startsWith(BACKUP_BUFFER_FILE_PREFIX)
                        && file.getName().endsWith(".zip"));
        if (bufferFiles == null || bufferFiles.length <= BACKUP_AUTOMATIC_BUFFER_MAX_FILES) {
            return;
        }

        List<File> sorted = new ArrayList<>(List.of(bufferFiles));
        sorted.sort((left, right) -> Long.compare(right.lastModified(), left.lastModified()));
        for (int index = BACKUP_AUTOMATIC_BUFFER_MAX_FILES; index < sorted.size(); index++) {
            sorted.get(index).delete();
        }
    }

    public static synchronized List<String> listBackups() {
        if (plugin == null) {
            return List.of();
        }
        File backupDir = new File(plugin.getDataFolder(), BACKUP_DIR_NAME);
        if (!backupDir.exists() || !backupDir.isDirectory()) {
            return List.of();
        }
        File[] files = backupDir.listFiles(file -> file != null && file.isFile() && file.getName().endsWith(".zip"));
        if (files == null || files.length == 0) {
            return List.of();
        }
        List<String> names = new ArrayList<>();
        for (File file : files) {
            names.add(file.getName());
        }
        Collections.sort(names);
        Collections.reverse(names);
        return names;
    }

    public static synchronized boolean markBackupForDeletion(String backupName) {
        if (plugin == null || backupName == null || backupName.isBlank()) {
            return false;
        }
        if (BACKUP_ROLLING_FILE_NAME.equals(backupName)) {
            return false;
        }

        File backupDir = new File(plugin.getDataFolder(), BACKUP_DIR_NAME);
        File source = new File(backupDir, backupName);
        if (!source.exists() || !source.isFile() || !source.getName().endsWith(".zip")) {
            return false;
        }

        Map<String, Long> marks = getMarkedBackupsMap();
        marks.put(backupName, System.currentTimeMillis());
        saveMarkedBackupsMap(marks);
        return true;
    }

    public static synchronized boolean recoverBackupMarkedForDeletion(String backupName) {
        if (plugin == null || backupName == null || backupName.isBlank()) {
            return false;
        }
        Map<String, Long> marks = getMarkedBackupsMap();
        Long removed = marks.remove(backupName);
        if (removed == null) {
            return false;
        }
        saveMarkedBackupsMap(marks);
        return true;
    }

    public static synchronized boolean isBackupMarkedForDeletion(String backupName) {
        if (backupName == null || backupName.isBlank()) {
            return false;
        }
        return getMarkedBackupsMap().containsKey(backupName);
    }

    public static synchronized int purgeMarkedBackupsIfEligibleOnStartup() {
        if (plugin == null) {
            return 0;
        }

        Map<String, Long> marks = getMarkedBackupsMap();
        if (marks.isEmpty()) {
            return 0;
        }

        File backupDir = new File(plugin.getDataFolder(), BACKUP_DIR_NAME);
        long now = System.currentTimeMillis();
        int deleted = 0;
        boolean changed = false;

        var iterator = marks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            long markedAt = Math.max(0L, entry.getValue());
            if (now - markedAt < BACKUP_SOFT_DELETE_GRACE_MS) {
                continue;
            }

            File target = new File(backupDir, entry.getKey());
            if (!target.exists() || target.delete()) {
                if (target.exists()) {
                    // No-op fallback; delete may fail and file still exists.
                } else {
                    deleted++;
                    iterator.remove();
                    changed = true;
                }
            }
        }

        if (changed) {
            saveMarkedBackupsMap(marks);
        }
        return deleted;
    }

    public static synchronized File loadBackup(String backupName) {
        if (plugin == null || backupName == null || backupName.isBlank()) {
            return null;
        }

        File backupDir = new File(plugin.getDataFolder(), BACKUP_DIR_NAME);
        File source = new File(backupDir, backupName);
        if (!source.exists() || !source.isFile() || !source.getName().endsWith(".zip")) {
            return null;
        }

        if (!restoreBackupArchive(source)) {
            return null;
        }

        plugin.reloadConfig();
        SheepMergeConfiguration.initialize(plugin);
        applyConfiguration(SheepMergeConfiguration.get());

        clearStateBeforeDataLoad();
        loadData();
        loadFarmLayout();

        for (World world : plugin.getServer().getWorlds()) {
            if (isSheepFarmWorld(world)) {
                rebuildFarmWorld(world);
            }
        }

        restoreTopPointsDisplayAfterRestart(null);

        File postLoadBackup = createBackup(true, "post-load");
        return postLoadBackup == null ? source : postLoadBackup;
    }

    private static void clearStateBeforeDataLoad() {
        pointsByPlayer.clear();
        extraLimitByPlayer.clear();
        eggSpeedLevelByPlayer.clear();
        woolRegenLevelByPlayer.clear();
        higherTierChanceLevelByPlayer.clear();
        prestigeLevelByPlayer.clear();
        prestigePointsByPlayer.clear();
        prestigeDoublePointsChanceByPlayer.clear();
        prestigeHigherMaxLevelByPlayer.clear();
        prestigeStartEggsByPlayer.clear();
        prestigeEggCapByPlayer.clear();
        prestigeBaseSpawnTierByPlayer.clear();
        prestigeQuestRewardByPlayer.clear();
        nextPrestigeRefundTimestampByPlayer.clear();
        highestAnnouncedTierByPlayer.clear();
        highestAnnouncedRainbowTierByPlayer.clear();
        shearShopLevelByPlayer.clear();
        shearWoolSaveLevelByPlayer.clear();
        shearTierBoostLevelByPlayer.clear();
        tutorialCompletedByPlayer.clear();
        tutorialBypassedByPlayer.clear();
        tutorialShearsByPlayer.clear();
        tutorialSpawnsByPlayer.clear();
        tutorialMergesByPlayer.clear();
        tutorialUpgradeOpenedByPlayer.clear();
        tutorialQuestOpenedByPlayer.clear();
        tutorialQuestUpgradesOpenedByPlayer.clear();
        tutorialPrestigeOpenedByPlayer.clear();
        tutorialAbilityUsedByPlayer.clear();
        tutorialShearUpgradedByPlayer.clear();
        tutorialRegularUpgradesBoughtByPlayer.clear();
        tutorialShearTaskRewardGrantedByPlayer.clear();
        tutorialPrestigePrepRewardGrantedByPlayer.clear();
        tutorialPrestigedOnceByPlayer.clear();
        tutorialShearShopOpenedByPlayer.clear();
        farmVisitEnabledByPlayer.clear();
        questPointsByPlayer.clear();
        nextQuestResetTimestampByPlayer.clear();
        questShearsByPlayer.clear();
        questSpawnsByPlayer.clear();
        questMergesByPlayer.clear();
        questShearsCompleteByPlayer.clear();
        questSpawnsCompleteByPlayer.clear();
        questMergesCompleteByPlayer.clear();
        questUpgradeDurationByPlayer.clear();
        questUpgradePowerByPlayer.clear();
        activeLuckyBurstUntilByPlayer.clear();
        activeWoolRushUntilByPlayer.clear();
        activeJackpotShearsUntilByPlayer.clear();
        activeAutoMergeUntilByPlayer.clear();
        activeAutoShearUntilByPlayer.clear();
        pausedLuckyBurstRemainingMsByPlayer.clear();
        pausedWoolRushRemainingMsByPlayer.clear();
        pausedJackpotShearsRemainingMsByPlayer.clear();
        pausedAutoMergeRemainingMsByPlayer.clear();
        pausedAutoShearRemainingMsByPlayer.clear();
        comboDecayUpgradeByPlayer.clear();
        comboMaxUpgradeByPlayer.clear();
        comboGainUpgradeByPlayer.clear();
        automationPointsByPlayer.clear();
        automationAutoBuyUpgradeByPlayer.clear();
        automationAutoAbilityUpgradeByPlayer.clear();
        automationSlowAutoMergeUpgradeByPlayer.clear();
        automationSlowAutoShearUpgradeByPlayer.clear();
        automationAutoSpawnUpgradeByPlayer.clear();
        automationAutoPrestigeUpgradeByPlayer.clear();
        automationAutoBuyEnabledByPlayer.clear();
        automationAutoAbilityEnabledByPlayer.clear();
        automationSlowAutoMergeEnabledByPlayer.clear();
        automationSlowAutoShearEnabledByPlayer.clear();
        automationAutoSpawnEnabledByPlayer.clear();
        automationAutoPrestigeEnabledByPlayer.clear();
        scoreboardLayoutModeByPlayer.clear();
        scoreboardShowQuestPointsByPlayer.clear();
        scoreboardShowAutomationPointsByPlayer.clear();
        scoreboardShowSacrificePointsByPlayer.clear();
        scoreboardShowQuestProgressByPlayer.clear();
        scoreboardShowAbilityStatusByPlayer.clear();
        sacrificePointsByPlayer.clear();
        sacrificeUnlocksBoughtByPlayer.clear();
        for (BossBar bar : visitFarmBossBarByPlayer.values()) {
            if (bar != null) {
                bar.removeAll();
                bar.setVisible(false);
            }
        }
        visitFarmBossBarByPlayer.clear();
        savedFarmSheepByPlayer.clear();
        savedTutorialSheepByPlayer.clear();
        savedInventories.clear();
        savedScoreboards.clear();
        carriedSheepByPlayer.clear();
        liveSheepCountByWorld.clear();
        lastOutOfEggWarningTimestampByPlayer.clear();
    }

    private static boolean writeBackupArchive(File destination) {
        if (plugin == null || destination == null) {
            return false;
        }

        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            return false;
        }

        File scores = new File(dataFolder, "scores.yml");
        File layout = new File(dataFolder, "farm-layout.yml");
        File config = new File(dataFolder, "config.yml");

        try (FileOutputStream fileOut = new FileOutputStream(destination);
                ZipOutputStream zipOut = new ZipOutputStream(fileOut)) {
            zipOut.setLevel(Deflater.BEST_COMPRESSION);
            addFileToZip(zipOut, scores, "scores.yml");
            addFileToZip(zipOut, layout, "farm-layout.yml");
            addFileToZip(zipOut, config, "config.yml");
            return true;
        } catch (IOException exception) {
            if (plugin != null) {
                plugin.getLogger().warning("Unable to create backup archive: " + exception.getMessage());
            }
            return false;
        }
    }

    private static boolean restoreBackupArchive(File source) {
        if (plugin == null || source == null || !source.exists()) {
            return false;
        }

        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            return false;
        }

        try (FileInputStream fileIn = new FileInputStream(source);
                ZipInputStream zipIn = new ZipInputStream(fileIn)) {
            ZipEntry entry;
            while ((entry = zipIn.getNextEntry()) != null) {
                String name = entry.getName();
                if (!("scores.yml".equals(name) || "farm-layout.yml".equals(name) || "config.yml".equals(name))) {
                    zipIn.closeEntry();
                    continue;
                }
                File target = new File(dataFolder, name);
                try (FileOutputStream out = new FileOutputStream(target)) {
                    copyStream(zipIn, out);
                }
                zipIn.closeEntry();
            }
            return true;
        } catch (IOException exception) {
            if (plugin != null) {
                plugin.getLogger().warning("Unable to restore backup archive: " + exception.getMessage());
            }
            return false;
        }
    }

    private static void addFileToZip(ZipOutputStream zipOut, File source, String entryName) throws IOException {
        if (zipOut == null || source == null || entryName == null || !source.exists() || !source.isFile()) {
            return;
        }
        ZipEntry entry = new ZipEntry(entryName);
        entry.setTime(source.lastModified());
        zipOut.putNextEntry(entry);
        try (FileInputStream in = new FileInputStream(source)) {
            copyStream(in, zipOut);
        }
        zipOut.closeEntry();
    }

    private static void copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) >= 0) {
            out.write(buffer, 0, read);
        }
    }

    private static String sanitizeBackupToken(String token) {
        if (token == null || token.isBlank()) {
            return "auto";
        }
        String cleaned = token.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9-]+", "-");
        return cleaned.isBlank() ? "auto" : cleaned;
    }

    private static Map<String, Long> getMarkedBackupsMap() {
        File indexFile = getBackupIndexFile();
        if (indexFile == null || !indexFile.exists()) {
            return new HashMap<>();
        }

        FileConfiguration indexConfig = YamlConfiguration.loadConfiguration(indexFile);
        if (!indexConfig.isConfigurationSection(BACKUP_INDEX_MARKED_FOR_DELETION_KEY)) {
            return new HashMap<>();
        }

        Map<String, Long> marks = new HashMap<>();
        var section = indexConfig.getConfigurationSection(BACKUP_INDEX_MARKED_FOR_DELETION_KEY);
        for (String encodedName : section.getKeys(false)) {
            String backupName = decodeBackupName(encodedName);
            if (backupName == null || backupName.isBlank()) {
                continue;
            }
            long markedAt = Math.max(0L,
                    indexConfig.getLong(BACKUP_INDEX_MARKED_FOR_DELETION_KEY + "." + encodedName, 0L));
            if (markedAt > 0L) {
                marks.put(backupName, markedAt);
            }
        }
        return marks;
    }

    private static void saveMarkedBackupsMap(Map<String, Long> marks) {
        File indexFile = getBackupIndexFile();
        if (indexFile == null) {
            return;
        }
        FileConfiguration indexConfig = YamlConfiguration.loadConfiguration(indexFile);
        indexConfig.set(BACKUP_INDEX_MARKED_FOR_DELETION_KEY, null);
        if (marks != null) {
            for (Map.Entry<String, Long> entry : marks.entrySet()) {
                String key = encodeBackupName(entry.getKey());
                if (key == null) {
                    continue;
                }
                indexConfig.set(BACKUP_INDEX_MARKED_FOR_DELETION_KEY + "." + key, Math.max(0L, entry.getValue()));
            }
        }
        try {
            indexConfig.save(indexFile);
        } catch (IOException ignored) {
            // Best effort metadata write.
        }
    }

    private static String encodeBackupName(String backupName) {
        if (backupName == null || backupName.isBlank()) {
            return null;
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(backupName.getBytes(StandardCharsets.UTF_8));
    }

    private static String decodeBackupName(String encodedBackupName) {
        if (encodedBackupName == null || encodedBackupName.isBlank()) {
            return null;
        }
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(encodedBackupName);
            return new String(decoded, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static long getLastPermanentBackupAt() {
        File indexFile = getBackupIndexFile();
        if (indexFile == null || !indexFile.exists()) {
            return 0L;
        }
        FileConfiguration indexConfig = YamlConfiguration.loadConfiguration(indexFile);
        return Math.max(0L, indexConfig.getLong(BACKUP_INDEX_LAST_PERMANENT_AT_KEY, 0L));
    }

    private static long getLastBufferBackupAt() {
        File indexFile = getBackupIndexFile();
        if (indexFile == null || !indexFile.exists()) {
            return 0L;
        }
        FileConfiguration indexConfig = YamlConfiguration.loadConfiguration(indexFile);
        return Math.max(0L, indexConfig.getLong(BACKUP_INDEX_LAST_BUFFER_AT_KEY, 0L));
    }

    private static void markLastPermanentBackupNow() {
        File indexFile = getBackupIndexFile();
        if (indexFile == null) {
            return;
        }
        FileConfiguration indexConfig = YamlConfiguration.loadConfiguration(indexFile);
        indexConfig.set(BACKUP_INDEX_LAST_PERMANENT_AT_KEY, System.currentTimeMillis());
        try {
            indexConfig.save(indexFile);
        } catch (IOException ignored) {
            // Best effort metadata write; backup file has already been created.
        }
    }

    private static void markLastBufferBackupNow() {
        File indexFile = getBackupIndexFile();
        if (indexFile == null) {
            return;
        }
        FileConfiguration indexConfig = YamlConfiguration.loadConfiguration(indexFile);
        indexConfig.set(BACKUP_INDEX_LAST_BUFFER_AT_KEY, System.currentTimeMillis());
        try {
            indexConfig.save(indexFile);
        } catch (IOException ignored) {
            // Best effort metadata write; backup file has already been created.
        }
    }

    private static File getBackupIndexFile() {
        if (plugin == null) {
            return null;
        }
        File backupDir = new File(plugin.getDataFolder(), BACKUP_DIR_NAME);
        if (!backupDir.exists() && !backupDir.mkdirs()) {
            return null;
        }
        return new File(backupDir, BACKUP_INDEX_FILE_NAME);
    }

    public static void addPoints(Player player, int points) {
        if (points <= 0) {
            return;
        }
        addPoints(player, BigInteger.valueOf(points));
    }

    public static void addPoints(Player player, BigInteger points) {
        if (player == null || points == null || points.signum() <= 0) {
            return;
        }
        UUID playerId = player.getUniqueId();
        pointsByPlayer.put(playerId, getPlayerPointsBig(player).add(points));
        refreshTopPointsDisplays();
        queuePointsGainOverlay(player,
                points.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0 ? Integer.MAX_VALUE : points.intValue());
        saveData();
        tickPrestigeReminder(player);
    }

    private static void queuePointsGainOverlay(Player player, int points) {
        if (player == null || points <= 0) {
            return;
        }
        UUID playerId = player.getUniqueId();
        lastPointsOverlayByPlayer.put(playerId, points);
        pointsOverlayExpiresAtByPlayer.put(playerId, System.currentTimeMillis() + POINTS_OVERLAY_DISPLAY_DURATION_MS);
    }

    public static void tickPointsGainOverlay(Player player) {
        if (player == null || !isSheepFarmWorld(player.getWorld())) {
            return;
        }
        UUID playerId = player.getUniqueId();
        Integer lastPoints = lastPointsOverlayByPlayer.get(playerId);
        if (lastPoints == null || lastPoints <= 0) {
            return;
        }

        long now = System.currentTimeMillis();
        long expiresAt = pointsOverlayExpiresAtByPlayer.getOrDefault(playerId, 0L);
        if (expiresAt <= now) {
            lastPointsOverlayByPlayer.remove(playerId);
            pointsOverlayExpiresAtByPlayer.remove(playerId);
            return;
        }

        showOverlay(player, action("+" + formatPoints(lastPoints) + " points"));
    }

    public static void showOverlay(Player player, String message) {
        if (player == null || message == null || message.isBlank()) {
            return;
        }
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
    }

    public static String color(String message) {
        if (message == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static String hint(String message) {
        return color("&7" + message);
    }

    public static String action(String message) {
        return color("&a" + message);
    }

    public static String warning(String message) {
        return color("&e" + message);
    }

    public static String accent(String message) {
        return color("&b" + message);
    }

    public static void resetMergeReminder(Player player) {
        if (player == null) {
            return;
        }
        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();
        lastMergeTimestampByPlayer.put(playerId, now);
        lastMergeReminderTimestampByPlayer.remove(playerId);
        mergeTitleReminderShownByPlayer.remove(playerId);
    }

    public static void clearMergeReminder(Player player) {
        if (player == null) {
            return;
        }
        UUID playerId = player.getUniqueId();
        lastMergeTimestampByPlayer.remove(playerId);
        lastMergeReminderTimestampByPlayer.remove(playerId);
        mergeTitleReminderShownByPlayer.remove(playerId);
    }

    public static void clearComboRuntime(Player player) {
        if (player == null) {
            return;
        }
        UUID playerId = player.getUniqueId();
        comboScoreByPlayer.remove(playerId);
        comboLastUpdateTimestampByPlayer.remove(playerId);
        lastPointsOverlayByPlayer.remove(playerId);
        pointsOverlayExpiresAtByPlayer.remove(playerId);
        lastAbilityAuraSoundTimestampByPlayer.remove(playerId);
        removeComboBossBar(playerId);
        clearVisitFarmBossBar(player);
    }

    public static void clearPrestigeReminder(Player player) {
        if (player == null) {
            return;
        }
        lastPrestigeReminderTimestampByPlayer.remove(player.getUniqueId());
    }

    public static void tickPrestigeReminder(Player player) {
        if (player == null || !isSheepFarmWorld(player.getWorld())) {
            return;
        }
        if (getPlayerPointsBig(player).compareTo(getPrestigeCostBig(player)) < 0) {
            clearPrestigeReminder(player);
            return;
        }

        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();
        long lastReminder = lastPrestigeReminderTimestampByPlayer.getOrDefault(playerId, 0L);
        if (now - lastReminder < 20_000L) {
            return;
        }

        if (!prestigeTitleReminderShownByPlayer.getOrDefault(playerId, false)) {
            player.sendTitle(
                    color("&ePrestige ready"),
                    color("&7Use /sheepmerge prestige"),
                    10,
                    60,
                    10);
            prestigeTitleReminderShownByPlayer.put(playerId, true);
        } else {
            player.sendMessage(hint("Prestige ready. Use /sheepmerge prestige"));
        }
        lastPrestigeReminderTimestampByPlayer.put(playerId, now);
    }

    public static void recordSheepMerge(Player player, SheepTier mergedFromTier, int woolReadySourceSheep) {
        if (player == null || mergedFromTier == null) {
            return;
        }
        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();
        lastMergeTimestampByPlayer.put(playerId, now);
        lastMergeReminderTimestampByPlayer.remove(playerId);
        mergeTitleReminderShownByPlayer.remove(playerId);

        tickComboDecay(player, now);
        double comboGain = (mergedFromTier.getLevel() + 1)
                * (1.0D + (getComboGainUpgradeLevel(player) * (COMBO_GAIN_PERCENT_PER_LEVEL / 100.0D)));
        if (comboFrenzyEventEndsAtMs > now) {
            comboGain *= COMBO_FRENZY_MULTIPLIER;
        }
        double updatedScore = Math.min(getComboMaxScore(player), getComboScore(player) + comboGain);
        comboScoreByPlayer.put(playerId, updatedScore);
        comboLastUpdateTimestampByPlayer.put(playerId, now);

        showOverlay(player, accent("Merge combo x" + formatComboMultiplier(getComboMultiplier(player, updatedScore))));
        updateComboBossBar(player, updatedScore);
    }

    private static double getComboMultiplier(Player player, double comboScore) {
        return Math.max(1.0D, 1.0D + comboScore * COMBO_POINT_MULTIPLIER_PER_SCORE);
    }

    private static String formatComboMultiplier(double multiplier) {
        return String.format(java.util.Locale.ROOT, "%.2f", multiplier);
    }

    private static double getComboScore(Player player) {
        if (player == null) {
            return 0.0D;
        }
        return Math.max(0.0D, comboScoreByPlayer.getOrDefault(player.getUniqueId(), 0.0D));
    }

    private static double getComboMaxScore(Player player) {
        return COMBO_BASE_MAX_SCORE + getComboMaxUpgradeLevel(player) * COMBO_MAX_SCORE_PER_LEVEL;
    }

    private static double getComboDecayMultiplier(Player player) {
        int level = getComboDecayUpgradeLevel(player);
        return Math.max(0.15D, 1.0D - level * 0.08D);
    }

    public static void tickCombo(Player player) {
        if (player == null) {
            return;
        }
        if (!isSheepFarmWorld(player.getWorld())) {
            removeComboBossBar(player.getUniqueId());
            return;
        }

        long now = System.currentTimeMillis();
        tickComboDecay(player, now);
        updateComboBossBar(player, getComboScore(player));
    }

    private static void tickComboDecay(Player player, long now) {
        if (player == null) {
            return;
        }

        UUID playerId = player.getUniqueId();
        double currentScore = getComboScore(player);
        long lastTick = comboLastUpdateTimestampByPlayer.getOrDefault(playerId, now);
        comboLastUpdateTimestampByPlayer.put(playerId, now);

        if (currentScore <= 0.0D || now <= lastTick) {
            if (currentScore <= 0.0D) {
                comboScoreByPlayer.remove(playerId);
            }
            return;
        }

        double elapsedSeconds = (now - lastTick) / 1000.0D;
        double maxScore = getComboMaxScore(player);
        double levelScaling = 1.0D + (currentScore / Math.max(1.0D, maxScore)) * COMBO_DECAY_HIGH_LEVEL_SCALING;
        double decayPerSecond = BASE_COMBO_DECAY_PER_SECOND * levelScaling * getComboDecayMultiplier(player);
        double updatedScore = Math.max(0.0D, currentScore - decayPerSecond * elapsedSeconds);

        if (updatedScore <= 0.01D) {
            comboScoreByPlayer.remove(playerId);
            return;
        }
        comboScoreByPlayer.put(playerId, Math.min(maxScore, updatedScore));
    }

    private static void updateComboBossBar(Player player, double comboScore) {
        if (player == null || !isSheepFarmWorld(player.getWorld())) {
            return;
        }

        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();
        boolean frenzyActive = comboFrenzyEventEndsAtMs > now;
        if (comboScore <= 0.0D && !frenzyActive) {
            removeComboBossBar(playerId);
            return;
        }

        BossBar bar = comboBossBarByPlayer.get(playerId);
        if (bar == null) {
            bar = Bukkit.createBossBar("Combo", BarColor.YELLOW, BarStyle.SEGMENTED_10);
            comboBossBarByPlayer.put(playerId, bar);
        }

        double maxScore = getComboMaxScore(player);
        double progress;
        if (frenzyActive) {
            long remaining = Math.max(0L, comboFrenzyEventEndsAtMs - now);
            progress = Math.max(0.0D, Math.min(1.0D, remaining / (double) COMBO_FRENZY_EVENT_DURATION_MS));
        } else {
            progress = Math.max(0.0D, Math.min(1.0D, comboScore / Math.max(1.0D, maxScore)));
        }
        bar.setProgress(progress);
        String title = color("&6Combo &f" + (int) Math.floor(comboScore)
                + "&7/&f" + (int) Math.floor(maxScore)
                + " &7| &ePoints x" + formatComboMultiplier(getComboMultiplier(player, comboScore)));
        if (frenzyActive) {
            long remaining = Math.max(0L, comboFrenzyEventEndsAtMs - now);
            title += color(" &7| &cFrenzy " + formatDuration(remaining));
        }
        bar.setTitle(title);
        bar.setVisible(true);
        if (!bar.getPlayers().contains(player)) {
            bar.addPlayer(player);
        }
    }

    private static void removeComboBossBar(UUID playerId) {
        if (playerId == null) {
            return;
        }
        BossBar bar = comboBossBarByPlayer.remove(playerId);
        if (bar == null) {
            return;
        }
        bar.removeAll();
        bar.setVisible(false);
    }

    public static void announceTierUnlock(Player player, SheepTier tier) {
        announceTierUnlock(player, tier, tier == SheepTier.RAINBOW ? 1 : 0);
    }

    public static void announceTierUnlock(Player player, SheepTier tier, int rainbowTier) {
        if (player == null || tier == null) {
            return;
        }

        String message = getTierUnlockMessage(player, tier, rainbowTier);
        if (plugin == null || plugin.getServer() == null) {
            player.sendMessage(message);
        } else {
            for (Player online : plugin.getServer().getOnlinePlayers()) {
                if (!isSheepFarmWorld(online.getWorld())) {
                    continue;
                }
                online.sendMessage(message);
            }
        }

        playTierUnlockSound(player, tier);
    }

    public static boolean shouldAnnounceTierUnlock(Player player, SheepTier tier) {
        return shouldAnnounceTierUnlock(player, tier, tier == SheepTier.RAINBOW ? 1 : 0);
    }

    public static boolean shouldAnnounceTierUnlock(Player player, SheepTier tier, int rainbowTier) {
        if (player == null || tier == null) {
            return false;
        }
        UUID playerId = player.getUniqueId();
        int highestAnnounced = highestAnnouncedTierByPlayer.getOrDefault(playerId, SheepTier.WHITE.getLevel());
        if (tier.getLevel() > highestAnnounced) {
            return true;
        }
        if (tier != SheepTier.RAINBOW) {
            return false;
        }
        int normalizedRainbowTier = Math.max(1, rainbowTier);
        int highestRainbowAnnounced = highestAnnouncedRainbowTierByPlayer.getOrDefault(playerId, 0);
        return normalizedRainbowTier > highestRainbowAnnounced;
    }

    public static void markTierUnlockAnnounced(Player player, SheepTier tier) {
        markTierUnlockAnnounced(player, tier, tier == SheepTier.RAINBOW ? 1 : 0);
    }

    public static void markTierUnlockAnnounced(Player player, SheepTier tier, int rainbowTier) {
        if (player == null || tier == null) {
            return;
        }
        UUID playerId = player.getUniqueId();
        int highestAnnounced = highestAnnouncedTierByPlayer.getOrDefault(playerId, SheepTier.WHITE.getLevel());
        if (tier.getLevel() > highestAnnounced) {
            highestAnnouncedTierByPlayer.put(playerId, tier.getLevel());
        }
        if (tier == SheepTier.RAINBOW) {
            int normalizedRainbowTier = Math.max(1, rainbowTier);
            int highestRainbowAnnounced = highestAnnouncedRainbowTierByPlayer.getOrDefault(playerId, 0);
            if (normalizedRainbowTier > highestRainbowAnnounced) {
                highestAnnouncedRainbowTierByPlayer.put(playerId, normalizedRainbowTier);
            }
        }
        saveData();
    }

    private static String getTierUnlockMessage(Player player, SheepTier tier, int rainbowTier) {
        String playerName = player.getName() == null || player.getName().isBlank() ? "Someone" : player.getName();
        return switch (tier) {
            case ORANGE -> color("&8[&6SheepMerge&8] &e" + playerName + " &7unlocked &6Orange Sheep&7!");
            case MAGENTA -> color("&8[&6SheepMerge&8] &e" + playerName + " &7unlocked &dMagenta Sheep&7!");
            case LIGHT_BLUE -> color("&8[&6SheepMerge&8] &e" + playerName + " &7unlocked &bLight Blue Sheep&7!");
            case YELLOW -> color("&8[&6SheepMerge&8] &e" + playerName + " &7unlocked &eYellow Sheep&7!");
            case LIME -> color("&8[&6SheepMerge&8] &e" + playerName + " &7unlocked &aLime Sheep&7!");
            case PINK -> color("&8[&6SheepMerge&8] &e" + playerName + " &7unlocked &dPink Sheep&7!");
            case GRAY -> color("&8[&6SheepMerge&8] &e" + playerName + " &7unlocked &7Gray Sheep&7!");
            case LIGHT_GRAY -> color("&8[&6SheepMerge&8] &e" + playerName + " &7unlocked &fLight Gray Sheep&7!");
            case CYAN -> color("&8[&6SheepMerge&8] &e" + playerName + " &7unlocked &3Cyan Sheep&7!");
            case PURPLE -> color("&8[&6SheepMerge&8] &e" + playerName + " &7unlocked &5Purple Sheep&7!");
            case BLUE -> color("&8[&6SheepMerge&8] &e" + playerName + " &7unlocked &9Blue Sheep&7!");
            case BROWN -> color("&8[&6SheepMerge&8] &e" + playerName + " &7unlocked &6Brown Sheep&7!");
            case GREEN -> color("&8[&6SheepMerge&8] &e" + playerName + " &7unlocked &2Green Sheep&7!");
            case RED -> color("&8[&6SheepMerge&8] &e" + playerName + " &7unlocked &cRed Sheep&7!");
            case BLACK -> color("&8[&6SheepMerge&8] &e" + playerName + " &7unlocked &8Black Sheep&7!");
            case RAINBOW -> {
                int normalizedRainbowTier = Math.max(1, rainbowTier);
                if (normalizedRainbowTier <= 1) {
                    yield color(
                            "&8[&6SheepMerge&8] &e" + playerName
                                    + " &7unlocked &dRainbow Sheep&b! &7Legendary tier reached!");
                }
                yield color("&8[&6SheepMerge&8] &e" + playerName
                        + " &7unlocked &dRainbow Tier &fT" + formatPoints(normalizedRainbowTier) + "&7!");
            }
            default -> color("&8[&6SheepMerge&8] &e" + playerName + " &7unlocked a new tier!");
        };
    }

    private static void playTierUnlockSound(Player player, SheepTier tier) {
        if (player == null || tier == null) {
            return;
        }

        Sound sound = switch (tier.getLevel()) {
            case 0, 1, 2, 3 -> Sound.BLOCK_NOTE_BLOCK_CHIME;
            case 4, 5, 6, 7 -> Sound.ENTITY_PLAYER_LEVELUP;
            case 8, 9, 10, 11 -> Sound.BLOCK_BEACON_POWER_SELECT;
            case 12, 13, 14 -> Sound.UI_TOAST_CHALLENGE_COMPLETE;
            default -> Sound.ENTITY_ENDER_DRAGON_GROWL;
        };

        float pitch = Math.min(2.0f, 0.9f + (tier.getLevel() * 0.08f));
        playSound(player, sound, 1.0f, pitch);
    }

    public static void tickMergeReminder(Player player) {
        if (player == null || !isSheepFarmWorld(player.getWorld())) {
            return;
        }
        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();
        long lastMerge = lastMergeTimestampByPlayer.getOrDefault(playerId, now);
        if (now - lastMerge < MERGE_REMINDER_DELAY_MS) {
            return;
        }
        long lastReminder = lastMergeReminderTimestampByPlayer.getOrDefault(playerId, 0L);
        if (now - lastReminder < MERGE_REMINDER_REPEAT_MS) {
            return;
        }
        if (!mergeTitleReminderShownByPlayer.getOrDefault(playerId, false)) {
            player.sendTitle(
                    color("&eMerge sheep"),
                    color("&7Sneak-right-click one sheep, then right-click the same tier"),
                    10,
                    60,
                    10);
            mergeTitleReminderShownByPlayer.put(playerId, true);
        } else {
            player.sendMessage(hint("Merge sheep. Sneak-right-click one sheep, then right-click the same tier."));
        }
        lastMergeReminderTimestampByPlayer.put(playerId, now);
    }

    public static void enforceFarmLoadout(Player player) {
        if (player == null || !isSheepFarmWorld(player.getWorld())) {
            return;
        }
        boolean shouldClearNonLoadoutItems = !player.isOp() && !isFarmBuildWorld(player.getWorld());

        var inventory = player.getInventory();
        ItemStack[] storageContents = inventory.getStorageContents();
        boolean storageChanged = false;

        for (int slot = 0; slot < storageContents.length; slot++) {
            ItemStack itemStack = storageContents[slot];
            if (slot == FARM_UPGRADE_COMMAND_SLOT) {
                if (itemStack == null
                        || !isSheepMergeUpgradeCommandItem(itemStack)
                        || itemStack.getAmount() != 1) {
                    storageContents[slot] = getSheepMergeUpgradeCommandItem();
                    storageChanged = true;
                }
                continue;
            }

            if (slot == FARM_EGG_ITEM_SLOT) {
                if (itemStack == null
                        || !isSheepMergeEggItem(itemStack)
                        || itemStack.getAmount() != 1) {
                    storageContents[slot] = getSheepMergeEggItem();
                    storageChanged = true;
                }
                continue;
            }

            if (itemStack == null) {
                continue;
            }
            if (!shouldClearNonLoadoutItems) {
                continue;
            }
            if (isForcedFarmLoadoutItem(itemStack)) {
                storageContents[slot] = null;
                storageChanged = true;
                continue;
            }

            storageContents[slot] = null;
            storageChanged = true;
        }

        if (storageChanged) {
            inventory.setStorageContents(storageContents);
        }

        if (shouldClearNonLoadoutItems) {
            boolean armorChanged = false;
            ItemStack[] armorContents = inventory.getArmorContents();
            for (int index = 0; index < armorContents.length; index++) {
                if (armorContents[index] == null) {
                    continue;
                }
                armorContents[index] = null;
                armorChanged = true;
            }
            if (armorChanged) {
                inventory.setArmorContents(armorContents);
            }
        }

        ItemStack offHand = inventory.getItemInOffHand();
        if (offHand == null || offHand.getType() != Material.SHEARS || offHand.getAmount() != 1) {
            inventory.setItemInOffHand(getSheepMergeShears());
        }
    }

    private static boolean isFarmChunk(int chunkX, int chunkZ) {
        int minChunk = Math.floorDiv(FARM_MIN_XZ, 16);
        int maxChunk = Math.floorDiv(FARM_MAX_XZ, 16);
        return chunkX >= minChunk && chunkX <= maxChunk && chunkZ >= minChunk && chunkZ <= maxChunk;
    }

    public static void applyFarmSaturation(Player player) {
        if (player == null || !isSheepFarmWorld(player.getWorld())) {
            return;
        }
        player.setFoodLevel(20);
        player.setSaturation(20.0f);
        player.setExhaustion(0.0f);
    }

    public static ItemStack getSheepMergeUpgradeCommandItem() {
        ItemStack item = new ItemStack(Material.NETHER_STAR, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color("&bSheep Merge Upgrades"));
            meta.setLore(List.of(
                    hint("Right-click to open upgrades"),
                    hint("Hotbar slot 9")));
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack getSheepMergeEggItem() {
        ItemStack item = new ItemStack(Material.SHEEP_SPAWN_EGG, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color("&eSheep Spawn Egg"));
            meta.setLore(List.of(
                    hint("Right-click a block to spawn a sheep"),
                    hint("Egg count is shown in your XP level"),
                    hint("Hotbar slot 8")));
            item.setItemMeta(meta);
        }
        return item;
    }

    public static boolean isSheepMergeUpgradeCommandItem(ItemStack itemStack) {
        return itemStack != null && itemStack.getType() == Material.NETHER_STAR;
    }

    public static boolean isSheepMergeEggItem(ItemStack itemStack) {
        return itemStack != null && itemStack.getType() == Material.SHEEP_SPAWN_EGG;
    }

    public static boolean isForcedFarmLoadoutItem(ItemStack itemStack) {
        return itemStack != null
                && (itemStack.getType() == Material.SHEARS
                        || isSheepMergeUpgradeCommandItem(itemStack)
                        || isSheepMergeEggItem(itemStack));
    }

    public static void playMergeSound(Player player) {
        playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
    }

    public static void playUpgradeSound(Player player) {
        playSound(player, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.1f);
    }

    public static void playPrestigeSound(Player player) {
        playSound(player, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
    }

    private static void playSound(Player player, Sound sound, float volume, float pitch) {
        if (player == null || sound == null) {
            return;
        }
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    public static boolean trySpendPoints(Player player, long points) {
        if (points <= 0L) {
            return false;
        }
        return trySpendPoints(player, BigInteger.valueOf(points));
    }

    public static boolean trySpendPoints(Player player, BigInteger points) {
        if (player == null || points == null || points.signum() <= 0) {
            return false;
        }
        UUID uuid = player.getUniqueId();
        BigInteger current = getPlayerPointsBig(player);
        if (current.compareTo(points) < 0) {
            return false;
        }
        pointsByPlayer.put(uuid, current.subtract(points));
        refreshTopPointsDisplays();
        saveData();
        return true;
    }

    public static int getPlayerLimit(Player player) {
        if (player == null) {
            return BASE_SHEEP_LIMIT;
        }
        int maxLimit = hasSacrificeUnlock(player, SACRIFICE_UNLOCK_MAX_SHEEP_100)
                ? SACRIFICE_UNLOCK_MAX_SHEEP_LIMIT
                : MAX_SHEEP_LIMIT;
        return Math.min(
                maxLimit,
                BASE_SHEEP_LIMIT + Math.max(0, extraLimitByPlayer.getOrDefault(player.getUniqueId(), 0)));
    }

    public static int getOwnerLimit(World world) {
        UUID ownerId = getOwnerId(world);
        if (ownerId == null) {
            return BASE_SHEEP_LIMIT;
        }
        int maxLimit = hasSacrificeUnlock(ownerId, SACRIFICE_UNLOCK_MAX_SHEEP_100)
                ? SACRIFICE_UNLOCK_MAX_SHEEP_LIMIT
                : MAX_SHEEP_LIMIT;
        return Math.min(
                maxLimit,
                BASE_SHEEP_LIMIT + Math.max(0, extraLimitByPlayer.getOrDefault(ownerId, 0)));
    }

    public static BigInteger getUpgradeCost(Player player) {
        return getDoubledUpgradeCostBig(LIMIT_UPGRADE_COST, getLimitUpgradeLevel(player));
    }

    public static int getLimitUpgradeStep() {
        return LIMIT_UPGRADE_STEP;
    }

    public static boolean upgradeLimit(Player player) {
        if (player == null) {
            return false;
        }
        int maxLimit = hasSacrificeUnlock(player, SACRIFICE_UNLOCK_MAX_SHEEP_100)
                ? SACRIFICE_UNLOCK_MAX_SHEEP_LIMIT
                : MAX_SHEEP_LIMIT;
        if (getPlayerLimit(player) >= maxLimit) {
            return false;
        }
        BigInteger cost = getUpgradeCost(player);
        if (!canSpendUpgradePointsDuringTutorial(player, cost)) {
            return false;
        }
        if (!trySpendPoints(player, cost)) {
            return false;
        }
        UUID playerId = player.getUniqueId();
        int currentExtra = Math.max(0, extraLimitByPlayer.getOrDefault(playerId, 0));
        int maxExtra = maxLimit - BASE_SHEEP_LIMIT;
        extraLimitByPlayer.put(playerId, Math.min(maxExtra, currentExtra + LIMIT_UPGRADE_STEP));
        saveData();
        return true;
    }

    public static int getLimitUpgradeLevel(Player player) {
        if (player == null) {
            return 0;
        }
        int extra = Math.max(0, extraLimitByPlayer.getOrDefault(player.getUniqueId(), 0));
        int maxLimit = hasSacrificeUnlock(player, SACRIFICE_UNLOCK_MAX_SHEEP_100)
                ? SACRIFICE_UNLOCK_MAX_SHEEP_LIMIT
                : MAX_SHEEP_LIMIT;
        int maxExtra = maxLimit - BASE_SHEEP_LIMIT;
        return Math.min(maxExtra, extra) / LIMIT_UPGRADE_STEP;
    }

    public static int getEggSpeedLevel(Player player) {
        if (player == null) {
            return 0;
        }
        return eggSpeedLevelByPlayer.getOrDefault(player.getUniqueId(), 0);
    }

    public static int getEggIntervalSeconds(Player player) {
        if (player == null) {
            return BASE_EGG_INTERVAL_SECONDS;
        }
        int minEggInterval = hasSacrificeUnlock(player, SACRIFICE_UNLOCK_EGG_COOLDOWN_TO_1S)
                ? MIN_EGG_INTERVAL_SECONDS_WITH_SACRIFICE
                : MIN_EGG_INTERVAL_SECONDS;
        return Math.max(minEggInterval,
                BASE_EGG_INTERVAL_SECONDS - eggSpeedLevelByPlayer.getOrDefault(player.getUniqueId(), 0));
    }

    public static int getEggSpeedMaxLevel(Player player) {
        long computed = (long) EGG_SPEED_BASE_MAX_LEVEL
                + (long) getPrestigeHigherMaxLevel(player) * PRESTIGE_CAP_BONUS_PER_LEVEL;
        int hardCap = hasSacrificeUnlock(player, SACRIFICE_UNLOCK_EGG_COOLDOWN_TO_1S)
                ? EGG_SPEED_MAX_LEVEL + 1
                : EGG_SPEED_MAX_LEVEL;
        return computed >= hardCap ? hardCap : (int) computed;
    }

    public static int getWoolRegenMaxLevel(Player player) {
        long computed = (long) WOOL_REGEN_BASE_MAX_LEVEL
                + (long) getPrestigeHigherMaxLevel(player) * PRESTIGE_CAP_BONUS_PER_LEVEL;
        return computed >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) computed;
    }

    public static int getHigherTierChanceMaxLevel(Player player) {
        long computed = (long) HIGHER_TIER_CHANCE_BASE_MAX_LEVEL
                + (long) getPrestigeHigherMaxLevel(player) * PRESTIGE_CAP_BONUS_PER_LEVEL;
        long softCapped = Math.min(computed, HIGHER_TIER_CHANCE_MAX_LEVEL);
        return (int) Math.min(softCapped, HIGHER_TIER_CHANCE_HARD_MAX_LEVEL);
    }

    public static int getWoolRegenLevel(Player player) {
        if (player == null) {
            return 0;
        }
        return Math.max(0, woolRegenLevelByPlayer.getOrDefault(player.getUniqueId(), 0));
    }

    public static int getHigherTierChanceLevel(Player player) {
        if (player == null) {
            return 0;
        }
        return higherTierChanceLevelByPlayer.getOrDefault(player.getUniqueId(), 0);
    }

    public static int getHigherTierChancePercent(Player player) {
        if (player == null) {
            return 0;
        }
        int base = Math.min(HIGHER_TIER_CHANCE_BASE_CAP_PERCENT,
                higherTierChanceLevelByPlayer.getOrDefault(player.getUniqueId(), 0) * 5);
        if (isCountAbilityActive(activeLuckyBurstUsesByPlayer, luckyBurstEnabledByPlayer, player.getUniqueId())) {
            base += QUEST_LUCKY_BURST_SPAWN_CHANCE_BONUS_PERCENT;
        }
        return Math.min(100, base);
    }

    public static int getHigherTierChancePercent(World world) {
        UUID ownerId = getOwnerId(world);
        if (ownerId == null) {
            return 0;
        }
        int base = Math.min(HIGHER_TIER_CHANCE_BASE_CAP_PERCENT,
                higherTierChanceLevelByPlayer.getOrDefault(ownerId, 0) * 5);
        if (isCountAbilityActive(activeLuckyBurstUsesByPlayer, luckyBurstEnabledByPlayer, ownerId)) {
            base += QUEST_LUCKY_BURST_SPAWN_CHANCE_BONUS_PERCENT;
        }
        return Math.min(100, base);
    }

    public static SheepTier rollSpawnTier(World world) {
        int cap = getUnlockedTierCap(world);
        int baseTierLevel = getBaseSpawnTierLevel(world);
        int chosen = Math.min(baseTierLevel, cap);
        int chance = getHigherTierChancePercent(world);
        int maxChanceTier = Math.min(cap, SheepTier.RAINBOW.getLevel() - 1);
        while (chosen < maxChanceTier && RANDOM.nextInt(100) < chance) {
            chosen++;
            chance = Math.max(5, chance / 2);
        }
        return SheepTier.byLevel(chosen);
    }

    public static void upgradeSheepBelowMinimumSpawnTier(World world) {
        if (world == null || !isSheepFarmWorld(world)) {
            return;
        }

        int minimumTierLevel = getBaseSpawnTierLevel(world);
        SheepTier minimumTier = SheepTier.byLevel(minimumTierLevel);
        for (Sheep sheep : world.getEntitiesByClass(Sheep.class)) {
            if (sheep == null || !sheep.isValid() || sheep.isDead()) {
                continue;
            }
            SheepTier currentTier = getSheepTier(sheep);
            if (currentTier.getLevel() >= minimumTierLevel) {
                continue;
            }
            setSheepTier(sheep, minimumTier);
        }
    }

    public static void tickEggDistribution(Player player) {
        EGG_MODULE.tickEggDistribution(player);
    }

    public static int getStartEggsBonus(Player player) {
        return player == null ? 0 : prestigeStartEggsByPlayer.getOrDefault(player.getUniqueId(), 0) * 10;
    }

    public static int getEggCap(Player player) {
        if (player == null) {
            return BASE_EGG_CAP;
        }
        return BASE_EGG_CAP + getPrestigeEggCapLevel(player) * PRESTIGE_EGG_CAP_STEP;
    }

    public static void addEggs(Player player, int amount) {
        EGG_MODULE.addEggs(player, amount);
    }

    private static boolean tryConsumeEgg(Player player) {
        return EGG_MODULE.tryConsumeEgg(player);
    }

    public static boolean spawnSheepFromEgg(Player player, Location spawnLocation) {
        if (player == null || spawnLocation == null || spawnLocation.getWorld() == null) {
            return false;
        }
        if (!isSheepFarmWorld(player.getWorld()) || !isFarmOwner(player, player.getWorld())) {
            return false;
        }
        if (isWorldAtLimit(player.getWorld())) {
            return false;
        }
        if (!tryConsumeEgg(player)) {
            return false;
        }

        Sheep sheep = player.getWorld().spawn(spawnLocation, Sheep.class);
        setSheepTier(sheep, rollSpawnTier(player.getWorld()));
        UUID playerId = player.getUniqueId();
        if (isCountAbilityActive(activeLuckyBurstUsesByPlayer, luckyBurstEnabledByPlayer, playerId)) {
            consumeCountAbilityUse(activeLuckyBurstUsesByPlayer, playerId);
            saveData();
        }
        return true;
    }

    public static int getPrestigeDoublePointsChanceLevel(Player player) {
        return player == null ? 0
                : Math.min(PRESTIGE_DOUBLE_POINTS_MAX_LEVEL,
                        Math.max(0, prestigeDoublePointsChanceByPlayer.getOrDefault(player.getUniqueId(), 0)));
    }

    public static int getDoublePointsChancePercent(Player player) {
        return Math.min(100, getPrestigeDoublePointsChanceLevel(player) * 5);
    }

    public static int getPrestigeHigherMaxLevel(Player player) {
        if (player == null) {
            return 0;
        }
        int raw = prestigeHigherMaxLevelByPlayer.getOrDefault(player.getUniqueId(), 0);
        return Math.max(0, raw);
    }

    public static int getPrestigeStartEggsLevel(Player player) {
        return player == null ? 0 : prestigeStartEggsByPlayer.getOrDefault(player.getUniqueId(), 0);
    }

    public static int getPrestigeQuestRewardLevel(Player player) {
        return player == null ? 0 : prestigeQuestRewardByPlayer.getOrDefault(player.getUniqueId(), 0);
    }

    public static int getPrestigeEggCapLevel(Player player) {
        return player == null ? 0 : prestigeEggCapByPlayer.getOrDefault(player.getUniqueId(), 0);
    }

    public static int getBaseSpawnTierLevel(Player player) {
        return player == null ? 0
                : Math.min(
                        SheepTier.RAINBOW.getLevel(),
                        prestigeBaseSpawnTierByPlayer.getOrDefault(player.getUniqueId(), 0));
    }

    public static int getBaseSpawnTierLevel(World world) {
        UUID ownerId = getOwnerId(world);
        if (ownerId == null) {
            return 0;
        }
        return Math.min(
                SheepTier.RAINBOW.getLevel(),
                prestigeBaseSpawnTierByPlayer.getOrDefault(ownerId, 0));
    }

    public static SheepTier getBaseSpawnTier(Player player) {
        return SheepTier.byLevel(getBaseSpawnTierLevel(player));
    }

    public static int getPrestigeDoublePointsCost(Player player) {
        if (getPrestigeDoublePointsChanceLevel(player) >= PRESTIGE_DOUBLE_POINTS_MAX_LEVEL) {
            return 0;
        }
        return getPrestigeUpgradeCost(PRESTIGE_DOUBLE_POINTS_BASE_COST, getPrestigeDoublePointsChanceLevel(player));
    }

    public static int getPrestigeHigherMaxLevelCost(Player player) {
        return getPrestigeUpgradeCost(PRESTIGE_HIGHER_MAX_LEVEL_BASE_COST, getPrestigeHigherMaxLevel(player));
    }

    public static int getPrestigeStartEggsCost(Player player) {
        return getPrestigeUpgradeCost(PRESTIGE_START_EGGS_BASE_COST, getPrestigeStartEggsLevel(player));
    }

    public static int getPrestigeEggCapCost(Player player) {
        return getPrestigeUpgradeCost(PRESTIGE_EGG_CAP_BASE_COST, getPrestigeEggCapLevel(player));
    }

    public static int getPrestigeBaseSpawnTierCost(Player player) {
        return getPrestigeUpgradeCost(PRESTIGE_BASE_SPAWN_TIER_BASE_COST, getBaseSpawnTierLevel(player));
    }

    public static int getPrestigeQuestRewardCost(Player player) {
        return getPrestigeUpgradeCost(PRESTIGE_QUEST_REWARD_BASE_COST, getPrestigeQuestRewardLevel(player));
    }

    private static double getPrestigeQuestRewardMultiplier(Player player) {
        return 1.0D + getPrestigeQuestRewardLevel(player) * PRESTIGE_QUEST_REWARD_BONUS_PER_LEVEL;
    }

    public static int getQuestUpgradeDurationLevel(Player player) {
        return player == null ? 0 : questUpgradeDurationByPlayer.getOrDefault(player.getUniqueId(), 0);
    }

    public static int getQuestUpgradePowerLevel(Player player) {
        return player == null ? 0 : questUpgradePowerByPlayer.getOrDefault(player.getUniqueId(), 0);
    }

    public static int getQuestUpgradeDurationCost(Player player) {
        return getDoubledUpgradeCost(QUEST_UPGRADE_DURATION_BASE_COST, getQuestUpgradeDurationLevel(player));
    }

    public static int getQuestUpgradePowerCost(Player player) {
        return getDoubledUpgradeCost(QUEST_UPGRADE_POWER_BASE_COST, getQuestUpgradePowerLevel(player));
    }

    private static long getAbilityDurationMs(Player player, long baseDurationMs) {
        return baseDurationMs + (getQuestUpgradeDurationLevel(player) * 30_000L);
    }

    private static int getAbilityUseCount(Player player, long baseDurationMs) {
        return Math.max(1, (int) Math.ceil(getAbilityDurationMs(player, baseDurationMs) / 1000.0D));
    }

    private static int getQuestLuckyBurstCost(Player player) {
        int reduction = getQuestUpgradePowerLevel(player);
        return Math.max(3, QUEST_LUCKY_BURST_BASE_COST - reduction);
    }

    private static int getQuestWoolRushCost(Player player) {
        int reduction = getQuestUpgradePowerLevel(player);
        return Math.max(4, QUEST_WOOL_RUSH_BASE_COST - reduction);
    }

    private static int getQuestJackpotCost(Player player) {
        int reduction = getQuestUpgradePowerLevel(player);
        return Math.max(6, QUEST_JACKPOT_SHEARS_BASE_COST - reduction);
    }

    private static int getQuestAutoMergeCost(Player player) {
        int reduction = getQuestUpgradePowerLevel(player);
        return Math.max(8, QUEST_AUTO_MERGE_BASE_COST - reduction);
    }

    private static int getQuestAutoShearCost(Player player) {
        int reduction = getQuestUpgradePowerLevel(player);
        return Math.max(5, QUEST_AUTO_SHEAR_BASE_COST - reduction);
    }

    private static boolean isAbilityActive(Map<UUID, Long> activeUntil, UUID playerId) {
        if (playerId == null) {
            return false;
        }
        return activeUntil.getOrDefault(playerId, 0L) > System.currentTimeMillis();
    }

    private static boolean isCountAbilityActive(Map<UUID, Integer> remainingUsesByPlayer,
            Map<UUID, Boolean> enabledByPlayer,
            UUID playerId) {
        if (remainingUsesByPlayer == null || enabledByPlayer == null || playerId == null) {
            return false;
        }
        return remainingUsesByPlayer.getOrDefault(playerId, 0) > 0
                && enabledByPlayer.getOrDefault(playerId, true);
    }

    private static int getCountAbilityRemainingUses(Map<UUID, Integer> remainingUsesByPlayer, UUID playerId) {
        if (remainingUsesByPlayer == null || playerId == null) {
            return 0;
        }
        return Math.max(0, remainingUsesByPlayer.getOrDefault(playerId, 0));
    }

    private static boolean toggleCountAbilityEnabled(Player player, Map<UUID, Integer> remainingUsesByPlayer,
            Map<UUID, Boolean> enabledByPlayer) {
        if (player == null || remainingUsesByPlayer == null || enabledByPlayer == null) {
            return false;
        }
        UUID playerId = player.getUniqueId();
        if (remainingUsesByPlayer.getOrDefault(playerId, 0) <= 0) {
            return false;
        }
        boolean next = !enabledByPlayer.getOrDefault(playerId, true);
        enabledByPlayer.put(playerId, next);
        saveData();
        return next;
    }

    private static void consumeCountAbilityUse(Map<UUID, Integer> remainingUsesByPlayer, UUID playerId) {
        if (remainingUsesByPlayer == null || playerId == null) {
            return;
        }
        int current = Math.max(0, remainingUsesByPlayer.getOrDefault(playerId, 0));
        if (current <= 1) {
            remainingUsesByPlayer.remove(playerId);
        } else {
            remainingUsesByPlayer.put(playerId, current - 1);
        }
    }

    private static boolean isAbilityPaused(Map<UUID, Long> pausedRemainingMsByPlayer, UUID playerId) {
        if (pausedRemainingMsByPlayer == null || playerId == null) {
            return false;
        }
        return pausedRemainingMsByPlayer.getOrDefault(playerId, 0L) > 0L;
    }

    private static boolean pauseQuestAbility(Player player, Map<UUID, Long> activeUntil,
            Map<UUID, Long> pausedRemainingMsByPlayer) {
        if (player == null || activeUntil == null || pausedRemainingMsByPlayer == null) {
            return false;
        }
        UUID playerId = player.getUniqueId();
        long remaining = getAbilityRemainingMs(activeUntil, playerId);
        if (remaining <= 0L) {
            return false;
        }
        activeUntil.remove(playerId);
        pausedRemainingMsByPlayer.put(playerId, remaining);
        saveData();
        return true;
    }

    private static boolean resumeQuestAbility(Player player, Map<UUID, Long> activeUntil,
            Map<UUID, Long> pausedRemainingMsByPlayer, Runnable onResume) {
        if (player == null || activeUntil == null || pausedRemainingMsByPlayer == null) {
            return false;
        }
        UUID playerId = player.getUniqueId();
        long remaining = Math.max(0L, pausedRemainingMsByPlayer.getOrDefault(playerId, 0L));
        if (remaining <= 0L) {
            return false;
        }
        pausedRemainingMsByPlayer.remove(playerId);
        activeUntil.put(playerId, System.currentTimeMillis() + remaining);
        if (onResume != null) {
            onResume.run();
        }
        saveData();
        return true;
    }

    private static boolean activateQuestAbility(Player player, Map<UUID, Long> activeUntil, int questPointCost,
            long durationMs, Sound sound, org.bukkit.Particle particle) {
        if (player == null) {
            return false;
        }
        if (!trySpendQuestPoints(player, questPointCost)) {
            return false;
        }
        long until = System.currentTimeMillis() + durationMs;
        activeUntil.put(player.getUniqueId(), until);
        playSound(player, sound, 1.0f, 1.2f);
        player.getWorld().spawnParticle(particle, player.getLocation().add(0, 1.0, 0), 25, 0.35, 0.5, 0.35, 0.02);
        return true;
    }

    private static boolean activateCountQuestAbility(Player player,
            Map<UUID, Integer> remainingUsesByPlayer,
            Map<UUID, Boolean> enabledByPlayer,
            int questPointCost,
            int useCount,
            Sound sound,
            org.bukkit.Particle particle) {
        if (player == null || remainingUsesByPlayer == null || enabledByPlayer == null) {
            return false;
        }
        if (!trySpendQuestPoints(player, questPointCost)) {
            return false;
        }
        UUID playerId = player.getUniqueId();
        remainingUsesByPlayer.put(playerId, addSaturated(remainingUsesByPlayer.getOrDefault(playerId, 0), useCount));
        enabledByPlayer.put(playerId, true);
        playSound(player, sound, 1.0f, 1.2f);
        player.getWorld().spawnParticle(particle, player.getLocation().add(0, 1.0, 0), 25, 0.35, 0.5, 0.35, 0.02);
        saveData();
        return true;
    }

    private static boolean upgradeQuestDuration(Player player) {
        int cost = getQuestUpgradeDurationCost(player);
        if (!trySpendQuestPoints(player, cost)) {
            return false;
        }
        questUpgradeDurationByPlayer.put(player.getUniqueId(), getQuestUpgradeDurationLevel(player) + 1);
        saveData();
        return true;
    }

    private static boolean upgradeQuestPower(Player player) {
        int cost = getQuestUpgradePowerCost(player);
        if (!trySpendQuestPoints(player, cost)) {
            return false;
        }
        questUpgradePowerByPlayer.put(player.getUniqueId(), getQuestUpgradePowerLevel(player) + 1);
        saveData();
        return true;
    }

    public static long getPrestigeRefundRemainingMs(Player player) {
        if (player == null) {
            return 0L;
        }
        long nextRefund = nextPrestigeRefundTimestampByPlayer.getOrDefault(player.getUniqueId(), 0L);
        return Math.max(0L, nextRefund - System.currentTimeMillis());
    }

    private static int getSpentCostForLevel(int baseCost, int currentLevel) {
        if (baseCost <= 0 || currentLevel <= 0) {
            return 0;
        }
        long total = 0L;
        for (int level = 0; level < currentLevel; level++) {
            total += getDoubledUpgradeCost(baseCost, level);
            if (total >= Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            }
        }
        return (int) total;
    }

    private static int getLinearSpentCostForLevel(int baseCost, int currentLevel) {
        if (baseCost <= 0 || currentLevel <= 0) {
            return 0;
        }
        BigInteger total = BigInteger.ZERO;
        for (int level = 0; level < currentLevel; level++) {
            total = total.add(getLinearUpgradeCostBig(baseCost, level));
            if (total.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) >= 0) {
                return Integer.MAX_VALUE;
            }
        }
        return total.intValue();
    }

    private static int getTotalPrestigePointsForLevel(int prestigeLevel) {
        if (prestigeLevel <= 0) {
            return 0;
        }
        long total = (long) prestigeLevel * (prestigeLevel + 1L) / 2L;
        return total >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;
    }

    private static int getPrestigeUpgradeCost(int baseCost, int level) {
        return toIntClamped(getLinearUpgradeCostBig(baseCost, level));
    }

    private static BigInteger getLinearUpgradeCostBig(int baseCost, int level) {
        if (baseCost <= 0) {
            return BigInteger.ZERO;
        }
        int normalizedLevel = Math.max(0, level);
        return BigInteger.valueOf(baseCost).multiply(BigInteger.valueOf((long) normalizedLevel + 1L));
    }

    private static int addSaturated(int current, int delta) {
        long total = (long) current + Math.max(0L, delta);
        return total >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;
    }

    private static long addSaturatedLong(long current, long delta) {
        long total = (long) current + Math.max(0L, delta);
        return total < 0L ? Long.MAX_VALUE : total;
    }

    private static int toIntClamped(BigInteger value) {
        if (value == null || value.signum() <= 0) {
            return 0;
        }
        if (value.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0) {
            return Integer.MAX_VALUE;
        }
        return value.intValue();
    }

    private static void saveSheepSnapshots(String basePath, Map<UUID, List<SheepSnapshot>> snapshotsByPlayer) {
        if (dataConfig == null || basePath == null || snapshotsByPlayer == null) {
            return;
        }
        for (Map.Entry<UUID, List<SheepSnapshot>> entry : snapshotsByPlayer.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            for (int index = 0; index < entry.getValue().size(); index++) {
                SheepSnapshot snapshot = entry.getValue().get(index);
                if (snapshot == null) {
                    continue;
                }
                String path = basePath + "." + entry.getKey() + "." + index;
                dataConfig.set(path + ".tier", snapshot.tierLevel);
                dataConfig.set(path + ".x", snapshot.x);
                dataConfig.set(path + ".y", snapshot.y);
                dataConfig.set(path + ".z", snapshot.z);
                dataConfig.set(path + ".sheared", snapshot.sheared);
                dataConfig.set(path + ".nextEatAt", snapshot.nextEatAt);
                dataConfig.set(path + ".mergedCount", snapshot.mergedCount);
            }
        }
    }

    private static void loadSheepSnapshots(String basePath, Map<UUID, List<SheepSnapshot>> snapshotsByPlayer) {
        if (dataConfig == null || basePath == null || snapshotsByPlayer == null
                || !dataConfig.isConfigurationSection(basePath)) {
            return;
        }
        org.bukkit.configuration.ConfigurationSection root = dataConfig.getConfigurationSection(basePath);
        if (root == null) {
            return;
        }
        root.getKeys(false).forEach(key -> {
            try {
                UUID uuid = UUID.fromString(key);
                org.bukkit.configuration.ConfigurationSection playerSection = root.getConfigurationSection(key);
                if (playerSection == null) {
                    return;
                }
                List<SheepSnapshot> snapshots = new ArrayList<>();
                playerSection.getKeys(false).stream().sorted().forEach(indexKey -> {
                    String path = basePath + "." + key + "." + indexKey;
                    snapshots.add(new SheepSnapshot(
                            dataConfig.getInt(path + ".tier", SheepTier.WHITE.getLevel()),
                            dataConfig.getDouble(path + ".x", 0.5D),
                            dataConfig.getDouble(path + ".y", FARM_BASE_Y + 1.0D),
                            dataConfig.getDouble(path + ".z", 0.5D),
                            dataConfig.getBoolean(path + ".sheared", false),
                            Math.max(0L, dataConfig.getLong(path + ".nextEatAt", 0L)),
                            Math.max(1, dataConfig.getInt(path + ".mergedCount", 1))));
                });
                snapshotsByPlayer.put(uuid, snapshots);
            } catch (IllegalArgumentException ignored) {
                // Ignore invalid UUIDs.
            }
        });
    }

    private static void resetPrestigeUpgrades(UUID playerId, boolean clearRefundCooldown) {
        if (playerId == null) {
            return;
        }
        prestigeDoublePointsChanceByPlayer.remove(playerId);
        prestigeHigherMaxLevelByPlayer.remove(playerId);
        prestigeStartEggsByPlayer.remove(playerId);
        prestigeEggCapByPlayer.remove(playerId);
        prestigeBaseSpawnTierByPlayer.remove(playerId);
        prestigeQuestRewardByPlayer.remove(playerId);
        comboMaxUpgradeByPlayer.remove(playerId);
        Double comboScore = comboScoreByPlayer.get(playerId);
        if (comboScore != null) {
            comboScoreByPlayer.put(playerId, Math.min(COMBO_BASE_MAX_SCORE, Math.max(0.0D, comboScore)));
        }
        if (clearRefundCooldown) {
            nextPrestigeRefundTimestampByPlayer.remove(playerId);
        }
    }

    private static int getPrestigeRefundAmount(Player player) {
        if (player == null) {
            return 0;
        }
        int total = 0;
        total += getLinearSpentCostForLevel(PRESTIGE_DOUBLE_POINTS_BASE_COST,
                getPrestigeDoublePointsChanceLevel(player));
        total += getLinearSpentCostForLevel(PRESTIGE_HIGHER_MAX_LEVEL_BASE_COST, getPrestigeHigherMaxLevel(player));
        total += getLinearSpentCostForLevel(PRESTIGE_START_EGGS_BASE_COST, getPrestigeStartEggsLevel(player));
        total += getLinearSpentCostForLevel(PRESTIGE_EGG_CAP_BASE_COST, getPrestigeEggCapLevel(player));
        total += getLinearSpentCostForLevel(PRESTIGE_BASE_SPAWN_TIER_BASE_COST, getBaseSpawnTierLevel(player));
        total += getLinearSpentCostForLevel(PRESTIGE_QUEST_REWARD_BASE_COST, getPrestigeQuestRewardLevel(player));
        total += getLinearSpentCostForLevel(COMBO_MAX_BASE_PRESTIGE_COST, getComboMaxUpgradeLevel(player));
        return Math.max(0, total);
    }

    private static boolean tryRefundPrestigePoints(Player player) {
        if (player == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        long nextRefund = nextPrestigeRefundTimestampByPlayer.getOrDefault(player.getUniqueId(), 0L);
        if (now < nextRefund) {
            return false;
        }

        int refund = getPrestigeRefundAmount(player);
        if (refund <= 0) {
            return false;
        }

        UUID playerId = player.getUniqueId();
        prestigePointsByPlayer.put(playerId, addSaturated(getPrestigePoints(player), refund));
        resetPrestigeUpgrades(playerId, false);
        nextPrestigeRefundTimestampByPlayer.put(playerId, now + PRESTIGE_REFUND_COOLDOWN_MS);
        saveData();
        return true;
    }

    private static String formatDuration(long durationMs) {
        long totalSeconds = Math.max(0L, durationMs / 1000L);
        return totalSeconds + "s";
    }

    private static long getAbilityRemainingMs(Map<UUID, Long> activeUntil, UUID playerId) {
        if (activeUntil == null || playerId == null) {
            return 0L;
        }
        return Math.max(0L, activeUntil.getOrDefault(playerId, 0L) - System.currentTimeMillis());
    }

    private static long getPausedAbilityRemainingMs(Map<UUID, Long> pausedRemainingMsByPlayer, UUID playerId) {
        return 0L;
    }

    private static String getAbilityMenuStatus(Map<UUID, Long> activeUntil, Map<UUID, Long> pausedRemainingMsByPlayer,
            UUID playerId) {
        long remaining = getAbilityRemainingMs(activeUntil, playerId);
        return remaining > 0L ? "Status: active (" + formatDuration(remaining) + " left)" : "Status: inactive";
    }

    private static String getAbilityScoreLine(String label, Map<UUID, Long> activeUntil,
            Map<UUID, Long> pausedRemainingMsByPlayer, UUID playerId) {
        long remaining = getAbilityRemainingMs(activeUntil, playerId);
        return label + ": " + (remaining > 0L ? formatDuration(remaining) : "inactive");
    }

    private static String getCountAbilityMenuStatus(Map<UUID, Integer> remainingUsesByPlayer,
            Map<UUID, Boolean> enabledByPlayer,
            UUID playerId) {
        int remaining = getCountAbilityRemainingUses(remainingUsesByPlayer, playerId);
        if (remaining <= 0) {
            return "&8Status: DEACTIVATED";
        }
        boolean enabled = enabledByPlayer.getOrDefault(playerId, true);
        return (enabled ? "&aStatus: ON" : "&cStatus: OFF") + " (&b" + remaining + "&7 uses left)";
    }

    private static String getCountAbilityToggleActionLine(Map<UUID, Integer> remainingUsesByPlayer,
            Map<UUID, Boolean> enabledByPlayer,
            UUID playerId) {
        int remaining = getCountAbilityRemainingUses(remainingUsesByPlayer, playerId);
        if (remaining <= 0) {
            return "&aClick: Activate";
        }
        boolean enabled = enabledByPlayer.getOrDefault(playerId, true);
        return enabled ? "&cClick: Toggle OFF" : "&aClick: Toggle ON";
    }

    private static String getCountAbilityScoreLine(String label,
            Map<UUID, Integer> remainingUsesByPlayer,
            Map<UUID, Boolean> enabledByPlayer,
            UUID playerId) {
        int remaining = getCountAbilityRemainingUses(remainingUsesByPlayer, playerId);
        if (remaining <= 0) {
            return label + ": inactive";
        }
        return label + ": " + remaining + " uses "
                + (enabledByPlayer.getOrDefault(playerId, true) ? "ON" : "OFF");
    }

    private static long getQuestResetRemainingMs(Player player) {
        if (player == null) {
            return 0L;
        }
        long now = System.currentTimeMillis();
        long nextReset = nextQuestResetTimestampByPlayer.getOrDefault(player.getUniqueId(), 0L);
        if (nextReset <= 0L) {
            return getQuestResetIntervalMs(player);
        }
        return Math.max(0L, nextReset - now);
    }

    private static boolean trySpendPrestigePoints(Player player, int points) {
        if (player == null || points <= 0) {
            return false;
        }
        int current = getPrestigePoints(player);
        if (current < points) {
            return false;
        }
        prestigePointsByPlayer.put(player.getUniqueId(), current - points);
        saveData();
        return true;
    }

    private static boolean wouldDipBelowTutorialPrestigeReserve(Player player, BigInteger spendPoints) {
        if (player == null || spendPoints == null || spendPoints.signum() <= 0
                || !isTutorialInProgress(player) || !isInTutorialWorld(player)) {
            return false;
        }

        TutorialStep step = getCurrentTutorialStep(player);
        if (step != TutorialStep.OPEN_PRESTIGE && step != TutorialStep.PRESTIGE_ONCE) {
            return false;
        }

        BigInteger afterSpend = getPlayerPointsBig(player).subtract(spendPoints);
        return afterSpend.compareTo(getPrestigeCostBig(player)) < 0;
    }

    private static boolean canSpendUpgradePointsDuringTutorial(Player player, BigInteger spendPoints) {
        if (!wouldDipBelowTutorialPrestigeReserve(player, spendPoints)) {
            return true;
        }
        player.sendMessage(warning("Tutorial: keep at least " + formatPoints(getPrestigeCostBig(player))
                + " points for your prestige step."));
        player.sendMessage(hint("Merge/shear a bit more before buying this upgrade."));
        return false;
    }

    private static boolean canSpendUpgradePointsDuringTutorial(Player player, long spendPoints) {
        return canSpendUpgradePointsDuringTutorial(player, BigInteger.valueOf(spendPoints));
    }

    private static boolean upgradePrestigeDoublePoints(Player player) {
        if (player == null || getPrestigeDoublePointsChanceLevel(player) >= PRESTIGE_DOUBLE_POINTS_MAX_LEVEL) {
            return false;
        }
        int cost = getPrestigeDoublePointsCost(player);
        if (!trySpendPrestigePoints(player, cost)) {
            return false;
        }
        prestigeDoublePointsChanceByPlayer.put(player.getUniqueId(), getPrestigeDoublePointsChanceLevel(player) + 1);
        saveData();
        return true;
    }

    private static boolean upgradePrestigeHigherMaxLevel(Player player) {
        if (player == null) {
            return false;
        }
        int current = getPrestigeHigherMaxLevel(player);
        if (current >= Integer.MAX_VALUE) {
            return false;
        }
        int cost = getPrestigeHigherMaxLevelCost(player);
        if (!trySpendPrestigePoints(player, cost)) {
            return false;
        }
        prestigeHigherMaxLevelByPlayer.put(player.getUniqueId(), current + 1);
        saveData();
        return true;
    }

    private static boolean upgradePrestigeStartEggs(Player player) {
        if (player == null) {
            return false;
        }
        int cost = getPrestigeStartEggsCost(player);
        if (!trySpendPrestigePoints(player, cost)) {
            return false;
        }
        int oldBonus = getStartEggsBonus(player);
        prestigeStartEggsByPlayer.put(player.getUniqueId(), getPrestigeStartEggsLevel(player) + 1);
        int newBonus = getStartEggsBonus(player);
        int gainedEggs = Math.max(0, newBonus - oldBonus);
        if (gainedEggs > 0) {
            addEggs(player, gainedEggs);
        }
        saveData();
        return true;
    }

    private static boolean upgradePrestigeEggCap(Player player) {
        int cost = getPrestigeEggCapCost(player);
        if (!trySpendPrestigePoints(player, cost)) {
            return false;
        }
        prestigeEggCapByPlayer.put(player.getUniqueId(), getPrestigeEggCapLevel(player) + 1);
        saveData();
        return true;
    }

    private static boolean upgradePrestigeBaseSpawnTier(Player player) {
        if (player == null) {
            return false;
        }
        int currentLevel = getBaseSpawnTierLevel(player);
        if (currentLevel >= SheepTier.RAINBOW.getLevel()) {
            return false;
        }
        int cost = getPrestigeBaseSpawnTierCost(player);
        if (!trySpendPrestigePoints(player, cost)) {
            return false;
        }
        prestigeBaseSpawnTierByPlayer.put(player.getUniqueId(), currentLevel + 1);
        upgradeSheepBelowMinimumSpawnTier(player.getWorld());
        saveData();
        return true;
    }

    private static boolean upgradePrestigeQuestReward(Player player) {
        if (player == null) {
            return false;
        }
        int currentLevel = getPrestigeQuestRewardLevel(player);
        if (currentLevel >= PRESTIGE_QUEST_REWARD_MAX_LEVEL) {
            return false;
        }
        int cost = getPrestigeQuestRewardCost(player);
        if (!trySpendPrestigePoints(player, cost)) {
            return false;
        }
        prestigeQuestRewardByPlayer.put(player.getUniqueId(), currentLevel + 1);
        saveData();
        return true;
    }

    public static void resetEggTimer(Player player) {
        EGG_MODULE.resetEggTimer(player);
    }

    public static void clearEggTimer(Player player) {
        EGG_MODULE.clearEggTimer(player);
    }

    public static void openUpgradeMenu(Player player) {
        if (player == null) {
            return;
        }
        markTutorialUpgradeOpened(player);

        Inventory inventory = Bukkit.createInventory(null, 27, UPGRADE_MENU_TITLE);
        inventory.setItem(4, MenuItemFactory.create(
                Material.BOOK,
                "Upgrade Guide",
                List.of(
                        "Quick order:",
                        "1) Sheep Limit or Egg Speed",
                        "2) Wool Regen",
                        "3) Tier Chance",
                        "Keep points for utility unlocks")));
        int limitLevel = getLimitUpgradeLevel(player);
        int currentLimit = getPlayerLimit(player);
        boolean limitMaxed = currentLimit >= MAX_SHEEP_LIMIT;
        BigInteger limitCost = getUpgradeCost(player);
        inventory.setItem(LIMIT_UPGRADE_SLOT, MenuItemFactory.create(
                Material.OAK_FENCE,
                "Sheep Limit",
                List.of(
                        "Level: " + limitLevel + " / " + ((MAX_SHEEP_LIMIT - BASE_SHEEP_LIMIT) / LIMIT_UPGRADE_STEP),
                        "Current limit: " + currentLimit + " / " + MAX_SHEEP_LIMIT,
                        "Next: " + currentLimit + " -> "
                                + Math.min(MAX_SHEEP_LIMIT, currentLimit + getLimitUpgradeStep()),
                        limitMaxed ? "MAXED" : "Cost: " + formatPoints(limitCost) + " points",
                        limitMaxed ? "Limit cap reached" : "Click to purchase")));

        int eggLevel = getEggSpeedLevel(player);
        int eggMaxLevel = getEggSpeedMaxLevel(player);
        int eggCurrentSeconds = getEggIntervalSeconds(player);
        int eggNextLevel = Math.min(eggMaxLevel, eggLevel + 1);
        int eggNextSeconds = Math.max(MIN_EGG_INTERVAL_SECONDS, BASE_EGG_INTERVAL_SECONDS - eggNextLevel);
        BigInteger eggCost = getEggSpeedUpgradeCost(player);
        inventory.setItem(EGG_SPEED_UPGRADE_SLOT, MenuItemFactory.create(
                Material.CLOCK,
                "Faster Egg Spawn",
                List.of(
                        "Level: " + eggLevel + " / " + eggMaxLevel,
                        "Current: " + eggCurrentSeconds + "s per egg",
                        eggLevel >= eggMaxLevel
                                ? "Next: MAXED"
                                : "Next: " + eggCurrentSeconds + "s -> " + eggNextSeconds + "s",
                        eggLevel >= eggMaxLevel ? "MAXED"
                                : "Cost: " + formatPoints(eggCost) + " points",
                        "Click to purchase")));

        int woolLevel = getWoolRegenLevel(player);
        int woolMaxLevel = getWoolRegenMaxLevel(player);
        String woolCurrentCooldownPercent = getWoolCooldownPercentDisplayAtLevel(woolLevel);
        String woolCurrentReductionPercent = getWoolCooldownReductionPercentDisplayAtLevel(woolLevel);
        String woolCurrentFactor = getWoolCooldownFactorDisplayAtLevel(woolLevel);
        int woolNextLevel = Math.min(woolMaxLevel, woolLevel + 1);
        String woolNextCooldownPercent = getWoolCooldownPercentDisplayAtLevel(woolNextLevel);
        String woolNextReductionPercent = getWoolCooldownReductionPercentDisplayAtLevel(woolNextLevel);
        String woolNextFactor = getWoolCooldownFactorDisplayAtLevel(woolNextLevel);
        BigInteger woolCost = getWoolRegenUpgradeCost(player);
        inventory.setItem(WOOL_REGEN_UPGRADE_SLOT, MenuItemFactory.createEnchanted(
                Material.WHITE_WOOL,
                "Faster Wool Regen",
                List.of(
                        "Level: " + woolLevel + " / " + woolMaxLevel,
                        "Current: " + woolCurrentReductionPercent + "% reduced " + "(duration * " + woolCurrentFactor
                                + ")",
                        woolLevel >= woolMaxLevel
                                ? "Next: MAXED"
                                : "Next: " + woolCurrentCooldownPercent + "% -> " + woolNextCooldownPercent
                                        + "% cooldown (" + woolNextReductionPercent + "% faster, x" + woolNextFactor
                                        + ")",
                        woolLevel >= woolMaxLevel
                                ? "MAXED"
                                : "Cost: " + formatPoints(woolCost) + " points",
                        "Click to purchase")));

        int chanceLevel = getHigherTierChanceLevel(player);
        int chanceMaxLevel = getHigherTierChanceMaxLevel(player);
        int chanceCurrentPercent = Math.min(HIGHER_TIER_CHANCE_BASE_CAP_PERCENT, chanceLevel * 5);
        int chanceNextLevel = Math.min(chanceMaxLevel, chanceLevel + 1);
        int chanceNextPercent = Math.min(HIGHER_TIER_CHANCE_BASE_CAP_PERCENT, chanceNextLevel * 5);
        BigInteger chanceCost = getHigherTierChanceUpgradeCost(player);
        inventory.setItem(HIGHER_TIER_CHANCE_UPGRADE_SLOT, MenuItemFactory.create(
                Material.GOLDEN_APPLE,
                "Higher Tier Spawn Chance",
                List.of(
                        "Level: " + chanceLevel + " / " + chanceMaxLevel,
                        "Current: " + chanceCurrentPercent + "% bonus chance",
                        chanceLevel >= chanceMaxLevel
                                ? "Next: MAXED"
                                : "Next: " + chanceCurrentPercent + "% -> " + chanceNextPercent + "%",
                        "Hard cap: " + HIGHER_TIER_CHANCE_BASE_CAP_PERCENT + "%",
                        chanceLevel >= chanceMaxLevel ? "MAXED"
                                : "Cost: " + formatPoints(chanceCost) + " points",
                        "Click to purchase")));

        inventory.setItem(PRESTIGE_MENU_OPEN_SLOT, MenuItemFactory.create(
                Material.NETHER_STAR,
                "Prestige Upgrades",
                List.of(
                        "Prestige level: " + getPrestigeLevel(player),
                        "Prestige points: " + formatPoints(getPrestigePoints(player)),
                        "Click to open")));

        inventory.setItem(QUEST_MENU_OPEN_SLOT, MenuItemFactory.create(
                Material.BOOK,
                "Quests",
                List.of(
                        "Quest points: " + formatPoints(getQuestPoints(player)),
                        "Next reset: " + formatDuration(getQuestResetRemainingMs(player)),
                        "Shear " + questShearsByPlayer.getOrDefault(player.getUniqueId(), 0) + "/"
                                + QUEST_SHEARS_TARGET,
                        "Spawn " + questSpawnsByPlayer.getOrDefault(player.getUniqueId(), 0) + "/"
                                + QUEST_SPAWNS_TARGET,
                        "Merge " + questMergesByPlayer.getOrDefault(player.getUniqueId(), 0) + "/"
                                + QUEST_MERGES_TARGET,
                        "Click to open")));

        inventory.setItem(SHOP_MENU_OPEN_SLOT, MenuItemFactory.create(
                Material.SHEARS,
                "Shear Shop",
                List.of(
                        "Shear level: " + getShearShopLevel(player),
                        "Points multiplier: x" + getShearPointMultiplier(player),
                        "Click to open")));

        inventory.setItem(COMBO_MENU_OPEN_SLOT, MenuItemFactory.create(
                Material.BLAZE_POWDER,
                "Combo Upgrades",
                List.of(
                        "Combo score: " + (int) Math.floor(getComboScore(player))
                                + " / " + (int) Math.floor(getComboMaxScore(player)),
                        "Points x" + formatComboMultiplier(getComboMultiplier(player, getComboScore(player))),
                        "Click to open")));

        inventory.setItem(AUTOMATION_MENU_OPEN_SLOT, MenuItemFactory.create(
                Material.REDSTONE,
                "Automation",
                List.of(
                        "Automation points: " + formatPoints(getAutomationPoints(player)),
                        "Gain: +1 per " + formatDuration(AUTOMATION_POINT_INTERVAL_MS),
                        "Click to open")));

        inventory.setItem(SACRIFICE_MENU_OPEN_SLOT, MenuItemFactory.create(
                Material.TOTEM_OF_UNDYING,
                "Sacrifice",
                List.of(
                        "Sacrifice points: " + formatPoints(getSacrificePoints(player)),
                        "Unlocks bought: " + getSacrificeUnlocksBought(player) + " / "
                                + SACRIFICE_UNLOCK_MAX_SHEEP_100,
                        "Click to open")));

        player.openInventory(inventory);
    }

    public static boolean isUpgradeMenuTitle(String title) {
        return UPGRADE_MENU_TITLE.equals(title);
    }

    public static boolean isPrestigeMenuTitle(String title) {
        return PRESTIGE_MENU_TITLE.equals(title);
    }

    public static boolean isQuestMenuTitle(String title) {
        return QUEST_MENU_TITLE.equals(title);
    }

    public static boolean isQuestUpgradesMenuTitle(String title) {
        return QUEST_UPGRADES_MENU_TITLE.equals(title);
    }

    public static boolean isShopMenuTitle(String title) {
        return SHOP_MENU_TITLE.equals(title);
    }

    public static boolean isComboShopMenuTitle(String title) {
        return COMBO_SHOP_MENU_TITLE.equals(title);
    }

    public static boolean isAutomationMenuTitle(String title) {
        return AUTOMATION_MENU_TITLE.equals(title);
    }

    public static boolean isSacrificeMenuTitle(String title) {
        return SACRIFICE_MENU_TITLE.equals(title);
    }

    public static boolean isScoreboardMenuTitle(String title) {
        return SCOREBOARD_MENU_TITLE.equals(title);
    }

    private static int getScoreboardLayoutMode(Player player) {
        if (player == null) {
            return 0;
        }
        return Math.max(0, Math.min(1, scoreboardLayoutModeByPlayer.getOrDefault(player.getUniqueId(), 0)));
    }

    private static boolean shouldShowScoreboardQuestPoints(Player player) {
        return player != null && scoreboardShowQuestPointsByPlayer.getOrDefault(player.getUniqueId(), true);
    }

    private static boolean shouldShowScoreboardAutomationPoints(Player player) {
        return player != null && scoreboardShowAutomationPointsByPlayer.getOrDefault(player.getUniqueId(), true);
    }

    private static boolean shouldShowScoreboardSacrificePoints(Player player) {
        return player != null && scoreboardShowSacrificePointsByPlayer.getOrDefault(player.getUniqueId(), true);
    }

    private static boolean shouldShowScoreboardQuestProgress(Player player) {
        return player != null && scoreboardShowQuestProgressByPlayer.getOrDefault(player.getUniqueId(), true);
    }

    private static boolean shouldShowScoreboardAbilityStatus(Player player) {
        return player != null && scoreboardShowAbilityStatusByPlayer.getOrDefault(player.getUniqueId(), true);
    }

    public static void openScoreboardMenu(Player player) {
        if (player == null) {
            return;
        }
        Inventory inventory = Bukkit.createInventory(null, 27, SCOREBOARD_MENU_TITLE);
        int layoutMode = getScoreboardLayoutMode(player);
        inventory.setItem(SCOREBOARD_LAYOUT_SLOT, MenuItemFactory.create(
                Material.BOOK,
                "Layout",
                List.of(
                        "Current: " + (layoutMode == 0 ? "Detailed" : "Compact"),
                        "Detailed: full sections",
                        "Compact: summary only",
                        "Click: Switch layout")));

        inventory.setItem(SCOREBOARD_QUEST_POINTS_SLOT, MenuItemFactory.create(
                Material.BOOK,
                "Quest Points",
                List.of(
                        "Status: " + (shouldShowScoreboardQuestPoints(player) ? "Shown" : "Hidden"),
                        shouldShowScoreboardQuestPoints(player) ? "Click: Hide" : "Click: Show")));

        inventory.setItem(SCOREBOARD_AUTOMATION_POINTS_SLOT, MenuItemFactory.create(
                Material.REDSTONE,
                "Automation Points",
                List.of(
                        "Status: " + (shouldShowScoreboardAutomationPoints(player) ? "Shown" : "Hidden"),
                        shouldShowScoreboardAutomationPoints(player) ? "Click: Hide" : "Click: Show")));

        inventory.setItem(SCOREBOARD_SACRIFICE_POINTS_SLOT, MenuItemFactory.create(
                Material.TOTEM_OF_UNDYING,
                "Sacrifice Points",
                List.of(
                        "Status: " + (shouldShowScoreboardSacrificePoints(player) ? "Shown" : "Hidden"),
                        shouldShowScoreboardSacrificePoints(player) ? "Click: Hide" : "Click: Show")));

        inventory.setItem(SCOREBOARD_QUEST_PROGRESS_SLOT, MenuItemFactory.create(
                Material.MAP,
                "Quest Progress",
                List.of(
                        "Status: " + (shouldShowScoreboardQuestProgress(player) ? "Shown" : "Hidden"),
                        shouldShowScoreboardQuestProgress(player) ? "Click: Hide" : "Click: Show")));

        inventory.setItem(SCOREBOARD_ABILITIES_SLOT, MenuItemFactory.create(
                Material.NETHER_STAR,
                "Ability Status",
                List.of(
                        "Status: " + (shouldShowScoreboardAbilityStatus(player) ? "Shown" : "Hidden"),
                        shouldShowScoreboardAbilityStatus(player) ? "Click: Hide" : "Click: Show")));

        inventory.setItem(SCOREBOARD_BACK_SLOT, MenuItemFactory.create(
                Material.ARROW,
                "Back",
                List.of("Click: Open upgrades")));

        player.openInventory(inventory);
    }

    public static void handleScoreboardMenuClick(Player player, int slot) {
        if (player == null) {
            return;
        }
        UUID playerId = player.getUniqueId();
        switch (slot) {
            case SCOREBOARD_LAYOUT_SLOT ->
                scoreboardLayoutModeByPlayer.put(playerId, getScoreboardLayoutMode(player) == 0 ? 1 : 0);
            case SCOREBOARD_QUEST_POINTS_SLOT -> scoreboardShowQuestPointsByPlayer.put(playerId,
                    !shouldShowScoreboardQuestPoints(player));
            case SCOREBOARD_AUTOMATION_POINTS_SLOT -> scoreboardShowAutomationPointsByPlayer.put(playerId,
                    !shouldShowScoreboardAutomationPoints(player));
            case SCOREBOARD_SACRIFICE_POINTS_SLOT -> scoreboardShowSacrificePointsByPlayer.put(playerId,
                    !shouldShowScoreboardSacrificePoints(player));
            case SCOREBOARD_QUEST_PROGRESS_SLOT -> scoreboardShowQuestProgressByPlayer.put(playerId,
                    !shouldShowScoreboardQuestProgress(player));
            case SCOREBOARD_ABILITIES_SLOT -> scoreboardShowAbilityStatusByPlayer.put(playerId,
                    !shouldShowScoreboardAbilityStatus(player));
            case SCOREBOARD_BACK_SLOT -> {
                openUpgradeMenu(player);
                return;
            }
            default -> {
                return;
            }
        }
        saveData();
        updatePointsScoreboard(player);
        openScoreboardMenu(player);
    }

    public static void handleUpgradeMenuClick(Player player, int slot) {
        if (player == null) {
            return;
        }
        switch (slot) {
            case LIMIT_UPGRADE_SLOT -> {
                if (blockTutorialMenuPurchase(player, TutorialStep.BUY_REGULAR_UPGRADE,
                        "Buy one regular upgrade in /sheepmerge upgrade")) {
                    break;
                }
                if (getPlayerLimit(player) >= MAX_SHEEP_LIMIT) {
                    player.sendMessage(warning("Sheep limit maxed."));
                } else if (upgradeLimit(player)) {
                    playUpgradeSound(player);
                    player.sendMessage(action("Limit up: " + getPlayerLimit(player)));
                    markTutorialRegularUpgradesIfComplete(player);
                } else {
                    player.sendMessage(warning("Not enough points."));
                }
            }
            case EGG_SPEED_UPGRADE_SLOT -> {
                if (blockTutorialMenuPurchase(player, TutorialStep.BUY_REGULAR_UPGRADE,
                        "Buy one regular upgrade in /sheepmerge upgrade")) {
                    break;
                }
                if (upgradeEggSpeed(player)) {
                    playUpgradeSound(player);
                    player.sendMessage(action("Eggs: every " + getEggIntervalSeconds(player) + "s"));
                    markTutorialRegularUpgradesIfComplete(player);
                } else {
                    player.sendMessage(warning("Not enough points."));
                }
            }
            case WOOL_REGEN_UPGRADE_SLOT -> {
                if (blockTutorialMenuPurchase(player, TutorialStep.BUY_REGULAR_UPGRADE,
                        "Buy one regular upgrade in /sheepmerge upgrade")) {
                    break;
                }
                if (upgradeWoolRegen(player)) {
                    playUpgradeSound(player);
                    player.sendMessage(action("Wool regen up"));
                    markTutorialRegularUpgradesIfComplete(player);
                } else {
                    player.sendMessage(warning("Not enough points."));
                }
            }
            case HIGHER_TIER_CHANCE_UPGRADE_SLOT -> {
                if (blockTutorialMenuPurchase(player, TutorialStep.BUY_REGULAR_UPGRADE,
                        "Buy one regular upgrade in /sheepmerge upgrade")) {
                    break;
                }
                int level = higherTierChanceLevelByPlayer.getOrDefault(player.getUniqueId(), 0);
                if (level >= getHigherTierChanceMaxLevel(player)) {
                    player.sendMessage(warning("Spawn chance maxed."));
                    break;
                }
                if (upgradeHigherTierChance(player)) {
                    playUpgradeSound(player);
                    player.sendMessage(action("Spawn chance: " + getHigherTierChancePercent(player) + "%"));
                    markTutorialRegularUpgradesIfComplete(player);
                } else {
                    player.sendMessage(warning("Not enough points."));
                }
            }
            case PRESTIGE_MENU_OPEN_SLOT -> {
                openPrestigeMenu(player);
                return;
            }
            case QUEST_MENU_OPEN_SLOT -> {
                openQuestMenu(player);
                return;
            }
            case SHOP_MENU_OPEN_SLOT -> {
                openShopMenu(player);
                return;
            }
            case COMBO_MENU_OPEN_SLOT -> {
                openComboShopMenu(player);
                return;
            }
            case AUTOMATION_MENU_OPEN_SLOT -> {
                openAutomationMenu(player);
                return;
            }
            case SACRIFICE_MENU_OPEN_SLOT -> {
                openSacrificeMenu(player);
                return;
            }
            default -> {
                return;
            }
        }
        updatePointsScoreboard(player);
        openUpgradeMenu(player);
    }

    public static void openPrestigeMenu(Player player) {
        if (player == null) {
            return;
        }
        markTutorialPrestigeOpened(player);
        int affordablePrestiges = getAffordablePrestigeLevels(player);
        BigInteger totalCostForAffordable = getTotalPrestigeCostForNextLevels(getPrestigeLevel(player),
                affordablePrestiges);
        int rewardForAffordable = getPrestigePointsRewardForNextLevels(getPrestigeLevel(player), affordablePrestiges);
        Inventory inventory = Bukkit.createInventory(null, 27, PRESTIGE_MENU_TITLE);
        inventory.setItem(PRESTIGE_UPGRADE_SLOT, MenuItemFactory.create(
                Material.NETHER_STAR,
                "Prestige Reset",
                List.of(
                        "Current prestige: " + getPrestigeLevel(player),
                        affordablePrestiges > 0
                                ? "Buy now: +" + affordablePrestiges + " prestige level(s)"
                                : "Buy now: +0 prestige level(s)",
                        affordablePrestiges > 0
                                ? "Gain prestige points: +" + formatPoints(rewardForAffordable)
                                : "Gain prestige points: +0",
                        "Next prestige cost: " + formatPoints(getPrestigeCostBig(player)) + " normal points",
                        affordablePrestiges > 0
                                ? "Total cost now: " + formatPoints(totalCostForAffordable) + " normal points"
                                : "Need more points for next prestige",
                        "Resets normal-point upgrades",
                        "Click to prestige multiple")));

        inventory.setItem(PRESTIGE_DOUBLE_POINTS_SLOT, MenuItemFactory.create(
                Material.EMERALD,
                "Double Points Chance",
                List.of(
                        "Level: " + getPrestigeDoublePointsChanceLevel(player) + " / "
                                + PRESTIGE_DOUBLE_POINTS_MAX_LEVEL,
                        "Chance: " + getDoublePointsChancePercent(player) + "%",
                        getPrestigeDoublePointsChanceLevel(player) >= PRESTIGE_DOUBLE_POINTS_MAX_LEVEL
                                ? "MAXED"
                                : "Cost: " + formatPoints(getPrestigeDoublePointsCost(player))
                                        + " prestige points",
                        "Click to purchase")));

        inventory.setItem(PRESTIGE_HIGHER_MAX_LEVEL_SLOT, MenuItemFactory.create(
                Material.ENCHANTED_BOOK,
                "Higher Maximum Levels",
                List.of(
                        "Level: " + getPrestigeHigherMaxLevel(player),
                        "Current caps: Egg " + getEggSpeedMaxLevel(player)
                                + ", Wool " + getWoolRegenMaxLevel(player)
                                + ", Chance " + getHigherTierChanceMaxLevel(player),
                        "Next caps: Egg " + Math.min(
                                EGG_SPEED_MAX_LEVEL,
                                EGG_SPEED_BASE_MAX_LEVEL
                                        + (getPrestigeHigherMaxLevel(player) + 1) * PRESTIGE_CAP_BONUS_PER_LEVEL)
                                + ", Wool " + (WOOL_REGEN_BASE_MAX_LEVEL
                                        + (getPrestigeHigherMaxLevel(player) + 1) * PRESTIGE_CAP_BONUS_PER_LEVEL)
                                + ", Chance " + Math.min(
                                        HIGHER_TIER_CHANCE_HARD_MAX_LEVEL,
                                        Math.min(HIGHER_TIER_CHANCE_MAX_LEVEL,
                                                HIGHER_TIER_CHANCE_BASE_MAX_LEVEL
                                                        + (getPrestigeHigherMaxLevel(player) + 1)
                                                                * PRESTIGE_CAP_BONUS_PER_LEVEL)),
                        "Base caps: Egg " + EGG_SPEED_BASE_MAX_LEVEL
                                + ", Wool " + WOOL_REGEN_BASE_MAX_LEVEL
                                + ", Chance " + HIGHER_TIER_CHANCE_BASE_MAX_LEVEL,
                        "Cost: " + formatPoints(getPrestigeHigherMaxLevelCost(player)) + " prestige points",
                        "Click to purchase")));

        inventory.setItem(PRESTIGE_START_EGGS_SLOT, MenuItemFactory.create(
                Material.SHEEP_SPAWN_EGG,
                "Start With Extra Eggs",
                List.of(
                        "Level: " + getPrestigeStartEggsLevel(player),
                        "Extra starting eggs: +" + getStartEggsBonus(player),
                        "Cost: " + formatPoints(getPrestigeStartEggsCost(player)) + " prestige points",
                        "Click to purchase")));

        inventory.setItem(PRESTIGE_EGG_CAP_SLOT, MenuItemFactory.create(
                Material.EGG,
                "Egg Capacity",
                List.of(
                        "Level: " + getPrestigeEggCapLevel(player),
                        "Egg cap: " + getEggCap(player),
                        "Adds: +" + PRESTIGE_EGG_CAP_STEP + " eggs per level",
                        "Cost: " + formatPoints(getPrestigeEggCapCost(player)) + " prestige points",
                        "Click to purchase")));

        int baseSpawnTierLevel = getBaseSpawnTierLevel(player);
        SheepTier baseSpawnTier = SheepTier.byLevel(baseSpawnTierLevel);
        inventory.setItem(PRESTIGE_BASE_SPAWN_TIER_SLOT, MenuItemFactory.create(
                Material.SHEEP_SPAWN_EGG,
                "Higher Base Spawn Tier",
                List.of(
                        "Level: " + baseSpawnTierLevel + " / " + SheepTier.RAINBOW.getLevel(),
                        "Current base tier: " + baseSpawnTier.getDisplayName(),
                        baseSpawnTierLevel >= SheepTier.RAINBOW.getLevel()
                                ? "MAXED"
                                : "Cost: " + formatPoints(getPrestigeBaseSpawnTierCost(player)) + " prestige points",
                        "Click to purchase")));

        int questRewardLevel = getPrestigeQuestRewardLevel(player);
        inventory.setItem(PRESTIGE_QUEST_REWARD_SLOT, MenuItemFactory.create(
                Material.BOOK,
                "Quest Reward Boost",
                List.of(
                        "Level: " + questRewardLevel + " / " + PRESTIGE_QUEST_REWARD_MAX_LEVEL,
                        "Quest rewards: +"
                                + (int) Math.round(questRewardLevel * PRESTIGE_QUEST_REWARD_BONUS_PER_LEVEL * 100)
                                + "%",
                        questRewardLevel >= PRESTIGE_QUEST_REWARD_MAX_LEVEL
                                ? "MAXED"
                                : "Cost: " + formatPoints(getPrestigeQuestRewardCost(player)) + " prestige points",
                        "Click to purchase")));

        long refundRemaining = getPrestigeRefundRemainingMs(player);
        int refundAmount = getPrestigeRefundAmount(player);
        inventory.setItem(PRESTIGE_REFUND_SLOT, MenuItemFactory.create(
                Material.BARRIER,
                "Refund Prestige Upgrades",
                List.of(
                        "Refund amount: " + formatPoints(refundAmount) + " prestige points",
                        refundRemaining > 0L ? "Cooldown: " + formatDuration(refundRemaining) : "Cooldown: ready",
                        "Resets prestige shop upgrades",
                        "Click to refund")));

        inventory.setItem(PRESTIGE_BACK_TO_UPGRADES_SLOT, MenuItemFactory.create(
                Material.ARROW,
                "Back To Upgrades",
                List.of("Click to go back")));
        player.openInventory(inventory);
    }

    public static void handlePrestigeMenuClick(Player player, int slot) {
        if (player == null) {
            return;
        }
        switch (slot) {
            case PRESTIGE_UPGRADE_SLOT -> {
                if (blockTutorialMenuPurchase(player, TutorialStep.PRESTIGE_ONCE,
                        "In Prestige menu, click Prestige once")) {
                    break;
                }
                int gained = prestige(player);
                if (gained > 0) {
                    playPrestigeSound(player);
                    player.sendMessage(action("Prestige +" + gained));
                } else {
                    player.sendMessage(warning("Not enough points."));
                }
            }
            case PRESTIGE_DOUBLE_POINTS_SLOT -> {
                if (blockTutorialMenuPurchase(player, TutorialStep.PRESTIGE_ONCE,
                        "In Prestige menu, click Prestige once")) {
                    break;
                }
                if (getPrestigeDoublePointsChanceLevel(player) >= PRESTIGE_DOUBLE_POINTS_MAX_LEVEL) {
                    player.sendMessage(warning("Double points chance is maxed."));
                    break;
                }
                if (upgradePrestigeDoublePoints(player)) {
                    playUpgradeSound(player);
                    player.sendMessage(action("Double points: " + getDoublePointsChancePercent(player) + "%"));
                } else {
                    player.sendMessage(warning("Not enough prestige points."));
                }
            }
            case PRESTIGE_HIGHER_MAX_LEVEL_SLOT -> {
                if (blockTutorialMenuPurchase(player, TutorialStep.PRESTIGE_ONCE,
                        "In Prestige menu, click Prestige once")) {
                    break;
                }
                if (upgradePrestigeHigherMaxLevel(player)) {
                    playUpgradeSound(player);
                    player.sendMessage(action("Higher max levels up"));
                } else {
                    player.sendMessage(warning("Not enough prestige points."));
                }
            }
            case PRESTIGE_START_EGGS_SLOT -> {
                if (blockTutorialMenuPurchase(player, TutorialStep.PRESTIGE_ONCE,
                        "In Prestige menu, click Prestige once")) {
                    break;
                }
                if (upgradePrestigeStartEggs(player)) {
                    playUpgradeSound(player);
                    player.sendMessage(action("Start eggs up"));
                } else {
                    player.sendMessage(warning("Not enough prestige points."));
                }
            }
            case PRESTIGE_EGG_CAP_SLOT -> {
                if (blockTutorialMenuPurchase(player, TutorialStep.PRESTIGE_ONCE,
                        "In Prestige menu, click Prestige once")) {
                    break;
                }
                if (upgradePrestigeEggCap(player)) {
                    playUpgradeSound(player);
                    player.sendMessage(action("Egg cap: " + getEggCap(player)));
                } else {
                    player.sendMessage(warning("Not enough prestige points."));
                }
            }
            case PRESTIGE_BASE_SPAWN_TIER_SLOT -> {
                if (blockTutorialMenuPurchase(player, TutorialStep.PRESTIGE_ONCE,
                        "In Prestige menu, click Prestige once")) {
                    break;
                }
                if (getBaseSpawnTierLevel(player) >= SheepTier.RAINBOW.getLevel()) {
                    player.sendMessage(warning("Base spawn tier maxed."));
                    break;
                }
                if (upgradePrestigeBaseSpawnTier(player)) {
                    playUpgradeSound(player);
                    player.sendMessage(action("Base spawn tier: " + getBaseSpawnTier(player).getDisplayName()));
                } else {
                    player.sendMessage(warning("Not enough prestige points."));
                }
            }
            case PRESTIGE_QUEST_REWARD_SLOT -> {
                if (blockTutorialMenuPurchase(player, TutorialStep.PRESTIGE_ONCE,
                        "In Prestige menu, click Prestige once")) {
                    break;
                }
                if (getPrestigeQuestRewardLevel(player) >= PRESTIGE_QUEST_REWARD_MAX_LEVEL) {
                    player.sendMessage(warning("Quest reward boost maxed."));
                    break;
                }
                if (upgradePrestigeQuestReward(player)) {
                    playUpgradeSound(player);
                    player.sendMessage(action("Quest rewards: +"
                            + (int) Math.round(
                                    getPrestigeQuestRewardLevel(player) * PRESTIGE_QUEST_REWARD_BONUS_PER_LEVEL * 100)
                            + "%"));
                } else {
                    player.sendMessage(warning("Not enough prestige points."));
                }
            }
            case PRESTIGE_REFUND_SLOT -> {
                if (blockTutorialMenuPurchase(player, TutorialStep.PRESTIGE_ONCE,
                        "In Prestige menu, click Prestige once")) {
                    break;
                }
                long refundRemaining = getPrestigeRefundRemainingMs(player);
                if (refundRemaining > 0L) {
                    player.sendMessage(warning("Refund cooldown: " + formatDuration(refundRemaining)));
                    break;
                }
                int refundAmount = getPrestigeRefundAmount(player);
                if (refundAmount <= 0) {
                    player.sendMessage(warning("No prestige upgrades to refund."));
                    break;
                }
                if (tryRefundPrestigePoints(player)) {
                    playUpgradeSound(player);
                    player.sendMessage(action("Refunded " + formatPoints(refundAmount) + " prestige points."));
                } else {
                    player.sendMessage(warning("Refund is not available right now."));
                }
            }
            case PRESTIGE_BACK_TO_UPGRADES_SLOT -> {
                openUpgradeMenu(player);
                return;
            }
            default -> {
                return;
            }
        }
        updatePointsScoreboard(player);
        openPrestigeMenu(player);
    }

    public static void openComboShopMenu(Player player) {
        if (player == null) {
            return;
        }
        Inventory inventory = Bukkit.createInventory(null, 27, COMBO_SHOP_MENU_TITLE);

        int decayLevel = getComboDecayUpgradeLevel(player);
        int gainLevel = getComboGainUpgradeLevel(player);
        int maxLevel = getComboMaxUpgradeLevel(player);

        inventory.setItem(COMBO_DECAY_SLOT, MenuItemFactory.create(
                Material.CLOCK,
                "Slower Combo Decay",
                List.of(
                        "Level: " + decayLevel + " / " + COMBO_DECAY_MAX_LEVEL,
                        "Decay speed: " + (int) Math.round(getComboDecayMultiplier(player) * 100) + "%",
                        decayLevel >= COMBO_DECAY_MAX_LEVEL
                                ? "MAXED"
                                : "Cost: " + formatPoints(getComboDecayUpgradeCost(player)) + " points",
                        "Click to purchase")));

        inventory.setItem(COMBO_MAX_SLOT, MenuItemFactory.create(
                Material.NETHER_STAR,
                "Maximum Combo",
                List.of(
                        "Level: " + maxLevel + " / " + COMBO_MAX_MAX_LEVEL,
                        "Max score: " + (int) Math.floor(getComboMaxScore(player)),
                        maxLevel >= COMBO_MAX_MAX_LEVEL
                                ? "MAXED"
                                : "Cost: " + formatPoints(getComboMaxUpgradePrestigeCost(player)) + " prestige points",
                        "Click to purchase")));

        inventory.setItem(COMBO_GAIN_SLOT, MenuItemFactory.create(
                Material.EMERALD,
                "Combo Gain Percentage",
                List.of(
                        "Level: " + gainLevel + " / " + COMBO_GAIN_MAX_LEVEL,
                        "Combo gain boost: +" + (int) Math.round(gainLevel * COMBO_GAIN_PERCENT_PER_LEVEL) + "%",
                        gainLevel >= COMBO_GAIN_MAX_LEVEL
                                ? "MAXED"
                                : "Cost: " + formatPoints(getComboGainUpgradeCost(player)) + " points",
                        "Click to purchase")));

        inventory.setItem(COMBO_BACK_TO_UPGRADES_SLOT, MenuItemFactory.create(
                Material.ARROW,
                "Back To Upgrades",
                List.of("Click to go back")));

        player.openInventory(inventory);
    }

    public static void handleComboShopMenuClick(Player player, int slot) {
        if (player == null) {
            return;
        }

        switch (slot) {
            case COMBO_DECAY_SLOT -> {
                if (upgradeComboDecay(player)) {
                    playUpgradeSound(player);
                    player.sendMessage(action("Combo decay slowed."));
                } else {
                    player.sendMessage(warning("Unable to buy combo decay upgrade."));
                }
            }
            case COMBO_MAX_SLOT -> {
                if (upgradeComboMax(player)) {
                    playUpgradeSound(player);
                    player.sendMessage(action("Combo max increased."));
                } else {
                    player.sendMessage(warning("Unable to buy combo max upgrade."));
                }
            }
            case COMBO_GAIN_SLOT -> {
                if (upgradeComboGain(player)) {
                    playUpgradeSound(player);
                    player.sendMessage(action("Combo score gain increased."));
                } else {
                    player.sendMessage(warning("Unable to buy combo gain upgrade."));
                }
            }
            case COMBO_BACK_TO_UPGRADES_SLOT -> {
                openUpgradeMenu(player);
                return;
            }
            default -> {
                return;
            }
        }

        updateComboBossBar(player, getComboScore(player));
        updatePointsScoreboard(player);
        openComboShopMenu(player);
    }

    public static void openAutomationMenu(Player player) {
        if (player == null) {
            return;
        }
        Inventory inventory = Bukkit.createInventory(null, 27, AUTOMATION_MENU_TITLE);
        inventory.setItem(4, MenuItemFactory.create(
                Material.EXPERIENCE_BOTTLE,
                "Automation Points",
                List.of(
                        "Current: " + formatPoints(getAutomationPoints(player)),
                        "Earned over time while playing")));

        inventory.setItem(AUTOMATION_AUTO_BUY_SLOT, MenuItemFactory.create(
                Material.HOPPER,
                "Auto Buy Upgrades",
                List.of(
                        "Level: " + getAutomationAutoBuyUpgradeLevel(player) + " / " + AUTOMATION_AUTO_BUY_MAX_LEVEL,
                        "Status: " + (isAutomationAutoBuyEnabled(player) ? "ENABLED" : "DISABLED"),
                        "Runs every " + formatDuration(getAutomationAutoBuyIntervalMs(player)),
                        getAutomationAutoBuyUpgradeLevel(player) >= AUTOMATION_AUTO_BUY_MAX_LEVEL
                                ? "Cost: MAXED"
                                : "Cost: " + formatPoints(getAutomationAutoBuyUpgradeCost(player))
                                        + " automation points",
                        "Buys one affordable upgrade",
                        getAutomationAutoBuyUpgradeLevel(player) >= AUTOMATION_AUTO_BUY_MAX_LEVEL
                                ? "Click: maxed"
                                : "Click: unlock")));

        inventory.setItem(AUTOMATION_AUTO_ABILITY_SLOT, MenuItemFactory.create(
                Material.BREWING_STAND,
                "Auto Activate Abilities",
                List.of(
                        "Level: " + getAutomationAutoAbilityUpgradeLevel(player) + " / "
                                + AUTOMATION_SINGLE_LEVEL_MAX,
                        "Status: " + (isAutomationAutoAbilityEnabled(player) ? "ENABLED" : "DISABLED"),
                        "Runs every " + formatDuration(AUTOMATION_AUTO_ABILITY_INTERVAL_MS),
                        getAutomationAutoAbilityUpgradeLevel(player) >= AUTOMATION_SINGLE_LEVEL_MAX
                                ? "Cost: MAXED"
                                : "Cost: " + formatPoints(getAutomationAutoAbilityUpgradeCost(player))
                                        + " automation points",
                        "Activates non-active quest abilities",
                        getAutomationAutoAbilityUpgradeLevel(player) >= AUTOMATION_SINGLE_LEVEL_MAX
                                ? "Click: maxed"
                                : "Click: unlock")));

        inventory.setItem(AUTOMATION_SLOW_AUTO_MERGE_SLOT, MenuItemFactory.create(
                Material.ANVIL,
                "Auto Merge",
                List.of(
                        "Level: " + getAutomationSlowAutoMergeUpgradeLevel(player) + " / "
                                + AUTOMATION_SLOW_AUTO_MERGE_MAX_LEVEL,
                        "Status: " + (isAutomationSlowAutoMergeEnabled(player) ? "ENABLED" : "DISABLED"),
                        "Runs every " + formatDuration(getAutomationSlowAutoMergeIntervalMs(player)),
                        getAutomationSlowAutoMergeUpgradeLevel(player) >= AUTOMATION_SLOW_AUTO_MERGE_MAX_LEVEL
                                ? "Cost: MAXED"
                                : "Cost: " + formatPoints(getAutomationSlowAutoMergeUpgradeCost(player))
                                        + " automation points",
                        "Slower passive auto-merge",
                        getAutomationSlowAutoMergeUpgradeLevel(player) >= AUTOMATION_SLOW_AUTO_MERGE_MAX_LEVEL
                                ? "Click: maxed"
                                : "Click: unlock")));

        inventory.setItem(AUTOMATION_AUTO_PRESTIGE_SLOT, MenuItemFactory.create(
                Material.NETHER_STAR,
                "Auto Prestige",
                List.of(
                        "Level: " + getAutomationAutoPrestigeUpgradeLevel(player) + " / 1",
                        "Status: " + (isAutomationAutoPrestigeEnabled(player) ? "ENABLED" : "DISABLED"),
                        "Runs every " + formatDuration(AUTOMATION_AUTO_PRESTIGE_INTERVAL_MS),
                        getAutomationAutoPrestigeUpgradeLevel(player) > 0
                                ? "Cost: MAXED"
                                : "Cost: " + formatPoints(getAutomationAutoPrestigeUpgradeCost(player))
                                        + " automation points",
                        "Automatically prestiges when affordable",
                        "Click: unlock")));

        inventory.setItem(AUTOMATION_SLOW_AUTO_SHEAR_SLOT, MenuItemFactory.create(
                Material.SHEARS,
                "Auto Shear",
                List.of(
                        "Level: " + getAutomationSlowAutoShearUpgradeLevel(player) + " / "
                                + AUTOMATION_SLOW_AUTO_SHEAR_MAX_LEVEL,
                        "Status: " + (isAutomationSlowAutoShearEnabled(player) ? "ENABLED" : "DISABLED"),
                        "Runs every " + formatDuration(getAutomationSlowAutoShearIntervalMs(player)),
                        getAutomationSlowAutoShearUpgradeLevel(player) >= AUTOMATION_SLOW_AUTO_SHEAR_MAX_LEVEL
                                ? "Cost: MAXED"
                                : "Cost: " + formatPoints(getAutomationSlowAutoShearUpgradeCost(player))
                                        + " automation points",
                        "Slower passive auto-shear",
                        getAutomationSlowAutoShearUpgradeLevel(player) >= AUTOMATION_SLOW_AUTO_SHEAR_MAX_LEVEL
                                ? "Click: maxed"
                                : "Click: unlock")));

        long autoSpawnInterval = getAutomationAutoSpawnIntervalMs(player);
        inventory.setItem(AUTOMATION_AUTO_SPAWN_SLOT, MenuItemFactory.create(
                Material.SHEEP_SPAWN_EGG,
                "Auto Spawn Sheep",
                List.of(
                        "Level: " + getAutomationAutoSpawnUpgradeLevel(player) + " / "
                                + AUTOMATION_AUTO_SPAWN_MAX_LEVEL,
                        "Status: " + (isAutomationAutoSpawnEnabled(player) ? "ENABLED" : "DISABLED"),
                        "Runs every " + (autoSpawnInterval <= 0L ? "instant" : formatDuration(autoSpawnInterval)),
                        "Consumes spawn eggs",
                        getAutomationAutoSpawnUpgradeLevel(player) >= AUTOMATION_AUTO_SPAWN_MAX_LEVEL
                                ? "Cost: MAXED"
                                : "Cost: " + formatPoints(getAutomationAutoSpawnUpgradeCost(player))
                                        + " automation points",
                        getAutomationAutoSpawnUpgradeLevel(player) >= AUTOMATION_AUTO_SPAWN_MAX_LEVEL
                                ? "Click: maxed"
                                : "Click: upgrade")));

        inventory.setItem(AUTOMATION_AUTO_BUY_TOGGLE_SLOT, MenuItemFactory.create(
                Material.LEVER,
                "Toggle Auto Buy",
                List.of(
                        "Current: " + (isAutomationAutoBuyEnabled(player) ? "ENABLED" : "DISABLED"),
                        getAutomationAutoBuyUpgradeLevel(player) > 0 ? "Click to toggle" : "Buy level 1 first")));

        inventory.setItem(AUTOMATION_AUTO_ABILITY_TOGGLE_SLOT, MenuItemFactory.create(
                Material.LEVER,
                "Toggle Auto Ability",
                List.of(
                        "Current: " + (isAutomationAutoAbilityEnabled(player) ? "ENABLED" : "DISABLED"),
                        getAutomationAutoAbilityUpgradeLevel(player) > 0 ? "Click to toggle" : "Buy level 1 first")));

        inventory.setItem(AUTOMATION_AUTO_SPAWN_TOGGLE_SLOT, MenuItemFactory.create(
                Material.LEVER,
                "Toggle Auto Spawn",
                List.of(
                        "Current: " + (isAutomationAutoSpawnEnabled(player) ? "ENABLED" : "DISABLED"),
                        getAutomationAutoSpawnUpgradeLevel(player) > 0 ? "Click to toggle" : "Buy level 1 first")));

        inventory.setItem(AUTOMATION_SLOW_AUTO_MERGE_TOGGLE_SLOT, MenuItemFactory.create(
                Material.LEVER,
                "Toggle Slow Auto Merge",
                List.of(
                        "Current: " + (isAutomationSlowAutoMergeEnabled(player) ? "ENABLED" : "DISABLED"),
                        getAutomationSlowAutoMergeUpgradeLevel(player) > 0 ? "Click to toggle" : "Buy level 1 first")));

        inventory.setItem(AUTOMATION_SLOW_AUTO_SHEAR_TOGGLE_SLOT, MenuItemFactory.create(
                Material.LEVER,
                "Toggle Slow Auto Shear",
                List.of(
                        "Current: " + (isAutomationSlowAutoShearEnabled(player) ? "ENABLED" : "DISABLED"),
                        getAutomationSlowAutoShearUpgradeLevel(player) > 0 ? "Click to toggle" : "Buy level 1 first")));

        inventory.setItem(AUTOMATION_AUTO_PRESTIGE_TOGGLE_SLOT, MenuItemFactory.create(
                Material.LEVER,
                "Toggle Auto Prestige",
                List.of(
                        "Current: " + (isAutomationAutoPrestigeEnabled(player) ? "ENABLED" : "DISABLED"),
                        getAutomationAutoPrestigeUpgradeLevel(player) > 0 ? "Click to toggle" : "Buy level 1 first")));

        int unlockedAutomations = getUnlockedAutomationCount(player);
        inventory.setItem(AUTOMATION_ENABLE_ALL_SLOT, MenuItemFactory.create(
                Material.LIME_DYE,
                "Enable All",
                List.of(
                        "Unlocked: " + unlockedAutomations + " / 6",
                        unlockedAutomations > 0 ? "Click to enable unlocked automations"
                                : "Unlock an automation first")));

        inventory.setItem(AUTOMATION_DISABLE_ALL_SLOT, MenuItemFactory.create(
                Material.GRAY_DYE,
                "Disable All",
                List.of(
                        "Unlocked: " + unlockedAutomations + " / 6",
                        unlockedAutomations > 0 ? "Click to disable unlocked automations"
                                : "Unlock an automation first")));

        inventory.setItem(AUTOMATION_BACK_TO_UPGRADES_SLOT, MenuItemFactory.create(
                Material.ARROW,
                "Back To Upgrades",
                List.of("Click to go back")));
        player.openInventory(inventory);
    }

    public static void handleAutomationMenuClick(Player player, int slot) {
        if (player == null) {
            return;
        }
        switch (slot) {
            case AUTOMATION_AUTO_BUY_SLOT -> {
                if (getAutomationAutoBuyUpgradeLevel(player) >= AUTOMATION_AUTO_BUY_MAX_LEVEL) {
                    player.sendMessage(warning("Auto Buy is already maxed."));
                    break;
                }
                if (upgradeAutomationAutoBuy(player)) {
                    playUpgradeSound(player);
                    player.sendMessage(action("Auto Buy upgraded."));
                } else {
                    player.sendMessage(warning("Not enough automation points."));
                }
            }
            case AUTOMATION_AUTO_ABILITY_SLOT -> {
                if (getAutomationAutoAbilityUpgradeLevel(player) >= AUTOMATION_SINGLE_LEVEL_MAX) {
                    player.sendMessage(warning("Auto Ability is already maxed."));
                    break;
                }
                if (upgradeAutomationAutoAbility(player)) {
                    playUpgradeSound(player);
                    player.sendMessage(action("Auto Ability upgraded."));
                } else {
                    player.sendMessage(warning("Not enough automation points."));
                }
            }
            case AUTOMATION_SLOW_AUTO_MERGE_SLOT -> {
                if (getAutomationSlowAutoMergeUpgradeLevel(player) >= AUTOMATION_SLOW_AUTO_MERGE_MAX_LEVEL) {
                    player.sendMessage(warning("Slow Auto Merge is already maxed."));
                    break;
                }
                if (upgradeAutomationSlowAutoMerge(player)) {
                    playUpgradeSound(player);
                    player.sendMessage(action("Slow Auto Merge upgraded."));
                } else {
                    player.sendMessage(warning("Not enough automation points."));
                }
            }
            case AUTOMATION_SLOW_AUTO_SHEAR_SLOT -> {
                if (getAutomationSlowAutoShearUpgradeLevel(player) >= AUTOMATION_SLOW_AUTO_SHEAR_MAX_LEVEL) {
                    player.sendMessage(warning("Slow Auto Shear is already maxed."));
                    break;
                }
                if (upgradeAutomationSlowAutoShear(player)) {
                    playUpgradeSound(player);
                    player.sendMessage(action("Slow Auto Shear upgraded."));
                } else {
                    player.sendMessage(warning("Not enough automation points."));
                }
            }
            case AUTOMATION_AUTO_PRESTIGE_SLOT -> {
                if (getAutomationAutoPrestigeUpgradeLevel(player) > 0) {
                    player.sendMessage(warning("Auto Prestige is already maxed."));
                } else if (upgradeAutomationAutoPrestige(player)) {
                    playUpgradeSound(player);
                    player.sendMessage(action("Auto Prestige unlocked."));
                } else {
                    player.sendMessage(warning("Not enough automation points."));
                }
            }
            case AUTOMATION_AUTO_SPAWN_SLOT -> {
                if (getAutomationAutoSpawnUpgradeLevel(player) >= AUTOMATION_AUTO_SPAWN_MAX_LEVEL) {
                    player.sendMessage(warning("Auto Spawn is already maxed."));
                    break;
                }
                if (upgradeAutomationAutoSpawn(player)) {
                    playUpgradeSound(player);
                    player.sendMessage(action("Auto Spawn upgraded."));
                } else {
                    player.sendMessage(warning("Not enough automation points."));
                }
            }
            case AUTOMATION_BACK_TO_UPGRADES_SLOT -> {
                openUpgradeMenu(player);
                return;
            }
            case AUTOMATION_AUTO_BUY_TOGGLE_SLOT -> {
                if (getAutomationAutoBuyUpgradeLevel(player) <= 0) {
                    player.sendMessage(warning("Buy Auto Buy level 1 first."));
                    break;
                }
                boolean enabled = toggleAutomationEnabled(player, automationAutoBuyEnabledByPlayer);
                player.sendMessage(action("Auto Buy " + (enabled ? "enabled" : "disabled") + "."));
            }
            case AUTOMATION_AUTO_ABILITY_TOGGLE_SLOT -> {
                if (getAutomationAutoAbilityUpgradeLevel(player) <= 0) {
                    player.sendMessage(warning("Buy Auto Ability level 1 first."));
                    break;
                }
                boolean enabled = toggleAutomationEnabled(player, automationAutoAbilityEnabledByPlayer);
                player.sendMessage(action("Auto Ability " + (enabled ? "enabled" : "disabled") + "."));
            }
            case AUTOMATION_SLOW_AUTO_MERGE_TOGGLE_SLOT -> {
                if (getAutomationSlowAutoMergeUpgradeLevel(player) <= 0) {
                    player.sendMessage(warning("Buy Slow Auto Merge level 1 first."));
                    break;
                }
                boolean enabled = toggleAutomationEnabled(player, automationSlowAutoMergeEnabledByPlayer);
                player.sendMessage(action("Slow Auto Merge " + (enabled ? "enabled" : "disabled") + "."));
            }
            case AUTOMATION_SLOW_AUTO_SHEAR_TOGGLE_SLOT -> {
                if (getAutomationSlowAutoShearUpgradeLevel(player) <= 0) {
                    player.sendMessage(warning("Buy Slow Auto Shear level 1 first."));
                    break;
                }
                boolean enabled = toggleAutomationEnabled(player, automationSlowAutoShearEnabledByPlayer);
                player.sendMessage(action("Slow Auto Shear " + (enabled ? "enabled" : "disabled") + "."));
            }
            case AUTOMATION_AUTO_PRESTIGE_TOGGLE_SLOT -> {
                if (getAutomationAutoPrestigeUpgradeLevel(player) <= 0) {
                    player.sendMessage(warning("Buy Auto Prestige first."));
                    break;
                }
                boolean enabled = toggleAutomationEnabled(player, automationAutoPrestigeEnabledByPlayer);
                player.sendMessage(action("Auto Prestige " + (enabled ? "enabled" : "disabled") + "."));
            }
            case AUTOMATION_AUTO_SPAWN_TOGGLE_SLOT -> {
                if (getAutomationAutoSpawnUpgradeLevel(player) <= 0) {
                    player.sendMessage(warning("Buy Auto Spawn level 1 first."));
                    break;
                }
                boolean enabled = toggleAutomationEnabled(player, automationAutoSpawnEnabledByPlayer);
                player.sendMessage(action("Auto Spawn " + (enabled ? "enabled" : "disabled") + "."));
            }
            case AUTOMATION_ENABLE_ALL_SLOT -> {
                int unlocked = getUnlockedAutomationCount(player);
                if (unlocked <= 0) {
                    player.sendMessage(warning("Unlock at least one automation first."));
                    break;
                }
                int changed = setAllAutomationsEnabled(player, true);
                player.sendMessage(action(changed > 0 ? "Enabled all unlocked automations."
                        : "All unlocked automations are already enabled."));
            }
            case AUTOMATION_DISABLE_ALL_SLOT -> {
                int unlocked = getUnlockedAutomationCount(player);
                if (unlocked <= 0) {
                    player.sendMessage(warning("Unlock at least one automation first."));
                    break;
                }
                int changed = setAllAutomationsEnabled(player, false);
                player.sendMessage(action(changed > 0 ? "Disabled all unlocked automations."
                        : "All unlocked automations are already disabled."));
            }
            default -> {
                return;
            }
        }
        updatePointsScoreboard(player);
        openAutomationMenu(player);
    }

    public static void openSacrificeMenu(Player player) {
        if (player == null) {
            return;
        }
        Inventory inventory = Bukkit.createInventory(null, 27, SACRIFICE_MENU_TITLE);
        UUID playerId = player.getUniqueId();
        int unlocksBought = getSacrificeUnlocksBought(playerId);
        BigInteger nextCost = getSacrificeUnlockCost(playerId);

        inventory.setItem(SACRIFICE_POINTS_SLOT, MenuItemFactory.create(
                Material.TOTEM_OF_UNDYING,
                "Sacrifice Progress",
                List.of(
                        "Sacrifice points: " + formatPoints(getSacrificePoints(player)),
                        "Unlocks bought: " + unlocksBought + " / " + SACRIFICE_UNLOCK_MAX,
                        unlocksBought >= SACRIFICE_UNLOCK_MAX
                                ? "Next unlock cost: MAXED"
                                : "Next unlock cost: " + formatPoints(nextCost))));

        inventory.setItem(SACRIFICE_ALL_SHEEP_SLOT, MenuItemFactory.create(
                Material.IRON_SWORD,
                "Sacrifice All Sheep",
                List.of(
                        "Converts all current farm sheep",
                        "into sacrifice points instantly",
                        "Per sheep value: 2^(tierIndex)",
                        "Click to sacrifice now")));

        inventory.setItem(SACRIFICE_UNLOCK_REGULAR_RESETS_SLOT, MenuItemFactory.create(
                Material.BARRIER,
                "Unlock 1: Keep Regular Upgrades",
                List.of(
                        "Status: " + (hasSacrificeUnlock(player, SACRIFICE_UNLOCK_NO_REGULAR_RESETS)
                                ? "UNLOCKED"
                                : "LOCKED"),
                        "No regular upgrade resets on prestige",
                        "Requires unlock #1")));

        inventory.setItem(SACRIFICE_UNLOCK_COMBO_RESETS_SLOT, MenuItemFactory.create(
                Material.BLAZE_POWDER,
                "Unlock 2: Keep Combo Upgrades",
                List.of(
                        "Status: " + (hasSacrificeUnlock(player, SACRIFICE_UNLOCK_NO_COMBO_RESETS)
                                ? "UNLOCKED"
                                : "LOCKED"),
                        "No combo upgrade resets on prestige",
                        "Requires unlock #2")));

        inventory.setItem(SACRIFICE_UNLOCK_SHEAR_RESETS_SLOT, MenuItemFactory.create(
                Material.SHEARS,
                "Unlock 3: Keep Shear Upgrades",
                List.of(
                        "Status: " + (hasSacrificeUnlock(player, SACRIFICE_UNLOCK_NO_SHEAR_RESETS)
                                ? "UNLOCKED"
                                : "LOCKED"),
                        "No shear shop resets on prestige",
                        "Requires unlock #3")));

        inventory.setItem(SACRIFICE_UNLOCK_EGG_COOLDOWN_SLOT, MenuItemFactory.create(
                Material.CLOCK,
                "Unlock 4: 1s Egg Cooldown Cap",
                List.of(
                        "Status: " + (hasSacrificeUnlock(player, SACRIFICE_UNLOCK_EGG_COOLDOWN_TO_1S)
                                ? "UNLOCKED"
                                : "LOCKED"),
                        "Adds +1 egg speed max level",
                        "Allows 1 egg per second",
                        "Requires unlock #4")));

        inventory.setItem(SACRIFICE_UNLOCK_MAX_SHEEP_SLOT, MenuItemFactory.create(
                Material.OAK_FENCE,
                "Unlock 5: 100 Sheep Cap",
                List.of(
                        "Status: " + (hasSacrificeUnlock(player, SACRIFICE_UNLOCK_MAX_SHEEP_100)
                                ? "UNLOCKED"
                                : "LOCKED"),
                        "Raises max sheep limit to 100",
                        "Requires unlock #5")));

        inventory.setItem(SACRIFICE_REFUND_SLOT, MenuItemFactory.create(
                Material.MILK_BUCKET,
                "Refund Sacrifice Unlocks",
                List.of(
                        "Refunds all spent sacrifice points",
                        "No cooldown, no penalty",
                        "Click to respec")));

        inventory.setItem(SACRIFICE_BACK_TO_UPGRADES_SLOT, MenuItemFactory.create(
                Material.ARROW,
                "Back To Upgrades",
                List.of("Click to go back")));

        player.openInventory(inventory);
    }

    public static void handleSacrificeMenuClick(Player player, int slot) {
        if (player == null) {
            return;
        }
        switch (slot) {
            case SACRIFICE_ALL_SHEEP_SLOT -> {
                BigInteger gained = sacrificeAllSheepForPlayer(player);
                if (gained.signum() > 0) {
                    playUpgradeSound(player);
                    player.sendMessage(
                            action("Sacrificed all sheep for " + formatPoints(gained) + " sacrifice points."));
                } else {
                    player.sendMessage(warning("No sheep available to sacrifice."));
                }
            }
            case SACRIFICE_UNLOCK_REGULAR_RESETS_SLOT,
                    SACRIFICE_UNLOCK_COMBO_RESETS_SLOT,
                    SACRIFICE_UNLOCK_SHEAR_RESETS_SLOT,
                    SACRIFICE_UNLOCK_EGG_COOLDOWN_SLOT,
                    SACRIFICE_UNLOCK_MAX_SHEEP_SLOT -> {
                int unlocksBought = getSacrificeUnlocksBought(player);
                if (unlocksBought >= SACRIFICE_UNLOCK_MAX) {
                    player.sendMessage(warning("All sacrifice unlocks are already purchased."));
                    break;
                }
                int expectedSlot = switch (unlocksBought + 1) {
                    case SACRIFICE_UNLOCK_NO_REGULAR_RESETS -> SACRIFICE_UNLOCK_REGULAR_RESETS_SLOT;
                    case SACRIFICE_UNLOCK_NO_COMBO_RESETS -> SACRIFICE_UNLOCK_COMBO_RESETS_SLOT;
                    case SACRIFICE_UNLOCK_NO_SHEAR_RESETS -> SACRIFICE_UNLOCK_SHEAR_RESETS_SLOT;
                    case SACRIFICE_UNLOCK_EGG_COOLDOWN_TO_1S -> SACRIFICE_UNLOCK_EGG_COOLDOWN_SLOT;
                    default -> SACRIFICE_UNLOCK_MAX_SHEEP_SLOT;
                };
                if (slot != expectedSlot) {
                    player.sendMessage(warning("Purchase sacrifice unlocks in order."));
                    break;
                }
                if (tryBuyNextSacrificeUnlock(player)) {
                    playUpgradeSound(player);
                    player.sendMessage(action("Sacrifice unlock purchased."));
                } else {
                    player.sendMessage(warning("Not enough sacrifice points."));
                }
            }
            case SACRIFICE_REFUND_SLOT -> {
                if (refundSacrificeUnlocks(player)) {
                    playUpgradeSound(player);
                    player.sendMessage(action("Sacrifice unlocks refunded."));
                } else {
                    player.sendMessage(warning("No sacrifice unlocks to refund."));
                }
            }
            case SACRIFICE_BACK_TO_UPGRADES_SLOT -> {
                openUpgradeMenu(player);
                return;
            }
            default -> {
                return;
            }
        }
        updatePointsScoreboard(player);
        openSacrificeMenu(player);
    }

    public static void openQuestMenu(Player player) {
        if (player == null) {
            return;
        }
        markTutorialQuestOpened(player);
        Inventory inventory = Bukkit.createInventory(null, 27, QUEST_MENU_TITLE);
        UUID playerId = player.getUniqueId();
        long remaining = getQuestResetRemainingMs(player);
        boolean shearsComplete = questShearsCompleteByPlayer.getOrDefault(playerId, false);
        boolean spawnsComplete = questSpawnsCompleteByPlayer.getOrDefault(playerId, false);
        boolean mergesComplete = questMergesCompleteByPlayer.getOrDefault(playerId, false);

        inventory.setItem(QUEST_BOARD_SLOT, MenuItemFactory.create(
                Material.BOOK,
                "Quest Board",
                List.of(
                        "Quest points: " + formatPoints(getQuestPoints(player)),
                        remaining > 0L ? "Next reset: " + formatDuration(remaining) : "Next reset: incoming",
                        (shearsComplete ? "DONE " : "TODO ")
                                + "Shear " + questShearsByPlayer.getOrDefault(playerId, 0) + "/" + QUEST_SHEARS_TARGET
                                + " (" + formatPoints(QUEST_SHEARS_REWARD) + " pts)",
                        (spawnsComplete ? "DONE " : "TODO ")
                                + "Spawn " + questSpawnsByPlayer.getOrDefault(playerId, 0) + "/" + QUEST_SPAWNS_TARGET
                                + " (" + formatPoints(QUEST_SPAWNS_REWARD) + " pts)",
                        (mergesComplete ? "DONE " : "TODO ")
                                + "Merge " + questMergesByPlayer.getOrDefault(playerId, 0) + "/" + QUEST_MERGES_TARGET
                                + " (" + formatPoints(QUEST_MERGES_REWARD) + " pts)")));

        inventory.setItem(QUEST_ABILITY_LUCKY_BURST_SLOT, MenuItemFactory.create(
                Material.ENDER_EYE,
                "Lucky Burst",
                List.of(
                        "Cost: " + formatPoints(getQuestLuckyBurstCost(player)) + " quest points",
                        "Effect: +" + QUEST_LUCKY_BURST_SPAWN_CHANCE_BONUS_PERCENT + "% spawn chance",
                        "Uses per activation: " + getAbilityUseCount(player, QUEST_LUCKY_BURST_BASE_DURATION_MS),
                        getCountAbilityMenuStatus(activeLuckyBurstUsesByPlayer, luckyBurstEnabledByPlayer, playerId),
                        getCountAbilityToggleActionLine(activeLuckyBurstUsesByPlayer, luckyBurstEnabledByPlayer,
                                playerId))));

        inventory.setItem(QUEST_ABILITY_WOOL_RUSH_SLOT, MenuItemFactory.create(
                Material.WHITE_WOOL,
                "Wool Rush",
                List.of(
                        "Cost: " + formatPoints(getQuestWoolRushCost(player)) + " quest points",
                        "Effect: 90% faster wool regen",
                        "Duration: " + formatDuration(getAbilityDurationMs(player, QUEST_WOOL_RUSH_BASE_DURATION_MS)),
                        getAbilityMenuStatus(activeWoolRushUntilByPlayer, null, playerId),
                        isAbilityActive(activeWoolRushUntilByPlayer, playerId)
                                ? "Already active"
                                : "Click to activate")));

        inventory.setItem(QUEST_ABILITY_JACKPOT_SHEARS_SLOT, MenuItemFactory.create(
                Material.GOLD_INGOT,
                "Jackpot Shears",
                List.of(
                        "Cost: " + formatPoints(getQuestJackpotCost(player)) + " quest points",
                        "Effect: x" + (2 + getQuestUpgradePowerLevel(player)) + " shear points",
                        "Duration: "
                                + formatDuration(getAbilityDurationMs(player, QUEST_JACKPOT_SHEARS_BASE_DURATION_MS)),
                        getAbilityMenuStatus(activeJackpotShearsUntilByPlayer, null, playerId),
                        isAbilityActive(activeJackpotShearsUntilByPlayer, playerId)
                                ? "Already active"
                                : "Click to activate")));

        inventory.setItem(QUEST_ABILITY_AUTO_MERGE_SLOT, MenuItemFactory.create(
                Material.ANVIL,
                "Auto Merge",
                List.of(
                        "Cost: " + formatPoints(getQuestAutoMergeCost(player)) + " quest points",
                        "Effect: Instantly merges when you pick up a sheep",
                        "Uses per activation: " + getAbilityUseCount(player, QUEST_AUTO_MERGE_BASE_DURATION_MS),
                        getCountAbilityMenuStatus(activeAutoMergeUsesByPlayer, autoMergeEnabledByPlayer, playerId),
                        getCountAbilityToggleActionLine(activeAutoMergeUsesByPlayer, autoMergeEnabledByPlayer,
                                playerId))));

        inventory.setItem(QUEST_ABILITY_AUTO_SHEAR_SLOT, MenuItemFactory.create(
                Material.SHEARS,
                "Auto Shear",
                List.of(
                        "Cost: " + formatPoints(getQuestAutoShearCost(player)) + " quest points",
                        "Effect: Instantly shears the sheep you are looking at",
                        "Uses per activation: " + getAbilityUseCount(player, QUEST_AUTO_SHEAR_BASE_DURATION_MS),
                        getCountAbilityMenuStatus(activeAutoShearUsesByPlayer, autoShearEnabledByPlayer, playerId),
                        getCountAbilityToggleActionLine(activeAutoShearUsesByPlayer, autoShearEnabledByPlayer,
                                playerId))));

        inventory.setItem(QUEST_OPEN_UPGRADES_SLOT, MenuItemFactory.create(
                Material.ENCHANTED_BOOK,
                "Quest Upgrades",
                List.of(
                        "Duration Lv: " + getQuestUpgradeDurationLevel(player),
                        "Power Lv: " + getQuestUpgradePowerLevel(player),
                        "Click to open")));

        inventory.setItem(QUEST_BACK_TO_UPGRADES_SLOT, MenuItemFactory.create(
                Material.ARROW,
                "Back To Upgrades",
                List.of(
                        "Quest points: " + formatPoints(getQuestPoints(player)),
                        remaining > 0L ? "Next reset: " + formatDuration(remaining) : "Next reset: incoming",
                        "Click to go back")));
        player.openInventory(inventory);
    }

    public static void handleQuestMenuClick(Player player, int slot) {
        if (player == null) {
            return;
        }
        switch (slot) {
            case QUEST_ABILITY_LUCKY_BURST_SLOT -> {
                if (toggleCountAbilityEnabled(player, activeLuckyBurstUsesByPlayer, luckyBurstEnabledByPlayer)) {
                    player.sendMessage(action("Lucky Burst "
                            + (luckyBurstEnabledByPlayer.getOrDefault(player.getUniqueId(), true) ? "enabled."
                                    : "disabled.")));
                    break;
                }
                if (blockTutorialMenuPurchase(player, TutorialStep.USE_ABILITY,
                        "Activate any quest ability")) {
                    break;
                }
                if (activateCountQuestAbility(
                        player,
                        activeLuckyBurstUsesByPlayer,
                        luckyBurstEnabledByPlayer,
                        getQuestLuckyBurstCost(player),
                        getAbilityUseCount(player, QUEST_LUCKY_BURST_BASE_DURATION_MS),
                        Sound.BLOCK_BEACON_POWER_SELECT,
                        org.bukkit.Particle.END_ROD)) {
                    markTutorialAbilityUsed(player);
                    player.getWorld().spawnParticle(org.bukkit.Particle.TOTEM,
                            player.getLocation().add(0, 1.1, 0), 18, 0.45, 0.45, 0.45, 0.0);
                    playSound(player, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 0.9f, 1.5f);
                    player.sendMessage(action("Lucky Burst active."));
                } else {
                    player.sendMessage(warning("Not enough quest points."));
                }
            }
            case QUEST_ABILITY_WOOL_RUSH_SLOT -> {
                if (isAbilityActive(activeWoolRushUntilByPlayer, player.getUniqueId())) {
                    player.sendMessage(warning("Wool Rush is already active."));
                    break;
                }
                if (blockTutorialMenuPurchase(player, TutorialStep.USE_ABILITY,
                        "Activate any quest ability")) {
                    break;
                }
                if (activateQuestAbility(
                        player,
                        activeWoolRushUntilByPlayer,
                        getQuestWoolRushCost(player),
                        getAbilityDurationMs(player, QUEST_WOOL_RUSH_BASE_DURATION_MS),
                        Sound.ENTITY_ENDER_DRAGON_FLAP,
                        org.bukkit.Particle.CLOUD)) {
                    markTutorialAbilityUsed(player);
                    applyWoolRushToShearedSheep(player);
                    player.getWorld().spawnParticle(org.bukkit.Particle.SPORE_BLOSSOM_AIR,
                            player.getLocation().add(0, 1.0, 0), 28, 0.5, 0.35, 0.5, 0.01);
                    playSound(player, Sound.BLOCK_MOSS_CARPET_PLACE, 1.0f, 0.8f);
                    player.sendMessage(action("Wool Rush active."));
                } else {
                    player.sendMessage(warning("Not enough quest points."));
                }
            }
            case QUEST_ABILITY_JACKPOT_SHEARS_SLOT -> {
                if (isAbilityActive(activeJackpotShearsUntilByPlayer, player.getUniqueId())) {
                    player.sendMessage(warning("Jackpot Shears is already active."));
                    break;
                }
                if (blockTutorialMenuPurchase(player, TutorialStep.USE_ABILITY,
                        "Activate any quest ability")) {
                    break;
                }
                if (activateQuestAbility(
                        player,
                        activeJackpotShearsUntilByPlayer,
                        getQuestJackpotCost(player),
                        getAbilityDurationMs(player, QUEST_JACKPOT_SHEARS_BASE_DURATION_MS),
                        Sound.ENTITY_PLAYER_LEVELUP,
                        org.bukkit.Particle.CRIT)) {
                    markTutorialAbilityUsed(player);
                    player.getWorld().spawnParticle(org.bukkit.Particle.FIREWORKS_SPARK,
                            player.getLocation().add(0, 1.1, 0), 22, 0.45, 0.45, 0.45, 0.02);
                    playSound(player, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.8f, 1.6f);
                    player.sendMessage(action("Jackpot Shears active."));
                } else {
                    player.sendMessage(warning("Not enough quest points."));
                }
            }
            case QUEST_ABILITY_AUTO_MERGE_SLOT -> {
                if (toggleCountAbilityEnabled(player, activeAutoMergeUsesByPlayer, autoMergeEnabledByPlayer)) {
                    player.sendMessage(action("Auto Merge "
                            + (autoMergeEnabledByPlayer.getOrDefault(player.getUniqueId(), true) ? "enabled."
                                    : "disabled.")));
                    break;
                }
                if (blockTutorialMenuPurchase(player, TutorialStep.USE_ABILITY,
                        "Activate any quest ability")) {
                    break;
                }
                if (activateCountQuestAbility(
                        player,
                        activeAutoMergeUsesByPlayer,
                        autoMergeEnabledByPlayer,
                        getQuestAutoMergeCost(player),
                        getAbilityUseCount(player, QUEST_AUTO_MERGE_BASE_DURATION_MS),
                        Sound.BLOCK_PISTON_EXTEND,
                        org.bukkit.Particle.ENCHANTMENT_TABLE)) {
                    markTutorialAbilityUsed(player);
                    nextAutoMergeAtByPlayer.put(player.getUniqueId(), 0L);
                    player.getWorld().spawnParticle(org.bukkit.Particle.WAX_ON,
                            player.getLocation().add(0, 1.0, 0), 26, 0.5, 0.4, 0.5, 0.03);
                    playSound(player, Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.3f);
                    player.sendMessage(action("Auto Merge active."));
                } else {
                    player.sendMessage(warning("Not enough quest points."));
                }
            }
            case QUEST_ABILITY_AUTO_SHEAR_SLOT -> {
                if (toggleCountAbilityEnabled(player, activeAutoShearUsesByPlayer, autoShearEnabledByPlayer)) {
                    player.sendMessage(action("Auto Shear "
                            + (autoShearEnabledByPlayer.getOrDefault(player.getUniqueId(), true) ? "enabled."
                                    : "disabled.")));
                    break;
                }
                if (blockTutorialMenuPurchase(player, TutorialStep.USE_ABILITY,
                        "Activate any quest ability")) {
                    break;
                }
                if (activateCountQuestAbility(
                        player,
                        activeAutoShearUsesByPlayer,
                        autoShearEnabledByPlayer,
                        getQuestAutoShearCost(player),
                        getAbilityUseCount(player, QUEST_AUTO_SHEAR_BASE_DURATION_MS),
                        Sound.ENTITY_SHEEP_SHEAR,
                        org.bukkit.Particle.WAX_OFF)) {
                    markTutorialAbilityUsed(player);
                    nextAutoShearAtByPlayer.put(player.getUniqueId(), 0L);
                    player.getWorld().spawnParticle(org.bukkit.Particle.WAX_OFF,
                            player.getLocation().add(0, 1.0, 0), 26, 0.5, 0.4, 0.5, 0.03);
                    playSound(player, Sound.ITEM_TRIDENT_RETURN, 0.8f, 1.4f);
                    player.sendMessage(action("Auto Shear active."));
                } else {
                    player.sendMessage(warning("Not enough quest points."));
                }
            }
            case QUEST_OPEN_UPGRADES_SLOT -> {
                markTutorialQuestUpgradesOpened(player);
                openQuestUpgradesMenu(player);
                return;
            }
            case QUEST_BACK_TO_UPGRADES_SLOT -> {
                openUpgradeMenu(player);
                return;
            }
            default -> {
                return;
            }
        }
        openQuestMenu(player);
    }

    public static void openQuestUpgradesMenu(Player player) {
        if (player == null) {
            return;
        }
        markTutorialQuestUpgradesOpened(player);
        Inventory inventory = Bukkit.createInventory(null, 27, QUEST_UPGRADES_MENU_TITLE);
        inventory.setItem(4, MenuItemFactory.create(
                Material.BOOK,
                "Tutorial Tip",
                List.of(
                        "Opening this menu completes",
                        "the 'Quest Upgrades' tutorial step.",
                        "You do NOT need to buy here.")));
        inventory.setItem(QUEST_UPGRADE_DURATION_SLOT, MenuItemFactory.create(
                Material.CLOCK,
                "Extended Buff Duration",
                List.of(
                        "Level: " + getQuestUpgradeDurationLevel(player),
                        "+30s ability duration per level",
                        "Cost: " + formatPoints(getQuestUpgradeDurationCost(player)) + " quest points",
                        "Click: Upgrade")));
        inventory.setItem(QUEST_UPGRADE_POWER_SLOT, MenuItemFactory.create(
                Material.BLAZE_POWDER,
                "Amplified Buff Power",
                List.of(
                        "Level: " + getQuestUpgradePowerLevel(player),
                        "Jackpot Shears: +1x per level",
                        "Quest ability costs: -1 per level",
                        "Cost: " + formatPoints(getQuestUpgradePowerCost(player)) + " quest points",
                        "Click: Upgrade")));
        inventory.setItem(QUEST_UPGRADE_BACK_SLOT, MenuItemFactory.create(
                Material.ARROW,
                "Back To Quest Abilities",
                List.of("Click to go back")));
        player.openInventory(inventory);
    }

    public static void handleQuestUpgradeMenuClick(Player player, int slot) {
        if (player == null) {
            return;
        }
        markTutorialQuestUpgradesOpened(player);
        switch (slot) {
            case QUEST_UPGRADE_DURATION_SLOT -> {
                if (blockTutorialMenuPurchase(player, TutorialStep.COMPLETE,
                        "Quest Upgrades only need to be opened for the tutorial")) {
                    break;
                }
                if (upgradeQuestDuration(player)) {
                    playUpgradeSound(player);
                    player.sendMessage(action("Quest duration upgrade purchased."));
                } else {
                    player.sendMessage(warning("Not enough quest points."));
                }
            }
            case QUEST_UPGRADE_POWER_SLOT -> {
                if (blockTutorialMenuPurchase(player, TutorialStep.COMPLETE,
                        "Quest Upgrades only need to be opened for the tutorial")) {
                    break;
                }
                if (upgradeQuestPower(player)) {
                    playUpgradeSound(player);
                    player.sendMessage(action("Quest power upgrade purchased."));
                } else {
                    player.sendMessage(warning("Not enough quest points."));
                }
            }
            case QUEST_UPGRADE_BACK_SLOT -> {
                openQuestMenu(player);
                return;
            }
            default -> {
                return;
            }
        }
        openQuestUpgradesMenu(player);
    }

    public static void openShopMenu(Player player) {
        if (player == null) {
            return;
        }
        markTutorialShearShopOpened(player);
        Inventory inventory = Bukkit.createInventory(null, 27, SHOP_MENU_TITLE);
        int woolSaveLevel = getShearWoolSaveLevel(player);
        int tierBoostLevel = getShearTierBoostLevel(player);
        inventory.setItem(SHOP_SHEAR_SLOT, MenuItemFactory.create(
                Material.SHEARS,
                "Shear Value",
                List.of(
                        "Level: " + getShearShopLevel(player),
                        "Cost: " + formatPoints(getShearUpgradeCost(player)) + " points",
                        "Points: base x" + getShearPointMultiplier(player),
                        "Wool reward scales with level",
                        "Click to purchase")));
        inventory.setItem(SHOP_SHEAR_KEEP_WOOL_SLOT, MenuItemFactory.create(
                Material.WHITE_WOOL,
                "Wool Keeper",
                List.of(
                        "Level: " + woolSaveLevel + " / " + SHEAR_WOOL_SAVE_MAX_LEVEL,
                        "Chance: " + getShearWoolSaveChancePercent(player) + "%",
                        woolSaveLevel >= SHEAR_WOOL_SAVE_MAX_LEVEL
                                ? "MAXED"
                                : "Cost: " + formatPoints(getShearWoolSaveUpgradeCost(player)) + " points",
                        "Chance for sheep to keep wool when sheared")));
        inventory.setItem(SHOP_SHEAR_TIER_BOOST_SLOT, MenuItemFactory.create(
                Material.GLOWSTONE_DUST,
                "Tier Booster",
                List.of(
                        "Level: " + tierBoostLevel + " / " + SHEAR_TIER_BOOST_MAX_LEVEL,
                        "Chance: " + getShearTierBoostChancePercent(player) + "%",
                        tierBoostLevel >= SHEAR_TIER_BOOST_MAX_LEVEL
                                ? "MAXED"
                                : "Cost: " + formatPoints(getShearTierBoostUpgradeCost(player)) + " points",
                        "Chance for shearing to upgrade sheep by one tier")));
        inventory.setItem(SHOP_BACK_TO_UPGRADES_SLOT, MenuItemFactory.create(
                Material.ARROW,
                "Back To Upgrades",
                List.of("Click to go back")));
        player.openInventory(inventory);
    }

    public static void handleShopMenuClick(Player player, int slot) {
        if (player == null) {
            return;
        }
        switch (slot) {
            case SHOP_SHEAR_SLOT -> {
                if (blockTutorialMenuPurchase(player, TutorialStep.BUY_SHEAR_UPGRADE,
                        "Buy one Shear Shop upgrade")) {
                    break;
                }
                if (upgradeShearShop(player)) {
                    markTutorialShearUpgraded(player);
                    playUpgradeSound(player);
                    player.sendMessage(action("Shear shop +1"));
                } else {
                    player.sendMessage(warning("Not enough points."));
                }
            }
            case SHOP_SHEAR_KEEP_WOOL_SLOT -> {
                if (blockTutorialMenuPurchase(player, TutorialStep.BUY_SHEAR_UPGRADE,
                        "Buy one Shear Shop upgrade")) {
                    break;
                }
                if (upgradeShearWoolSave(player)) {
                    markTutorialShearUpgraded(player);
                    playUpgradeSound(player);
                    player.sendMessage(action("Wool Keeper: " + getShearWoolSaveChancePercent(player) + "%"));
                } else {
                    player.sendMessage(warning("Unable to buy Wool Keeper upgrade."));
                }
            }
            case SHOP_SHEAR_TIER_BOOST_SLOT -> {
                if (blockTutorialMenuPurchase(player, TutorialStep.BUY_SHEAR_UPGRADE,
                        "Buy one Shear Shop upgrade")) {
                    break;
                }
                if (upgradeShearTierBoost(player)) {
                    markTutorialShearUpgraded(player);
                    playUpgradeSound(player);
                    player.sendMessage(action("Tier Booster: " + getShearTierBoostChancePercent(player) + "%"));
                } else {
                    player.sendMessage(warning("Unable to buy Tier Booster upgrade."));
                }
            }
            case SHOP_BACK_TO_UPGRADES_SLOT -> {
                openUpgradeMenu(player);
                return;
            }
            default -> {
                return;
            }
        }
        updatePointsScoreboard(player);
        openShopMenu(player);
    }

    private static BigInteger getEggSpeedUpgradeCost(Player player) {
        int level = getEggSpeedLevel(player);
        return getDoubledUpgradeCostBig(EGG_SPEED_UPGRADE_BASE_COST, level);
    }

    private static BigInteger getWoolRegenUpgradeCost(Player player) {
        int level = getWoolRegenLevel(player);
        return getDoubledUpgradeCostBig(WOOL_REGEN_UPGRADE_BASE_COST, level);
    }

    private static BigInteger getHigherTierChanceUpgradeCost(Player player) {
        int level = getHigherTierChanceLevel(player);
        return getDoubledUpgradeCostBig(HIGHER_TIER_CHANCE_UPGRADE_BASE_COST, level);
    }

    private static boolean upgradeEggSpeed(Player player) {
        if (player == null) {
            return false;
        }
        int currentLevel = getEggSpeedLevel(player);
        if (currentLevel >= getEggSpeedMaxLevel(player)) {
            return false;
        }
        BigInteger cost = getEggSpeedUpgradeCost(player);
        if (!canSpendUpgradePointsDuringTutorial(player, cost)) {
            return false;
        }
        if (!trySpendPoints(player, cost)) {
            return false;
        }
        eggSpeedLevelByPlayer.put(player.getUniqueId(), currentLevel + 1);
        resetEggTimer(player);
        saveData();
        return true;
    }

    private static boolean upgradeWoolRegen(Player player) {
        if (player == null) {
            return false;
        }
        int currentLevel = getWoolRegenLevel(player);
        if (currentLevel >= getWoolRegenMaxLevel(player)) {
            return false;
        }
        BigInteger cost = getWoolRegenUpgradeCost(player);
        if (!canSpendUpgradePointsDuringTutorial(player, cost)) {
            return false;
        }
        if (!trySpendPoints(player, cost)) {
            return false;
        }
        int newLevel = currentLevel + 1;
        woolRegenLevelByPlayer.put(player.getUniqueId(), newLevel);
        applyWoolRegenReductionToActiveCooldowns(player, currentLevel, newLevel);
        saveData();
        return true;
    }

    private static void applyWoolRegenReductionToActiveCooldowns(Player player, int oldLevel, int newLevel) {
        if (player == null || plugin == null || newLevel <= oldLevel) {
            return;
        }

        double oldMultiplier = Math.pow(WOOL_REGEN_PER_LEVEL_MULTIPLIER, Math.max(0, oldLevel));
        double newMultiplier = Math.pow(WOOL_REGEN_PER_LEVEL_MULTIPLIER, Math.max(0, newLevel));
        if (oldMultiplier <= 0.0D || newMultiplier >= oldMultiplier) {
            return;
        }

        double ratio = newMultiplier / oldMultiplier;
        long now = System.currentTimeMillis();
        UUID ownerId = player.getUniqueId();

        for (World world : plugin.getServer().getWorlds()) {
            if (!isSheepFarmWorld(world) || !ownerId.equals(getOwnerId(world))) {
                continue;
            }
            for (Sheep sheep : world.getEntitiesByClass(Sheep.class)) {
                if (sheep == null || !sheep.isValid() || sheep.isDead() || !sheep.isSheared()) {
                    continue;
                }
                long nextEatAt = getNextEatTimestamp(sheep);
                if (nextEatAt <= now) {
                    continue;
                }
                long remaining = nextEatAt - now;
                long reducedRemaining = Math.max(1L, (long) Math.ceil(remaining * ratio));
                setNextEatTimestamp(sheep, now + reducedRemaining);
                updateSheepName(sheep);
            }
        }
    }

    private static boolean upgradeHigherTierChance(Player player) {
        if (player == null) {
            return false;
        }
        int currentLevel = getHigherTierChanceLevel(player);
        if (currentLevel >= getHigherTierChanceMaxLevel(player)) {
            return false;
        }
        BigInteger cost = getHigherTierChanceUpgradeCost(player);
        if (!canSpendUpgradePointsDuringTutorial(player, cost)) {
            return false;
        }
        if (!trySpendPoints(player, cost)) {
            return false;
        }
        higherTierChanceLevelByPlayer.put(player.getUniqueId(), currentLevel + 1);
        saveData();
        return true;
    }

    private static int getWoolRegenLevel(World world) {
        UUID ownerId = getOwnerId(world);
        if (ownerId == null) {
            return 0;
        }
        return woolRegenLevelByPlayer.getOrDefault(ownerId, 0);
    }

    private static int getWoolCooldownPercentAtLevel(int level) {
        int normalizedLevel = Math.max(0, level);
        return Math.max(1, (int) Math.ceil(getWoolCooldownPercentRawAtLevel(normalizedLevel)));
    }

    private static double getWoolCooldownFactorAtLevel(int level) {
        double factor = Math.pow(WOOL_REGEN_PER_LEVEL_MULTIPLIER, Math.max(0, level));
        if (!Double.isFinite(factor) || factor <= 0.0D) {
            return (100.0D - WOOL_REGEN_MAX_REDUCTION_PERCENT) / 100.0D;
        }
        return Math.max((100.0D - WOOL_REGEN_MAX_REDUCTION_PERCENT) / 100.0D, factor);
    }

    private static double getWoolCooldownPercentRawAtLevel(int level) {
        return getWoolCooldownFactorAtLevel(level) * 100.0D;
    }

    private static double getWoolCooldownReductionPercentRawAtLevel(int level) {
        return Math.min(WOOL_REGEN_MAX_REDUCTION_PERCENT, 100.0D - getWoolCooldownPercentRawAtLevel(level));
    }

    private static String getWoolCooldownPercentDisplayAtLevel(int level) {
        return String.format(Locale.ROOT, "%1$." + WOOL_REGEN_PERCENT_DISPLAY_DECIMALS + "f",
                getWoolCooldownPercentRawAtLevel(level));
    }

    private static String getWoolCooldownReductionPercentDisplayAtLevel(int level) {
        return String.format(Locale.ROOT, "%1$." + WOOL_REGEN_PERCENT_DISPLAY_DECIMALS + "f",
                getWoolCooldownReductionPercentRawAtLevel(level));
    }

    private static String getWoolCooldownFactorDisplayAtLevel(int level) {
        return String.format(Locale.ROOT, "%1$." + WOOL_REGEN_FACTOR_DISPLAY_DECIMALS + "f",
                getWoolCooldownFactorAtLevel(level));
    }

    private static int getWoolCooldownReductionPercentAtLevel(int level) {
        return Math.min(99, Math.max(0, 100 - getWoolCooldownPercentAtLevel(level)));
    }

    private static int getDoubledUpgradeCost(int baseCost, int level) {
        if (level <= 0) {
            return baseCost;
        }
        long multiplier = 1L << Math.min(30, level);
        long cost = baseCost * multiplier;
        return cost > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) cost;
    }

    private static BigInteger getDoubledUpgradeCostBig(int baseCost, int level) {
        if (baseCost <= 0) {
            return BigInteger.ZERO;
        }
        int normalizedLevel = Math.max(0, level);
        return BigInteger.valueOf(baseCost).shiftLeft(normalizedLevel);
    }

    public static int getSheepCount(World world) {
        if (world == null) {
            return 0;
        }
        return liveSheepCountByWorld.getOrDefault(world.getUID(), countLiveSheep(world));
    }

    public static boolean isWorldAtLimit(World world) {
        if (world == null) {
            return false;
        }
        refreshLiveSheepCount(world);
        return getSheepCount(world) >= getOwnerLimit(world);
    }

    public static void refreshLiveSheepCounts(Iterable<World> worlds) {
        if (worlds == null) {
            return;
        }
        Set<UUID> knownFarmWorlds = new HashSet<>();
        for (World world : worlds) {
            if (!isSheepFarmWorld(world)) {
                continue;
            }
            knownFarmWorlds.add(world.getUID());
            refreshLiveSheepCount(world);
        }
        liveSheepCountByWorld.keySet().removeIf(worldId -> !knownFarmWorlds.contains(worldId));
    }

    public static void refreshLiveSheepCount(World world) {
        if (world == null || !isSheepFarmWorld(world)) {
            return;
        }
        liveSheepCountByWorld.put(world.getUID(), countLiveSheep(world));
    }

    private static int countLiveSheep(World world) {
        int liveCount = 0;
        for (Sheep sheep : world.getEntitiesByClass(Sheep.class)) {
            if (sheep != null && sheep.isValid() && !sheep.isDead()) {
                liveCount++;
            }
        }
        return liveCount;
    }

    public static void savePlayerInventory(Player player) {
        if (player == null || savedInventories.containsKey(player.getUniqueId())) {
            return;
        }
        ItemStack[] contents = InventoryDataUtils.cloneItemStackArray(player.getInventory().getContents());
        ItemStack[] armor = InventoryDataUtils.cloneItemStackArray(player.getInventory().getArmorContents());
        ItemStack offhand = player.getInventory().getItemInOffHand() == null ? null
                : player.getInventory().getItemInOffHand().clone();
        savedInventories.put(player.getUniqueId(), new InventoryDataUtils.Snapshot(contents, armor, offhand));
        saveData();
    }

    public static void restorePlayerInventory(Player player) {
        if (player == null) {
            return;
        }
        InventoryDataUtils.Snapshot snapshot = savedInventories.remove(player.getUniqueId());
        if (snapshot == null) {
            return;
        }
        player.getInventory().clear();
        player.getInventory().setContents(InventoryDataUtils.cloneItemStackArray(snapshot.contents()));
        player.getInventory().setArmorContents(InventoryDataUtils.cloneItemStackArray(snapshot.armor()));
        player.getInventory().setItemInOffHand(snapshot.offhand() == null ? null : snapshot.offhand().clone());
        saveData();
    }

    public static boolean hasSavedInventory(Player player) {
        return player != null && savedInventories.containsKey(player.getUniqueId());
    }

    public static void restoreSavedInventoryOutsideFarm(Player player) {
        if (player == null || isSheepFarmWorld(player.getWorld())) {
            return;
        }
        if (!hasSavedInventory(player)) {
            return;
        }
        restorePlayerInventory(player);
    }

    public static void restoreSavedStateOutsideFarm(Player player) {
        if (player == null || isSheepFarmWorld(player.getWorld())) {
            return;
        }
        if (savedInventories.containsKey(player.getUniqueId())) {
            restorePlayerInventory(player);
        }
        if (savedScoreboards.containsKey(player.getUniqueId())) {
            restorePlayerScoreboard(player);
        }
        player.setPlayerListName(null);
        clearEggTimer(player);
    }

    public static void showPointsScoreboard(Player player) {
        if (player == null) {
            return;
        }
        if (!savedScoreboards.containsKey(player.getUniqueId())) {
            savedScoreboards.put(player.getUniqueId(), player.getScoreboard());
        }

        Scoreboard scoreboard = player.getServer().getScoreboardManager().getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("sheepmerge_points", "dummy", "Sheep Merge Points");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        renderPointsScoreboard(player, scoreboard, objective);
        player.setScoreboard(scoreboard);
    }

    public static void updatePointsScoreboard(Player player) {
        if (player == null) {
            return;
        }
        Scoreboard scoreboard = player.getScoreboard();
        Objective objective = scoreboard == null ? null : scoreboard.getObjective("sheepmerge_points");
        if (objective == null) {
            showPointsScoreboard(player);
            return;
        }
        renderPointsScoreboard(player, scoreboard, objective);
    }

    private static String getQuestScoreLine(String label, int progress, int target, boolean complete) {
        return label + ": " + (complete ? "done" : (progress + "/" + target));
    }

    private static String makeScoreboardSpacer(int index) {
        return " ".repeat(Math.max(1, index));
    }

    private static void renderPointsScoreboard(Player player, Scoreboard scoreboard, Objective objective) {
        if (player == null || scoreboard == null || objective == null) {
            return;
        }

        for (String entry : new HashSet<>(scoreboard.getEntries())) {
            scoreboard.resetScores(entry);
        }

        UUID playerId = player.getUniqueId();
        List<String> lines = new ArrayList<>();
        lines.add("Points: " + formatPoints(getPlayerPointsBig(player)));
        lines.add("Prestige Lv: " + getPrestigeLevel(player));
        lines.add("Prestige Pts: " + formatPoints(getPrestigePoints(player)));

        if (shouldShowScoreboardQuestPoints(player)) {
            lines.add("Quest Pts: " + formatPoints(getQuestPoints(player)));
        }
        if (shouldShowScoreboardAutomationPoints(player)) {
            lines.add("Auto Pts: " + formatPoints(getAutomationPoints(player)));
        }
        if (shouldShowScoreboardSacrificePoints(player)) {
            lines.add("Sac Pts: " + formatPoints(getSacrificePoints(player)));
        }

        boolean compact = getScoreboardLayoutMode(player) == 1;

        if (!compact && shouldShowScoreboardQuestProgress(player)) {
            lines.add(makeScoreboardSpacer(lines.size() + 1));
            lines.add("Quest Reset: " + formatDuration(getQuestResetRemainingMs(player)));
            lines.add(getQuestScoreLine("Shear", questShearsByPlayer.getOrDefault(playerId, 0),
                    QUEST_SHEARS_TARGET, questShearsCompleteByPlayer.getOrDefault(playerId, false)));
            lines.add(getQuestScoreLine("Spawn", questSpawnsByPlayer.getOrDefault(playerId, 0),
                    QUEST_SPAWNS_TARGET, questSpawnsCompleteByPlayer.getOrDefault(playerId, false)));
            lines.add(getQuestScoreLine("Merge", questMergesByPlayer.getOrDefault(playerId, 0),
                    QUEST_MERGES_TARGET, questMergesCompleteByPlayer.getOrDefault(playerId, false)));
        }

        if (!compact && shouldShowScoreboardAbilityStatus(player)) {
            lines.add(makeScoreboardSpacer(lines.size() + 1));
            lines.add("Abilities");
            lines.add(getCountAbilityScoreLine("Lucky", activeLuckyBurstUsesByPlayer,
                    luckyBurstEnabledByPlayer, playerId));
            lines.add(getAbilityScoreLine("Wool", activeWoolRushUntilByPlayer,
                    pausedWoolRushRemainingMsByPlayer, playerId));
            lines.add(getAbilityScoreLine("Jackpot", activeJackpotShearsUntilByPlayer,
                    pausedJackpotShearsRemainingMsByPlayer, playerId));
            lines.add(getCountAbilityScoreLine("Merge", activeAutoMergeUsesByPlayer,
                    autoMergeEnabledByPlayer, playerId));
            lines.add(getCountAbilityScoreLine("Shear", activeAutoShearUsesByPlayer,
                    autoShearEnabledByPlayer, playerId));
        }

        int score = Math.min(15, lines.size());
        for (int index = 0; index < lines.size() && score > 0; index++) {
            objective.getScore(lines.get(index)).setScore(score);
            score--;
        }

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online == null) {
                continue;
            }
            updateTabListPointsVisibility(online);
        }
    }

    public static void updateTabListPointsVisibility(Player player) {
        if (player == null) {
            return;
        }
        if (!isOwnedSheepFarmWorld(player.getWorld())) {
            player.setPlayerListName(null);
            return;
        }
        BigInteger points = getPlayerPointsBig(player).max(BigInteger.ZERO);
        player.setPlayerListName(color("&e" + formatPoints(points) + " &7| &f" + player.getName()));
    }

    public static void restorePlayerScoreboard(Player player) {
        if (player == null) {
            return;
        }
        Scoreboard previous = savedScoreboards.remove(player.getUniqueId());
        if (previous != null) {
            player.setScoreboard(previous);
        }
    }

    public static void restoreAllPlayerStates() {
        if (plugin == null) {
            return;
        }
        World fallbackWorld = plugin.getServer().getWorlds().isEmpty() ? null : plugin.getServer().getWorlds().get(0);
        for (World world : plugin.getServer().getWorlds()) {
            if (isSheepFarmWorld(world)) {
                world.save();
            }
        }
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (savedInventories.containsKey(player.getUniqueId())) {
                restorePlayerInventory(player);
            }
            if (savedScoreboards.containsKey(player.getUniqueId())) {
                restorePlayerScoreboard(player);
            }
            clearEggTimer(player);
            clearPickedUpSheep(player);
            clearComboRuntime(player);
            if (fallbackWorld != null && isSheepFarmWorld(player.getWorld())) {
                Location fallbackSpawn = fallbackWorld.getSpawnLocation().clone().add(0.5D, 0.0D, 0.5D);
                player.teleport(fallbackSpawn);
            }
        }
        savedInventories.clear();
        savedScoreboards.clear();
        EGG_MODULE.clearSavedExperienceCache();
    }

    public static ItemStack getSheepMergeShears() {
        ItemStack shears = new ItemStack(org.bukkit.Material.SHEARS, 1);
        var meta = shears.getItemMeta();
        if (meta != null) {
            meta.setUnbreakable(true);
            meta.setDisplayName("Sheep Merge Shears");
            shears.setItemMeta(meta);
        }
        return shears;
    }

    public static void saveData() {
        if (plugin == null || dataFile == null) {
            return;
        }
        try {
            if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
                return;
            }
            if (dataConfig == null) {
                dataConfig = YamlConfiguration.loadConfiguration(dataFile);
            }
            dataConfig.set("points", null);
            dataConfig.set("extraLimit", null);
            dataConfig.set("eggSpeed", null);
            dataConfig.set("woolRegen", null);
            dataConfig.set("higherTierChance", null);
            dataConfig.set("prestigeLevel", null);
            dataConfig.set("prestigePoints", null);
            dataConfig.set("prestigeDoublePoints", null);
            dataConfig.set("prestigeHigherMax", null);
            dataConfig.set("prestigeStartEggs", null);
            dataConfig.set("prestigeEggCap", null);
            dataConfig.set("prestigeBaseSpawnTier", null);
            dataConfig.set("prestigeQuestReward", null);
            dataConfig.set("prestigeRefundCooldown", null);
            dataConfig.set("highestAnnouncedTier", null);
            dataConfig.set("highestAnnouncedRainbowTier", null);
            dataConfig.set("prestigeExpandFarm", null);
            dataConfig.set("shearShop", null);
            dataConfig.set("shearWoolSave", null);
            dataConfig.set("shearTierBoost", null);
            dataConfig.set("tutorialCompleted", null);
            dataConfig.set("tutorialBypassed", null);
            dataConfig.set("tutorialShears", null);
            dataConfig.set("tutorialSpawns", null);
            dataConfig.set("tutorialMerges", null);
            dataConfig.set("tutorialUpgradeOpened", null);
            dataConfig.set("tutorialQuestOpened", null);
            dataConfig.set("tutorialQuestUpgradesOpened", null);
            dataConfig.set("tutorialPrestigeOpened", null);
            dataConfig.set("tutorialAbilityUsed", null);
            dataConfig.set("tutorialShearUpgraded", null);
            dataConfig.set("tutorialRegularUpgradesBought", null);
            dataConfig.set("tutorialPrestigedOnce", null);
            dataConfig.set("tutorialShearShopOpened", null);
            dataConfig.set("farmVisitEnabled", null);
            dataConfig.set("questPoints", null);
            dataConfig.set("questReset", null);
            dataConfig.set("questUpgradeDuration", null);
            dataConfig.set("questUpgradePower", null);
            dataConfig.set("activeLuckyBurstUntil", null);
            dataConfig.set("activeWoolRushUntil", null);
            dataConfig.set("activeJackpotShearsUntil", null);
            dataConfig.set("activeAutoMergeUntil", null);
            dataConfig.set("activeAutoShearUntil", null);
            dataConfig.set("pausedLuckyBurstRemaining", null);
            dataConfig.set("pausedWoolRushRemaining", null);
            dataConfig.set("pausedJackpotShearsRemaining", null);
            dataConfig.set("pausedAutoMergeRemaining", null);
            dataConfig.set("pausedAutoShearRemaining", null);
            dataConfig.set("comboDecayUpgrade", null);
            dataConfig.set("comboMaxUpgrade", null);
            dataConfig.set("comboGainUpgrade", null);
            dataConfig.set("automationPoints", null);
            dataConfig.set("automationAutoBuy", null);
            dataConfig.set("automationAutoAbility", null);
            dataConfig.set("automationSlowAutoMerge", null);
            dataConfig.set("automationSlowAutoShear", null);
            dataConfig.set("automationAutoSpawn", null);
            dataConfig.set("automationAutoPrestige", null);
            dataConfig.set("automationAutoBuyEnabled", null);
            dataConfig.set("automationAutoAbilityEnabled", null);
            dataConfig.set("automationSlowAutoMergeEnabled", null);
            dataConfig.set("automationSlowAutoShearEnabled", null);
            dataConfig.set("automationAutoSpawnEnabled", null);
            dataConfig.set("automationAutoPrestigeEnabled", null);
            dataConfig.set("scoreboardLayoutMode", null);
            dataConfig.set("scoreboardShowQuestPoints", null);
            dataConfig.set("scoreboardShowAutomationPoints", null);
            dataConfig.set("scoreboardShowSacrificePoints", null);
            dataConfig.set("scoreboardShowQuestProgress", null);
            dataConfig.set("scoreboardShowAbilityStatus", null);
            dataConfig.set("sacrificePoints", null);
            dataConfig.set("sacrificeUnlocksBought", null);
            dataConfig.set("farmSheep", null);
            dataConfig.set("tutorialSheep", null);
            dataConfig.set("pendingInventory", null);
            for (Map.Entry<UUID, BigInteger> entry : pointsByPlayer.entrySet()) {
                dataConfig.set("points." + entry.getKey().toString(), entry.getValue().toString());
            }
            for (Map.Entry<UUID, Integer> entry : extraLimitByPlayer.entrySet()) {
                dataConfig.set("extraLimit." + entry.getKey().toString(), entry.getValue());
            }
            for (Map.Entry<UUID, Integer> entry : eggSpeedLevelByPlayer.entrySet()) {
                dataConfig.set("eggSpeed." + entry.getKey().toString(), entry.getValue());
            }
            for (Map.Entry<UUID, Integer> entry : woolRegenLevelByPlayer.entrySet()) {
                dataConfig.set("woolRegen." + entry.getKey().toString(), entry.getValue());
            }
            for (Map.Entry<UUID, Integer> entry : higherTierChanceLevelByPlayer.entrySet()) {
                dataConfig.set("higherTierChance." + entry.getKey().toString(), entry.getValue());
            }
            for (Map.Entry<UUID, Integer> entry : prestigeLevelByPlayer.entrySet()) {
                dataConfig.set("prestigeLevel." + entry.getKey().toString(), entry.getValue());
            }
            for (Map.Entry<UUID, Integer> entry : prestigePointsByPlayer.entrySet()) {
                dataConfig.set("prestigePoints." + entry.getKey().toString(), entry.getValue());
            }
            for (Map.Entry<UUID, Integer> entry : prestigeDoublePointsChanceByPlayer.entrySet()) {
                dataConfig.set("prestigeDoublePoints." + entry.getKey().toString(), entry.getValue());
            }
            for (Map.Entry<UUID, Integer> entry : prestigeHigherMaxLevelByPlayer.entrySet()) {
                dataConfig.set("prestigeHigherMax." + entry.getKey().toString(), entry.getValue());
            }
            for (Map.Entry<UUID, Integer> entry : prestigeStartEggsByPlayer.entrySet()) {
                dataConfig.set("prestigeStartEggs." + entry.getKey().toString(), entry.getValue());
            }
            for (Map.Entry<UUID, Integer> entry : prestigeEggCapByPlayer.entrySet()) {
                dataConfig.set("prestigeEggCap." + entry.getKey().toString(), entry.getValue());
            }
            for (Map.Entry<UUID, Integer> entry : prestigeBaseSpawnTierByPlayer.entrySet()) {
                dataConfig.set("prestigeBaseSpawnTier." + entry.getKey().toString(), entry.getValue());
            }
            for (Map.Entry<UUID, Integer> entry : prestigeQuestRewardByPlayer.entrySet()) {
                dataConfig.set("prestigeQuestReward." + entry.getKey().toString(), entry.getValue());
            }
            for (Map.Entry<UUID, Long> entry : nextPrestigeRefundTimestampByPlayer.entrySet()) {
                dataConfig.set("prestigeRefundCooldown." + entry.getKey().toString(), entry.getValue());
            }
            for (Map.Entry<UUID, Integer> entry : highestAnnouncedTierByPlayer.entrySet()) {
                dataConfig.set("highestAnnouncedTier." + entry.getKey().toString(), entry.getValue());
            }
            for (Map.Entry<UUID, Integer> entry : highestAnnouncedRainbowTierByPlayer.entrySet()) {
                dataConfig.set("highestAnnouncedRainbowTier." + entry.getKey().toString(), entry.getValue());
            }
            for (Map.Entry<UUID, Integer> entry : shearShopLevelByPlayer.entrySet()) {
                dataConfig.set("shearShop." + entry.getKey().toString(), entry.getValue());
            }
            for (Map.Entry<UUID, Integer> entry : shearWoolSaveLevelByPlayer.entrySet()) {
                dataConfig.set("shearWoolSave." + entry.getKey().toString(), entry.getValue());
            }
            for (Map.Entry<UUID, Integer> entry : shearTierBoostLevelByPlayer.entrySet()) {
                dataConfig.set("shearTierBoost." + entry.getKey().toString(), entry.getValue());
            }
            for (Map.Entry<UUID, Boolean> entry : tutorialCompletedByPlayer.entrySet()) {
                dataConfig.set("tutorialCompleted." + entry.getKey().toString(), entry.getValue());
            }
            for (Map.Entry<UUID, Boolean> entry : tutorialBypassedByPlayer.entrySet()) {
                dataConfig.set("tutorialBypassed." + entry.getKey().toString(), entry.getValue());
            }
            for (Map.Entry<UUID, Integer> entry : tutorialShearsByPlayer.entrySet()) {
                dataConfig.set("tutorialShears." + entry.getKey().toString(), entry.getValue());
            }
            for (Map.Entry<UUID, Integer> entry : tutorialSpawnsByPlayer.entrySet()) {
                dataConfig.set("tutorialSpawns." + entry.getKey().toString(), entry.getValue());
            }
            for (Map.Entry<UUID, Integer> entry : tutorialMergesByPlayer.entrySet()) {
                dataConfig.set("tutorialMerges." + entry.getKey().toString(), entry.getValue());
            }
            for (Map.Entry<UUID, Boolean> entry : tutorialUpgradeOpenedByPlayer.entrySet()) {
                dataConfig.set("tutorialUpgradeOpened." + entry.getKey().toString(), entry.getValue());
            }
            for (Map.Entry<UUID, Boolean> entry : tutorialQuestOpenedByPlayer.entrySet()) {
                dataConfig.set("tutorialQuestOpened." + entry.getKey().toString(), entry.getValue());
            }
            for (Map.Entry<UUID, Boolean> entry : tutorialQuestUpgradesOpenedByPlayer.entrySet()) {
                dataConfig.set("tutorialQuestUpgradesOpened." + entry.getKey().toString(), entry.getValue());
            }
            for (Map.Entry<UUID, Boolean> entry : tutorialPrestigeOpenedByPlayer.entrySet()) {
                dataConfig.set("tutorialPrestigeOpened." + entry.getKey().toString(), entry.getValue());
            }
            for (Map.Entry<UUID, Boolean> entry : tutorialAbilityUsedByPlayer.entrySet()) {
                dataConfig.set("tutorialAbilityUsed." + entry.getKey().toString(), entry.getValue());
            }
            for (Map.Entry<UUID, Boolean> entry : tutorialShearUpgradedByPlayer.entrySet()) {
                dataConfig.set("tutorialShearUpgraded." + entry.getKey().toString(), entry.getValue());
            }
            for (Map.Entry<UUID, Boolean> entry : tutorialRegularUpgradesBoughtByPlayer.entrySet()) {
                dataConfig.set("tutorialRegularUpgradesBought." + entry.getKey().toString(), entry.getValue());
            }
            for (Map.Entry<UUID, Boolean> entry : tutorialShearTaskRewardGrantedByPlayer.entrySet()) {
                dataConfig.set("tutorialShearTaskRewardGranted." + entry.getKey().toString(), entry.getValue());
            }
            for (Map.Entry<UUID, Boolean> entry : tutorialPrestigePrepRewardGrantedByPlayer.entrySet()) {
                dataConfig.set("tutorialPrestigePrepRewardGranted." + entry.getKey().toString(), entry.getValue());
            }
            for (Map.Entry<UUID, Boolean> entry : tutorialPrestigedOnceByPlayer.entrySet()) {
                dataConfig.set("tutorialPrestigedOnce." + entry.getKey().toString(), entry.getValue());
            }
            for (Map.Entry<UUID, Boolean> entry : tutorialShearShopOpenedByPlayer.entrySet()) {
                dataConfig.set("tutorialShearShopOpened." + entry.getKey().toString(), entry.getValue());
            }
            for (Map.Entry<UUID, Boolean> entry : farmVisitEnabledByPlayer.entrySet()) {
                dataConfig.set("farmVisitEnabled." + entry.getKey().toString(), entry.getValue());
            }
            for (Map.Entry<UUID, Integer> entry : questPointsByPlayer.entrySet()) {
                dataConfig.set("questPoints." + entry.getKey().toString(), entry.getValue());
            }
            for (Map.Entry<UUID, Long> entry : nextQuestResetTimestampByPlayer.entrySet()) {
                dataConfig.set("questReset." + entry.getKey().toString(), entry.getValue());
            }
            for (Map.Entry<UUID, Integer> entry : questUpgradeDurationByPlayer.entrySet()) {
                dataConfig.set("questUpgradeDuration." + entry.getKey().toString(), entry.getValue());
            }
            for (Map.Entry<UUID, Integer> entry : questUpgradePowerByPlayer.entrySet()) {
                dataConfig.set("questUpgradePower." + entry.getKey().toString(), entry.getValue());
            }
            for (Map.Entry<UUID, Long> entry : activeLuckyBurstUntilByPlayer.entrySet()) {
                dataConfig.set("activeLuckyBurstUntil." + entry.getKey().toString(), entry.getValue());
            }
            for (Map.Entry<UUID, Long> entry : activeWoolRushUntilByPlayer.entrySet()) {
                dataConfig.set("activeWoolRushUntil." + entry.getKey().toString(), entry.getValue());
            }
            for (Map.Entry<UUID, Long> entry : activeJackpotShearsUntilByPlayer.entrySet()) {
                dataConfig.set("activeJackpotShearsUntil." + entry.getKey().toString(), entry.getValue());
            }
            for (Map.Entry<UUID, Long> entry : activeAutoMergeUntilByPlayer.entrySet()) {
                dataConfig.set("activeAutoMergeUntil." + entry.getKey().toString(), entry.getValue());
            }
            for (Map.Entry<UUID, Long> entry : activeAutoShearUntilByPlayer.entrySet()) {
                dataConfig.set("activeAutoShearUntil." + entry.getKey().toString(), entry.getValue());
            }
            for (Map.Entry<UUID, Long> entry : pausedLuckyBurstRemainingMsByPlayer.entrySet()) {
                dataConfig.set("pausedLuckyBurstRemaining." + entry.getKey().toString(), entry.getValue());
            }
            for (Map.Entry<UUID, Long> entry : pausedWoolRushRemainingMsByPlayer.entrySet()) {
                dataConfig.set("pausedWoolRushRemaining." + entry.getKey().toString(), entry.getValue());
            }
            for (Map.Entry<UUID, Long> entry : pausedJackpotShearsRemainingMsByPlayer.entrySet()) {
                dataConfig.set("pausedJackpotShearsRemaining." + entry.getKey().toString(), entry.getValue());
            }
            for (Map.Entry<UUID, Long> entry : pausedAutoMergeRemainingMsByPlayer.entrySet()) {
                dataConfig.set("pausedAutoMergeRemaining." + entry.getKey().toString(), entry.getValue());
            }
            for (Map.Entry<UUID, Long> entry : pausedAutoShearRemainingMsByPlayer.entrySet()) {
                dataConfig.set("pausedAutoShearRemaining." + entry.getKey().toString(), entry.getValue());
            }
            for (Map.Entry<UUID, Integer> entry : comboDecayUpgradeByPlayer.entrySet()) {
                dataConfig.set("comboDecayUpgrade." + entry.getKey().toString(), entry.getValue());
            }
            for (Map.Entry<UUID, Integer> entry : comboMaxUpgradeByPlayer.entrySet()) {
                dataConfig.set("comboMaxUpgrade." + entry.getKey().toString(), entry.getValue());
            }
            for (Map.Entry<UUID, Integer> entry : comboGainUpgradeByPlayer.entrySet()) {
                dataConfig.set("comboGainUpgrade." + entry.getKey().toString(), entry.getValue());
            }
            for (Map.Entry<UUID, Integer> entry : automationPointsByPlayer.entrySet()) {
                dataConfig.set("automationPoints." + entry.getKey().toString(), entry.getValue());
            }
            for (Map.Entry<UUID, Integer> entry : automationAutoBuyUpgradeByPlayer.entrySet()) {
                dataConfig.set("automationAutoBuy." + entry.getKey().toString(), entry.getValue());
            }
            for (Map.Entry<UUID, Integer> entry : automationAutoAbilityUpgradeByPlayer.entrySet()) {
                dataConfig.set("automationAutoAbility." + entry.getKey().toString(), entry.getValue());
            }
            for (Map.Entry<UUID, Integer> entry : automationSlowAutoMergeUpgradeByPlayer.entrySet()) {
                dataConfig.set("automationSlowAutoMerge." + entry.getKey().toString(), entry.getValue());
            }
            for (Map.Entry<UUID, Integer> entry : automationSlowAutoShearUpgradeByPlayer.entrySet()) {
                dataConfig.set("automationSlowAutoShear." + entry.getKey().toString(), entry.getValue());
            }
            for (Map.Entry<UUID, Integer> entry : automationAutoSpawnUpgradeByPlayer.entrySet()) {
                dataConfig.set("automationAutoSpawn." + entry.getKey().toString(), entry.getValue());
            }
            for (Map.Entry<UUID, Integer> entry : automationAutoPrestigeUpgradeByPlayer.entrySet()) {
                dataConfig.set("automationAutoPrestige." + entry.getKey().toString(), entry.getValue());
            }
            for (Map.Entry<UUID, Boolean> entry : automationAutoBuyEnabledByPlayer.entrySet()) {
                dataConfig.set("automationAutoBuyEnabled." + entry.getKey().toString(), entry.getValue());
            }
            for (Map.Entry<UUID, Boolean> entry : automationAutoAbilityEnabledByPlayer.entrySet()) {
                dataConfig.set("automationAutoAbilityEnabled." + entry.getKey().toString(), entry.getValue());
            }
            for (Map.Entry<UUID, Boolean> entry : automationSlowAutoMergeEnabledByPlayer.entrySet()) {
                dataConfig.set("automationSlowAutoMergeEnabled." + entry.getKey().toString(), entry.getValue());
            }
            for (Map.Entry<UUID, Boolean> entry : automationSlowAutoShearEnabledByPlayer.entrySet()) {
                dataConfig.set("automationSlowAutoShearEnabled." + entry.getKey().toString(), entry.getValue());
            }
            for (Map.Entry<UUID, Boolean> entry : automationAutoSpawnEnabledByPlayer.entrySet()) {
                dataConfig.set("automationAutoSpawnEnabled." + entry.getKey().toString(), entry.getValue());
            }
            for (Map.Entry<UUID, Boolean> entry : automationAutoPrestigeEnabledByPlayer.entrySet()) {
                dataConfig.set("automationAutoPrestigeEnabled." + entry.getKey().toString(), entry.getValue());
            }
            for (Map.Entry<UUID, Integer> entry : scoreboardLayoutModeByPlayer.entrySet()) {
                dataConfig.set("scoreboardLayoutMode." + entry.getKey().toString(), entry.getValue());
            }
            for (Map.Entry<UUID, Boolean> entry : scoreboardShowQuestPointsByPlayer.entrySet()) {
                dataConfig.set("scoreboardShowQuestPoints." + entry.getKey().toString(), entry.getValue());
            }
            for (Map.Entry<UUID, Boolean> entry : scoreboardShowAutomationPointsByPlayer.entrySet()) {
                dataConfig.set("scoreboardShowAutomationPoints." + entry.getKey().toString(), entry.getValue());
            }
            for (Map.Entry<UUID, Boolean> entry : scoreboardShowSacrificePointsByPlayer.entrySet()) {
                dataConfig.set("scoreboardShowSacrificePoints." + entry.getKey().toString(), entry.getValue());
            }
            for (Map.Entry<UUID, Boolean> entry : scoreboardShowQuestProgressByPlayer.entrySet()) {
                dataConfig.set("scoreboardShowQuestProgress." + entry.getKey().toString(), entry.getValue());
            }
            for (Map.Entry<UUID, Boolean> entry : scoreboardShowAbilityStatusByPlayer.entrySet()) {
                dataConfig.set("scoreboardShowAbilityStatus." + entry.getKey().toString(), entry.getValue());
            }
            for (Map.Entry<UUID, BigInteger> entry : sacrificePointsByPlayer.entrySet()) {
                dataConfig.set("sacrificePoints." + entry.getKey().toString(), entry.getValue().toString());
            }
            for (Map.Entry<UUID, Integer> entry : sacrificeUnlocksBoughtByPlayer.entrySet()) {
                dataConfig.set("sacrificeUnlocksBought." + entry.getKey().toString(),
                        Math.max(0, Math.min(SACRIFICE_UNLOCK_MAX, entry.getValue())));
            }
            saveSheepSnapshots("farmSheep", savedFarmSheepByPlayer);
            saveSheepSnapshots("tutorialSheep", savedTutorialSheepByPlayer);
            for (Map.Entry<UUID, InventoryDataUtils.Snapshot> entry : savedInventories.entrySet()) {
                String basePath = "pendingInventory." + entry.getKey();
                InventoryDataUtils.Snapshot snapshot = entry.getValue();
                dataConfig.set(basePath + ".contents", InventoryDataUtils.serializeInventoryList(snapshot.contents()));
                dataConfig.set(basePath + ".armor", InventoryDataUtils.serializeInventoryList(snapshot.armor()));
                dataConfig.set(basePath + ".offhand", snapshot.offhand() == null ? null : snapshot.offhand().clone());
            }
            dataConfig.save(dataFile);
        } catch (IOException exception) {
            if (plugin != null) {
                plugin.getLogger().warning("Unable to save sheep merge scores: " + exception.getMessage());
            }
        }
    }

    private static void loadData() {
        if (plugin == null || dataFile == null) {
            return;
        }
        if (!dataFile.exists()) {
            dataConfig = YamlConfiguration.loadConfiguration(dataFile);
            return;
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        if (dataConfig.isConfigurationSection("points")) {
            dataConfig.getConfigurationSection("points").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    String path = "points." + key;
                    String raw = dataConfig.getString(path, null);
                    BigInteger parsed;
                    if (raw != null && !raw.isBlank()) {
                        parsed = new BigInteger(raw.trim());
                    } else {
                        parsed = BigInteger.valueOf(Math.max(0L, dataConfig.getLong(path, 0L)));
                    }
                    pointsByPlayer.put(uuid, parsed.max(BigInteger.ZERO));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
        if (dataConfig.isConfigurationSection("extraLimit")) {
            dataConfig.getConfigurationSection("extraLimit").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    extraLimitByPlayer.put(uuid, dataConfig.getInt("extraLimit." + key, 0));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
        if (dataConfig.isConfigurationSection("eggSpeed")) {
            dataConfig.getConfigurationSection("eggSpeed").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    eggSpeedLevelByPlayer.put(uuid, dataConfig.getInt("eggSpeed." + key, 0));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
        if (dataConfig.isConfigurationSection("woolRegen")) {
            dataConfig.getConfigurationSection("woolRegen").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    woolRegenLevelByPlayer.put(uuid, dataConfig.getInt("woolRegen." + key, 0));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
        if (dataConfig.isConfigurationSection("higherTierChance")) {
            dataConfig.getConfigurationSection("higherTierChance").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    higherTierChanceLevelByPlayer.put(uuid, dataConfig.getInt("higherTierChance." + key, 0));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
        if (dataConfig.isConfigurationSection("prestigeLevel")) {
            dataConfig.getConfigurationSection("prestigeLevel").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    prestigeLevelByPlayer.put(uuid, dataConfig.getInt("prestigeLevel." + key, 0));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
        if (dataConfig.isConfigurationSection("prestigePoints")) {
            dataConfig.getConfigurationSection("prestigePoints").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    prestigePointsByPlayer.put(uuid, dataConfig.getInt("prestigePoints." + key, 0));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
        if (dataConfig.isConfigurationSection("prestigeDoublePoints")) {
            dataConfig.getConfigurationSection("prestigeDoublePoints").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    prestigeDoublePointsChanceByPlayer.put(uuid,
                            Math.max(0,
                                    Math.min(PRESTIGE_DOUBLE_POINTS_MAX_LEVEL,
                                            dataConfig.getInt("prestigeDoublePoints." + key, 0))));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
        if (dataConfig.isConfigurationSection("prestigeHigherMax")) {
            dataConfig.getConfigurationSection("prestigeHigherMax").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    prestigeHigherMaxLevelByPlayer.put(uuid, dataConfig.getInt("prestigeHigherMax." + key, 0));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
        if (dataConfig.isConfigurationSection("prestigeStartEggs")) {
            dataConfig.getConfigurationSection("prestigeStartEggs").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    prestigeStartEggsByPlayer.put(uuid, dataConfig.getInt("prestigeStartEggs." + key, 0));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
        if (dataConfig.isConfigurationSection("prestigeEggCap")) {
            dataConfig.getConfigurationSection("prestigeEggCap").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    prestigeEggCapByPlayer.put(uuid, dataConfig.getInt("prestigeEggCap." + key, 0));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
        if (dataConfig.isConfigurationSection("prestigeBaseSpawnTier")) {
            dataConfig.getConfigurationSection("prestigeBaseSpawnTier").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    prestigeBaseSpawnTierByPlayer.put(
                            uuid,
                            Math.min(SheepTier.RAINBOW.getLevel(),
                                    dataConfig.getInt("prestigeBaseSpawnTier." + key, 0)));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
        if (dataConfig.isConfigurationSection("prestigeQuestReward")) {
            dataConfig.getConfigurationSection("prestigeQuestReward").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    prestigeQuestRewardByPlayer.put(uuid, dataConfig.getInt("prestigeQuestReward." + key, 0));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
        loadSheepSnapshots("farmSheep", savedFarmSheepByPlayer);
        loadSheepSnapshots("tutorialSheep", savedTutorialSheepByPlayer);
        if (dataConfig.isConfigurationSection("prestigeRefundCooldown")) {
            dataConfig.getConfigurationSection("prestigeRefundCooldown").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    nextPrestigeRefundTimestampByPlayer.put(
                            uuid,
                            Math.max(0L, dataConfig.getLong("prestigeRefundCooldown." + key, 0L)));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
        if (dataConfig.isConfigurationSection("highestAnnouncedTier")) {
            dataConfig.getConfigurationSection("highestAnnouncedTier").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    int loaded = dataConfig.getInt("highestAnnouncedTier." + key, SheepTier.WHITE.getLevel());
                    highestAnnouncedTierByPlayer.put(
                            uuid,
                            Math.max(SheepTier.WHITE.getLevel(), Math.min(SheepTier.RAINBOW.getLevel(), loaded)));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
        if (dataConfig.isConfigurationSection("highestAnnouncedRainbowTier")) {
            dataConfig.getConfigurationSection("highestAnnouncedRainbowTier").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    int loaded = dataConfig.getInt("highestAnnouncedRainbowTier." + key, 0);
                    highestAnnouncedRainbowTierByPlayer.put(uuid, Math.max(0, loaded));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
        if (dataConfig.isConfigurationSection("shearShop")) {
            dataConfig.getConfigurationSection("shearShop").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    shearShopLevelByPlayer.put(uuid, dataConfig.getInt("shearShop." + key, 0));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
        if (dataConfig.isConfigurationSection("shearWoolSave")) {
            dataConfig.getConfigurationSection("shearWoolSave").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    shearWoolSaveLevelByPlayer.put(uuid, dataConfig.getInt("shearWoolSave." + key, 0));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
        if (dataConfig.isConfigurationSection("shearTierBoost")) {
            dataConfig.getConfigurationSection("shearTierBoost").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    shearTierBoostLevelByPlayer.put(uuid, dataConfig.getInt("shearTierBoost." + key, 0));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
        if (dataConfig.isConfigurationSection("tutorialCompleted")) {
            dataConfig.getConfigurationSection("tutorialCompleted").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    tutorialCompletedByPlayer.put(uuid, dataConfig.getBoolean("tutorialCompleted." + key, false));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
        if (dataConfig.isConfigurationSection("tutorialBypassed")) {
            dataConfig.getConfigurationSection("tutorialBypassed").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    tutorialBypassedByPlayer.put(uuid, dataConfig.getBoolean("tutorialBypassed." + key, false));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
        if (dataConfig.isConfigurationSection("tutorialShears")) {
            dataConfig.getConfigurationSection("tutorialShears").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    tutorialShearsByPlayer.put(uuid, dataConfig.getInt("tutorialShears." + key, 0));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
        if (dataConfig.isConfigurationSection("tutorialSpawns")) {
            dataConfig.getConfigurationSection("tutorialSpawns").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    tutorialSpawnsByPlayer.put(uuid, dataConfig.getInt("tutorialSpawns." + key, 0));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
        if (dataConfig.isConfigurationSection("tutorialMerges")) {
            dataConfig.getConfigurationSection("tutorialMerges").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    tutorialMergesByPlayer.put(uuid, dataConfig.getInt("tutorialMerges." + key, 0));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
        if (dataConfig.isConfigurationSection("tutorialUpgradeOpened")) {
            dataConfig.getConfigurationSection("tutorialUpgradeOpened").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    tutorialUpgradeOpenedByPlayer.put(uuid,
                            dataConfig.getBoolean("tutorialUpgradeOpened." + key, false));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
        if (dataConfig.isConfigurationSection("tutorialQuestOpened")) {
            dataConfig.getConfigurationSection("tutorialQuestOpened").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    tutorialQuestOpenedByPlayer.put(uuid, dataConfig.getBoolean("tutorialQuestOpened." + key, false));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
        if (dataConfig.isConfigurationSection("tutorialQuestUpgradesOpened")) {
            dataConfig.getConfigurationSection("tutorialQuestUpgradesOpened").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    tutorialQuestUpgradesOpenedByPlayer.put(uuid,
                            dataConfig.getBoolean("tutorialQuestUpgradesOpened." + key, false));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
        if (dataConfig.isConfigurationSection("tutorialPrestigeOpened")) {
            dataConfig.getConfigurationSection("tutorialPrestigeOpened").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    tutorialPrestigeOpenedByPlayer.put(uuid,
                            dataConfig.getBoolean("tutorialPrestigeOpened." + key, false));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
        if (dataConfig.isConfigurationSection("tutorialAbilityUsed")) {
            dataConfig.getConfigurationSection("tutorialAbilityUsed").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    tutorialAbilityUsedByPlayer.put(uuid, dataConfig.getBoolean("tutorialAbilityUsed." + key, false));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
        if (dataConfig.isConfigurationSection("tutorialShearUpgraded")) {
            dataConfig.getConfigurationSection("tutorialShearUpgraded").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    tutorialShearUpgradedByPlayer.put(uuid,
                            dataConfig.getBoolean("tutorialShearUpgraded." + key, false));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
        if (dataConfig.isConfigurationSection("tutorialRegularUpgradesBought")) {
            dataConfig.getConfigurationSection("tutorialRegularUpgradesBought").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    tutorialRegularUpgradesBoughtByPlayer.put(uuid,
                            dataConfig.getBoolean("tutorialRegularUpgradesBought." + key, false));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
        if (dataConfig.isConfigurationSection("tutorialShearTaskRewardGranted")) {
            dataConfig.getConfigurationSection("tutorialShearTaskRewardGranted").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    tutorialShearTaskRewardGrantedByPlayer.put(uuid,
                            dataConfig.getBoolean("tutorialShearTaskRewardGranted." + key, false));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
        if (dataConfig.isConfigurationSection("tutorialPrestigePrepRewardGranted")) {
            dataConfig.getConfigurationSection("tutorialPrestigePrepRewardGranted").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    tutorialPrestigePrepRewardGrantedByPlayer.put(uuid,
                            dataConfig.getBoolean("tutorialPrestigePrepRewardGranted." + key, false));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
        if (dataConfig.isConfigurationSection("tutorialPrestigedOnce")) {
            dataConfig.getConfigurationSection("tutorialPrestigedOnce").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    tutorialPrestigedOnceByPlayer.put(uuid,
                            dataConfig.getBoolean("tutorialPrestigedOnce." + key, false));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
        if (dataConfig.isConfigurationSection("tutorialShearShopOpened")) {
            dataConfig.getConfigurationSection("tutorialShearShopOpened").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    tutorialShearShopOpenedByPlayer.put(uuid,
                            dataConfig.getBoolean("tutorialShearShopOpened." + key, false));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
        if (dataConfig.isConfigurationSection("farmVisitEnabled")) {
            dataConfig.getConfigurationSection("farmVisitEnabled").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    farmVisitEnabledByPlayer.put(uuid, dataConfig.getBoolean("farmVisitEnabled." + key, true));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
        if (dataConfig.isConfigurationSection("questPoints")) {
            dataConfig.getConfigurationSection("questPoints").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    questPointsByPlayer.put(uuid, dataConfig.getInt("questPoints." + key, 0));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
        if (dataConfig.isConfigurationSection("questReset")) {
            dataConfig.getConfigurationSection("questReset").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    nextQuestResetTimestampByPlayer.put(uuid,
                            Math.max(0L, dataConfig.getLong("questReset." + key, 0L)));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
        if (dataConfig.isConfigurationSection("questUpgradeDuration")) {
            dataConfig.getConfigurationSection("questUpgradeDuration").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    questUpgradeDurationByPlayer.put(uuid, dataConfig.getInt("questUpgradeDuration." + key, 0));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
        if (dataConfig.isConfigurationSection("questUpgradePower")) {
            dataConfig.getConfigurationSection("questUpgradePower").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    questUpgradePowerByPlayer.put(uuid, dataConfig.getInt("questUpgradePower." + key, 0));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
        if (dataConfig.isConfigurationSection("activeLuckyBurstUntil")) {
            dataConfig.getConfigurationSection("activeLuckyBurstUntil").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    activeLuckyBurstUntilByPlayer.put(uuid,
                            Math.max(0L, dataConfig.getLong("activeLuckyBurstUntil." + key, 0L)));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
        if (dataConfig.isConfigurationSection("activeWoolRushUntil")) {
            dataConfig.getConfigurationSection("activeWoolRushUntil").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    activeWoolRushUntilByPlayer.put(uuid,
                            Math.max(0L, dataConfig.getLong("activeWoolRushUntil." + key, 0L)));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
        if (dataConfig.isConfigurationSection("activeJackpotShearsUntil")) {
            dataConfig.getConfigurationSection("activeJackpotShearsUntil").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    activeJackpotShearsUntilByPlayer.put(uuid,
                            Math.max(0L, dataConfig.getLong("activeJackpotShearsUntil." + key, 0L)));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
        if (dataConfig.isConfigurationSection("activeAutoMergeUntil")) {
            dataConfig.getConfigurationSection("activeAutoMergeUntil").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    activeAutoMergeUntilByPlayer.put(uuid,
                            Math.max(0L, dataConfig.getLong("activeAutoMergeUntil." + key, 0L)));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
        if (dataConfig.isConfigurationSection("activeAutoShearUntil")) {
            dataConfig.getConfigurationSection("activeAutoShearUntil").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    activeAutoShearUntilByPlayer.put(uuid,
                            Math.max(0L, dataConfig.getLong("activeAutoShearUntil." + key, 0L)));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
        if (dataConfig.isConfigurationSection("pausedLuckyBurstRemaining")) {
            dataConfig.getConfigurationSection("pausedLuckyBurstRemaining").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    pausedLuckyBurstRemainingMsByPlayer.put(uuid,
                            Math.max(0L, dataConfig.getLong("pausedLuckyBurstRemaining." + key, 0L)));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
        if (dataConfig.isConfigurationSection("pausedWoolRushRemaining")) {
            dataConfig.getConfigurationSection("pausedWoolRushRemaining").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    pausedWoolRushRemainingMsByPlayer.put(uuid,
                            Math.max(0L, dataConfig.getLong("pausedWoolRushRemaining." + key, 0L)));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
        if (dataConfig.isConfigurationSection("pausedJackpotShearsRemaining")) {
            dataConfig.getConfigurationSection("pausedJackpotShearsRemaining").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    pausedJackpotShearsRemainingMsByPlayer.put(uuid,
                            Math.max(0L, dataConfig.getLong("pausedJackpotShearsRemaining." + key, 0L)));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
        if (dataConfig.isConfigurationSection("pausedAutoMergeRemaining")) {
            dataConfig.getConfigurationSection("pausedAutoMergeRemaining").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    pausedAutoMergeRemainingMsByPlayer.put(uuid,
                            Math.max(0L, dataConfig.getLong("pausedAutoMergeRemaining." + key, 0L)));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
        if (dataConfig.isConfigurationSection("pausedAutoShearRemaining")) {
            dataConfig.getConfigurationSection("pausedAutoShearRemaining").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    pausedAutoShearRemainingMsByPlayer.put(uuid,
                            Math.max(0L, dataConfig.getLong("pausedAutoShearRemaining." + key, 0L)));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
        if (dataConfig.isConfigurationSection("comboDecayUpgrade")) {
            dataConfig.getConfigurationSection("comboDecayUpgrade").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    comboDecayUpgradeByPlayer.put(uuid,
                            Math.max(0,
                                    Math.min(COMBO_DECAY_MAX_LEVEL, dataConfig.getInt("comboDecayUpgrade." + key, 0))));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
        if (dataConfig.isConfigurationSection("comboMaxUpgrade")) {
            dataConfig.getConfigurationSection("comboMaxUpgrade").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    comboMaxUpgradeByPlayer.put(uuid,
                            Math.max(0, Math.min(COMBO_MAX_MAX_LEVEL, dataConfig.getInt("comboMaxUpgrade." + key, 0))));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
        if (dataConfig.isConfigurationSection("comboGainUpgrade")) {
            dataConfig.getConfigurationSection("comboGainUpgrade").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    comboGainUpgradeByPlayer.put(uuid,
                            Math.max(0,
                                    Math.min(COMBO_GAIN_MAX_LEVEL, dataConfig.getInt("comboGainUpgrade." + key, 0))));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
        if (dataConfig.isConfigurationSection("automationPoints")) {
            dataConfig.getConfigurationSection("automationPoints").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    automationPointsByPlayer.put(uuid, Math.max(0, dataConfig.getInt("automationPoints." + key, 0)));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
        if (dataConfig.isConfigurationSection("automationAutoBuy")) {
            dataConfig.getConfigurationSection("automationAutoBuy").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    automationAutoBuyUpgradeByPlayer.put(uuid,
                            Math.max(0, Math.min(AUTOMATION_AUTO_BUY_MAX_LEVEL,
                                    dataConfig.getInt("automationAutoBuy." + key, 0))));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
        if (dataConfig.isConfigurationSection("automationAutoAbility")) {
            dataConfig.getConfigurationSection("automationAutoAbility").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    automationAutoAbilityUpgradeByPlayer.put(uuid,
                            Math.max(0, Math.min(AUTOMATION_SINGLE_LEVEL_MAX,
                                    dataConfig.getInt("automationAutoAbility." + key, 0))));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
        if (dataConfig.isConfigurationSection("automationSlowAutoMerge")) {
            dataConfig.getConfigurationSection("automationSlowAutoMerge").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    automationSlowAutoMergeUpgradeByPlayer.put(uuid,
                            Math.max(0, Math.min(AUTOMATION_SLOW_AUTO_MERGE_MAX_LEVEL,
                                    dataConfig.getInt("automationSlowAutoMerge." + key, 0))));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
        if (dataConfig.isConfigurationSection("automationSlowAutoShear")) {
            dataConfig.getConfigurationSection("automationSlowAutoShear").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    automationSlowAutoShearUpgradeByPlayer.put(uuid,
                            Math.max(0, Math.min(AUTOMATION_SLOW_AUTO_SHEAR_MAX_LEVEL,
                                    dataConfig.getInt("automationSlowAutoShear." + key, 0))));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
        if (dataConfig.isConfigurationSection("automationAutoSpawn")) {
            dataConfig.getConfigurationSection("automationAutoSpawn").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    automationAutoSpawnUpgradeByPlayer.put(uuid,
                            Math.max(0, Math.min(AUTOMATION_AUTO_SPAWN_MAX_LEVEL,
                                    dataConfig.getInt("automationAutoSpawn." + key, 0))));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
        if (dataConfig.isConfigurationSection("automationAutoPrestige")) {
            dataConfig.getConfigurationSection("automationAutoPrestige").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    automationAutoPrestigeUpgradeByPlayer.put(uuid,
                            Math.max(0, Math.min(AUTOMATION_SINGLE_LEVEL_MAX,
                                    dataConfig.getInt("automationAutoPrestige." + key, 0))));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
        if (dataConfig.isConfigurationSection("automationAutoBuyEnabled")) {
            dataConfig.getConfigurationSection("automationAutoBuyEnabled").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    automationAutoBuyEnabledByPlayer.put(uuid,
                            dataConfig.getBoolean("automationAutoBuyEnabled." + key, false));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
        if (dataConfig.isConfigurationSection("automationAutoAbilityEnabled")) {
            dataConfig.getConfigurationSection("automationAutoAbilityEnabled").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    automationAutoAbilityEnabledByPlayer.put(uuid,
                            dataConfig.getBoolean("automationAutoAbilityEnabled." + key, false));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
        if (dataConfig.isConfigurationSection("automationSlowAutoMergeEnabled")) {
            dataConfig.getConfigurationSection("automationSlowAutoMergeEnabled").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    automationSlowAutoMergeEnabledByPlayer.put(uuid,
                            dataConfig.getBoolean("automationSlowAutoMergeEnabled." + key, false));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
        if (dataConfig.isConfigurationSection("automationSlowAutoShearEnabled")) {
            dataConfig.getConfigurationSection("automationSlowAutoShearEnabled").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    automationSlowAutoShearEnabledByPlayer.put(uuid,
                            dataConfig.getBoolean("automationSlowAutoShearEnabled." + key, false));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
        if (dataConfig.isConfigurationSection("automationAutoSpawnEnabled")) {
            dataConfig.getConfigurationSection("automationAutoSpawnEnabled").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    automationAutoSpawnEnabledByPlayer.put(uuid,
                            dataConfig.getBoolean("automationAutoSpawnEnabled." + key, false));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
        if (dataConfig.isConfigurationSection("automationAutoPrestigeEnabled")) {
            dataConfig.getConfigurationSection("automationAutoPrestigeEnabled").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    automationAutoPrestigeEnabledByPlayer.put(uuid,
                            dataConfig.getBoolean("automationAutoPrestigeEnabled." + key, false));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
        if (dataConfig.isConfigurationSection("scoreboardLayoutMode")) {
            dataConfig.getConfigurationSection("scoreboardLayoutMode").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    int layoutMode = dataConfig.getInt("scoreboardLayoutMode." + key, 0);
                    scoreboardLayoutModeByPlayer.put(uuid, Math.max(0, Math.min(1, layoutMode)));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
        if (dataConfig.isConfigurationSection("scoreboardShowQuestPoints")) {
            dataConfig.getConfigurationSection("scoreboardShowQuestPoints").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    scoreboardShowQuestPointsByPlayer.put(uuid,
                            dataConfig.getBoolean("scoreboardShowQuestPoints." + key, true));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
        if (dataConfig.isConfigurationSection("scoreboardShowAutomationPoints")) {
            dataConfig.getConfigurationSection("scoreboardShowAutomationPoints").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    scoreboardShowAutomationPointsByPlayer.put(uuid,
                            dataConfig.getBoolean("scoreboardShowAutomationPoints." + key, true));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
        if (dataConfig.isConfigurationSection("scoreboardShowSacrificePoints")) {
            dataConfig.getConfigurationSection("scoreboardShowSacrificePoints").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    scoreboardShowSacrificePointsByPlayer.put(uuid,
                            dataConfig.getBoolean("scoreboardShowSacrificePoints." + key, true));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
        if (dataConfig.isConfigurationSection("scoreboardShowQuestProgress")) {
            dataConfig.getConfigurationSection("scoreboardShowQuestProgress").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    scoreboardShowQuestProgressByPlayer.put(uuid,
                            dataConfig.getBoolean("scoreboardShowQuestProgress." + key, true));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
        if (dataConfig.isConfigurationSection("scoreboardShowAbilityStatus")) {
            dataConfig.getConfigurationSection("scoreboardShowAbilityStatus").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    scoreboardShowAbilityStatusByPlayer.put(uuid,
                            dataConfig.getBoolean("scoreboardShowAbilityStatus." + key, true));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
        if (dataConfig.isConfigurationSection("sacrificePoints")) {
            dataConfig.getConfigurationSection("sacrificePoints").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    String path = "sacrificePoints." + key;
                    String raw = dataConfig.getString(path, null);
                    BigInteger parsed;
                    if (raw != null && !raw.isBlank()) {
                        parsed = new BigInteger(raw.trim());
                    } else {
                        parsed = BigInteger.valueOf(Math.max(0L, dataConfig.getLong(path, 0L)));
                    }
                    sacrificePointsByPlayer.put(uuid, parsed.max(BigInteger.ZERO));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
        if (dataConfig.isConfigurationSection("sacrificeUnlocksBought")) {
            dataConfig.getConfigurationSection("sacrificeUnlocksBought").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    int loaded = dataConfig.getInt("sacrificeUnlocksBought." + key, 0);
                    sacrificeUnlocksBoughtByPlayer.put(uuid,
                            Math.max(0, Math.min(SACRIFICE_UNLOCK_MAX, loaded)));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
        if (dataConfig.isConfigurationSection("pendingInventory")) {
            dataConfig.getConfigurationSection("pendingInventory").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    String basePath = "pendingInventory." + key;
                    ItemStack[] contents = InventoryDataUtils
                            .deserializeInventoryList(dataConfig.getList(basePath + ".contents"));
                    ItemStack[] armor = InventoryDataUtils
                            .deserializeInventoryList(dataConfig.getList(basePath + ".armor"));
                    ItemStack offhand = dataConfig.getItemStack(basePath + ".offhand");
                    savedInventories.put(uuid,
                            new InventoryDataUtils.Snapshot(contents, armor, offhand == null ? null : offhand.clone()));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
    }

    public static void storePickedUpSheep(Player player, Sheep sheep) {
        if (player == null || sheep == null) {
            return;
        }
        sheep.setAI(false);
        sheep.setGravity(false);
        sheep.setInvulnerable(true);
        sheep.setVelocity(new Vector(0.0D, 0.0D, 0.0D));
        carriedSheepByPlayer.put(player.getUniqueId(), sheep);
        updateCarriedSheepPosition(player);
    }

    public static boolean hasPickedUpSheep(Player player) {
        return player != null && carriedSheepByPlayer.containsKey(player.getUniqueId());
    }

    public static Sheep getPickedUpSheep(Player player) {
        if (player == null) {
            return null;
        }
        Sheep sheep = carriedSheepByPlayer.get(player.getUniqueId());
        if (sheep != null && !sheep.isValid()) {
            carriedSheepByPlayer.remove(player.getUniqueId());
            return null;
        }
        return sheep;
    }

    public static boolean dropPickedUpSheep(Player player) {
        if (player == null) {
            return false;
        }
        Sheep sheep = getPickedUpSheep(player);
        if (sheep == null) {
            return false;
        }
        sheep.setGravity(true);
        sheep.setAI(true);
        sheep.setInvulnerable(false);
        Vector forward = player.getLocation().getDirection().normalize();
        Location dropLocation = player.getLocation().clone().add(forward.clone().multiply(1.6D)).add(0, 0.2D, 0);
        if (!isDropSpacePassable(dropLocation)) {
            dropLocation = player.getLocation().clone().add(0, 0.2D, 0);
        }
        if (!isDropSpacePassable(dropLocation)) {
            dropLocation = player.getLocation().clone().add(0, 1.0D, 0);
        }

        sheep.teleport(dropLocation);
        sheep.setVelocity(forward.multiply(0.2D).setY(0.15D));
        carriedSheepByPlayer.remove(player.getUniqueId());
        return true;
    }

    public static void updateCarriedSheepPosition(Player player) {
        if (player == null) {
            return;
        }
        Sheep sheep = getPickedUpSheep(player);
        if (sheep == null || sheep.getWorld() == null || !sheep.getWorld().equals(player.getWorld())) {
            return;
        }

        Location carryLocation = player.getLocation().clone().add(0.0D, 2.15D, 0.0D);
        carryLocation.setYaw(player.getLocation().getYaw());
        carryLocation.setPitch(0.0F);
        sheep.teleport(carryLocation);
        sheep.setVelocity(new Vector(0.0D, 0.0D, 0.0D));
        sheep.setFallDistance(0.0F);
    }

    private static boolean isDropSpacePassable(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        return location.getBlock().isPassable()
                && location.clone().add(0, 1, 0).getBlock().isPassable();
    }

    public static void clearPickedUpSheep(Player player) {
        if (player == null) {
            return;
        }
        Sheep sheep = carriedSheepByPlayer.remove(player.getUniqueId());
        if (sheep != null && sheep.isValid()) {
            sheep.setGravity(true);
            sheep.setAI(true);
            sheep.setInvulnerable(false);
        }
    }

    private static UUID getOwnerId(World world) {
        if (world == null) {
            return null;
        }

        Matcher matcher = OWNER_ID_PATTERN.matcher(world.getName());
        if (!matcher.matches()) {
            matcher = TUTORIAL_OWNER_ID_PATTERN.matcher(world.getName());
            if (!matcher.matches()) {
                return null;
            }
        }

        String raw = matcher.group(1);
        if (raw.length() != 32) {
            return null;
        }

        String formatted = raw.replaceFirst(
                "([0-9a-fA-F]{8})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{12})", "$1-$2-$3-$4-$5");
        return UUID.fromString(formatted);
    }

    public static int getGrassEatDelayChance(SheepTier tier) {
        if (tier == null || tier.getLevel() <= 0) {
            return 0;
        }
        return Math.min(100, tier.getLevel() * 10);
    }

    public static boolean shouldDelayGrassEat(SheepTier tier) {
        return tier != null && tier.getLevel() > 0 && RANDOM.nextInt(100) < getGrassEatDelayChance(tier);
    }
}
