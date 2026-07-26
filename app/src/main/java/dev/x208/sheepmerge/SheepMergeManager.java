package dev.x208.sheepmerge;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Fence;
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
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public final class SheepMergeManager {

    private static final Object TOP_POINTS_REFRESH_LOCK = new Object();
    private static long topPointsRefreshVersion = 0L;
    private static final Map<UUID, List<SheepSnapshot>> savedFarmSheepByPlayer = new HashMap<>();
    private static final Map<UUID, List<SheepSnapshot>> savedTutorialSheepByPlayer = new HashMap<>();
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
    private static final int SACRIFICE_UNLOCK_MAX_SHEEP_BONUS = 50;
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
    private static final double PRESTIGE_QUEST_REWARD_BONUS_PER_LEVEL = 0.25D;
    private static final long PRESTIGE_REFUND_COOLDOWN_MS = 30L * 60L * 1000L;
    private static final long REBIRTH_RESPEC_COOLDOWN_MS = 30L * 60L * 1000L;
    private static final int REGULAR_POINTS_UPGRADE_COST_MULTIPLIER = 6;
    private static final String MILESTONE_ITEM_NAME_RAW_COPPER = "Raw Copper";
    private static final String MILESTONE_ITEM_NAME_RAW_COPPER_BLOCK = "Raw Copper Block";
    private static final String MILESTONE_ITEM_NAME_COPPER_NUGGET = "Copper Nugget";
    private static final String MILESTONE_ITEM_NAME_COPPER_INGOT = "Copper Ingot";
    private static final String MILESTONE_ITEM_NAME_COPPER_BLOCK = "Copper Block";
    private static final String FARM_BUILD_WORLD_NAME = "sheepfarm_build";
    private static final double FARM_CENTER_X = 0.5D;
    private static final double FARM_CENTER_Z = 0.5D;
    private static final int FARM_WORLD_RADIUS_CHUNKS = 5;
    private static final int FARM_WORLD_RADIUS_BLOCKS = FARM_WORLD_RADIUS_CHUNKS * 16;
    private static final int FARM_LAYOUT_SAVE_CHUNK_SPAN = 4;
    private static final int FARM_LAYOUT_SAVE_CHUNK_HALF_SPAN = FARM_LAYOUT_SAVE_CHUNK_SPAN / 2;
    private static final int FARM_MIN_XZ = -5;
    private static final int FARM_MAX_XZ = 6;
    private static final int FARM_RADIUS = Math.max(Math.abs(FARM_MIN_XZ), Math.abs(FARM_MAX_XZ));
    private static final int FARM_BASE_Y = 100;
    private static final int FARM_MIN_Y = FARM_BASE_Y - 1;
    private static final int FARM_MAX_Y = FARM_BASE_Y + 4;
    private static final int FARM_LAYOUT_BLOCKS_PER_TICK = 2400;
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
    private static final double PLAYER_FALL_RECOVERY_MARGIN_ABOVE_VOID = 2.0D;
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
    private static final long SCOREBOARD_UPDATE_INTERVAL_MS = 1000L;
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
    private static final int AUTOMATION_AUTO_ABILITY_MAX_LEVEL = 3;
    private static final int AUTOMATION_AUTO_BUY_MAX_LEVEL = 5;
    private static final int AUTOMATION_SLOW_AUTO_MERGE_MAX_LEVEL = 3;
    private static final int AUTOMATION_SLOW_AUTO_SHEAR_MAX_LEVEL = 3;
    private static final int AUTOMATION_AUTO_SPAWN_MAX_LEVEL = 10;
    private static int AUTOMATION_AUTO_PRESTIGE_BASE_COST = 64;
    private static long AUTOMATION_POINT_INTERVAL_MS = 60_000L;
    private static long AUTOMATION_AUTO_BUY_INTERVAL_MS = 5_000L;
    private static final int AUTOMATION_AUTO_BUY_MAX_PURCHASES_PER_TICK = 512;
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
    private static final long TIER_BOOST_SOUND_COOLDOWN_MS = 175L;
    private static long STARTING_PLAYER_POINTS = 1_000L;
    private static int TUTORIAL_SHEAR_TARGET = 3;
    private static int TUTORIAL_SPAWN_TARGET = 3;
    private static int TUTORIAL_MERGE_TARGET = 1;
    private static int TUTORIAL_MENU_SECTION_TARGET = 7;
    private static int PRESTIGE_LEVEL_BASE_COST = 500;
    public static final String UPGRADE_MENU_TITLE = "Sheep Merge Menu";
    public static final String PRESTIGE_MENU_TITLE = "Prestige Upgrades";
    public static final String QUEST_MENU_TITLE = "Quest Abilities";
    public static final String QUEST_UPGRADES_MENU_TITLE = "Quest Upgrades";
    public static final String SHOP_MENU_TITLE = "Shear Shop";
    public static final String COMBO_SHOP_MENU_TITLE = "Combo Upgrades";
    public static final String AUTOMATION_MENU_TITLE = "Automation Upgrades";
    public static final String ACHIEVEMENTS_MENU_TITLE = "Achievements";
    public static final String ACHIEVEMENTS_VIEW_MENU_TITLE = "Achievement List";
    public static final String ACHIEVEMENTS_UPGRADES_MENU_TITLE = "Achievement Milestones";
    public static final String SACRIFICE_MENU_TITLE = "Sacrifice Unlocks";
    public static final String REBIRTH_MENU_TITLE = "Rebirth Upgrades";
    public static final String REBIRTH_TREE_MENU_TITLE = "Rebirth Skill Tree";
    public static final String SCOREBOARD_MENU_TITLE = "Scoreboard Settings";
    public static final int CURRENT_DATA_SCHEMA_VERSION = 2;
    public static final String SETTINGS_MENU_TITLE = "Settings";
    public static final String UNIVERSAL_LAYOUT_MENU_TITLE = SETTINGS_MENU_TITLE;
    public static final String SCOREBOARD_LAYOUT_MENU_TITLE = "Scoreboard Layout";
    public static final String INVENTORY_LAYOUT_MENU_TITLE = "Inventory Layout";
    public static final String SOUND_EFFECTS_MENU_TITLE = "Sound Effects";
    public static final int SHEEP_SOUNDS_TOGGLE_SLOT = 15;
    public static final String PARTICLE_EFFECTS_MENU_TITLE = "Particle Effects";
    public static final String VISIT_ACCESS_MENU_TITLE = "Visit Access";
    public static final String SOCIALS_MENU_TITLE = "Socials";
    public static final int LIMIT_UPGRADE_SLOT = 10;
    public static final int EGG_SPEED_UPGRADE_SLOT = 12;
    public static final int WOOL_REGEN_UPGRADE_SLOT = 14;
    public static final int HIGHER_TIER_CHANCE_UPGRADE_SLOT = 16;
    public static final int PRESTIGE_MENU_OPEN_SLOT = 22;
    public static final int QUEST_MENU_OPEN_SLOT = 20;
    public static final int SHOP_MENU_OPEN_SLOT = 24;
    public static final int COMBO_MENU_OPEN_SLOT = 18;
    public static final int AUTOMATION_MENU_OPEN_SLOT = 26;
    public static final int ACHIEVEMENTS_MENU_OPEN_SLOT = 8;
    public static final int SACRIFICE_MENU_OPEN_SLOT = 2;
    public static final int REBIRTH_MENU_OPEN_SLOT = 6;
    public static final int LAYOUTS_MENU_OPEN_SLOT = 0;
    public static final int SOCIALS_MENU_OPEN_SLOT = 4;
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
    public static final int SACRIFICE_BACK_TO_UPGRADES_SLOT = 26;
    public static final int REBIRTH_PROGRESS_SLOT = 4;
    public static final int REBIRTH_ACTION_SLOT = 11;
    public static final int REBIRTH_OPEN_TREE_SLOT = 15;
    public static final int REBIRTH_BACK_TO_UPGRADES_SLOT = 26;
    public static final int REBIRTH_TREE_RESPEC_SLOT = 45;
    public static final int REBIRTH_TREE_BACK_SLOT = 53;
    public static final int SCOREBOARD_LAYOUT_SLOT = 24;
    public static final int SCOREBOARD_QUEST_POINTS_SLOT = 10;
    public static final int SCOREBOARD_AUTOMATION_POINTS_SLOT = 12;
    public static final int SCOREBOARD_SACRIFICE_POINTS_SLOT = 14;
    public static final int SCOREBOARD_PRESTIGE_STATS_SLOT = 16;
    public static final int SCOREBOARD_QUEST_PROGRESS_SLOT = 18;
    public static final int SCOREBOARD_ABILITIES_SLOT = 20;
    public static final int SCOREBOARD_ACHIEVEMENT_POINTS_SLOT = 22;
    public static final int SCOREBOARD_BACK_SLOT = 26;
    public static final int UNIVERSAL_LAYOUT_INVENTORY_SLOT = 10;
    public static final int UNIVERSAL_LAYOUT_SCOREBOARD_SLOT = 12;
    public static final int UNIVERSAL_LAYOUT_VISIT_SLOT = 14;
    public static final int UNIVERSAL_LAYOUT_SOUND_SLOT = 16;
    public static final int UNIVERSAL_LAYOUT_PARTICLE_SLOT = 18;
    public static final int UNIVERSAL_LAYOUT_BACK_SLOT = 22;
    public static final int SCOREBOARD_LAYOUT_DETAILED_SLOT = 11;
    public static final int SCOREBOARD_LAYOUT_COMPACT_SLOT = 15;
    public static final int SCOREBOARD_LAYOUT_BACK_SLOT = 26;
    public static final int SOUND_EFFECTS_TOGGLE_SLOT = 11;
    public static final int SOUND_EFFECTS_BACK_SLOT = 26;
    public static final int PARTICLE_EFFECTS_TOGGLE_SLOT = 11;
    public static final int PARTICLE_EFFECTS_BACK_SLOT = 26;
    public static final int VISIT_ACCESS_TOGGLE_SLOT = 4;
    public static final int VISIT_ACCESS_SUMMARY_SLOT = 8;
    public static final int VISIT_ACCESS_PREVIOUS_PAGE_SLOT = 45;
    public static final int VISIT_ACCESS_NEXT_PAGE_SLOT = 46;
    public static final int VISIT_ACCESS_BACK_SLOT = 53;
    public static final int INVENTORY_LAYOUT_SELECTED_SLOT = 4;
    public static final int INVENTORY_LAYOUT_CASTING_TOGGLE_SLOT = 6;
    public static final int INVENTORY_LAYOUT_BACK_SLOT = 49;
    public static final int SOCIALS_TOP_POINTS_SLOT = 4;
    public static final int SOCIALS_PREVIOUS_PAGE_SLOT = 45;
    public static final int SOCIALS_NEXT_PAGE_SLOT = 46;
    public static final int SOCIALS_RETURN_HOME_SLOT = 49;
    public static final int SOCIALS_BACK_SLOT = 53;
    public static final int ACHIEVEMENTS_VIEW_SLOT = 11;
    public static final int ACHIEVEMENTS_UPGRADES_SLOT = 15;
    public static final int ACHIEVEMENTS_BACK_SLOT = 26;
    public static final int ACHIEVEMENTS_VIEW_BACK_SLOT = 49;
    public static final int ACHIEVEMENTS_UPGRADES_BACK_SLOT = 49;

    private static final int SACRIFICE_UNLOCK_NO_REGULAR_RESETS = 1;
    private static final int SACRIFICE_UNLOCK_NO_COMBO_RESETS = 2;
    private static final int SACRIFICE_UNLOCK_NO_SHEAR_RESETS = 3;
    private static final int SACRIFICE_UNLOCK_EGG_COOLDOWN_TO_1S = 4;
    private static final int SACRIFICE_UNLOCK_MAX_SHEEP_100 = 5;
    static final int SACRIFICE_UNLOCK_MAX = SACRIFICE_UNLOCK_MAX_SHEEP_100;
    private static final int REBIRTH_PRESTIGE_LEVEL_COST_STEP = 10;
    private static final int REBIRTH_SKILL_ROOT_COST = 1;
    private static final int REBIRTH_SKILL_POINTS_X10_ROOT = 1;
    private static final int REBIRTH_SKILL_POINTS_X10_LEFT = 2;
    private static final int REBIRTH_SKILL_QUEST_POINTS_X10 = 3;
    private static final int REBIRTH_SKILL_SACRIFICE_POINTS_X10 = 4;
    private static final int REBIRTH_SKILL_KEEP_POINTS_AFTER_PRESTIGE = 5;
    private static final int REBIRTH_SKILL_KEEP_SACRIFICE_AFTER_REBIRTH = 6;
    private static final int REBIRTH_SKILL_KEEP_SHEEP_AFTER_PRESTIGE = 7;
    private static final int REBIRTH_SKILL_WOOL_REGEN_X10 = 8;
    private static final int REBIRTH_SKILL_QUEST_MASTER = 9;
    private static final int FARM_SHEARS_ITEM_SLOT = 6;
    private static final int FARM_EGG_ITEM_SLOT = 7;
    private static final int INVENTORY_QUICK_ACCESS_MAX_ITEMS = 6;
    private static final int INVENTORY_QUICK_ACCESS_FIRST_SLOT = 0;
    private static final int INVENTORY_QUICK_ACCESS_LAST_SLOT = INVENTORY_QUICK_ACCESS_FIRST_SLOT
            + INVENTORY_QUICK_ACCESS_MAX_ITEMS - 1;
    private static final UUID SOCIALS_AUTHOR_UUID = UUID.fromString("27268675-a9b7-4abd-9628-e6c4515a5cf6");
    private static final int SECRET_AUTHOR_ONLINE_SLOT = 2;
    private static final int SECRET_OWNER_FARM_SLOT = 6;
    private static final int SOCIALS_VISIT_PAGE_SIZE = 31;

    public static boolean isAuthor(Player player) {
        return player != null && SOCIALS_AUTHOR_UUID.equals(player.getUniqueId());
    }

    private static SheepMergePlugin plugin;
    private static final SheepEggModule EGG_MODULE = new SheepEggModule();
    private static FileConfiguration dataConfig;
    private static File dataFile;
    private static FileConfiguration farmLayoutConfig;
    private static File farmLayoutFile;
    private static File farmStructureCacheDirectory;
    private static final Object FARM_STRUCTURE_CACHE_REFRESH_LOCK = new Object();
    private static long lastFarmStructureCacheRefreshAtMs = 0L;
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

    private static final class ChunkApplyCursor {
        private final String chunkPath;
        private final int chunkX;
        private final int chunkZ;
        private final int minY;
        private final int maxY;
        private final List<BlockData> paletteData;
        private final String[] tokens;
        private final boolean legacyFormat;
        private int blockIndex;
        private int tokenIndex;
        private int runRemaining;
        private int activePaletteIndex = -1;

        private ChunkApplyCursor(String chunkPath, int chunkX, int chunkZ, int minY, int maxY,
                List<BlockData> paletteData, String[] tokens, boolean legacyFormat) {
            this.chunkPath = chunkPath;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.minY = minY;
            this.maxY = maxY;
            this.paletteData = paletteData;
            this.tokens = tokens;
            this.legacyFormat = legacyFormat;
        }

        private int totalBlocks() {
            return Math.max(0, (maxY - minY) * 16 * 16);
        }

        private boolean isComplete() {
            return blockIndex >= totalBlocks();
        }

        private BlockData nextBlockData() {
            if (legacyFormat) {
                return null;
            }
            while (runRemaining <= 0) {
                if (tokens == null || tokenIndex >= tokens.length) {
                    return Bukkit.createBlockData(Material.AIR);
                }
                String token = tokens[tokenIndex++];
                if (token == null || token.isBlank()) {
                    continue;
                }
                String[] runParts = token.split("\\*", 2);
                int paletteIndex = parseChunkEncodedNumber(runParts[0], -1);
                int runLengthRaw = runParts.length >= 2 ? parseChunkEncodedNumber(runParts[1], 1) : 1;
                if (paletteIndex < 0 || paletteData == null || paletteIndex >= paletteData.size()
                        || runLengthRaw <= 0) {
                    continue;
                }
                activePaletteIndex = paletteIndex;
                runRemaining = runLengthRaw;
            }
            runRemaining--;
            if (activePaletteIndex < 0 || paletteData == null || activePaletteIndex >= paletteData.size()) {
                return Bukkit.createBlockData(Material.AIR);
            }
            return paletteData.get(activePaletteIndex);
        }
    }

    private static final class RebirthSkillNode {
        private final int id;
        private final int parentId;
        private final int layer;
        private final int slot;
        private final Material material;
        private final String name;
        private final String effectLine;

        private RebirthSkillNode(int id, int parentId, int layer, int slot, Material material, String name,
                String effectLine) {
            this.id = id;
            this.parentId = parentId;
            this.layer = layer;
            this.slot = slot;
            this.material = material;
            this.name = name;
            this.effectLine = effectLine;
        }
    }

    private enum AchievementMilestoneRewardType {
        POINTS,
        WOOL_REGEN
    }

    private static final class AchievementDefinition {
        private final String id;
        private final Material material;
        private final String name;
        private final String objective;
        private final String reward;
        private final int achievementPoints;

        private AchievementDefinition(String id, Material material, String name, String objective, String reward,
                int achievementPoints) {
            this.id = id;
            this.material = material;
            this.name = name;
            this.objective = objective;
            this.reward = reward;
            this.achievementPoints = Math.max(0, achievementPoints);
        }
    }

    private static final class AchievementMilestoneDefinition {
        private final String id;
        private final int requiredPoints;
        private final Material material;
        private final String name;
        private final AchievementMilestoneRewardType rewardType;
        private final int rewardMultiplier;
        private final String reward;

        private AchievementMilestoneDefinition(String id, int requiredPoints, Material material, String name,
                AchievementMilestoneRewardType rewardType, int rewardMultiplier) {
            this.id = id;
            this.requiredPoints = Math.max(0, requiredPoints);
            this.material = material;
            this.name = name;
            this.rewardType = rewardType;
            this.rewardMultiplier = Math.max(1, rewardMultiplier);
            this.reward = switch (this.rewardType) {
                case POINTS -> "Bonus: x" + this.rewardMultiplier + " Coins";
                case WOOL_REGEN -> "Bonus: x" + this.rewardMultiplier + " wool regen speed";
            };
        }
    }

    private static final class QuickAccessDefinition {
        private final String id;
        private final Material material;
        private final String name;
        private final String description;

        private QuickAccessDefinition(String id, Material material, String name, String description) {
            this.id = id;
            this.material = material;
            this.name = name;
            this.description = description;
        }
    }

    private static final List<AchievementDefinition> ACHIEVEMENT_DEFINITIONS = List.of(
            new AchievementDefinition("tutorial_mastery", Material.TARGET, "Tutorial Mastery",
                    "Complete the tutorial", "Reward: +4 achievement points", 4),
            new AchievementDefinition("first_hatch", Material.SHEEP_SPAWN_EGG, "First Hatch",
                    "Spawn at least 10 sheep", "Reward: +2 achievement points", 2),
            new AchievementDefinition("first_shear", Material.SHEARS, "First Cut",
                    "Shear at least 10 sheep", "Reward: +2 achievement points", 2),
            new AchievementDefinition("pair_maker", Material.ANVIL, "Pair Maker",
                    "Merge at least 250 sheep pairs", "Reward: +3 achievement points", 3),
            new AchievementDefinition("socials_explorer", Material.PLAYER_HEAD, "Socials Explorer",
                    "Visit another player's farm at least once", "Reward: +5 achievement points", 5),
            new AchievementDefinition("breeder", Material.CHICKEN_SPAWN_EGG, "Breeder",
                    "Spawn at least 2,000 sheep", "Reward: +6 achievement points", 6),
            new AchievementDefinition("wool_tycoon", Material.WHITE_WOOL, "Wool Tycoon",
                    "Shear at least 1,000 sheep", "Reward: +4 achievement points", 4),
            new AchievementDefinition("fusion_engine", Material.BLAST_FURNACE, "Fusion Engine",
                    "Merge at least 2,500 sheep pairs", "Reward: +7 achievement points", 7),
            new AchievementDefinition("quest_cadet", Material.BOOK, "Quest Cadet",
                    "Complete at least 3 full quest cycles", "Reward: +3 achievement points", 3),
            new AchievementDefinition("quest_veteran", Material.WRITABLE_BOOK, "Quest Veteran",
                    "Complete at least 15 full quest cycles", "Reward: +6 achievement points", 6),
            new AchievementDefinition("upgrade_mechanic", Material.CRAFTING_TABLE, "Upgrade Mechanic",
                    "Reach 20 total regular levels (Limit, Egg Speed, Wool Regen, Tier Chance)",
                    "Reward: +3 achievement points", 3),
            new AchievementDefinition("shear_specialist", Material.SHEARS, "Shear Specialist",
                    "Reach Shear Shop value level 15", "Reward: +4 achievement points", 4),
            new AchievementDefinition("combo_champion", Material.BLAZE_POWDER, "Combo Champion",
                    "Reach Combo Max upgrade level 15", "Reward: +5 achievement points", 5),
            new AchievementDefinition("quest_engineer", Material.CLOCK, "Quest Engineer",
                    "Reach level 10 in both quest upgrades (Duration and Power)", "Reward: +5 achievement points",
                    5),
            new AchievementDefinition("prestige_initiate", Material.NETHER_STAR, "Prestige Initiate",
                    "Earn at least 3 total prestige levels", "Reward: +4 achievement points", 4),
            new AchievementDefinition("egg_cap_collector", Material.EGG, "Egg Cap Collector",
                    "Reach prestige Egg Cap level 10", "Reward: +6 achievement points", 6),
            new AchievementDefinition("spawn_architect", Material.SPAWNER, "Spawn Architect",
                    "Reach Base Spawn Tier level 8", "Reward: +6 achievement points", 6),
            new AchievementDefinition("prestige_planner", Material.NETHER_STAR, "Prestige Planner",
                    "Reach prestige Quest Reward level 18", "Reward: +7 achievement points", 7),
            new AchievementDefinition("prestige_veteran", Material.BEACON, "Prestige Veteran",
                    "Earn at least 100 total prestige levels", "Reward: +7 achievement points", 7),
            new AchievementDefinition("automation_online", Material.REDSTONE, "Automation Online",
                    "Unlock at least 2 automation tracks", "Reward: +4 achievement points", 4),
            new AchievementDefinition("automation_specialist", Material.REPEATER, "Automation Specialist",
                    "Unlock at least 5 automation tracks", "Reward: +7 achievement points", 7),
            new AchievementDefinition("automation_matrix", Material.COMPARATOR, "Automation Matrix",
                    "Unlock all 6 automation tracks", "Reward: +8 achievement points", 8),
            new AchievementDefinition("sacrifice_initiate", Material.TOTEM_OF_UNDYING, "Sacrifice Initiate",
                    "Buy at least 2 sacrifice unlocks", "Reward: +4 achievement points", 4),
            new AchievementDefinition("sacrifice_mastery", Material.NETHERITE_INGOT, "Sacrifice Mastery",
                    "Buy at least 5 sacrifice unlocks", "Reward: +6 achievement points", 6),
            new AchievementDefinition("reborn", Material.DRAGON_EGG, "Reborn",
                    "Reach rebirth level 3", "Reward: +6 achievement points", 6),
            new AchievementDefinition("rebirth_architect", Material.DRAGON_HEAD, "Rebirth Architect",
                    "Reach rebirth level 10", "Reward: +10 achievement points", 10),
            new AchievementDefinition("rainbow_ascension", Material.PRISMARINE_CRYSTALS, "Rainbow Ascension",
                    "Reach Rainbow tier T4 or higher", "Reward: +6 achievement points", 6),
            new AchievementDefinition("layout_designer", Material.ENDER_CHEST, "Layout Designer",
                    "Set scoreboard to Compact, fill all quick access slots, and open Socials",
                    "Reward: +5 achievement points", 5),
            new AchievementDefinition("quick_access_curator", Material.COMPASS, "Quick Access Curator",
                    "Fill all quick access slots and enable quick-access casting", "Reward: +6 achievement points", 6),
            new AchievementDefinition("sheep_limit_master", Material.OAK_FENCE, "Sheep Limit Master",
                    "Reach your current maximum sheep limit", "Reward: +8 achievement points", 8),
            new AchievementDefinition("wool_guardian", Material.SHIELD, "Wool Guardian",
                    "Reach your current Wool Regen max level", "Reward: +11 achievement points", 11),
            new AchievementDefinition("secret_owner_farm", Material.COMMAND_BLOCK, "Secret: Owner Visitor",
                    "Secret objective", "Reward: +12 achievement points", 12),
            new AchievementDefinition("secret_author_online", Material.DRAGON_BREATH, "Secret: Shared Session",
                    "Secret objective", "Reward: +12 achievement points", 12));

    private static final int ACHIEVEMENT_MILESTONE_COUNT = 26;
    private static final List<AchievementMilestoneDefinition> ACHIEVEMENT_MILESTONE_DEFINITIONS = createAchievementMilestones(
            getAchievementPointPool());

    private static List<AchievementMilestoneDefinition> createAchievementMilestones(int totalAchievementPoints) {
        int total = Math.max(0, totalAchievementPoints);
        return List.of(
                new AchievementMilestoneDefinition("points_1", getAchievementMilestoneTarget(total, 1),
                        Material.COAL, "Coal", AchievementMilestoneRewardType.POINTS, 2),
                new AchievementMilestoneDefinition("points_2", getAchievementMilestoneTarget(total, 2),
                        Material.COAL_BLOCK, "Coal Block", AchievementMilestoneRewardType.WOOL_REGEN, 2),
                new AchievementMilestoneDefinition("points_3", getAchievementMilestoneTarget(total, 3),
                        resolveMaterial("COPPER_NUGGET", Material.GOLD_NUGGET), MILESTONE_ITEM_NAME_COPPER_NUGGET,
                        AchievementMilestoneRewardType.POINTS, 2),
                new AchievementMilestoneDefinition("points_4", getAchievementMilestoneTarget(total, 4),
                        Material.COPPER_INGOT, MILESTONE_ITEM_NAME_COPPER_INGOT,
                        AchievementMilestoneRewardType.WOOL_REGEN, 2),
                new AchievementMilestoneDefinition("points_5", getAchievementMilestoneTarget(total, 5),
                        Material.COPPER_BLOCK, MILESTONE_ITEM_NAME_COPPER_BLOCK,
                        AchievementMilestoneRewardType.POINTS, 2),
                new AchievementMilestoneDefinition("points_6", getAchievementMilestoneTarget(total, 6),
                        Material.IRON_NUGGET, "Iron Nugget",
                        AchievementMilestoneRewardType.WOOL_REGEN, 2),
                new AchievementMilestoneDefinition("points_7", getAchievementMilestoneTarget(total, 7),
                        Material.IRON_INGOT, "Iron Ingot",
                        AchievementMilestoneRewardType.POINTS, 2),
                new AchievementMilestoneDefinition("points_8", getAchievementMilestoneTarget(total, 8),
                        Material.IRON_BLOCK, "Iron Block", AchievementMilestoneRewardType.WOOL_REGEN, 2),
                new AchievementMilestoneDefinition("points_9", getAchievementMilestoneTarget(total, 9),
                        Material.LAPIS_LAZULI, "Lapis Lazuli", AchievementMilestoneRewardType.POINTS, 2),
                new AchievementMilestoneDefinition("points_10", getAchievementMilestoneTarget(total, 10),
                        Material.LAPIS_BLOCK, "Lapis Block", AchievementMilestoneRewardType.WOOL_REGEN, 2),
                new AchievementMilestoneDefinition("points_11", getAchievementMilestoneTarget(total, 11),
                        Material.REDSTONE, "Redstone", AchievementMilestoneRewardType.POINTS, 2),
                new AchievementMilestoneDefinition("points_12", getAchievementMilestoneTarget(total, 12),
                        Material.REDSTONE_BLOCK, "Redstone Block", AchievementMilestoneRewardType.WOOL_REGEN, 2),
                new AchievementMilestoneDefinition("points_13", getAchievementMilestoneTarget(total, 13),
                        Material.GOLD_NUGGET, "Gold Nugget", AchievementMilestoneRewardType.POINTS, 2),
                new AchievementMilestoneDefinition("points_14", getAchievementMilestoneTarget(total, 14),
                        Material.GOLD_INGOT, "Gold Ingot", AchievementMilestoneRewardType.WOOL_REGEN, 2),
                new AchievementMilestoneDefinition("points_15", getAchievementMilestoneTarget(total, 15),
                        Material.GOLD_BLOCK, "Gold Block", AchievementMilestoneRewardType.POINTS, 2),
                new AchievementMilestoneDefinition("points_16", getAchievementMilestoneTarget(total, 16),
                        Material.EMERALD, "Emerald", AchievementMilestoneRewardType.WOOL_REGEN, 2),
                new AchievementMilestoneDefinition("points_17", getAchievementMilestoneTarget(total, 17),
                        Material.EMERALD_BLOCK, "Emerald Block", AchievementMilestoneRewardType.POINTS, 2),
                new AchievementMilestoneDefinition("points_18", getAchievementMilestoneTarget(total, 18),
                        Material.DIAMOND, "Diamond", AchievementMilestoneRewardType.WOOL_REGEN, 2),
                new AchievementMilestoneDefinition("points_19", getAchievementMilestoneTarget(total, 19),
                        Material.DIAMOND_BLOCK, "Diamond Block", AchievementMilestoneRewardType.POINTS, 2),
                new AchievementMilestoneDefinition("points_20", getAchievementMilestoneTarget(total, 20),
                        Material.NETHERITE_SCRAP, "Netherite Scrap", AchievementMilestoneRewardType.WOOL_REGEN, 2),
                new AchievementMilestoneDefinition("points_21", getAchievementMilestoneTarget(total, 21),
                        Material.ANCIENT_DEBRIS, "Ancient Debris", AchievementMilestoneRewardType.POINTS, 2),
                new AchievementMilestoneDefinition("points_22", getAchievementMilestoneTarget(total, 22),
                        Material.NETHERITE_INGOT, "Netherite Ingot", AchievementMilestoneRewardType.WOOL_REGEN, 2),
                new AchievementMilestoneDefinition("points_23", getAchievementMilestoneTarget(total, 23),
                        Material.NETHERITE_BLOCK, "Netherite Block", AchievementMilestoneRewardType.POINTS, 2),
                new AchievementMilestoneDefinition("points_24", getAchievementMilestoneTarget(total, 24),
                        Material.NETHER_STAR, "Nether Star", AchievementMilestoneRewardType.WOOL_REGEN, 2),
                new AchievementMilestoneDefinition("points_25", getAchievementMilestoneTarget(total, 25),
                        Material.BEACON, "Beacon", AchievementMilestoneRewardType.POINTS, 10),
                new AchievementMilestoneDefinition("points_26", getAchievementMilestoneTarget(total, 26),
                        Material.COMMAND_BLOCK, "Command Block", AchievementMilestoneRewardType.WOOL_REGEN, 10));
    }

    private static Material resolveMaterial(String materialName, Material fallback) {
        if (materialName == null || materialName.isBlank()) {
            return fallback;
        }
        Material resolved = Material.matchMaterial(materialName);
        return resolved == null ? fallback : resolved;
    }

    private static int getAchievementPointPool() {
        int total = 0;
        for (AchievementDefinition definition : ACHIEVEMENT_DEFINITIONS) {
            if ("secret_author_online".equals(definition.id)
                    || "secret_owner_farm".equals(definition.id)) {
                continue;
            }
            total = addSaturated(total, definition.achievementPoints);
        }
        return total;
    }

    private static int getAchievementMilestoneTarget(int totalAchievementPoints, int milestoneIndex) {
        int total = Math.max(0, totalAchievementPoints);
        int count = Math.max(1, ACHIEVEMENT_MILESTONE_COUNT);
        int index = Math.max(1, Math.min(count, milestoneIndex));
        if (total <= 0) {
            return index;
        }
        int target = (int) Math.ceil(total * (index / (double) count));
        return Math.max(index, Math.min(total, target));
    }

    private static boolean isSecretAchievementId(String achievementId) {
        return "secret_author_online".equals(achievementId)
                || "secret_owner_farm".equals(achievementId);
    }

    private static List<Integer> getAchievementGridSlots() {
        List<Integer> slots = new ArrayList<>();
        int[][] rows = {
                { 3, 4, 5 },
                { 13, 12, 14, 11, 15, 10, 16 },
                { 22, 21, 23, 20, 24, 19, 25 },
                { 31, 30, 32, 29, 33, 28, 34 },
                { 40, 39, 41, 38, 42, 37, 43 }
        };
        for (int[] row : rows) {
            for (int slot : row) {
                slots.add(slot);
            }
        }
        return slots;
    }

    private static List<Integer> getAchievementMilestoneGridSlots() {
        List<Integer> slots = new ArrayList<>();
        int[][] rows = {
                { 10, 11, 12, 13, 14, 15, 16 },
                { 19, 20, 21, 22, 23, 24, 25 },
                { 28, 29, 30, 31, 32, 33, 34 },
                { 38, 39, 40, 41, 42 }
        };
        for (int[] row : rows) {
            for (int slot : row) {
                slots.add(slot);
            }
        }
        return slots;
    }

    private static final List<QuickAccessDefinition> QUICK_ACCESS_DEFINITIONS = List.of(
            new QuickAccessDefinition("menu_quest", Material.WRITABLE_BOOK, "Open Quest Abilities",
                    "Open quest abilities menu"),
            new QuickAccessDefinition("menu_automation", Material.COMPARATOR, "Open Automation",
                    "Open automation menu"),
            new QuickAccessDefinition("menu_socials", Material.PLAYER_HEAD, "Open Socials",
                    "Open socials menu"),
            new QuickAccessDefinition("menu_scoreboard", Material.MAP, "Open Scoreboard Settings",
                    "Open scoreboard settings"),
            new QuickAccessDefinition("upgrade_limit", Material.OAK_FENCE, "Buy Sheep Limit",
                    "Buy sheep limit upgrade"),
            new QuickAccessDefinition("upgrade_egg_speed", Material.CLOCK, "Buy Egg Speed",
                    "Buy faster egg spawn"),
            new QuickAccessDefinition("upgrade_wool", Material.WHITE_WOOL, "Buy Wool Regen",
                    "Buy faster wool regen"),
            new QuickAccessDefinition("upgrade_tier_chance", Material.GOLDEN_APPLE, "Buy Tier Chance",
                    "Buy higher tier spawn chance"),
            new QuickAccessDefinition("quest_lucky_burst", Material.ENDER_EYE, "Cast Lucky Burst",
                    "Buy/toggle Lucky Burst"),
            new QuickAccessDefinition("quest_merge_assist", Material.ANVIL, "Cast Merge Assist",
                    "Buy/toggle Merge Assist"),
            new QuickAccessDefinition("quest_shear_all", Material.FLINT, "Cast Shear All Sheep",
                    "Buy/toggle Shear All Sheep"),
            new QuickAccessDefinition("automation_toggle_auto_buy", Material.HOPPER, "Toggle Auto Buy",
                    "Toggle automation: Auto Buy"),
            new QuickAccessDefinition("automation_toggle_auto_ability", Material.REDSTONE_TORCH,
                    "Toggle Auto Ability", "Toggle automation: Auto Ability"),
            new QuickAccessDefinition("automation_toggle_auto_merge", Material.PISTON,
                    "Toggle Auto Merge", "Toggle automation: Auto Merge"),
            new QuickAccessDefinition("automation_toggle_auto_spawn", Material.FIREWORK_STAR,
                    "Toggle Auto Spawn", "Toggle automation: Auto Spawn"),
            new QuickAccessDefinition("automation_toggle_auto_prestige", Material.BEACON,
                    "Toggle Auto Prestige", "Toggle automation: Auto Prestige"),
            new QuickAccessDefinition("automation_enable_all", Material.LIME_DYE, "Enable All Automation",
                    "Turn all automation toggles on"),
            new QuickAccessDefinition("automation_disable_all", Material.RED_DYE, "Disable All Automation",
                    "Turn all automation toggles off"));

    private static final List<RebirthSkillNode> REBIRTH_SKILL_NODES = List.of(
            new RebirthSkillNode(
                    REBIRTH_SKILL_POINTS_X10_ROOT,
                    0,
                    1,
                    49,
                    Material.NETHER_STAR,
                    "Point Surge",
                    "x10 Coins"),
            new RebirthSkillNode(
                    REBIRTH_SKILL_POINTS_X10_LEFT,
                    REBIRTH_SKILL_POINTS_X10_ROOT,
                    2,
                    38,
                    Material.EMERALD,
                    "Deep Surge",
                    "x10 more Coins"),
            new RebirthSkillNode(
                    REBIRTH_SKILL_QUEST_POINTS_X10,
                    REBIRTH_SKILL_POINTS_X10_LEFT,
                    3,
                    28,
                    Material.BOOK,
                    "Quest Tide",
                    "x10 quest points"),
            new RebirthSkillNode(
                    REBIRTH_SKILL_SACRIFICE_POINTS_X10,
                    REBIRTH_SKILL_POINTS_X10_LEFT,
                    3,
                    30,
                    Material.TOTEM_OF_UNDYING,
                    "Sacrifice Tide",
                    "x10 sacrifice points"),
            new RebirthSkillNode(
                    REBIRTH_SKILL_WOOL_REGEN_X10,
                    REBIRTH_SKILL_QUEST_POINTS_X10,
                    4,
                    18,
                    Material.LIME_WOOL,
                    "Wool Surge",
                    "x10 wool regen speed"),
            new RebirthSkillNode(
                    REBIRTH_SKILL_QUEST_MASTER,
                    REBIRTH_SKILL_QUEST_POINTS_X10,
                    4,
                    19,
                    Material.ENCHANTED_BOOK,
                    "Quest Master",
                    "2x quest size, 2x rewards, 2.5m resets"),
            new RebirthSkillNode(
                    REBIRTH_SKILL_KEEP_SACRIFICE_AFTER_REBIRTH,
                    REBIRTH_SKILL_POINTS_X10_ROOT,
                    2,
                    42,
                    Material.MILK_BUCKET,
                    "Keep Sacrifice",
                    "Keep sacrifice points after rebirth"),
            new RebirthSkillNode(
                    REBIRTH_SKILL_KEEP_POINTS_AFTER_PRESTIGE,
                    REBIRTH_SKILL_KEEP_SACRIFICE_AFTER_REBIRTH,
                    3,
                    32,
                    Material.CHEST,
                    "Keep Coins",
                    "Keep Coins after prestige"),
            new RebirthSkillNode(
                    REBIRTH_SKILL_KEEP_SHEEP_AFTER_PRESTIGE,
                    REBIRTH_SKILL_KEEP_POINTS_AFTER_PRESTIGE,
                    3,
                    34,
                    Material.SHEEP_SPAWN_EGG,
                    "Keep Sheep",
                    "Keep sheep after prestige; no sacrifice gain"));

    private static final List<String> GAMEPLAY_TIPS = List.of(
            "&7Use &e/sheepmerge &7to jump to your farm. Use it again while visiting to return home.",
            "&7Your menu item is the &bNether Star &7in hotbar slot 9. Right-click it to open Sheep Merge Menu.",
            "&7Eggs are shown as your XP level. The XP bar shows time until the next egg.",
            "&7Spawn eggs are in hotbar slot 8. No eggs? Wait for the timer or raise egg speed.",
            "&7Merge faster: sneak-right-click a sheep to carry it, then right-click a same-tier sheep.",
            "&7Shearing and merging together are your main point income. Keep both loops active.",
            "&7Rainbow sheep can merge with matching rainbow tier to push rainbow tiers higher forever.",
            "&7Shear Shop boosts shear value and adds procs like Wool Keeper and Tier Booster.",
            "&7Quest objectives reset over time. Finish them to earn quest points for active abilities.",
            "&7Quest Upgrades increase ability duration and lower ability costs.",
            "&7Merge Assist auto-merges carried sheep while charges remain.",
            "&7Combo score multiplies your gains. Keep merging to avoid decay and maintain high value.",
            "&7Combo Upgrades improve decay, gain, and max combo cap.",
            "&7Prestige resets normal progress and grants prestige points for permanent account upgrades.",
            "&7Prestige upgrades can raise egg cap, base spawn tier, and several maximum upgrade caps.",
            "&7Prestige refund lets you respec prestige upgrades after cooldown.",
            "&7Automation points are earned over playtime. Spend them in Automation Upgrades.",
            "&7Automation tracks start disabled. Buy and toggle each track on when you are ready.",
            "&7Auto Spawn now drops sheep from the sky and still spends eggs.",
            "&7Farm worlds now load from a cached structure copy instead of rebuilding every block.",
            "&7Auto Prestige can run automatically once unlocked and toggled on.",
            "&7Use &e/sheepmerge visit <player> &7to visit open farms and &e/sheepmerge visit -toggle &7to manage access.",
            "&7Use &e/sheepmerge status &7to quickly check your Coins, quests, combo, and prestige progress.",
            "&7Admins: &e/sheepmerge backup list/load/delete/recover &7manage compressed backups safely.");

    private SheepMergeManager() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void initialize(SheepMergePlugin plugin) {
        SheepMergeManager.plugin = plugin;
        dataFile = new File(plugin.getDataFolder(), "scores.yml");
        farmLayoutFile = new File(plugin.getDataFolder(), "farm-layout.yml");
        farmStructureCacheDirectory = new File(plugin.getDataFolder(), "farm-structure-cache");
        loadData();
        applyLiveDataSchemaVersion(CURRENT_DATA_SCHEMA_VERSION, "startup");
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

    public static boolean isLiveUpdateEnabled() {
        return SheepLiveUpdateState.isLiveUpdateEnabled();
    }

    public static void setLiveUpdateEnabled(boolean enabled) {
        SheepLiveUpdateState.setLiveUpdateEnabled(enabled, SheepMergeManager::saveData);
    }

    public static int getCurrentDataSchemaVersion() {
        return CURRENT_DATA_SCHEMA_VERSION;
    }

    public static int getDataSchemaVersion() {
        return SheepLiveUpdateState.getDataSchemaVersion();
    }

    public static synchronized boolean applyLiveDataSchemaVersion(int targetVersion, String reason) {
        return SheepLiveUpdateState.applyDataSchemaVersion(
                targetVersion,
                CURRENT_DATA_SCHEMA_VERSION,
                reason,
                SheepMergeManager::reconcileAchievementAutomationPointGrants,
                SheepMergeManager::saveData);
    }

    public static void recordLiveUpdateCheck(String status) {
        SheepLiveUpdateState.recordLiveUpdateCheck(status, SheepMergeManager::saveData);
    }

    public static void recordStagedLiveUpdate(String version, String status) {
        SheepLiveUpdateState.recordStagedLiveUpdate(version, status, SheepMergeManager::saveData);
    }

    public static String getStagedLiveUpdateVersion() {
        return SheepLiveUpdateState.getStagedLiveUpdateVersion();
    }

    public static void clearStagedLiveUpdate(String status) {
        SheepLiveUpdateState.clearStagedLiveUpdate(status, SheepMergeManager::saveData);
    }

    public static void recordLiveUpdateApply(String status) {
        SheepLiveUpdateState.recordLiveUpdateApply(
                status,
                CURRENT_DATA_SCHEMA_VERSION,
                SheepMergeManager::saveData);
    }

    public static List<String> getLiveUpdateStatusLines() {
        return SheepLiveUpdateState.getLiveUpdateStatusLines(CURRENT_DATA_SCHEMA_VERSION);
    }

    public static int getFarmRadius() {
        return FARM_RADIUS;
    }

    public static int getFarmWorldRadiusChunks() {
        return FARM_WORLD_RADIUS_CHUNKS;
    }

    public static double getFarmWorldCenterX() {
        int minBlockX = (-FARM_LAYOUT_SAVE_CHUNK_HALF_SPAN) << 4;
        int maxBlockX = (((-FARM_LAYOUT_SAVE_CHUNK_HALF_SPAN) + FARM_LAYOUT_SAVE_CHUNK_SPAN - 1) << 4) + 15;
        return (minBlockX + maxBlockX) / 2.0D;
    }

    public static double getFarmWorldCenterZ() {
        int minBlockZ = (-FARM_LAYOUT_SAVE_CHUNK_HALF_SPAN) << 4;
        int maxBlockZ = (((-FARM_LAYOUT_SAVE_CHUNK_HALF_SPAN) + FARM_LAYOUT_SAVE_CHUNK_SPAN - 1) << 4) + 15;
        return (minBlockZ + maxBlockZ) / 2.0D;
    }

    public static double getFarmWorldBorderSizeBlocks() {
        return FARM_LAYOUT_SAVE_CHUNK_SPAN * 16.0D;
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

    public static boolean saveBuildWorldToLayoutFile() {
        if (plugin == null) {
            return false;
        }
        World buildWorld = Bukkit.getWorld(FARM_BUILD_WORLD_NAME);
        if (!isFarmBuildWorld(buildWorld)) {
            return false;
        }
        buildWorld.save();
        boolean saved = saveSharedFarmLayoutFromWorld(buildWorld);
        if (saved) {
            refreshFarmWorldStructureCache();
        }
        return saved;
    }

    public static void warmFarmWorldStructureCacheOnStartup() {
        if (plugin == null) {
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            World buildWorld = SheepFarmWorldCommand.ensureFarmBuildWorld();
            if (!isFarmBuildWorld(buildWorld)) {
                return;
            }
            refreshFarmWorldStructureCache();
        });
    }

    public static boolean prepareTransientWorldStructure(String worldName) {
        if (plugin == null || worldName == null || worldName.isBlank() || farmStructureCacheDirectory == null
                || !farmStructureCacheDirectory.exists() || isFarmBuildWorldName(worldName)) {
            return false;
        }

        File targetWorldFolder = new File(Bukkit.getWorldContainer(), worldName);
        if (targetWorldFolder.exists()) {
            return false;
        }

        try {
            copyWorldDirectory(farmStructureCacheDirectory.toPath(), targetWorldFolder.toPath());
            return true;
        } catch (IOException exception) {
            if (targetWorldFolder.exists()) {
                try {
                    deleteDirectory(targetWorldFolder.toPath());
                } catch (IOException ignored) {
                }
            }
            if (plugin != null) {
                plugin.getLogger().warning("Unable to prepare cached farm world structure for '" + worldName
                        + "': " + exception.getMessage());
            }
            return false;
        }
    }

    public static boolean refreshFarmWorldStructureCache() {
        return refreshFarmWorldStructureCache(true);
    }

    public static boolean refreshFarmWorldStructureCacheAfterBuildWorldSave() {
        return refreshFarmWorldStructureCache(false);
    }

    public static void refreshFarmWorldStructureCacheAfterBuildWorldSaveIfStale(long minIntervalMs) {
        long threshold = Math.max(0L, minIntervalMs);
        synchronized (FARM_STRUCTURE_CACHE_REFRESH_LOCK) {
            long now = System.currentTimeMillis();
            if (now - lastFarmStructureCacheRefreshAtMs < threshold) {
                return;
            }
        }
        refreshFarmWorldStructureCacheAfterBuildWorldSave();
    }

    private static boolean refreshFarmWorldStructureCache(boolean saveBuildWorldFirst) {
        if (plugin == null || farmStructureCacheDirectory == null) {
            return false;
        }

        World buildWorld = Bukkit.getWorld(FARM_BUILD_WORLD_NAME);
        if (!isFarmBuildWorld(buildWorld) || buildWorld.getWorldFolder() == null
                || !buildWorld.getWorldFolder().exists()) {
            return false;
        }

        if (saveBuildWorldFirst) {
            buildWorld.save();
        }

        File tempDirectory = new File(plugin.getDataFolder(), "farm-structure-cache.tmp");
        try {
            if (tempDirectory.exists()) {
                deleteDirectory(tempDirectory.toPath());
            }
            copyWorldDirectory(buildWorld.getWorldFolder().toPath(), tempDirectory.toPath());

            if (farmStructureCacheDirectory.exists()) {
                deleteDirectory(farmStructureCacheDirectory.toPath());
            }
            if (!tempDirectory.renameTo(farmStructureCacheDirectory)) {
                copyWorldDirectory(tempDirectory.toPath(), farmStructureCacheDirectory.toPath());
                deleteDirectory(tempDirectory.toPath());
            }
            synchronized (FARM_STRUCTURE_CACHE_REFRESH_LOCK) {
                lastFarmStructureCacheRefreshAtMs = System.currentTimeMillis();
            }
            return true;
        } catch (IOException exception) {
            if (plugin != null) {
                plugin.getLogger().warning("Unable to refresh farm structure cache: " + exception.getMessage());
            }
            return false;
        }
    }

    private static boolean isFarmBuildWorldName(String worldName) {
        return worldName != null && FARM_BUILD_WORLD_NAME.equals(worldName);
    }

    private static void copyWorldDirectory(Path sourceRoot, Path targetRoot) throws IOException {
        try (Stream<Path> paths = Files.walk(sourceRoot)) {
            paths.forEach(currentPath -> {
                Path relative = sourceRoot.relativize(currentPath);
                if (relative.getNameCount() > 0 && shouldSkipWorldCachePath(relative)) {
                    return;
                }

                Path targetPath = targetRoot.resolve(relative.toString());
                try {
                    if (Files.isDirectory(currentPath)) {
                        Files.createDirectories(targetPath);
                    } else {
                        Path parent = targetPath.getParent();
                        if (parent != null) {
                            Files.createDirectories(parent);
                        }
                        Files.copy(currentPath, targetPath,
                                StandardCopyOption.REPLACE_EXISTING,
                                StandardCopyOption.COPY_ATTRIBUTES);
                    }
                } catch (IOException exception) {
                    throw new UncheckedIOException(exception);
                }
            });
        } catch (UncheckedIOException exception) {
            throw exception.getCause();
        }
    }

    private static boolean shouldSkipWorldCachePath(Path relativePath) {
        String topLevelName = relativePath.getName(0).toString();
        return "session.lock".equals(topLevelName)
                || "uid.dat".equals(topLevelName)
                || "playerdata".equals(topLevelName)
                || "stats".equals(topLevelName)
                || "advancements".equals(topLevelName)
                || "entities".equals(topLevelName);
    }

    private static void deleteDirectory(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }

        try (Stream<Path> paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException exception) {
                    throw new UncheckedIOException(exception);
                }
            });
        } catch (UncheckedIOException exception) {
            throw exception.getCause();
        }
    }

    public static boolean saveSharedFarmLayoutFromWorld(World sourceWorld) {
        if (sourceWorld == null || (!isSheepFarmWorld(sourceWorld) && !isFarmBuildWorld(sourceWorld))) {
            return false;
        }
        if (farmLayoutConfig == null) {
            farmLayoutConfig = new YamlConfiguration();
        }
        int minChunkX = Integer.MAX_VALUE;
        int maxChunkX = Integer.MIN_VALUE;
        int minChunkZ = Integer.MAX_VALUE;
        int maxChunkZ = Integer.MIN_VALUE;
        for (Chunk loadedChunk : sourceWorld.getLoadedChunks()) {
            if (loadedChunk == null) {
                continue;
            }
            minChunkX = Math.min(minChunkX, loadedChunk.getX());
            maxChunkX = Math.max(maxChunkX, loadedChunk.getX());
            minChunkZ = Math.min(minChunkZ, loadedChunk.getZ());
            maxChunkZ = Math.max(maxChunkZ, loadedChunk.getZ());
        }
        if (minChunkX > maxChunkX || minChunkZ > maxChunkZ) {
            minChunkX = -FARM_LAYOUT_SAVE_CHUNK_HALF_SPAN;
            maxChunkX = minChunkX + FARM_LAYOUT_SAVE_CHUNK_SPAN - 1;
            minChunkZ = -FARM_LAYOUT_SAVE_CHUNK_HALF_SPAN;
            maxChunkZ = minChunkZ + FARM_LAYOUT_SAVE_CHUNK_SPAN - 1;
        }
        int minAllowedChunkX = -FARM_LAYOUT_SAVE_CHUNK_HALF_SPAN;
        int maxAllowedChunkX = minAllowedChunkX + FARM_LAYOUT_SAVE_CHUNK_SPAN - 1;
        int minAllowedChunkZ = -FARM_LAYOUT_SAVE_CHUNK_HALF_SPAN;
        int maxAllowedChunkZ = minAllowedChunkZ + FARM_LAYOUT_SAVE_CHUNK_SPAN - 1;
        minChunkX = Math.max(minChunkX, minAllowedChunkX);
        maxChunkX = Math.min(maxChunkX, maxAllowedChunkX);
        minChunkZ = Math.max(minChunkZ, minAllowedChunkZ);
        maxChunkZ = Math.min(maxChunkZ, maxAllowedChunkZ);
        if (minChunkX > maxChunkX || minChunkZ > maxChunkZ) {
            minChunkX = minAllowedChunkX;
            maxChunkX = maxAllowedChunkX;
            minChunkZ = minAllowedChunkZ;
            maxChunkZ = maxAllowedChunkZ;
        }
        int minX = minChunkX << 4;
        int maxX = (maxChunkX << 4) + 15;
        int minZ = minChunkZ << 4;
        int maxZ = (maxChunkZ << 4) + 15;

        farmLayoutConfig.set("version", 3);
        farmLayoutConfig.set("world.minY", sourceWorld.getMinHeight());
        farmLayoutConfig.set("world.maxY", sourceWorld.getMaxHeight());
        farmLayoutConfig.set("world.minX", minX);
        farmLayoutConfig.set("world.maxX", maxX);
        farmLayoutConfig.set("world.minZ", minZ);
        farmLayoutConfig.set("world.maxZ", maxZ);
        farmLayoutConfig.set("world.name", sourceWorld.getName());
        farmLayoutConfig.set("world.savedAt", System.currentTimeMillis());
        farmLayoutConfig.set("chunks", null);
        farmLayoutConfig.set("blocks", null);

        int minY = sourceWorld.getMinHeight();
        int maxY = sourceWorld.getMaxHeight();
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
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
        clearFarmPlatformBoundingBox(world);
        boolean hasSavedLayout = hasSavedFarmLayout();
        if (hasSavedLayout) {
            applySavedFarmLayout(world);
        } else {
            applyDefaultFarmLayout(world);
            enforceFarmPerimeter(world);
        }
        ensureMandatoryFarmBaseLayer(world);
    }

    public static void applyFarmLayoutAsync(World world, Runnable onComplete) {
        if (world == null) {
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }
        if (plugin == null) {
            applyFarmLayout(world);
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }

        clearFarmPlatformBoundingBox(world);

        if (hasSavedFarmLayout() && farmLayoutConfig != null && farmLayoutConfig.isConfigurationSection("chunks")) {
            applySavedChunkLayoutAsync(world, () -> {
                enforceFarmPerimeter(world);
                ensureMandatoryFarmBaseLayer(world);
                if (onComplete != null) {
                    onComplete.run();
                }
            });
            return;
        }

        if (hasSavedFarmLayout()) {
            applySavedBlockLayoutAsync(world, () -> {
                enforceFarmPerimeter(world);
                ensureMandatoryFarmBaseLayer(world);
                if (onComplete != null) {
                    onComplete.run();
                }
            });
            return;
        }

        applyDefaultFarmLayoutAsync(world, () -> {
            enforceFarmPerimeter(world);
            ensureMandatoryFarmBaseLayer(world);
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }

    private static void applyDefaultFarmLayoutAsync(World world, Runnable onComplete) {
        if (world == null) {
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }

        applyDefaultFarmLayout(world);
        if (onComplete != null) {
            onComplete.run();
        }
    }

    private static void applySavedBlockLayoutAsync(World world, Runnable onComplete) {
        if (plugin == null || world == null || farmLayoutConfig == null) {
            applySavedFarmLayout(world);
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }

        final int[] state = { FARM_MIN_XZ, FARM_MIN_Y, FARM_MIN_XZ };
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            if (Bukkit.getWorld(world.getUID()) == null) {
                task.cancel();
                if (onComplete != null) {
                    onComplete.run();
                }
                return;
            }

            int processed = 0;
            while (state[1] <= FARM_MAX_Y && processed < FARM_LAYOUT_BLOCKS_PER_TICK) {
                int x = state[0];
                int y = state[1];
                int z = state[2];

                String serialized = farmLayoutConfig.getString("blocks." + keyFor(x, y, z));
                BlockData data = (serialized == null || serialized.isBlank())
                        ? Bukkit.createBlockData(getDefaultFarmMaterialAt(x, y, z))
                        : parseBlockData(serialized);
                world.getBlockAt(x, y, z).setBlockData(data, false);
                processed++;

                state[2]++;
                if (state[2] > FARM_MAX_XZ) {
                    state[2] = FARM_MIN_XZ;
                    state[0]++;
                    if (state[0] > FARM_MAX_XZ) {
                        state[0] = FARM_MIN_XZ;
                        state[1]++;
                    }
                }
            }

            if (state[1] > FARM_MAX_Y) {
                task.cancel();
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        }, 1L, 1L);
    }

    private static void applySavedChunkLayoutAsync(World world, Runnable onComplete) {
        if (plugin == null || world == null || farmLayoutConfig == null
                || !farmLayoutConfig.isConfigurationSection("chunks")) {
            applySavedChunkLayout(world);
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }

        org.bukkit.configuration.ConfigurationSection chunksSection = farmLayoutConfig
                .getConfigurationSection("chunks");
        if (chunksSection == null || chunksSection.getKeys(false).isEmpty()) {
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }

        int minY = Math.max(world.getMinHeight(), farmLayoutConfig.getInt("world.minY", world.getMinHeight()));
        int maxY = Math.min(world.getMaxHeight(), farmLayoutConfig.getInt("world.maxY", world.getMaxHeight()));
        if (minY >= maxY) {
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }

        List<ChunkApplyCursor> cursors = new ArrayList<>();
        for (String chunkKey : chunksSection.getKeys(false)) {
            String chunkPath = "chunks." + chunkKey;
            int chunkX = resolveChunkCoordinate(chunkKey, chunkPath + ".x", 0);
            int chunkZ = resolveChunkCoordinate(chunkKey, chunkPath + ".z", 1);
            if (chunkX == Integer.MIN_VALUE || chunkZ == Integer.MIN_VALUE) {
                continue;
            }

            List<String> palette = farmLayoutConfig.getStringList(chunkPath + ".palette");
            List<BlockData> paletteData = new ArrayList<>();
            if (palette != null) {
                for (String serialized : palette) {
                    BlockData data = (serialized == null || serialized.isBlank())
                            ? Bukkit.createBlockData(Material.AIR)
                            : parseBlockData(serialized);
                    paletteData.add(data);
                }
            }

            String encodedRuns = farmLayoutConfig.getString(chunkPath + ".data", "");
            boolean hasRleData = encodedRuns != null && !encodedRuns.isBlank() && !paletteData.isEmpty();
            String[] tokens = hasRleData ? encodedRuns.split(";") : null;
            cursors.add(new ChunkApplyCursor(chunkPath, chunkX, chunkZ, minY, maxY, paletteData, tokens, !hasRleData));
        }

        if (cursors.isEmpty()) {
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }

        final Iterator<ChunkApplyCursor> iterator = cursors.iterator();
        final ChunkApplyCursor[] activeCursor = { iterator.hasNext() ? iterator.next() : null };

        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            if (Bukkit.getWorld(world.getUID()) == null) {
                task.cancel();
                if (onComplete != null) {
                    onComplete.run();
                }
                return;
            }

            int processed = 0;
            while (processed < FARM_LAYOUT_BLOCKS_PER_TICK && activeCursor[0] != null) {
                ChunkApplyCursor cursor = activeCursor[0];
                if (cursor.isComplete()) {
                    activeCursor[0] = iterator.hasNext() ? iterator.next() : null;
                    continue;
                }

                int blockIndex = cursor.blockIndex++;
                int yOffset = blockIndex / (16 * 16);
                int withinLayer = blockIndex % (16 * 16);
                int localX = withinLayer / 16;
                int localZ = withinLayer % 16;
                int worldX = (cursor.chunkX << 4) + localX;
                int worldZ = (cursor.chunkZ << 4) + localZ;
                int y = cursor.minY + yOffset;

                BlockData data;
                if (cursor.legacyFormat) {
                    String serialized = farmLayoutConfig.getString(
                            cursor.chunkPath + ".blocks." + blockIndex);
                    data = (serialized == null || serialized.isBlank())
                            ? Bukkit.createBlockData(Material.AIR)
                            : parseBlockData(serialized);
                } else {
                    data = cursor.nextBlockData();
                    if (data == null) {
                        data = Bukkit.createBlockData(Material.AIR);
                    }
                }

                world.getBlockAt(worldX, y, worldZ).setBlockData(data, false);
                processed++;
            }

            if (activeCursor[0] == null) {
                task.cancel();
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        }, 1L, 1L);
    }

    private static void enforceFarmPerimeter(World world) {
        if (world == null) {
            return;
        }
        for (int x = FARM_MIN_XZ; x <= FARM_MAX_XZ; x++) {
            for (int z = FARM_MIN_XZ; z <= FARM_MAX_XZ; z++) {
                boolean border = x == FARM_MIN_XZ || x == FARM_MAX_XZ || z == FARM_MIN_XZ || z == FARM_MAX_XZ;
                if (!border) {
                    continue;
                }
                world.getBlockAt(x, FARM_BASE_Y + 1, z).setBlockData(createPerimeterFenceData(x, z), false);
                world.getBlockAt(x, FARM_BASE_Y + 2, z)
                        .setBlockData(Bukkit.createBlockData(Material.WHITE_CARPET), false);
            }
        }
    }

    private static BlockData createPerimeterFenceData(int x, int z) {
        Fence fence = (Fence) Bukkit.createBlockData(Material.OAK_FENCE);
        if (z == FARM_MIN_XZ || z == FARM_MAX_XZ) {
            if (x > FARM_MIN_XZ) {
                fence.setFace(org.bukkit.block.BlockFace.WEST, true);
            }
            if (x < FARM_MAX_XZ) {
                fence.setFace(org.bukkit.block.BlockFace.EAST, true);
            }
        }
        if (x == FARM_MIN_XZ || x == FARM_MAX_XZ) {
            if (z > FARM_MIN_XZ) {
                fence.setFace(org.bukkit.block.BlockFace.NORTH, true);
            }
            if (z < FARM_MAX_XZ) {
                fence.setFace(org.bukkit.block.BlockFace.SOUTH, true);
            }
        }
        return fence;
    }

    private static void clearFarmPlatformBoundingBox(World world) {
        if (world == null) {
            return;
        }
        for (int x = FARM_MIN_XZ; x <= FARM_MAX_XZ; x++) {
            for (int y = FARM_MIN_Y; y <= FARM_MAX_Y; y++) {
                for (int z = FARM_MIN_XZ; z <= FARM_MAX_XZ; z++) {
                    world.getBlockAt(x, y, z).setBlockData(Bukkit.createBlockData(Material.AIR), false);
                }
            }
        }
    }

    private static void applySavedFarmLayout(World world) {
        if (farmLayoutConfig != null && farmLayoutConfig.isConfigurationSection("chunks")) {
            applySavedChunkLayout(world);
            return;
        }

        int minX = farmLayoutConfig == null ? FARM_MIN_XZ : farmLayoutConfig.getInt("world.minX", FARM_MIN_XZ);
        int maxX = farmLayoutConfig == null ? FARM_MAX_XZ : farmLayoutConfig.getInt("world.maxX", FARM_MAX_XZ);
        int minZ = farmLayoutConfig == null ? FARM_MIN_XZ : farmLayoutConfig.getInt("world.minZ", FARM_MIN_XZ);
        int maxZ = farmLayoutConfig == null ? FARM_MAX_XZ : farmLayoutConfig.getInt("world.maxZ", FARM_MAX_XZ);
        int minY = farmLayoutConfig == null ? world.getMinHeight()
                : Math.max(world.getMinHeight(), farmLayoutConfig.getInt("world.minY", world.getMinHeight()));
        int maxY = farmLayoutConfig == null ? world.getMaxHeight()
                : Math.min(world.getMaxHeight(), farmLayoutConfig.getInt("world.maxY", world.getMaxHeight()));

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    String serialized = farmLayoutConfig.getString("blocks." + keyFor(x, y, z));
                    BlockData data = (serialized == null || serialized.isBlank())
                            ? Bukkit.createBlockData(getDefaultFarmMaterialAt(x, y, z))
                            : parseBlockData(serialized);
                    world.getBlockAt(x, y, z).setBlockData(data, false);
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
            int chunkX = resolveChunkCoordinate(chunkKey, chunkPath + ".x", 0);
            int chunkZ = resolveChunkCoordinate(chunkKey, chunkPath + ".z", 1);
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
                        world.getBlockAt(worldX, y, worldZ).setBlockData(data, false);
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
                        world.getBlockAt(worldX, y, worldZ).setBlockData(data, false);
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
                    world.getBlockAt(x, y, z).setBlockData(Bukkit.createBlockData(material), false);
                }
            }
        }
    }

    private static void ensureMandatoryFarmBaseLayer(World world) {
        if (world == null || !isSheepFarmWorld(world)) {
            return;
        }

        for (int x = FARM_MIN_XZ; x <= FARM_MAX_XZ; x++) {
            for (int z = FARM_MIN_XZ; z <= FARM_MAX_XZ; z++) {
                if (world.getBlockAt(x, FARM_MIN_Y, z).getType().isAir()) {
                    world.getBlockAt(x, FARM_MIN_Y, z)
                            .setBlockData(Bukkit.createBlockData(Material.DIRT), false);
                }
                if (world.getBlockAt(x, FARM_BASE_Y, z).getType().isAir()) {
                    world.getBlockAt(x, FARM_BASE_Y, z)
                            .setBlockData(Bukkit.createBlockData(Material.GRASS_BLOCK), false);
                }
            }
        }
    }

    private static boolean chunkIntersectsFarmBounds(int chunkX, int chunkZ) {
        int chunkMinX = chunkX << 4;
        int chunkMaxX = chunkMinX + 15;
        int chunkMinZ = chunkZ << 4;
        int chunkMaxZ = chunkMinZ + 15;
        return chunkMaxX >= FARM_MIN_XZ
                && chunkMinX <= FARM_MAX_XZ
                && chunkMaxZ >= FARM_MIN_XZ
                && chunkMinZ <= FARM_MAX_XZ;
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

    public static void restoreSavedSheepForWorldAsync(World world) {
        restoreSavedSheepForWorldAsync(world, null);
    }

    public static void restoreSavedSheepForWorldAsync(World world, Runnable onComplete) {
        if (plugin == null || world == null || !isSheepFarmWorld(world)) {
            restoreSavedSheepForWorld(world);
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }
        UUID ownerId = getOwnerId(world);
        if (ownerId == null) {
            refreshLiveSheepCount(world);
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }
        List<SheepSnapshot> snapshots = getSavedSheepSnapshots(world).get(ownerId);
        if (snapshots == null || snapshots.isEmpty()) {
            refreshLiveSheepCount(world);
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }

        final long now = System.currentTimeMillis();
        final int batchSize = 25;
        final int[] index = { 0 };
        final UUID worldId = world.getUID();

        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            if (Bukkit.getWorld(worldId) == null || !isSheepFarmWorld(world)) {
                task.cancel();
                if (onComplete != null) {
                    onComplete.run();
                }
                return;
            }
            int processed = 0;
            while (index[0] < snapshots.size() && processed < batchSize) {
                SheepSnapshot snapshot = snapshots.get(index[0]++);
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
                processed++;
            }
            if (index[0] >= snapshots.size()) {
                refreshLiveSheepCount(world);
                task.cancel();
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        }, 1L, 1L);
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
            return -1;
        }
        World buildWorld = Bukkit.getWorld(FARM_BUILD_WORLD_NAME);
        if (buildWorld == null || !isFarmBuildWorld(buildWorld) || !saveSharedFarmLayoutFromWorld(buildWorld)) {
            return -1;
        }
        refreshFarmWorldStructureCache();

        List<World> farmWorlds = collectFarmWorldsForLayoutRefresh();
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

    public static int startLoadSavedFarmLayoutToBuildAndLoadedFarms(Player initiator) {
        if (plugin == null || farmCommitInProgress) {
            return -1;
        }

        loadFarmLayout();
        if (!hasSavedFarmLayout()) {
            return -1;
        }

        World buildWorld = Bukkit.getWorld(FARM_BUILD_WORLD_NAME);
        if (isFarmBuildWorld(buildWorld)) {
            applyFarmLayout(buildWorld);
            buildWorld.save();
            refreshFarmWorldStructureCache();
        }

        List<World> farmWorlds = collectFarmWorldsForLayoutRefresh();
        if (farmWorlds.isEmpty()) {
            saveData();
            return 0;
        }

        World fallbackWorld = plugin.getServer().getWorlds().isEmpty() ? null : plugin.getServer().getWorlds().get(0);
        farmCommitInProgress = true;
        processFarmLayoutLoadBatch(farmWorlds, fallbackWorld, initiator, 0, 0);
        return farmWorlds.size();
    }

    public static int startCommitFarmBuildWorldToLoadedFarms(Player initiator) {
        if (plugin == null || farmCommitInProgress) {
            return -1;
        }

        World buildWorld = Bukkit.getWorld(FARM_BUILD_WORLD_NAME);
        if (buildWorld == null || !isFarmBuildWorld(buildWorld) || !saveSharedFarmLayoutFromWorld(buildWorld)) {
            return -1;
        }
        refreshFarmWorldStructureCache();

        List<World> farmWorlds = collectFarmWorldsForLayoutRefresh();
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

    private static void processFarmLayoutLoadBatch(List<World> farmWorlds, World fallbackWorld, Player initiator,
            int index, int updatedCount) {
        if (plugin == null) {
            farmCommitInProgress = false;
            return;
        }

        if (index >= farmWorlds.size()) {
            saveData();
            farmCommitInProgress = false;
            if (initiator != null && initiator.isOnline()) {
                initiator.sendMessage(action("Loaded the saved farm layout into " + updatedCount
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
                () -> processFarmLayoutLoadBatch(farmWorlds, fallbackWorld, initiator, nextIndex, nextUpdatedCount),
                1L);
    }

    private static List<World> collectFarmWorldsForLayoutRefresh() {
        if (plugin == null || plugin.getServer() == null) {
            return List.of();
        }

        Map<String, World> worldsByName = new HashMap<>();
        for (World world : plugin.getServer().getWorlds()) {
            if (world == null || !isSheepFarmWorld(world) || isTutorialWorld(world)) {
                continue;
            }
            worldsByName.put(world.getName(), world);
        }

        for (UUID ownerId : collectKnownFarmOwnerIds()) {
            if (ownerId == null) {
                continue;
            }
            String worldName = SheepFarmWorldCommand.getWorldName(ownerId);
            World farmWorld = SheepFarmWorldCommand.ensureFarmWorld(worldName);
            if (farmWorld == null || isTutorialWorld(farmWorld)) {
                continue;
            }
            worldsByName.put(farmWorld.getName(), farmWorld);
        }

        return new ArrayList<>(worldsByName.values());
    }

    private static Set<UUID> collectKnownFarmOwnerIds() {
        Set<UUID> ownerIds = new HashSet<>();
        addKnownFarmOwners(ownerIds, SheepEconomyState.getPointsTrackedPlayerIds());
        addKnownFarmOwners(ownerIds, SheepTutorialState.completedPlayerIds());
        addKnownFarmOwners(ownerIds, SheepTutorialState.bypassedPlayerIds());
        addKnownFarmOwners(ownerIds, savedFarmSheepByPlayer.keySet());
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online == null || !online.isOnline()) {
                continue;
            }
            ownerIds.add(online.getUniqueId());
        }
        ownerIds.removeIf(id -> id == null || !hasUnlockedFarm(id));
        return ownerIds;
    }

    private static void addKnownFarmOwners(Set<UUID> sink, Collection<UUID> source) {
        if (sink == null || source == null || source.isEmpty()) {
            return;
        }
        for (UUID id : source) {
            if (id != null) {
                sink.add(id);
            }
        }
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
        refreshFarmWorldStructureCache();
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
            restoreSavedStateOutsideFarm(online);
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
        if (y == FARM_BASE_Y + 2
                && (x == FARM_MIN_XZ || x == FARM_MAX_XZ || z == FARM_MIN_XZ || z == FARM_MAX_XZ)) {
            return Material.WHITE_CARPET;
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

    private static int parseChunkCoordinateFromKey(String chunkKey, int axisIndex, int fallback) {
        if (chunkKey == null || chunkKey.isBlank()) {
            return fallback;
        }
        String[] parts = chunkKey.split(",", 2);
        if (parts.length < 2) {
            return fallback;
        }
        String raw = axisIndex <= 0 ? parts[0] : parts[1];
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static int resolveChunkCoordinate(String chunkKey, String configuredPath, int axisIndex) {
        if (farmLayoutConfig == null) {
            return Integer.MIN_VALUE;
        }
        int configured = farmLayoutConfig.getInt(configuredPath, Integer.MIN_VALUE);
        if (configured != Integer.MIN_VALUE) {
            return configured;
        }
        return parseChunkCoordinateFromKey(chunkKey, axisIndex, Integer.MIN_VALUE);
    }

    private static void loadFarmLayout() {
        if (plugin == null || farmLayoutFile == null) {
            return;
        }
        farmLayoutConfig = YamlConfiguration.loadConfiguration(farmLayoutFile);
        if (pruneFarmLayoutChunksToFarmBounds()) {
            saveFarmLayout();
        }
    }

    private static boolean pruneFarmLayoutChunksToFarmBounds() {
        if (farmLayoutConfig == null || !farmLayoutConfig.isConfigurationSection("chunks")) {
            return false;
        }

        org.bukkit.configuration.ConfigurationSection chunksSection = farmLayoutConfig
                .getConfigurationSection("chunks");
        if (chunksSection == null || chunksSection.getKeys(false).isEmpty()) {
            return false;
        }

        int minChunkX = -FARM_LAYOUT_SAVE_CHUNK_HALF_SPAN;
        int maxChunkX = minChunkX + FARM_LAYOUT_SAVE_CHUNK_SPAN - 1;
        int minChunkZ = -FARM_LAYOUT_SAVE_CHUNK_HALF_SPAN;
        int maxChunkZ = minChunkZ + FARM_LAYOUT_SAVE_CHUNK_SPAN - 1;

        boolean modified = false;
        for (String chunkKey : new ArrayList<>(chunksSection.getKeys(false))) {
            String chunkPath = "chunks." + chunkKey;
            int chunkX = resolveChunkCoordinate(chunkKey, chunkPath + ".x", 0);
            int chunkZ = resolveChunkCoordinate(chunkKey, chunkPath + ".z", 1);
            if (chunkX == Integer.MIN_VALUE || chunkZ == Integer.MIN_VALUE) {
                farmLayoutConfig.set(chunkPath, null);
                modified = true;
                continue;
            }
            if (chunkX < minChunkX || chunkX > maxChunkX || chunkZ < minChunkZ || chunkZ > maxChunkZ) {
                farmLayoutConfig.set(chunkPath, null);
                modified = true;
            }
        }

        if (modified) {
            farmLayoutConfig.set("world.minX", FARM_MIN_XZ);
            farmLayoutConfig.set("world.maxX", FARM_MAX_XZ);
            farmLayoutConfig.set("world.minZ", FARM_MIN_XZ);
            farmLayoutConfig.set("world.maxZ", FARM_MAX_XZ);
        }
        return modified;
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

    private static void normalizeFarmLayoutConfigToDirectBlocks() {
        if (farmLayoutConfig == null || !farmLayoutConfig.isConfigurationSection("chunks")
                || farmLayoutConfig.isConfigurationSection("blocks")) {
            return;
        }

        org.bukkit.configuration.ConfigurationSection chunksSection = farmLayoutConfig
                .getConfigurationSection("chunks");
        if (chunksSection == null || chunksSection.getKeys(false).isEmpty()) {
            return;
        }

        int sourceMinY = farmLayoutConfig.getInt("world.minY", FARM_MIN_Y);
        int sourceMaxY = farmLayoutConfig.getInt("world.maxY", FARM_MAX_Y + 1);
        if (sourceMinY >= sourceMaxY) {
            return;
        }
        int targetMinY = sourceMinY;
        int targetMaxY = sourceMaxY;

        Map<String, String> directBlocks = new HashMap<>();
        for (String chunkKey : chunksSection.getKeys(false)) {
            String chunkPath = "chunks." + chunkKey;
            int chunkX = resolveChunkCoordinate(chunkKey, chunkPath + ".x", 0);
            int chunkZ = resolveChunkCoordinate(chunkKey, chunkPath + ".z", 1);
            if (chunkX == Integer.MIN_VALUE || chunkZ == Integer.MIN_VALUE) {
                continue;
            }

            List<String> palette = farmLayoutConfig.getStringList(chunkPath + ".palette");
            String encodedRuns = farmLayoutConfig.getString(chunkPath + ".data", "");
            if (!palette.isEmpty() && encodedRuns != null && !encodedRuns.isBlank()) {
                int totalBlocks = (sourceMaxY - sourceMinY) * 16 * 16;
                int blockIndex = 0;
                for (String token : encodedRuns.split(";")) {
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
                    for (int i = 0; i < runLength && blockIndex < totalBlocks; i++) {
                        int yOffset = blockIndex / (16 * 16);
                        int withinLayer = blockIndex % (16 * 16);
                        int localX = withinLayer / 16;
                        int localZ = withinLayer % 16;
                        int worldX = (chunkX << 4) + localX;
                        int worldZ = (chunkZ << 4) + localZ;
                        int y = sourceMinY + yOffset;
                        if (y >= targetMinY && y < targetMaxY
                                && worldX >= FARM_MIN_XZ && worldX <= FARM_MAX_XZ
                                && worldZ >= FARM_MIN_XZ && worldZ <= FARM_MAX_XZ) {
                            directBlocks.put(keyFor(worldX, y, worldZ), serialized);
                        }
                        blockIndex++;
                    }
                }
                continue;
            }

            int blockIndex = 0;
            for (int y = sourceMinY; y < sourceMaxY; y++) {
                for (int localX = 0; localX < 16; localX++) {
                    for (int localZ = 0; localZ < 16; localZ++) {
                        int worldX = (chunkX << 4) + localX;
                        int worldZ = (chunkZ << 4) + localZ;
                        if (y < targetMinY || y >= targetMaxY
                                || worldX < FARM_MIN_XZ || worldX > FARM_MAX_XZ || worldZ < FARM_MIN_XZ
                                || worldZ > FARM_MAX_XZ) {
                            blockIndex++;
                            continue;
                        }
                        String serialized = farmLayoutConfig.getString(chunkPath + ".blocks." + blockIndex);
                        if (serialized != null && !serialized.isBlank()) {
                            directBlocks.put(keyFor(worldX, y, worldZ), serialized);
                        }
                        blockIndex++;
                    }
                }
            }
        }

        if (directBlocks.isEmpty()) {
            return;
        }

        farmLayoutConfig.set("version", 3);
        farmLayoutConfig.set("world.minY", targetMinY);
        farmLayoutConfig.set("world.maxY", targetMaxY);
        farmLayoutConfig.set("world.minX", FARM_MIN_XZ);
        farmLayoutConfig.set("world.maxX", FARM_MAX_XZ);
        farmLayoutConfig.set("world.minZ", FARM_MIN_XZ);
        farmLayoutConfig.set("world.maxZ", FARM_MAX_XZ);
        farmLayoutConfig.set("chunks", null);
        farmLayoutConfig.set("blocks", null);
        for (Map.Entry<String, String> entry : directBlocks.entrySet()) {
            farmLayoutConfig.set("blocks." + entry.getKey(), entry.getValue());
        }
        saveFarmLayout();
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

    private static NamespacedKey getSocialVisitOwnerKey() {
        return new NamespacedKey(plugin, "social-visit-owner");
    }

    private static NamespacedKey getQuickAccessActionKey() {
        if (plugin == null) {
            return null;
        }
        return new NamespacedKey(plugin, "inventory-quick-access-action");
    }

    private static NamespacedKey getInventoryLayoutOptionKey() {
        if (plugin == null) {
            return null;
        }
        return new NamespacedKey(plugin, "inventory-layout-option");
    }

    private static NamespacedKey getLegacyRainbowMergedCountKey() {
        return new NamespacedKey(plugin, "rainbow-merged-count");
    }

    public static boolean isSheepFarmWorld(World world) {
        return world != null
                && !isFarmBuildWorld(world)
                && (world.getName().startsWith("sheepfarm_") || world.getName().startsWith("sheeptutorial_"));
    }

    private static QuickAccessDefinition getQuickAccessDefinition(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        for (QuickAccessDefinition definition : QUICK_ACCESS_DEFINITIONS) {
            if (definition.id.equals(id)) {
                return definition;
            }
        }
        return null;
    }

    private static List<String> getInventoryQuickAccessActions(UUID playerId) {
        return SheepUiPreferences.getInventoryQuickAccessActions(playerId,
                INVENTORY_QUICK_ACCESS_MAX_ITEMS, id -> getQuickAccessDefinition(id) != null);
    }

    private static boolean isInventoryQuickAccessCastingEnabled(UUID playerId) {
        return SheepUiPreferences.isInventoryQuickAccessCastingEnabled(playerId);
    }

    private static boolean isInventoryQuickAccessCastingEnabled(Player player) {
        return player != null && isInventoryQuickAccessCastingEnabled(player.getUniqueId());
    }

    private static boolean toggleInventoryQuickAccessCasting(Player player) {
        if (player == null) {
            return false;
        }
        UUID playerId = player.getUniqueId();
        boolean enabled = !isInventoryQuickAccessCastingEnabled(playerId);
        SheepUiPreferences.setInventoryQuickAccessCastingEnabled(playerId, enabled);
        saveData();
        return enabled;
    }

    private static void setInventoryQuickAccessActions(UUID playerId, List<String> actions) {
        SheepUiPreferences.setInventoryQuickAccessActions(playerId, actions,
                INVENTORY_QUICK_ACCESS_MAX_ITEMS, id -> getQuickAccessDefinition(id) != null);
    }

    private static boolean toggleInventoryQuickAccessAction(Player player, String actionId) {
        if (player == null || actionId == null || actionId.isBlank() || getQuickAccessDefinition(actionId) == null) {
            return false;
        }
        UUID playerId = player.getUniqueId();
        List<String> selected = new ArrayList<>(getInventoryQuickAccessActions(playerId));
        if (selected.contains(actionId)) {
            selected.remove(actionId);
            setInventoryQuickAccessActions(playerId, selected);
            saveData();
            return true;
        }
        if (selected.size() >= INVENTORY_QUICK_ACCESS_MAX_ITEMS) {
            return false;
        }
        selected.add(actionId);
        setInventoryQuickAccessActions(playerId, selected);
        saveData();
        return true;
    }

    private static List<ItemStack> buildQuickAccessHotbarItems(Player player) {
        if (player == null) {
            return List.of();
        }
        List<String> selectedActions = getInventoryQuickAccessActions(player.getUniqueId());
        if (selectedActions.isEmpty()) {
            return List.of();
        }

        List<ItemStack> items = new ArrayList<>();
        for (String actionId : selectedActions) {
            QuickAccessDefinition definition = getQuickAccessDefinition(actionId);
            if (definition == null) {
                continue;
            }
            ItemStack item = MenuItemFactory.create(
                    definition.material,
                    definition.name,
                    List.of(
                            definition.description,
                            "Right-click: cast"));
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                if ("menu_socials".equals(definition.id) && meta instanceof SkullMeta skullMeta) {
                    skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(SOCIALS_AUTHOR_UUID));
                    meta = skullMeta;
                }
                NamespacedKey key = getQuickAccessActionKey();
                if (key != null) {
                    meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, definition.id);
                }
                item.setItemMeta(meta);
            }
            items.add(item);
            if (items.size() >= INVENTORY_QUICK_ACCESS_MAX_ITEMS) {
                break;
            }
        }
        return items;
    }

    private static String getQuickAccessActionId(ItemStack itemStack) {
        if (itemStack == null) {
            return null;
        }
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return null;
        }
        NamespacedKey key = getQuickAccessActionKey();
        if (key == null) {
            return null;
        }
        String actionId = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (actionId == null || actionId.isBlank() || getQuickAccessDefinition(actionId) == null) {
            return null;
        }
        return actionId;
    }

    private static String getInventoryLayoutOptionId(ItemStack itemStack) {
        if (itemStack == null) {
            return null;
        }
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return null;
        }
        NamespacedKey key = getInventoryLayoutOptionKey();
        if (key == null) {
            return null;
        }
        String actionId = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (actionId == null || actionId.isBlank() || getQuickAccessDefinition(actionId) == null) {
            return null;
        }
        return actionId;
    }

    public static boolean isQuickAccessCommandItem(ItemStack itemStack) {
        return getQuickAccessActionId(itemStack) != null;
    }

    private static boolean executeQuickAccessAction(Player player, String actionId) {
        if (player == null || actionId == null || actionId.isBlank()) {
            return false;
        }
        switch (actionId) {
            case "menu_quest" -> openQuestMenu(player);
            case "menu_automation" -> openAutomationMenu(player);
            case "menu_socials" -> openSocialsMenu(player);
            case "menu_scoreboard" -> openScoreboardMenu(player);
            case "upgrade_limit" -> handleUpgradeMenuClick(player, LIMIT_UPGRADE_SLOT);
            case "upgrade_egg_speed" -> handleUpgradeMenuClick(player, EGG_SPEED_UPGRADE_SLOT);
            case "upgrade_wool" -> handleUpgradeMenuClick(player, WOOL_REGEN_UPGRADE_SLOT);
            case "upgrade_tier_chance" -> handleUpgradeMenuClick(player, HIGHER_TIER_CHANCE_UPGRADE_SLOT);
            case "quest_lucky_burst" -> handleQuestMenuClick(player, QUEST_ABILITY_LUCKY_BURST_SLOT);
            case "quest_merge_assist" -> handleQuestMenuClick(player, QUEST_ABILITY_AUTO_MERGE_SLOT);
            case "quest_shear_all" -> handleQuestMenuClick(player, QUEST_ABILITY_AUTO_SHEAR_SLOT);
            case "automation_toggle_auto_buy" ->
                handleAutomationMenuClick(player, AUTOMATION_AUTO_BUY_TOGGLE_SLOT, false);
            case "automation_toggle_auto_ability" ->
                handleAutomationMenuClick(player, AUTOMATION_AUTO_ABILITY_TOGGLE_SLOT, false);
            case "automation_toggle_auto_merge" ->
                handleAutomationMenuClick(player, AUTOMATION_SLOW_AUTO_MERGE_TOGGLE_SLOT, false);
            case "automation_toggle_auto_spawn" ->
                handleAutomationMenuClick(player, AUTOMATION_AUTO_SPAWN_TOGGLE_SLOT, false);
            case "automation_toggle_auto_prestige" ->
                handleAutomationMenuClick(player, AUTOMATION_AUTO_PRESTIGE_TOGGLE_SLOT, false);
            case "automation_enable_all" -> handleAutomationMenuClick(player, AUTOMATION_ENABLE_ALL_SLOT, false);
            case "automation_disable_all" ->
                handleAutomationMenuClick(player, AUTOMATION_DISABLE_ALL_SLOT, false);
            default -> {
                return false;
            }
        }
        return true;
    }

    public static boolean tryUseQuickAccessItem(Player player, ItemStack itemStack) {
        if (player == null || itemStack == null || !isSheepFarmWorld(player.getWorld())) {
            return false;
        }
        String actionId = getQuickAccessActionId(itemStack);
        if (actionId == null) {
            return false;
        }
        if (!isInventoryQuickAccessCastingEnabled(player)) {
            return true;
        }
        return executeQuickAccessAction(player, actionId);
    }

    public static boolean needsFarmLayoutBootstrap(World world) {
        if (world == null || (!isSheepFarmWorld(world) && !isFarmBuildWorld(world))) {
            return false;
        }
        return world.getBlockAt(0, FARM_MIN_Y, 0).getType().isAir()
                || world.getBlockAt(0, FARM_BASE_Y, 0).getType().isAir();
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
        BossBar bar = SheepRuntimeUiState.visitFarmBossBars().get(playerId);
        if (bar == null) {
            bar = Bukkit.createBossBar("Visiting Farm", BarColor.BLUE, BarStyle.SOLID);
            SheepRuntimeUiState.visitFarmBossBars().put(playerId, bar);
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
        BossBar bar = SheepRuntimeUiState.visitFarmBossBars().remove(player.getUniqueId());
        if (bar == null) {
            return;
        }
        bar.removeAll();
        bar.setVisible(false);
    }

    public static boolean isFarmVisitable(UUID ownerId) {
        return SheepVisitAccessState.isFarmVisitable(ownerId);
    }

    public static boolean toggleFarmVisitable(Player owner) {
        boolean next = SheepVisitAccessState.toggleFarmVisitable(owner);
        if (owner == null) {
            return false;
        }
        saveData();
        return next;
    }

    public static boolean areSoundEffectsEnabled(Player player) {
        return SheepEffectPreferences.areSoundEffectsEnabled(player);
    }

    public static boolean toggleSoundEffects(Player player) {
        boolean next = SheepEffectPreferences.toggleSoundEffects(player);
        if (player == null) {
            return false;
        }
        saveData();
        return next;
    }

    public static boolean areSheepSoundsEnabled(Player player) {
        return SheepEffectPreferences.areSheepSoundsEnabled(player);
    }

    public static boolean toggleSheepSounds(Player player) {
        boolean next = SheepEffectPreferences.toggleSheepSounds(player);
        if (player == null) {
            return false;
        }
        saveData();
        return next;
    }

    public static boolean areParticleEffectsEnabled(Player player) {
        return SheepEffectPreferences.areParticleEffectsEnabled(player);
    }

    public static boolean toggleParticleEffects(Player player) {
        boolean next = SheepEffectPreferences.toggleParticleEffects(player);
        if (player == null) {
            return false;
        }
        saveData();
        return next;
    }

    public static Set<UUID> getBlockedFarmVisitors(UUID ownerId) {
        return SheepVisitAccessState.getBlockedFarmVisitors(ownerId);
    }

    public static boolean isFarmVisitorBlocked(UUID ownerId, UUID visitorId) {
        return SheepVisitAccessState.isFarmVisitorBlocked(ownerId, visitorId);
    }

    public static boolean isFarmVisitorBlocked(Player owner, Player visitor) {
        return SheepVisitAccessState.isFarmVisitorBlocked(owner, visitor);
    }

    public static boolean toggleFarmVisitorBlocked(Player owner, UUID visitorId) {
        if (owner == null || visitorId == null || visitorId.equals(owner.getUniqueId())) {
            return false;
        }
        boolean nextBlocked = SheepVisitAccessState.toggleFarmVisitorBlocked(owner, visitorId);
        saveData();
        return nextBlocked;
    }

    public static int getBlockedFarmVisitorCount(UUID ownerId) {
        return SheepVisitAccessState.getBlockedFarmVisitorCount(ownerId);
    }

    public static boolean shouldNotifySpawnLimit(Player player) {
        if (player == null) {
            return false;
        }
        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();
        long last = SheepRuntimeUiState.lastSpawnLimitWarnings().getOrDefault(playerId, 0L);
        if (now - last < SPAWN_LIMIT_WARNING_COOLDOWN_MS) {
            return false;
        }
        SheepRuntimeUiState.lastSpawnLimitWarnings().put(playerId, now);
        return true;
    }

    public static boolean shouldNotifyOutOfEggs(Player player) {
        if (player == null) {
            return false;
        }
        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();
        long last = SheepRuntimeUiState.lastOutOfEggWarnings().getOrDefault(playerId, 0L);
        if (now - last < OUT_OF_EGGS_WARNING_COOLDOWN_MS) {
            return false;
        }
        SheepRuntimeUiState.lastOutOfEggWarnings().put(playerId, now);
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
        return player != null && SheepTutorialState.isCompleted(player.getUniqueId());
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
        return SheepTutorialState.isCompleted(playerId) || SheepTutorialState.isBypassed(playerId);
    }

    private static void clearTutorialRuntimeState(UUID playerId) {
        if (playerId == null) {
            return;
        }
        SheepTutorialState.clearRuntimeState(playerId);
    }

    private static void resetTutorialProgress(UUID playerId) {
        if (playerId == null) {
            return;
        }
        savedTutorialSheepByPlayer.remove(playerId);
        SheepTutorialState.resetPlayer(playerId);
    }

    public static int getPrestigeLevel(Player player) {
        return player == null ? 0 : SheepPrestigeState.getLevel(player.getUniqueId());
    }

    public static int getPrestigePoints(Player player) {
        return player == null ? 0 : SheepPrestigeState.getPoints(player.getUniqueId());
    }

    public static int getPrestigeMaxLevel() {
        return PRESTIGE_MAX_LEVEL;
    }

    public static String formatPoints(long points) {
        return SheepFormatting.formatPoints(points);
    }

    public static String formatPoints(BigInteger points) {
        return SheepFormatting.formatPoints(points);
    }

    public static int getQuestPoints(Player player) {
        return player == null ? 0 : SheepQuestState.questPoints().getOrDefault(player.getUniqueId(), 10);
    }

    public static int getRebirthLevel(Player player) {
        return player == null ? 0 : SheepRebirthState.getLevel(player.getUniqueId());
    }

    public static int getRebirthPoints(Player player) {
        return player == null ? 0 : SheepRebirthState.getPoints(player.getUniqueId());
    }

    public static int getUnspentRebirthPointsDisplay(Player player) {
        return getUnspentRebirthPoints(player);
    }

    public static int getRebirthNextCostInPrestigeLevels(Player player) {
        return getRebirthCostInPrestigeLevels(getRebirthLevel(player));
    }

    public static int getAffordableRebirthLevelsDisplay(Player player) {
        return getAffordableRebirthLevels(player);
    }

    private static int getRebirthSkillUnlockMask(UUID playerId) {
        if (playerId == null) {
            return 0;
        }
        return SheepRebirthState.getSkillUnlockMask(playerId);
    }

    private static int getRebirthSkillPendingMask(UUID playerId) {
        if (playerId == null) {
            return 0;
        }
        return SheepRebirthState.getSkillPendingMask(playerId);
    }

    private static int getRebirthSkillBit(int skillId) {
        if (skillId <= 0 || skillId > REBIRTH_SKILL_NODES.size()) {
            return 0;
        }
        return 1 << (skillId - 1);
    }

    private static boolean hasRebirthSkill(Player player, int skillId) {
        return player != null && hasRebirthSkill(player.getUniqueId(), skillId);
    }

    private static boolean hasRebirthSkill(UUID playerId, int skillId) {
        if (playerId == null) {
            return false;
        }
        int bit = getRebirthSkillBit(skillId);
        return bit != 0 && (getRebirthSkillUnlockMask(playerId) & bit) != 0;
    }

    private static boolean hasActiveRebirthSkill(UUID playerId, int skillId) {
        return hasRebirthSkill(playerId, skillId);
    }

    private static RebirthSkillNode getRebirthSkillNode(int skillId) {
        for (RebirthSkillNode node : REBIRTH_SKILL_NODES) {
            if (node.id == skillId) {
                return node;
            }
        }
        return null;
    }

    private static int getRebirthSkillCost(RebirthSkillNode node) {
        if (node == null) {
            return Integer.MAX_VALUE;
        }
        return Math.max(1, REBIRTH_SKILL_ROOT_COST + (node.layer - 1));
    }

    private static int getUnspentRebirthPoints(Player player) {
        if (player == null) {
            return 0;
        }
        UUID playerId = player.getUniqueId();
        int spent = 0;
        int mask = getRebirthSkillUnlockMask(playerId);
        for (RebirthSkillNode node : REBIRTH_SKILL_NODES) {
            if ((mask & getRebirthSkillBit(node.id)) != 0) {
                spent += getRebirthSkillCost(node);
            }
        }
        return Math.max(0, getRebirthPoints(player) - spent);
    }

    public static long getRebirthRespecRemainingMs(Player player) {
        if (player == null) {
            return 0L;
        }
        long nextRespec = SheepRebirthState.getNextRespecTimestamp(player.getUniqueId());
        return Math.max(0L, nextRespec - System.currentTimeMillis());
    }

    private static int getRebirthCostInPrestigeLevels(int rebirthLevel) {
        return Math.max(REBIRTH_PRESTIGE_LEVEL_COST_STEP,
                (Math.max(0, rebirthLevel) + 1) * REBIRTH_PRESTIGE_LEVEL_COST_STEP);
    }

    private static int getAffordableRebirthLevels(Player player) {
        if (player == null) {
            return 0;
        }
        int remainingPrestigeLevels = Math.max(0, getPrestigeLevel(player));
        int rebirthLevel = Math.max(0, getRebirthLevel(player));
        int affordable = 0;
        while (remainingPrestigeLevels > 0) {
            int cost = getRebirthCostInPrestigeLevels(rebirthLevel + affordable);
            if (remainingPrestigeLevels < cost) {
                break;
            }
            remainingPrestigeLevels -= cost;
            affordable++;
        }
        return affordable;
    }

    private static int getRebirthPointsRewardForNextLevels(int currentRebirth, int levels) {
        int total = 0;
        for (int i = 1; i <= Math.max(0, levels); i++) {
            total += currentRebirth + i;
        }
        return Math.max(0, total);
    }

    private static int getPointsGainMultiplierFromRebirthSkills(Player player) {
        int multiplier = 1;
        if (hasRebirthSkill(player, REBIRTH_SKILL_POINTS_X10_ROOT)) {
            multiplier *= 10;
        }
        if (hasRebirthSkill(player, REBIRTH_SKILL_POINTS_X10_LEFT)) {
            multiplier *= 10;
        }
        int unspent = getUnspentRebirthPoints(player);
        if (unspent > 0) {
            multiplier *= (1 + unspent);
        }
        return Math.max(1, multiplier);
    }

    private static int getQuestPointsGainMultiplierFromRebirthSkills(Player player) {
        return player != null && hasActiveRebirthSkill(player.getUniqueId(), REBIRTH_SKILL_QUEST_POINTS_X10) ? 10 : 1;
    }

    private static int getSacrificePointsGainMultiplierFromRebirthSkills(Player player) {
        return player != null && hasActiveRebirthSkill(player.getUniqueId(), REBIRTH_SKILL_SACRIFICE_POINTS_X10)
                ? 10
                : 1;
    }

    private static boolean hasQuestMaster(Player player) {
        return player != null && hasActiveRebirthSkill(player.getUniqueId(), REBIRTH_SKILL_QUEST_MASTER);
    }

    private static int getQuestTarget(Player player, int baseTarget) {
        int effectiveBase = Math.max(1, baseTarget);
        return hasQuestMaster(player) ? effectiveBase * 2 : effectiveBase;
    }

    private static int getQuestReward(Player player, int baseReward) {
        int effectiveBase = Math.max(1, baseReward);
        return hasQuestMaster(player) ? effectiveBase * 2 : effectiveBase;
    }

    private static void addQuestPoints(Player player, int amount) {
        if (player == null || amount <= 0) {
            return;
        }
        int boosted = Math.max(1, amount) * getQuestPointsGainMultiplierFromRebirthSkills(player);
        UUID playerId = player.getUniqueId();
        SheepQuestState.questPoints().put(playerId, addSaturated(getQuestPoints(player), boosted));
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
        SheepQuestState.questPoints().put(player.getUniqueId(), current - amount);
        saveData();
        return true;
    }

    private static long getQuestResetIntervalMs(Player player) {
        if (hasQuestMaster(player)) {
            return 150_000L;
        }
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
        long nextReset = SheepQuestState.nextQuestResetTimestamps().getOrDefault(playerId, 0L);
        long interval = getQuestResetIntervalMs(player);
        if (nextReset > now + interval) {
            nextReset = now + interval;
            SheepQuestState.nextQuestResetTimestamps().put(playerId, nextReset);
        }
        if (nextReset <= 0L) {
            SheepQuestState.nextQuestResetTimestamps().put(playerId, now + interval);
            return;
        }
        if (now < nextReset) {
            return;
        }

        SheepQuestState.questShears().put(playerId, 0);
        SheepQuestState.questSpawns().put(playerId, 0);
        SheepQuestState.questMerges().put(playerId, 0);
        SheepQuestState.questShearsComplete().put(playerId, false);
        SheepQuestState.questSpawnsComplete().put(playerId, false);
        SheepQuestState.questMergesComplete().put(playerId, false);
        SheepQuestState.nextQuestResetTimestamps().put(playerId, now + interval);
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
        long startedAt = SheepTutorialState.ensureStartedAt(playerId, now);
        if (now - startedAt < TUTORIAL_REMINDER_DELAY_MS) {
            return;
        }

        long lastReminder = SheepTutorialState.getLastReminderTimestamp(playerId);
        if (now - lastReminder < TUTORIAL_REMINDER_REPEAT_MS) {
            return;
        }

        SheepTutorialState.setLastReminderTimestamp(playerId, now);
        player.sendMessage(warning("Finish the tutorial to unlock your farm."));
        player.sendMessage(hint("Next: " + getTutorialNextStepLine(player)));
    }

    private static void tickTutorialTaskTitle(Player player, long now) {
        if (player == null) {
            return;
        }
        UUID playerId = player.getUniqueId();
        String titleStep = getTutorialNextStepLine(player);

        String previousStep = SheepTutorialState.getLastTaskTitleStep(playerId);
        long lastShownAt = SheepTutorialState.getLastTaskTitleTimestamp(playerId);
        boolean stepChanged = !titleStep.equals(previousStep);
        if (!stepChanged && now - lastShownAt < TUTORIAL_TASK_TITLE_REPEAT_MS) {
            return;
        }

        sendTutorialTitle(player, "&eTutorial Step", "&f" + titleStep);
        SheepTutorialState.setLastTaskTitleTimestamp(playerId, now);
        SheepTutorialState.setLastTaskTitleStep(playerId, titleStep);
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
        updateQuestProgress(player, SheepQuestState.questShears(), SheepQuestState.questShearsComplete(),
                getQuestTarget(player, QUEST_SHEARS_TARGET),
                getQuestReward(player, QUEST_SHEARS_REWARD),
                "Shearing quest complete", Sound.ENTITY_PLAYER_LEVELUP);
    }

    public static void recordQuestSpawn(Player player) {
        updateQuestProgress(player, SheepQuestState.questSpawns(), SheepQuestState.questSpawnsComplete(),
                getQuestTarget(player, QUEST_SPAWNS_TARGET),
                getQuestReward(player, QUEST_SPAWNS_REWARD),
                "Spawning quest complete", Sound.ENTITY_PLAYER_LEVELUP);
    }

    public static void recordQuestMerge(Player player) {
        updateQuestProgress(player, SheepQuestState.questMerges(), SheepQuestState.questMergesComplete(),
                getQuestTarget(player, QUEST_MERGES_TARGET),
                getQuestReward(player, QUEST_MERGES_REWARD),
                "Merging quest complete", Sound.ENTITY_PLAYER_LEVELUP);
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
        spawnParticle(player,
                org.bukkit.Particle.VILLAGER_HAPPY,
                player.getLocation().add(0, 1.0, 0),
                14,
                0.35,
                0.4,
                0.35,
                0.02);
        if (areAllQuestsCompleted(playerId)) {
            SheepLifetimeProgressState.incrementCompletedQuestCycles(playerId);
            player.sendTitle(color("&aAll Quests Complete"), color("&7Nice cycle. New quests on reset."), 10, 45, 10);
            playSound(player, Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.9f, 1.2f);
            player.sendMessage(action("All current quests are completed."));
        }
    }

    private static boolean areAllQuestsCompleted(UUID playerId) {
        if (playerId == null) {
            return false;
        }
        return SheepQuestState.questShearsComplete().getOrDefault(playerId, false)
                && SheepQuestState.questSpawnsComplete().getOrDefault(playerId, false)
                && SheepQuestState.questMergesComplete().getOrDefault(playerId, false);
    }

    private static AchievementDefinition getAchievementDefinition(String achievementId) {
        if (achievementId == null || achievementId.isBlank()) {
            return null;
        }
        for (AchievementDefinition definition : ACHIEVEMENT_DEFINITIONS) {
            if (definition.id.equals(achievementId)) {
                return definition;
            }
        }
        return null;
    }

    private static String normalizeAchievementId(String achievementId) {
        if (achievementId == null) {
            return "";
        }
        return achievementId.trim().toLowerCase(Locale.ROOT);
    }

    public static List<String> getAchievementIds() {
        return ACHIEVEMENT_DEFINITIONS.stream()
                .map(definition -> definition.id)
                .toList();
    }

    public static String getAchievementDisplayName(String achievementId) {
        AchievementDefinition definition = getAchievementDefinition(normalizeAchievementId(achievementId));
        return definition == null ? null : definition.name;
    }

    public static boolean isAchievementUnlocked(Player player, String achievementId) {
        if (player == null) {
            return false;
        }
        String normalized = normalizeAchievementId(achievementId);
        if (normalized.isBlank()) {
            return false;
        }
        return getUnlockedAchievementIds(player.getUniqueId()).contains(normalized);
    }

    public static boolean adminCompleteAchievement(Player player, String achievementId, boolean notify) {
        if (player == null) {
            return false;
        }
        String normalized = normalizeAchievementId(achievementId);
        AchievementDefinition definition = getAchievementDefinition(normalized);
        if (definition == null) {
            return false;
        }

        UUID playerId = player.getUniqueId();
        Set<String> unlockedAchievements = getOrCreateUnlockedAchievementIds(playerId);
        if (!unlockedAchievements.add(definition.id)) {
            return true;
        }

        grantAchievementAutomationPoints(playerId, definition.achievementPoints);

        if (notify) {
            notifyAchievementUnlocked(player, definition, getAchievementPoints(playerId));
        }
        saveData();
        evaluateAchievementProgress(player, notify);
        return true;
    }

    public static int adminCompleteAllAchievements(Player player, boolean notify) {
        if (player == null) {
            return 0;
        }

        UUID playerId = player.getUniqueId();
        Set<String> unlockedAchievements = getOrCreateUnlockedAchievementIds(playerId);
        int unlockedCount = 0;
        for (AchievementDefinition definition : ACHIEVEMENT_DEFINITIONS) {
            if (!unlockedAchievements.add(definition.id)) {
                continue;
            }
            unlockedCount++;
            grantAchievementAutomationPoints(playerId, definition.achievementPoints);
            if (notify) {
                notifyAchievementUnlocked(player, definition, getAchievementPoints(playerId));
            }
        }

        if (unlockedCount > 0) {
            saveData();
            evaluateAchievementProgress(player, notify);
        }
        return unlockedCount;
    }

    private static AchievementMilestoneDefinition getAchievementMilestoneDefinition(String milestoneId) {
        if (milestoneId == null || milestoneId.isBlank()) {
            return null;
        }
        for (AchievementMilestoneDefinition definition : ACHIEVEMENT_MILESTONE_DEFINITIONS) {
            if (definition.id.equals(milestoneId)) {
                return definition;
            }
        }
        return null;
    }

    private static Set<String> getUnlockedAchievementIds(UUID playerId) {
        return SheepAchievementState.getUnlockedAchievementIds(playerId);
    }

    private static Set<String> getOrCreateUnlockedAchievementIds(UUID playerId) {
        return SheepAchievementState.getOrCreateUnlockedAchievementIds(playerId);
    }

    private static Set<String> getUnlockedAchievementMilestoneIds(UUID playerId) {
        return SheepAchievementState.getUnlockedAchievementMilestoneIds(playerId);
    }

    private static Set<String> getOrCreateUnlockedAchievementMilestoneIds(UUID playerId) {
        return SheepAchievementState.getOrCreateUnlockedAchievementMilestoneIds(playerId);
    }

    private static int getUnlockedAchievementCount(UUID playerId) {
        return getUnlockedAchievementIds(playerId).size();
    }

    private static int getAchievementPoints(UUID playerId) {
        if (playerId == null) {
            return 0;
        }
        Set<String> unlocked = getUnlockedAchievementIds(playerId);
        int total = 0;
        for (AchievementDefinition definition : ACHIEVEMENT_DEFINITIONS) {
            if (unlocked.contains(definition.id)) {
                total = addSaturated(total, definition.achievementPoints);
            }
        }
        return total;
    }

    public static int getAchievementPoints(Player player) {
        return player == null ? 0 : getAchievementPoints(player.getUniqueId());
    }

    private static int getNextAchievementMilestoneTarget(int achievementPoints) {
        int points = Math.max(0, achievementPoints);
        for (AchievementMilestoneDefinition milestone : ACHIEVEMENT_MILESTONE_DEFINITIONS) {
            if (points < milestone.requiredPoints) {
                return milestone.requiredPoints;
            }
        }
        return 0;
    }

    private static int getAchievementPointMultiplier(Player player) {
        if (player == null) {
            return 1;
        }
        return Math.max(1,
                getAchievementMilestoneMultiplier(player.getUniqueId(), AchievementMilestoneRewardType.POINTS));
    }

    private static double getAchievementWoolRegenSpeedMultiplier(Player player) {
        return player == null ? 1.0D : getAchievementWoolRegenSpeedMultiplier(player.getUniqueId());
    }

    private static double getAchievementWoolRegenSpeedMultiplier(UUID playerId) {
        return Math.max(1.0D, getAchievementMilestoneMultiplier(playerId, AchievementMilestoneRewardType.WOOL_REGEN));
    }

    private static int getAchievementMilestoneMultiplier(UUID playerId, AchievementMilestoneRewardType rewardType) {
        if (playerId == null || rewardType == null) {
            return 1;
        }
        long multiplier = 1L;
        Set<String> unlockedMilestones = getUnlockedAchievementMilestoneIds(playerId);
        for (AchievementMilestoneDefinition milestone : ACHIEVEMENT_MILESTONE_DEFINITIONS) {
            if (!unlockedMilestones.contains(milestone.id) || milestone.rewardType != rewardType) {
                continue;
            }
            multiplier *= Math.max(1, milestone.rewardMultiplier);
            if (multiplier >= Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            }
        }
        return (int) multiplier;
    }

    private static void grantAchievementAutomationPoints(UUID playerId, int amount) {
        if (playerId == null || amount <= 0) {
            return;
        }
        SheepAutomationState.setPoints(playerId,
                addSaturated(SheepAutomationState.getPoints(playerId), amount));
        SheepAchievementState.setAutomationPointsGranted(playerId,
                addSaturated(SheepAchievementState.getAutomationPointsGranted(playerId), amount));
    }

    private static void reconcileAchievementAutomationPointGrants() {
        for (UUID playerId : SheepAchievementState.getTrackedPlayerIds()) {
            if (playerId == null) {
                continue;
            }
            int unlockedPoints = getAchievementPoints(playerId);
            int grantedPoints = SheepAchievementState.getAutomationPointsGranted(playerId);

            if (unlockedPoints <= 0) {
                SheepAchievementState.removeAutomationPointsGranted(playerId);
                continue;
            }

            if (grantedPoints < unlockedPoints) {
                int missingPoints = unlockedPoints - grantedPoints;
                SheepAutomationState.setPoints(playerId,
                        addSaturated(SheepAutomationState.getPoints(playerId), missingPoints));
            }

            SheepAchievementState.setAutomationPointsGranted(playerId, unlockedPoints);
        }
    }

    private static void applyAchievementWoolRegenBonusToActiveCooldowns(Player player, double oldMultiplier,
            double newMultiplier) {
        if (player == null || plugin == null || oldMultiplier <= 0.0D || newMultiplier <= oldMultiplier) {
            return;
        }

        double ratio = oldMultiplier / newMultiplier;
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

    private static boolean hasMetAchievementCondition(Player player, UUID playerId, String achievementId) {
        if (player == null || playerId == null || achievementId == null) {
            return false;
        }
        return switch (achievementId) {
            case "first_shear" -> SheepLifetimeProgressState.getLifetimeShears(playerId) >= 10;
            case "wool_tycoon" -> SheepLifetimeProgressState.getLifetimeShears(playerId) >= 1000;
            case "first_hatch" -> SheepLifetimeProgressState.getLifetimeSpawns(playerId) >= 10;
            case "breeder" -> SheepLifetimeProgressState.getLifetimeSpawns(playerId) >= 2000;
            case "pair_maker" -> SheepLifetimeProgressState.getLifetimeMerges(playerId) >= 250;
            case "fusion_engine" -> SheepLifetimeProgressState.getLifetimeMerges(playerId) >= 2500;
            case "quest_cadet" -> SheepLifetimeProgressState.getCompletedQuestCycles(playerId) >= 3;
            case "quest_veteran" -> SheepLifetimeProgressState.getCompletedQuestCycles(playerId) >= 15;
            case "upgrade_mechanic" -> (SheepEconomyState.getExtraLimit(playerId)
                    + SheepEconomyState.getEggSpeedLevel(playerId)
                    + SheepEconomyState.getWoolRegenLevel(playerId)
                    + SheepEconomyState.getHigherTierChanceLevel(playerId)) >= 20;
            case "shear_specialist" -> getShearShopLevel(player) >= 15;
            case "prestige_initiate" -> SheepPrestigeState.getTotalLevelsEarned(playerId) >= 3;
            case "prestige_veteran" -> SheepPrestigeState.getTotalLevelsEarned(playerId) >= 100;
            case "automation_online" -> getUnlockedAutomationCount(player) >= 2;
            case "automation_matrix" -> getUnlockedAutomationCount(player) >= 6;
            case "sacrifice_initiate" -> SheepSacrificeProgression.getTotalUnlocksPurchased(playerId) >= 2;
            case "sacrifice_mastery" -> getSacrificeUnlocksBought(playerId) >= SACRIFICE_UNLOCK_MAX;
            case "reborn" -> getRebirthLevel(player) >= 3;
            case "rainbow_ascension" -> SheepUpgradeState.getHighestAnnouncedRainbowTier(playerId) >= 4;
            case "tutorial_mastery" -> SheepTutorialState.isCompleted(playerId);
            case "layout_designer" -> getScoreboardLayoutMode(player) == 1
                    && getInventoryQuickAccessActions(playerId).size() >= INVENTORY_QUICK_ACCESS_MAX_ITEMS
                    && SheepRuntimeUiState.socialsPages().containsKey(playerId);
            case "socials_explorer" -> SheepLifetimeProgressState.getLifetimeOtherFarmVisits(playerId) >= 1;
            case "quick_access_curator" ->
                getInventoryQuickAccessActions(playerId).size() >= INVENTORY_QUICK_ACCESS_MAX_ITEMS
                        && isInventoryQuickAccessCastingEnabled(playerId);
            case "quest_engineer" ->
                getQuestUpgradeDurationLevel(player) >= 10 && getQuestUpgradePowerLevel(player) >= 10;
            case "combo_champion" -> getComboMaxUpgradeLevel(player) >= 15;
            case "egg_cap_collector" -> getPrestigeEggCapLevel(player) >= 10;
            case "spawn_architect" -> getBaseSpawnTierLevel(player) >= 8;
            case "prestige_planner" -> getPrestigeQuestRewardLevel(player) >= 18;
            case "automation_specialist" -> getUnlockedAutomationCount(player) >= 5;
            case "rebirth_architect" -> getRebirthLevel(player) >= 10;
            case "sheep_limit_master" -> getPlayerLimit(player) >= getMaxSheepLimit(playerId);
            case "wool_guardian" -> getWoolRegenLevel(player) >= getWoolRegenMaxLevel(player);
            case "secret_author_online" -> {
                if (SOCIALS_AUTHOR_UUID.equals(playerId)) {
                    yield true;
                }
                Player author = Bukkit.getPlayer(SOCIALS_AUTHOR_UUID);
                yield author != null
                        && author.isOnline()
                        && !SOCIALS_AUTHOR_UUID.equals(playerId)
                        && isSheepFarmWorld(player.getWorld())
                        && isSheepFarmWorld(author.getWorld());
            }
            case "secret_owner_farm" -> SOCIALS_AUTHOR_UUID.equals(playerId)
                    || SheepLifetimeProgressState.hasVisitedOwnerFarm(playerId);
            default -> false;
        };
    }

    private static void notifyAchievementUnlocked(Player player, AchievementDefinition definition, int totalPoints) {
        if (player == null || definition == null) {
            return;
        }
        player.sendTitle(
                color("&6Achievement Unlocked"),
                color("&f" + definition.name),
                5,
                50,
                10);
        player.sendMessage(action("Achievement unlocked: " + definition.name
                + " &7(" + definition.reward + ", total " + totalPoints + " AP)"));
        playSound(player, Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.9f, 1.2f);
    }

    private static void notifyAchievementMilestoneUnlocked(Player player,
            AchievementMilestoneDefinition milestone,
            int totalPoints) {
        if (player == null || milestone == null) {
            return;
        }
        player.sendTitle(
                color("&bAchievement Milestone"),
                color("&f" + milestone.name),
                5,
                55,
                10);
        player.sendMessage(action("Milestone unlocked: " + milestone.name
                + " &7(" + milestone.reward + ", " + milestone.requiredPoints + " AP reached, total " + totalPoints
                + " AP)"));
        playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 0.9f, 1.25f);
        if ("points_26".equals(milestone.id)) {
            notifyCommandBlockMilestoneServerwide(player);
        }
    }

    private static void notifyCommandBlockMilestoneServerwide(Player unlockedBy) {
        if (unlockedBy == null || plugin == null) {
            return;
        }
        String playerName = unlockedBy.getName() == null ? "Unknown" : unlockedBy.getName();
        String message = color("&d" + playerName + " &7unlocked the &5Command Block &7achievement milestone!");
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online == null) {
                continue;
            }
            online.sendMessage(message);
            if (!isSheepFarmWorld(online.getWorld())) {
                continue;
            }
            online.playSound(online.getLocation(), Sound.ENTITY_ENDER_DRAGON_DEATH, 0.8f, 1.0f);
        }
    }

    private static void evaluateAchievementProgress(Player player, boolean notify) {
        if (player == null) {
            return;
        }
        UUID playerId = player.getUniqueId();
        Set<String> unlockedAchievements = getOrCreateUnlockedAchievementIds(playerId);
        Set<String> unlockedMilestones = getOrCreateUnlockedAchievementMilestoneIds(playerId);
        double previousWoolMultiplier = getAchievementWoolRegenSpeedMultiplier(playerId);
        boolean changed = false;

        for (AchievementDefinition definition : ACHIEVEMENT_DEFINITIONS) {
            if (unlockedAchievements.contains(definition.id)
                    || !hasMetAchievementCondition(player, playerId, definition.id)) {
                continue;
            }
            unlockedAchievements.add(definition.id);
            grantAchievementAutomationPoints(playerId, definition.achievementPoints);
            changed = true;
            if (notify) {
                notifyAchievementUnlocked(player, definition, getAchievementPoints(playerId));
            }
        }

        int achievementPoints = getAchievementPoints(playerId);
        for (AchievementMilestoneDefinition milestone : ACHIEVEMENT_MILESTONE_DEFINITIONS) {
            if (achievementPoints < milestone.requiredPoints || unlockedMilestones.contains(milestone.id)) {
                continue;
            }
            unlockedMilestones.add(milestone.id);
            changed = true;
            if (notify) {
                notifyAchievementMilestoneUnlocked(player, milestone, achievementPoints);
            }
        }

        double newWoolMultiplier = getAchievementWoolRegenSpeedMultiplier(playerId);
        if (newWoolMultiplier > previousWoolMultiplier) {
            applyAchievementWoolRegenBonusToActiveCooldowns(player, previousWoolMultiplier, newWoolMultiplier);
        }

        if (changed) {
            saveData();
        }
    }

    public static void evaluateAuthorOnlineSecretForOnlinePlayers() {
        if (plugin == null) {
            return;
        }
        Player author = Bukkit.getPlayer(SOCIALS_AUTHOR_UUID);
        if (author == null || !author.isOnline()) {
            return;
        }
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online == null || !online.isOnline()) {
                continue;
            }
            evaluateAchievementProgress(online, true);
        }
    }

    public static void tickActiveAbilities(Player player) {
        if (player == null || !isSheepFarmWorld(player.getWorld())) {
            return;
        }
        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();

        tickAbilityVisual(player, playerId, now, SheepQuestState.activeWoolRushUntil(), org.bukkit.Particle.CLOUD,
                "Wool Rush ended");
        tickAbilityVisual(player, playerId, now, SheepQuestState.activeJackpotShearsUntil(), org.bukkit.Particle.CRIT,
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
        tickAutomationAutoPrestige(player, playerId, now);
    }

    public static void tickAutomationAutoSpawnRealtime(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        tickAutomationAutoSpawn(player, player.getUniqueId(), System.currentTimeMillis());
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
        long nextAt = SheepAutomationState.getNextPointAt(playerId);
        if (nextAt <= 0L) {
            SheepAutomationState.setNextPointAt(playerId, now + AUTOMATION_POINT_INTERVAL_MS);
            return;
        }
        if (now < nextAt) {
            return;
        }
        SheepAutomationState.setPoints(playerId, addSaturated(getAutomationPoints(player), 1));
        SheepAutomationState.setNextPointAt(playerId, now + AUTOMATION_POINT_INTERVAL_MS);
        saveData();
    }

    private static void tickAutomationAutoBuy(Player player, UUID playerId, long now) {
        int level = getAutomationAutoBuyUpgradeLevel(player);
        if (level <= 0 || !isAutomationAutoBuyEnabled(player)) {
            return;
        }
        if (level < AUTOMATION_AUTO_BUY_MAX_LEVEL) {
            long interval = getAutomationAutoBuyIntervalMs(player);
            if (interval <= 0L) {
                return;
            }
            long nextAt = SheepAutomationState.getNextAutoBuyAt(playerId);
            if (now < nextAt) {
                return;
            }
            SheepAutomationState.setNextAutoBuyAt(playerId, now + interval);
        } else {
            SheepAutomationState.setNextAutoBuyAt(playerId, 0L);
        }
        if (!canAutomationRun(player, false)) {
            return;
        }
        if (level >= AUTOMATION_AUTO_BUY_MAX_LEVEL) {
            for (int i = 0; i < AUTOMATION_AUTO_BUY_MAX_PURCHASES_PER_TICK; i++) {
                if (!tryAutoBuyOneUpgrade(player)) {
                    break;
                }
            }
            return;
        }
        tryAutoBuyOneUpgrade(player);
    }

    private static void tickAutomationAutoAbility(Player player, UUID playerId, long now) {
        int level = getAutomationAutoAbilityUpgradeLevel(player);
        if (level <= 0 || !isAutomationAutoAbilityEnabled(player)) {
            return;
        }
        if (level < AUTOMATION_AUTO_ABILITY_MAX_LEVEL) {
            if (AUTOMATION_AUTO_ABILITY_INTERVAL_MS <= 0L) {
                return;
            }
            long nextAt = SheepAutomationState.getNextAutoAbilityAt(playerId);
            if (now < nextAt) {
                return;
            }
            SheepAutomationState.setNextAutoAbilityAt(playerId, now + AUTOMATION_AUTO_ABILITY_INTERVAL_MS);
        } else {
            SheepAutomationState.setNextAutoAbilityAt(playerId, 0L);
        }
        if (!canAutomationRun(player, true)) {
            return;
        }
        tryAutoActivateAbility(player, level >= 2);
    }

    private static void tickAutomationSlowMerge(Player player, UUID playerId, long now) {
        long interval = getAutomationSlowAutoMergeIntervalMs(player);
        if (getAutomationSlowAutoMergeUpgradeLevel(player) <= 0 || interval <= 0L
                || !isAutomationSlowAutoMergeEnabled(player)) {
            return;
        }
        long nextAt = SheepAutomationState.getNextSlowMergeAt(playerId);
        if (now < nextAt) {
            return;
        }
        SheepAutomationState.setNextSlowMergeAt(playerId, now + interval);
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
        long nextAt = SheepAutomationState.getNextSlowShearAt(playerId);
        if (now < nextAt) {
            return;
        }
        SheepAutomationState.setNextSlowShearAt(playerId, now + interval);
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
        long nextAt = SheepAutomationState.getNextAutoSpawnAt(playerId);
        if (interval > 0L && now < nextAt) {
            return;
        }
        if (interval > 0L) {
            SheepAutomationState.setNextAutoSpawnAt(playerId, now + interval);
        } else {
            SheepAutomationState.setNextAutoSpawnAt(playerId, now);
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
        if (spawnAutomationSheepFromSky(player)) {
            recordQuestSpawn(player);
            recordTutorialSpawn(player);
        }
    }

    private static void tickAutomationAutoPrestige(Player player, UUID playerId, long now) {
        if (getAutomationAutoPrestigeUpgradeLevel(player) <= 0 || !isAutomationAutoPrestigeEnabled(player)
                || AUTOMATION_AUTO_PRESTIGE_INTERVAL_MS <= 0L) {
            return;
        }
        long nextAt = SheepAutomationState.getNextAutoPrestigeAt(playerId);
        if (now < nextAt) {
            return;
        }
        SheepAutomationState.setNextAutoPrestigeAt(playerId, now + AUTOMATION_AUTO_PRESTIGE_INTERVAL_MS);
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
        BigInteger availablePoints = getPlayerPointsBig(player);
        int selection = getCheapestAutoBuyUpgradeSelection(player, availablePoints);
        return switch (selection) {
            case 1 -> upgradeLimit(player);
            case 2 -> upgradeEggSpeed(player);
            case 3 -> upgradeWoolRegen(player);
            case 4 -> upgradeHigherTierChance(player);
            case 5 -> upgradeComboDecay(player);
            case 6 -> upgradeComboGain(player);
            case 7 -> upgradeShearWoolSave(player);
            case 8 -> upgradeShearTierBoost(player);
            case 9 -> upgradeShearShop(player);
            default -> false;
        };
    }

    private static int getCheapestAutoBuyUpgradeSelection(Player player, BigInteger availablePoints) {
        if (player == null || availablePoints == null || availablePoints.signum() <= 0) {
            return 0;
        }

        int selected = 0;
        BigInteger cheapest = null;

        if (getPlayerLimit(player) < getMaxSheepLimit(player.getUniqueId())) {
            BigInteger cost = getUpgradeCost(player);
            if (canAutoBuyUpgradeNow(player, availablePoints, cost)) {
                selected = 1;
                cheapest = cost;
            }
        }
        if (getEggSpeedLevel(player) < getEggSpeedMaxLevel(player)) {
            BigInteger cost = getEggSpeedUpgradeCost(player);
            if (canAutoBuyUpgradeNow(player, availablePoints, cost)
                    && (cheapest == null || cost.compareTo(cheapest) < 0)) {
                selected = 2;
                cheapest = cost;
            }
        }
        if (getWoolRegenLevel(player) < getWoolRegenMaxLevel(player)) {
            BigInteger cost = getWoolRegenUpgradeCost(player);
            if (canAutoBuyUpgradeNow(player, availablePoints, cost)
                    && (cheapest == null || cost.compareTo(cheapest) < 0)) {
                selected = 3;
                cheapest = cost;
            }
        }
        if (getHigherTierChanceLevel(player) < getHigherTierChanceMaxLevel(player)) {
            BigInteger cost = getHigherTierChanceUpgradeCost(player);
            if (canAutoBuyUpgradeNow(player, availablePoints, cost)
                    && (cheapest == null || cost.compareTo(cheapest) < 0)) {
                selected = 4;
                cheapest = cost;
            }
        }
        if (getComboDecayUpgradeLevel(player) < COMBO_DECAY_MAX_LEVEL) {
            BigInteger cost = getComboDecayUpgradeCost(player);
            if (canAutoBuyUpgradeNow(player, availablePoints, cost)
                    && (cheapest == null || cost.compareTo(cheapest) < 0)) {
                selected = 5;
                cheapest = cost;
            }
        }
        if (getComboGainUpgradeLevel(player) < COMBO_GAIN_MAX_LEVEL) {
            BigInteger cost = getComboGainUpgradeCost(player);
            if (canAutoBuyUpgradeNow(player, availablePoints, cost)
                    && (cheapest == null || cost.compareTo(cheapest) < 0)) {
                selected = 6;
                cheapest = cost;
            }
        }
        if (getShearWoolSaveLevel(player) < SHEAR_WOOL_SAVE_MAX_LEVEL) {
            BigInteger cost = getShearWoolSaveUpgradeCost(player);
            if (canAutoBuyUpgradeNow(player, availablePoints, cost)
                    && (cheapest == null || cost.compareTo(cheapest) < 0)) {
                selected = 7;
                cheapest = cost;
            }
        }
        if (getShearTierBoostLevel(player) < SHEAR_TIER_BOOST_MAX_LEVEL) {
            BigInteger cost = getShearTierBoostUpgradeCost(player);
            if (canAutoBuyUpgradeNow(player, availablePoints, cost)
                    && (cheapest == null || cost.compareTo(cheapest) < 0)) {
                selected = 8;
                cheapest = cost;
            }
        }

        BigInteger shearShopCost = getShearUpgradeCost(player);
        if (canAutoBuyUpgradeNow(player, availablePoints, shearShopCost)
                && (cheapest == null || shearShopCost.compareTo(cheapest) < 0)) {
            selected = 9;
        }
        return selected;
    }

    private static boolean canAutoBuyUpgradeNow(Player player, BigInteger availablePoints, BigInteger cost) {
        return player != null
                && availablePoints != null
                && cost != null
                && cost.signum() > 0
                && availablePoints.compareTo(cost) >= 0
                && canSpendUpgradePointsDuringTutorial(player, cost);
    }

    private static boolean tryAutoActivateAbility(Player player, boolean buyAllMissing) {
        if (player == null) {
            return false;
        }
        UUID playerId = player.getUniqueId();
        boolean changed = false;
        if (getCountAbilityRemainingUses(SheepQuestState.activeAutoShearUses(), playerId) <= 0
                && activateCountQuestAbility(player,
                        SheepQuestState.activeAutoShearUses(),
                        SheepQuestState.autoShearEnabled(),
                        getQuestAutoShearCost(player),
                        getAbilityUseCount(player, QUEST_AUTO_SHEAR_BASE_DURATION_MS),
                        Sound.ENTITY_SHEEP_SHEAR,
                        org.bukkit.Particle.WAX_OFF)) {
            SheepQuestState.nextAutoShearAt().put(playerId, 0L);
            if (!buyAllMissing) {
                return true;
            }
            changed = true;
        }
        if (getCountAbilityRemainingUses(SheepQuestState.activeAutoMergeUses(), playerId) <= 0
                && activateCountQuestAbility(player,
                        SheepQuestState.activeAutoMergeUses(),
                        SheepQuestState.autoMergeEnabled(),
                        getQuestAutoMergeCost(player),
                        getAbilityUseCount(player, QUEST_AUTO_MERGE_BASE_DURATION_MS),
                        Sound.BLOCK_PISTON_EXTEND,
                        org.bukkit.Particle.ENCHANTMENT_TABLE)) {
            SheepQuestState.nextAutoMergeAt().put(playerId, 0L);
            if (!buyAllMissing) {
                return true;
            }
            changed = true;
        }
        if (!isAbilityActive(SheepQuestState.activeJackpotShearsUntil(), playerId)
                && activateQuestAbility(player, SheepQuestState.activeJackpotShearsUntil(), getQuestJackpotCost(player),
                        getAbilityDurationMs(player, QUEST_JACKPOT_SHEARS_BASE_DURATION_MS),
                        Sound.ENTITY_PLAYER_LEVELUP, org.bukkit.Particle.CRIT)) {
            if (!buyAllMissing) {
                return true;
            }
            changed = true;
        }
        if (!isAbilityActive(SheepQuestState.activeWoolRushUntil(), playerId)
                && activateQuestAbility(player, SheepQuestState.activeWoolRushUntil(), getQuestWoolRushCost(player),
                        getAbilityDurationMs(player, QUEST_WOOL_RUSH_BASE_DURATION_MS),
                        Sound.ENTITY_ENDER_DRAGON_FLAP, org.bukkit.Particle.CLOUD)) {
            if (!buyAllMissing) {
                return true;
            }
            changed = true;
        }
        if (getCountAbilityRemainingUses(SheepQuestState.activeLuckyBurstUses(), playerId) <= 0
                && activateCountQuestAbility(player,
                        SheepQuestState.activeLuckyBurstUses(),
                        SheepQuestState.luckyBurstEnabled(),
                        getQuestLuckyBurstCost(player),
                        getAbilityUseCount(player, QUEST_LUCKY_BURST_BASE_DURATION_MS),
                        Sound.BLOCK_BEACON_POWER_SELECT,
                        org.bukkit.Particle.END_ROD)) {
            if (!buyAllMissing) {
                return true;
            }
            changed = true;
        }
        return changed;
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

    private static Location createSkySheepSpawnLocation(World world) {
        double min = FARM_MIN_XZ + SHEEP_RAIN_HORIZONTAL_PADDING;
        double max = FARM_MAX_XZ - SHEEP_RAIN_HORIZONTAL_PADDING;
        double x = min + RANDOM.nextDouble() * Math.max(0.01D, max - min);
        double z = min + RANDOM.nextDouble() * Math.max(0.01D, max - min);
        double y = FARM_BASE_Y + SHEEP_RAIN_SPAWN_HEIGHT;
        return new Location(world, x, y, z);
    }

    private static void spawnRainSheep(World world) {
        Location spawnLocation = createSkySheepSpawnLocation(world);

        Sheep sheep = world.spawn(spawnLocation, Sheep.class);
        setSheepTier(sheep, rollSpawnTier(world));
        sheep.setVelocity(new Vector(0.0D, -0.1D, 0.0D));

        spawnParticle(world,
                org.bukkit.Particle.CLOUD,
                spawnLocation.clone().add(0.0D, 0.4D, 0.0D),
                14,
                0.2D,
                0.4D,
                0.2D,
                0.02D);
        playSheepSound(world, spawnLocation, Sound.ENTITY_SHEEP_AMBIENT, 0.7f, 1.5f);
    }

    private static void tickAutoMergeAbility(Player player, UUID playerId, long now) {
        if (!isCountAbilityActive(SheepQuestState.activeAutoMergeUses(), SheepQuestState.autoMergeEnabled(),
                playerId)) {
            SheepQuestState.nextAutoMergeAt().remove(playerId);
        }
    }

    private static void tickAutoShearAbility(Player player, UUID playerId, long now) {
        if (!isCountAbilityActive(SheepQuestState.activeAutoShearUses(), SheepQuestState.autoShearEnabled(),
                playerId)) {
            SheepQuestState.nextAutoShearAt().remove(playerId);
            return;
        }

        long nextAutoShearAt = SheepQuestState.nextAutoShearAt().getOrDefault(playerId, 0L);
        if (now < nextAutoShearAt) {
            return;
        }

        SheepQuestState.nextAutoShearAt().put(playerId, now + 100L);
        tryAutoShearLookTarget(player);
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
        if (!isCountAbilityActive(SheepQuestState.activeAutoMergeUses(), SheepQuestState.autoMergeEnabled(),
                playerId)) {
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
        consumeCountAbilityUse(SheepQuestState.activeAutoMergeUses(), playerId);
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
        spawnParticle(world,
                org.bukkit.Particle.VILLAGER_HAPPY,
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
        spawnParticle(player, particle, player.getLocation().add(0, 2.0, 0), 5, 0.25, 0.35, 0.25, 0.01);
    }

    private static void emitAbilityAura(Player player, UUID playerId, long now) {
        boolean hasActiveAbility = false;
        if (isCountAbilityActive(SheepQuestState.activeLuckyBurstUses(), SheepQuestState.luckyBurstEnabled(),
                playerId)) {
            hasActiveAbility = true;
            spawnParticle(player,
                    org.bukkit.Particle.TOTEM,
                    player.getLocation().add(0.0D, 2.1D, 0.0D),
                    2,
                    0.18D,
                    0.28D,
                    0.18D,
                    0.0D);
        }

        if (isAbilityActive(SheepQuestState.activeWoolRushUntil(), playerId)) {
            hasActiveAbility = true;
            spawnParticle(player,
                    org.bukkit.Particle.SPORE_BLOSSOM_AIR,
                    player.getLocation().add(0.0D, 1.9D, 0.0D),
                    4,
                    0.22D,
                    0.26D,
                    0.22D,
                    0.01D);
        }

        if (isAbilityActive(SheepQuestState.activeJackpotShearsUntil(), playerId)) {
            hasActiveAbility = true;
            spawnParticle(player,
                    org.bukkit.Particle.FIREWORKS_SPARK,
                    player.getLocation().add(0.0D, 2.25D, 0.0D),
                    3,
                    0.25D,
                    0.35D,
                    0.25D,
                    0.01D);
        }

        if (isCountAbilityActive(SheepQuestState.activeAutoMergeUses(), SheepQuestState.autoMergeEnabled(), playerId)) {
            hasActiveAbility = true;
            spawnParticle(player,
                    org.bukkit.Particle.WAX_ON,
                    player.getLocation().add(0.0D, 2.0D, 0.0D),
                    5,
                    0.22D,
                    0.28D,
                    0.22D,
                    0.02D);
        }

        if (isCountAbilityActive(SheepQuestState.activeAutoShearUses(), SheepQuestState.autoShearEnabled(), playerId)) {
            hasActiveAbility = true;
            spawnParticle(player,
                    org.bukkit.Particle.WAX_OFF,
                    player.getLocation().add(0.0D, 2.0D, 0.0D),
                    5,
                    0.22D,
                    0.28D,
                    0.22D,
                    0.02D);
        }

        if (!hasActiveAbility) {
            return;
        }

        long lastSoundAt = SheepQuestState.lastAbilityAuraSoundTimestamps().getOrDefault(playerId, 0L);
        if (now - lastSoundAt < ABILITY_AURA_SOUND_INTERVAL_MS) {
            return;
        }

        SheepQuestState.lastAbilityAuraSoundTimestamps().put(playerId, now);
        Sound[] gentleAuraSounds = {
                Sound.BLOCK_NOTE_BLOCK_CHIME,
                Sound.BLOCK_NOTE_BLOCK_HARP,
                Sound.BLOCK_AMETHYST_BLOCK_CHIME
        };
        playSound(player, gentleAuraSounds[RANDOM.nextInt(gentleAuraSounds.length)], 0.16f, 1.0f);
    }

    public static int getShearShopLevel(Player player) {
        return player == null ? 0 : SheepUpgradeState.getShearShopLevel(player.getUniqueId());
    }

    public static int getShearWoolSaveLevel(Player player) {
        return player == null ? 0 : SheepUpgradeState.getShearWoolSaveLevel(player.getUniqueId());
    }

    public static int getShearTierBoostLevel(Player player) {
        return player == null ? 0 : SheepUpgradeState.getShearTierBoostLevel(player.getUniqueId());
    }

    public static int getShearFlatBonus(Player player) {
        return 0;
    }

    public static int getShearPointGainUpgradeLevel(Player player) {
        return Math.max(1, getShearShopLevel(player) + 1);
    }

    public static int getShearPointMultiplier(Player player) {
        long multiplier = getShearPointGainUpgradeLevel(player);
        return (int) Math.min(Integer.MAX_VALUE, Math.max(1L, multiplier));
    }

    public static BigInteger getShearUpgradeCost(Player player) {
        return getDoubledUpgradeCostBig(scaleRegularPointsUpgradeBaseCost(SHEAR_SHOP_BASE_COST),
                getShearShopLevel(player));
    }

    public static int getShearWoolSaveChancePercent(Player player) {
        return Math.min(SHEAR_WOOL_SAVE_CHANCE_CAP, getShearWoolSaveLevel(player) * SHEAR_WOOL_SAVE_CHANCE_PER_LEVEL);
    }

    public static int getShearTierBoostChancePercent(Player player) {
        return Math.min(SHEAR_TIER_BOOST_CHANCE_CAP,
                getShearTierBoostLevel(player) * SHEAR_TIER_BOOST_CHANCE_PER_LEVEL);
    }

    public static BigInteger getShearWoolSaveUpgradeCost(Player player) {
        return getDoubledUpgradeCostBig(scaleRegularPointsUpgradeBaseCost(SHEAR_WOOL_SAVE_BASE_COST),
                getShearWoolSaveLevel(player));
    }

    public static BigInteger getShearTierBoostUpgradeCost(Player player) {
        return getDoubledUpgradeCostBig(scaleRegularPointsUpgradeBaseCost(SHEAR_TIER_BOOST_BASE_COST),
                getShearTierBoostLevel(player));
    }

    public static int getComboDecayUpgradeLevel(Player player) {
        return player == null ? 0 : SheepComboState.getDecayUpgrade(player.getUniqueId());
    }

    public static int getComboMaxUpgradeLevel(Player player) {
        return player == null ? 0 : SheepComboState.getMaxUpgrade(player.getUniqueId());
    }

    public static int getComboGainUpgradeLevel(Player player) {
        return player == null ? 0 : SheepComboState.getGainUpgrade(player.getUniqueId());
    }

    public static int getAutomationPoints(Player player) {
        return player == null ? 0 : SheepAutomationState.getPoints(player.getUniqueId());
    }

    public static int getAutomationAutoBuyUpgradeLevel(Player player) {
        return player == null ? 0
                : Math.min(AUTOMATION_AUTO_BUY_MAX_LEVEL,
                        Math.max(0, SheepAutomationState.getAutoBuyUpgrade(player.getUniqueId())));
    }

    public static int getAutomationAutoAbilityUpgradeLevel(Player player) {
        return player == null ? 0
                : Math.min(AUTOMATION_AUTO_ABILITY_MAX_LEVEL,
                        Math.max(0, SheepAutomationState.getAutoAbilityUpgrade(player.getUniqueId())));
    }

    public static int getAutomationSlowAutoMergeUpgradeLevel(Player player) {
        return player == null ? 0
                : Math.min(AUTOMATION_SLOW_AUTO_MERGE_MAX_LEVEL,
                        Math.max(0, SheepAutomationState.getSlowAutoMergeUpgrade(player.getUniqueId())));
    }

    public static int getAutomationSlowAutoShearUpgradeLevel(Player player) {
        return player == null ? 0
                : Math.min(AUTOMATION_SLOW_AUTO_SHEAR_MAX_LEVEL,
                        Math.max(0, SheepAutomationState.getSlowAutoShearUpgrade(player.getUniqueId())));
    }

    private static long getAutomationAutoBuyIntervalMs(Player player) {
        int level = getAutomationAutoBuyUpgradeLevel(player);
        if (level <= 0) {
            return AUTOMATION_AUTO_BUY_INTERVAL_MS;
        }
        if (level >= AUTOMATION_AUTO_BUY_MAX_LEVEL) {
            return 0L;
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
                        Math.max(0, SheepAutomationState.getAutoSpawnUpgrade(player.getUniqueId())));
    }

    public static int getAutomationAutoPrestigeUpgradeLevel(Player player) {
        return player == null ? 0
                : Math.min(AUTOMATION_SINGLE_LEVEL_MAX,
                        Math.max(0, SheepAutomationState.getAutoPrestigeUpgrade(player.getUniqueId())));
    }

    public static BigInteger getSacrificePoints(Player player) {
        return SheepSacrificeProgression.getPoints(player);
    }

    private static BigInteger getSacrificePoints(UUID playerId) {
        return SheepSacrificeProgression.getPoints(playerId);
    }

    public static int getSacrificeUnlocksBought(Player player) {
        return SheepSacrificeProgression.getUnlocksBought(player);
    }

    private static int getSacrificeUnlocksBought(UUID playerId) {
        return SheepSacrificeProgression.getUnlocksBought(playerId);
    }

    private static int getSacrificeUnlockMask(UUID playerId) {
        return SheepSacrificeProgression.getUnlockMask(playerId);
    }

    private static boolean isSacrificeUnlockActive(UUID playerId, int unlockId) {
        return SheepSacrificeProgression.isUnlockActive(playerId, unlockId);
    }

    private static String sacrificeUnlockStatusLine(Player player, int unlockId) {
        return SheepSacrificeProgression.getUnlockStatusLine(player, unlockId);
    }

    private static boolean hasSacrificeUnlock(Player player, int unlockId) {
        return SheepSacrificeProgression.hasUnlock(player, unlockId);
    }

    private static boolean hasSacrificeUnlock(UUID playerId, int unlockId) {
        return SheepSacrificeProgression.hasUnlock(playerId, unlockId);
    }

    private static BigInteger getSacrificeUnlockCost(UUID playerId) {
        return SheepSacrificeProgression.getUnlockCost(playerId);
    }

    private static BigInteger getSacrificeUnlockCost(Player player) {
        return SheepSacrificeProgression.getUnlockCost(player);
    }

    private static void addSacrificePoints(UUID playerId, BigInteger amount) {
        SheepSacrificeProgression.addPoints(playerId, amount,
                SheepMergeManager::getSacrificePointsGainMultiplierFromRebirthSkills);
    }

    private static BigInteger getSpentSacrificePoints(int unlocksBought) {
        return SheepSacrificeProgression.getSpentPoints(unlocksBought);
    }

    private static BigInteger getSacrificeValueForSheep(Sheep sheep) {
        return SheepSacrificeProgression.getValueForSheep(sheep);
    }

    private static BigInteger sacrificeAllSheepForPlayer(Player player) {
        return SheepSacrificeProgression.sacrificeAllSheepForPlayer(player,
                SheepMergeManager::getSacrificePointsGainMultiplierFromRebirthSkills,
                SheepMergeManager::saveData);
    }

    private static boolean tryBuySacrificeUnlock(Player player, int unlockId) {
        return SheepSacrificeProgression.tryBuyUnlock(player, unlockId,
                SheepMergeManager::saveData,
                currentPlayer -> evaluateAchievementProgress(currentPlayer, true));
    }

    public static boolean isAutomationAutoBuyEnabled(Player player) {
        return player != null && SheepAutomationState.isAutoBuyEnabled(player.getUniqueId());
    }

    public static boolean isAutomationAutoAbilityEnabled(Player player) {
        return player != null && SheepAutomationState.isAutoAbilityEnabled(player.getUniqueId());
    }

    public static boolean isAutomationSlowAutoMergeEnabled(Player player) {
        return player != null && SheepAutomationState.isSlowAutoMergeEnabled(player.getUniqueId());
    }

    public static boolean isAutomationSlowAutoShearEnabled(Player player) {
        return player != null && SheepAutomationState.isSlowAutoShearEnabled(player.getUniqueId());
    }

    public static boolean isAutomationAutoSpawnEnabled(Player player) {
        return player != null && SheepAutomationState.isAutoSpawnEnabled(player.getUniqueId());
    }

    public static boolean isAutomationAutoPrestigeEnabled(Player player) {
        return player != null && SheepAutomationState.isAutoPrestigeEnabled(player.getUniqueId());
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
                && SheepAutomationState.isAutoBuyEnabled(playerId) != enabled) {
            SheepAutomationState.setAutoBuyEnabled(playerId, enabled);
            changed++;
        }
        if (getAutomationAutoAbilityUpgradeLevel(player) > 0
                && SheepAutomationState.isAutoAbilityEnabled(playerId) != enabled) {
            SheepAutomationState.setAutoAbilityEnabled(playerId, enabled);
            changed++;
        }
        if (getAutomationSlowAutoMergeUpgradeLevel(player) > 0
                && SheepAutomationState.isSlowAutoMergeEnabled(playerId) != enabled) {
            SheepAutomationState.setSlowAutoMergeEnabled(playerId, enabled);
            changed++;
        }
        if (getAutomationSlowAutoShearUpgradeLevel(player) > 0
                && SheepAutomationState.isSlowAutoShearEnabled(playerId) != enabled) {
            SheepAutomationState.setSlowAutoShearEnabled(playerId, enabled);
            changed++;
        }
        if (getAutomationAutoSpawnUpgradeLevel(player) > 0
                && SheepAutomationState.isAutoSpawnEnabled(playerId) != enabled) {
            SheepAutomationState.setAutoSpawnEnabled(playerId, enabled);
            changed++;
        }
        if (getAutomationAutoPrestigeUpgradeLevel(player) > 0
                && SheepAutomationState.isAutoPrestigeEnabled(playerId) != enabled) {
            SheepAutomationState.setAutoPrestigeEnabled(playerId, enabled);
            changed++;
        }

        if (changed > 0) {
            saveData();
        }
        return changed;
    }

    private static BigInteger getComboDecayUpgradeCost(Player player) {
        return getDoubledUpgradeCostBig(scaleRegularPointsUpgradeBaseCost(COMBO_DECAY_BASE_COST),
                getComboDecayUpgradeLevel(player));
    }

    private static BigInteger getComboGainUpgradeCost(Player player) {
        return getDoubledUpgradeCostBig(scaleRegularPointsUpgradeBaseCost(COMBO_GAIN_BASE_COST),
                getComboGainUpgradeLevel(player));
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
        if (getAutomationAutoAbilityUpgradeLevel(player) >= AUTOMATION_AUTO_ABILITY_MAX_LEVEL) {
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
        SheepAutomationState.setPoints(player.getUniqueId(), current - amount);
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
        SheepComboState.setDecayUpgrade(player.getUniqueId(), currentLevel + 1);
        saveData();
        return true;
    }

    private static boolean upgradeComboMax(Player player) {
        if (player == null) {
            return false;
        }
        int currentLevel = getComboMaxUpgradeLevel(player);
        int cost = getComboMaxUpgradePrestigeCost(player);
        if (!trySpendPrestigePoints(player, cost)) {
            return false;
        }
        SheepComboState.setMaxUpgrade(player.getUniqueId(), currentLevel + 1);
        double score = Math.min(getComboMaxScore(player), getComboScore(player));
        SheepComboState.setScore(player.getUniqueId(), score);
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
        SheepComboState.setGainUpgrade(player.getUniqueId(), currentLevel + 1);
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
        SheepAutomationState.setAutoBuyUpgrade(player.getUniqueId(), getAutomationAutoBuyUpgradeLevel(player) + 1);
        saveData();
        return true;
    }

    private static boolean upgradeAutomationAutoAbility(Player player) {
        if (player == null || getAutomationAutoAbilityUpgradeLevel(player) >= AUTOMATION_AUTO_ABILITY_MAX_LEVEL) {
            return false;
        }
        int cost = getAutomationAutoAbilityUpgradeCost(player);
        if (!trySpendAutomationPoints(player, cost)) {
            return false;
        }
        SheepAutomationState.setAutoAbilityUpgrade(player.getUniqueId(),
                getAutomationAutoAbilityUpgradeLevel(player) + 1);
        SheepAutomationState.setNextAutoAbilityAt(player.getUniqueId(), 0L);
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
        SheepAutomationState.setSlowAutoMergeUpgrade(player.getUniqueId(),
                getAutomationSlowAutoMergeUpgradeLevel(player) + 1);
        SheepAutomationState.setNextSlowMergeAt(player.getUniqueId(), 0L);
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
        SheepAutomationState.setSlowAutoShearUpgrade(player.getUniqueId(),
                getAutomationSlowAutoShearUpgradeLevel(player) + 1);
        SheepAutomationState.setNextSlowShearAt(player.getUniqueId(), 0L);
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
        SheepAutomationState.setAutoSpawnUpgrade(player.getUniqueId(), getAutomationAutoSpawnUpgradeLevel(player) + 1);
        SheepAutomationState.setNextAutoSpawnAt(player.getUniqueId(), 0L);
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
        SheepAutomationState.setAutoPrestigeUpgrade(playerId, 1);
        SheepAutomationState.setNextAutoPrestigeAt(playerId, 0L);
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
        SheepUpgradeState.setShearShopLevel(player.getUniqueId(), getShearShopLevel(player) + 1);
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
        SheepUpgradeState.setShearWoolSaveLevel(player.getUniqueId(), currentLevel + 1);
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
        SheepUpgradeState.setShearTierBoostLevel(player.getUniqueId(), currentLevel + 1);
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
        SheepPrestigeState.setTotalLevelsEarned(playerId,
                addSaturated(SheepPrestigeState.getTotalLevelsEarned(playerId), affordableLevels));
        SheepPrestigeState.setLevel(playerId, nextPrestige);
        SheepPrestigeState.setPoints(playerId, addSaturated(getPrestigePoints(player), gainedPrestigePoints));
        clearPrestigeReminder(player);

        runPrestigeResetEffects(player, false);
        saveData();
        evaluateAchievementProgress(player, true);
        markTutorialPrestigedOnce(player);
        return affordableLevels;
    }

    public static int rebirth(Player player) {
        if (player == null) {
            return 0;
        }
        int currentRebirth = getRebirthLevel(player);
        int affordable = getAffordableRebirthLevels(player);
        if (affordable <= 0) {
            return 0;
        }

        int gainedRebirthPoints = getRebirthPointsRewardForNextLevels(currentRebirth, affordable);
        UUID playerId = player.getUniqueId();
        SheepRebirthState.setLevel(playerId, currentRebirth + affordable);
        SheepRebirthState.setPoints(playerId, addSaturated(getRebirthPoints(player), gainedRebirthPoints));

        resetPrestigeUpgrades(playerId, true);
        SheepPrestigeState.removeLevel(playerId);
        SheepPrestigeState.removePoints(playerId);
        clearPrestigeReminder(player);
        clearMergeReminder(player);
        clearRebirthReminder(player);

        runPrestigeResetEffects(player, true);
        if (!hasActiveRebirthSkill(playerId, REBIRTH_SKILL_KEEP_SACRIFICE_AFTER_REBIRTH)) {
            SheepSacrificeProgression.removeProgress(playerId);
        }
        saveData();
        evaluateAchievementProgress(player, true);
        return affordable;
    }

    private static void runPrestigeResetEffects(Player player, boolean forRebirth) {
        if (player == null) {
            return;
        }
        UUID playerId = player.getUniqueId();
        boolean keepPoints = hasActiveRebirthSkill(playerId, REBIRTH_SKILL_KEEP_POINTS_AFTER_PRESTIGE);
        boolean keepSheep = hasActiveRebirthSkill(playerId, REBIRTH_SKILL_KEEP_SHEEP_AFTER_PRESTIGE);

        BigInteger sacrificeGained = BigInteger.ZERO;
        World world = player.getWorld();
        if (!keepSheep && isSheepFarmWorld(world) && isFarmOwner(player, world)) {
            for (Sheep sheep : world.getEntitiesByClass(Sheep.class)) {
                sacrificeGained = sacrificeGained.add(getSacrificeValueForSheep(sheep));
                if (sheep != null && sheep.isValid()) {
                    sheep.remove();
                }
            }
            refreshLiveSheepCount(world);
        }
        if (sacrificeGained.signum() > 0 && !keepSheep) {
            addSacrificePoints(playerId, sacrificeGained);
        }

        if (!keepPoints) {
            SheepEconomyState.setPoints(playerId, BigInteger.ZERO);
            refreshTopPointsDisplays();
        }
        if (!isSacrificeUnlockActive(playerId, SACRIFICE_UNLOCK_NO_REGULAR_RESETS) || forRebirth) {
            SheepEconomyState.resetRegularUpgrades(playerId);
        }
        if (!isSacrificeUnlockActive(playerId, SACRIFICE_UNLOCK_NO_SHEAR_RESETS) || forRebirth) {
            SheepUpgradeState.resetShearUpgrades(playerId);
        }
        if (!isSacrificeUnlockActive(playerId, SACRIFICE_UNLOCK_NO_COMBO_RESETS) || forRebirth) {
            SheepComboState.resetRegularUpgrades(playerId);
        }
        clearMergeReminder(player);
        EGG_MODULE.clearRuntimeState(playerId);
        clearComboRuntime(player);
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
        int prestigeLevel = SheepPrestigeState.getLevel(ownerId);
        int maxLevelBonus = SheepPrestigeState.getHigherMaxLevel(ownerId);
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
        return player == null ? 0 : SheepTutorialState.getShears(player.getUniqueId());
    }

    public static int getTutorialSpawnCount(Player player) {
        return player == null ? 0 : SheepTutorialState.getSpawns(player.getUniqueId());
    }

    public static int getTutorialMergeCount(Player player) {
        return player == null ? 0 : SheepTutorialState.getMerges(player.getUniqueId());
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
        if (SheepTutorialState.isSectionComplete(playerId, SheepTutorialState.Section.UPGRADE_OPENED)) {
            count++;
        }
        if (SheepTutorialState.isSectionComplete(playerId, SheepTutorialState.Section.QUEST_OPENED)) {
            count++;
        }
        if (SheepTutorialState.isSectionComplete(playerId, SheepTutorialState.Section.PRESTIGE_OPENED)) {
            count++;
        }
        if (SheepTutorialState.isSectionComplete(playerId, SheepTutorialState.Section.ABILITY_USED)) {
            count++;
        }
        if (SheepTutorialState.isSectionComplete(playerId, SheepTutorialState.Section.SHEAR_UPGRADED)) {
            count++;
        }
        if (SheepTutorialState.isSectionComplete(playerId, SheepTutorialState.Section.REGULAR_UPGRADES_BOUGHT)) {
            count++;
        }
        if (SheepTutorialState.isSectionComplete(playerId, SheepTutorialState.Section.PRESTIGED_ONCE)) {
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
        if (!SheepTutorialState.isSectionComplete(playerId, SheepTutorialState.Section.UPGRADE_OPENED)) {
            return "Hotbar Slot 9 -> Upgrade Menu";
        }
        if (!SheepTutorialState.isSectionComplete(playerId, SheepTutorialState.Section.REGULAR_UPGRADES_BOUGHT)) {
            return "Hotbar Slot 9 -> Upgrade Menu -> Buy any regular upgrade";
        }
        if (!SheepTutorialState.isSectionComplete(playerId, SheepTutorialState.Section.QUEST_OPENED)) {
            return "Upgrade Menu -> Quest Menu";
        }
        if (!SheepTutorialState.isSectionComplete(playerId, SheepTutorialState.Section.ABILITY_USED)) {
            return "Upgrade Menu -> Quest Menu -> Activate any quest ability";
        }
        if (!SheepTutorialState.isSectionComplete(playerId, SheepTutorialState.Section.SHEAR_UPGRADED)) {
            return "Upgrade Menu -> Shear Shop -> Buy one Shear Shop upgrade";
        }
        if (!SheepTutorialState.isSectionComplete(playerId, SheepTutorialState.Section.PRESTIGE_OPENED)) {
            return "Upgrade Menu -> Prestige Menu";
        }
        if (!SheepTutorialState.isSectionComplete(playerId, SheepTutorialState.Section.PRESTIGED_ONCE)) {
            return "Upgrade Menu -> Prestige Menu -> Prestige Reset";
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

        SheepTutorialState.setStartedAt(playerId, System.currentTimeMillis());
        SheepTutorialState.clearLastReminderTimestamp(playerId);

        if (!SheepFarmWorldCommand.teleportToTutorialWorld(player)) {
            player.sendMessage(warning("Unable to open your tutorial world right now."));
            return;
        }

        EGG_MODULE.addEggs(player, 10);

        sendTutorialTitle(player, "&eSheepMerge Tutorial", "&fFollow the steps to unlock your farm");
        player.sendMessage(hint("Step 1: Spawn " + TUTORIAL_SPAWN_TARGET + " sheep."));
        player.sendMessage(hint("Step 2: Shear " + TUTORIAL_SHEAR_TARGET + " sheep."));
        player.sendMessage(hint("Step 3: Merge " + TUTORIAL_MERGE_TARGET + " pair (SHIFT + RIGHT-CLICK)."));
        player.sendMessage(hint("Step 4: Menus -> Upgrades, Quests, Shear Shop, Prestige."));
        player.sendMessage(accent("Tip: /sheepmerge status shows your current step."));
        sendTutorialStatusFeed(player);
    }

    private static void markTutorialSection(Player player, SheepTutorialState.Section section, String message) {
        if (!isTutorialInProgress(player) || !isInTutorialWorld(player) || section == null) {
            return;
        }
        UUID playerId = player.getUniqueId();
        if (SheepTutorialState.isSectionComplete(playerId, section)) {
            return;
        }
        SheepTutorialState.setSectionComplete(playerId, section);
        player.sendMessage(action(message));
        sendTutorialStatusFeed(player);
        maybeGrantTutorialPrestigePrepReward(player);
        checkTutorialCompletion(player);
    }

    public static void markTutorialUpgradeOpened(Player player) {
        markTutorialSection(player, SheepTutorialState.Section.UPGRADE_OPENED, "Tutorial step done: Upgrades opened.");
    }

    public static void markTutorialQuestOpened(Player player) {
        markTutorialSection(player, SheepTutorialState.Section.QUEST_OPENED, "Tutorial step done: Quests opened.");
    }

    public static void markTutorialPrestigeOpened(Player player) {
        markTutorialSection(player, SheepTutorialState.Section.PRESTIGE_OPENED, "Tutorial step done: Prestige opened.");
    }

    public static void markTutorialQuestUpgradesOpened(Player player) {
        markTutorialSection(player, SheepTutorialState.Section.QUEST_UPGRADES_OPENED,
                "Tutorial step done: Quest Upgrades opened.");
    }

    public static void markTutorialAbilityUsed(Player player) {
        markTutorialSection(player, SheepTutorialState.Section.ABILITY_USED, "Tutorial step done: Ability used.");
    }

    public static void markTutorialShearUpgraded(Player player) {
        markTutorialSection(player, SheepTutorialState.Section.SHEAR_UPGRADED,
                "Tutorial step done: Shear upgrade bought.");
    }

    public static void markTutorialPrestigedOnce(Player player) {
        markTutorialSection(player, SheepTutorialState.Section.PRESTIGED_ONCE,
                "Tutorial step done: Prestiged once.");
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
                SheepTutorialState.Section.REGULAR_UPGRADES_BOUGHT,
                "Tutorial step done: Regular upgrade bought.");
    }

    public static void markTutorialShearShopOpened(Player player) {
        markTutorialSection(player, SheepTutorialState.Section.SHEAR_SHOP_OPENED,
                "Tutorial step done: Shear Shop opened.");
    }

    public static void recordTutorialShear(Player player) {
        if (!isTutorialInProgress(player) || !isInTutorialWorld(player)) {
            return;
        }
        SheepTutorialState.setShears(player.getUniqueId(), getTutorialShearCount(player) + 1);
        sendTutorialStatusFeed(player);
        maybeGrantTutorialShearTaskReward(player);
        maybeGrantTutorialPrestigePrepReward(player);
        checkTutorialCompletion(player);
    }

    public static void recordTutorialSpawn(Player player) {
        if (!isTutorialInProgress(player) || !isInTutorialWorld(player)) {
            return;
        }
        SheepTutorialState.setSpawns(player.getUniqueId(), getTutorialSpawnCount(player) + 1);
        sendTutorialStatusFeed(player);
        maybeGrantTutorialShearTaskReward(player);
        maybeGrantTutorialPrestigePrepReward(player);
        checkTutorialCompletion(player);
    }

    public static void recordTutorialMerge(Player player) {
        if (!isTutorialInProgress(player) || !isInTutorialWorld(player)) {
            return;
        }
        SheepTutorialState.setMerges(player.getUniqueId(), getTutorialMergeCount(player) + 1);
        sendTutorialStatusFeed(player);
        maybeGrantTutorialPrestigePrepReward(player);
        checkTutorialCompletion(player);
    }

    private static void maybeGrantTutorialShearTaskReward(Player player) {
        if (player == null) {
            return;
        }
        UUID playerId = player.getUniqueId();
        if (SheepTutorialState.isShearTaskRewardGranted(playerId)) {
            return;
        }
        if (getTutorialSpawnCount(player) < TUTORIAL_SPAWN_TARGET
                || getTutorialShearCount(player) < TUTORIAL_SHEAR_TARGET) {
            return;
        }
        SheepTutorialState.setShearTaskRewardGranted(playerId);
        player.sendMessage(action("Tutorial milestone: spawn + shear goals complete."));
    }

    private static void maybeGrantTutorialPrestigePrepReward(Player player) {
        if (player == null) {
            return;
        }
        UUID playerId = player.getUniqueId();
        if (SheepTutorialState.isPrestigePrepRewardGranted(playerId)
                || SheepTutorialState.isSectionComplete(playerId, SheepTutorialState.Section.PRESTIGED_ONCE)) {
            return;
        }
        if (getTutorialShearCount(player) < TUTORIAL_SHEAR_TARGET
                || getTutorialSpawnCount(player) < TUTORIAL_SPAWN_TARGET
                || getTutorialMergeCount(player) < TUTORIAL_MERGE_TARGET
                || !SheepTutorialState.isSectionComplete(playerId,
                        SheepTutorialState.Section.REGULAR_UPGRADES_BOUGHT)
                || !SheepTutorialState.isSectionComplete(playerId, SheepTutorialState.Section.UPGRADE_OPENED)
                || !SheepTutorialState.isSectionComplete(playerId, SheepTutorialState.Section.QUEST_OPENED)
                || !SheepTutorialState.isSectionComplete(playerId, SheepTutorialState.Section.PRESTIGE_OPENED)
                || !SheepTutorialState.isSectionComplete(playerId, SheepTutorialState.Section.ABILITY_USED)
                || !SheepTutorialState.isSectionComplete(playerId, SheepTutorialState.Section.SHEAR_UPGRADED)) {
            return;
        }

        SheepTutorialState.setPrestigePrepRewardGranted(playerId);
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
        if (!SheepTutorialState.isSectionComplete(playerId, SheepTutorialState.Section.UPGRADE_OPENED)) {
            return TutorialStep.OPEN_UPGRADES;
        }
        if (!SheepTutorialState.isSectionComplete(playerId, SheepTutorialState.Section.REGULAR_UPGRADES_BOUGHT)) {
            return TutorialStep.BUY_REGULAR_UPGRADE;
        }
        if (!SheepTutorialState.isSectionComplete(playerId, SheepTutorialState.Section.QUEST_OPENED)) {
            return TutorialStep.OPEN_QUESTS;
        }
        if (!SheepTutorialState.isSectionComplete(playerId, SheepTutorialState.Section.ABILITY_USED)) {
            return TutorialStep.USE_ABILITY;
        }
        if (!SheepTutorialState.isSectionComplete(playerId, SheepTutorialState.Section.SHEAR_UPGRADED)) {
            return TutorialStep.BUY_SHEAR_UPGRADE;
        }
        if (!SheepTutorialState.isSectionComplete(playerId, SheepTutorialState.Section.PRESTIGE_OPENED)) {
            return TutorialStep.OPEN_PRESTIGE;
        }
        if (!SheepTutorialState.isSectionComplete(playerId, SheepTutorialState.Section.PRESTIGED_ONCE)) {
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
            case OPEN_UPGRADES -> "Hotbar Slot 9 -> Upgrade Menu";
            case BUY_REGULAR_UPGRADE -> "Hotbar Slot 9 -> Upgrade Menu -> Buy one regular upgrade";
            case OPEN_QUESTS -> "Upgrade Menu -> Quest Menu";
            case USE_ABILITY -> "Upgrade Menu -> Quest Menu -> Activate any quest ability";
            case BUY_SHEAR_UPGRADE -> "Upgrade Menu -> Shear Shop -> Buy one Shear Shop upgrade";
            case OPEN_PRESTIGE -> "Upgrade Menu -> Prestige Menu";
            case PRESTIGE_ONCE -> "Upgrade Menu -> Prestige Menu -> Prestige Reset";
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
        if (getPlayerLimit(player) < getMaxSheepLimit(player.getUniqueId())) {
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

        long lastReminder = SheepTutorialState.getLastMergePointsReminderTimestamp(playerId);
        if (now - lastReminder < TUTORIAL_MERGE_POINTS_REMINDER_REPEAT_MS) {
            return;
        }

        BigInteger missing = requiredPoints.subtract(currentPoints).max(BigInteger.ZERO);
        String taskLabel = getCurrentTutorialTaskLabel(step);
        SheepTutorialState.setLastMergePointsReminderTimestamp(playerId, now);
        player.sendMessage(warning("Need " + formatPoints(requiredPoints) + " Coins for: " + taskLabel));
        player.sendMessage(hint("You are short " + formatPoints(missing) + ". Merge sheep to gain Coins fast."));
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
        long lastShownAt = SheepTutorialState.getLastFocusNotificationTimestamp(playerId);
        if (now - lastShownAt < TUTORIAL_FOCUS_NOTIFICATION_COOLDOWN_MS) {
            return;
        }

        SheepTutorialState.setLastFocusNotificationTimestamp(playerId, now);
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
        String previousProgressLine = SheepTutorialState.getLastProgressFeedLine(playerId);
        String previousStepLine = SheepTutorialState.getLastStepFeedLine(playerId);
        long lastSentAt = SheepTutorialState.getLastStatusFeedTimestamp(playerId);
        boolean changed = !progressLine.equals(previousProgressLine) || !stepLine.equals(previousStepLine);
        if (!changed && now - lastSentAt < TUTORIAL_STATUS_FEED_REPEAT_MS) {
            return;
        }

        SheepTutorialState.setLastStatusFeedTimestamp(playerId, now);
        SheepTutorialState.setLastProgressFeedLine(playerId, progressLine);
        SheepTutorialState.setLastStepFeedLine(playerId, stepLine);
        player.sendMessage(hint("Step: " + stepLine));
        player.sendMessage(accent(progressLine));
    }

    public static String getTutorialProgressLine(Player player) {
        int menuSectionTarget = getEffectiveTutorialMenuSectionTarget();
        return "Spawn " + getTutorialSpawnCount(player) + "/" + TUTORIAL_SPAWN_TARGET
                + " | Shear " + getTutorialShearCount(player) + "/" + TUTORIAL_SHEAR_TARGET
                + " | Merge " + getTutorialMergeCount(player) + "/" + TUTORIAL_MERGE_TARGET
                + " | Menus " + getTutorialSectionCount(player) + "/" + menuSectionTarget;
    }

    private static int getEffectiveTutorialMenuSectionTarget() {
        return Math.max(1, Math.min(TUTORIAL_MENU_SECTION_TARGET, 7));
    }

    private static void checkTutorialCompletion(Player player) {
        if (player == null || hasUnlockedFarm(player)) {
            return;
        }
        int menuSectionTarget = getEffectiveTutorialMenuSectionTarget();
        if (getTutorialShearCount(player) >= TUTORIAL_SHEAR_TARGET
                && getTutorialSpawnCount(player) >= TUTORIAL_SPAWN_TARGET
                && getTutorialMergeCount(player) >= TUTORIAL_MERGE_TARGET
                && getTutorialSectionCount(player) >= menuSectionTarget) {
            UUID playerId = player.getUniqueId();
            SheepTutorialState.setCompleted(playerId, true);
            clearTutorialRuntimeState(playerId);
            migrateTutorialSheepToFarmWorld(playerId);
            String worldName = SheepFarmWorldCommand.getWorldName(playerId);
            SheepFarmWorldCommand.ensureFarmWorldAsync(worldName, world -> {
                if (player == null || !player.isOnline()) {
                    return;
                }
                if (world == null) {
                    sendTutorialTitle(player, "&aTutorial Complete", "&fRun /sheepmerge to go to your farm");
                    player.sendMessage(action("Tutorial complete! Run /sheepmerge to go to your farm."));
                    return;
                }
                player.teleportAsync(world.getSpawnLocation().clone().add(0.5D, 0.0D, 0.5D));
                sendTutorialTitle(player, "&aTutorial Complete", "&fWelcome to your sheep farm");
                player.sendMessage(action("Tutorial complete! You were sent to your sheep farm."));
            });
            if (!player.isOnline()) {
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
        SheepEconomyState.resetAdminPlayer(id);
        refreshTopPointsDisplays();
        SheepPrestigeState.resetAdminPlayer(id);
        SheepUpgradeState.resetAdminPlayer(id);
        resetTutorialProgress(id);
        SheepVisitAccessState.resetPlayer(id);
        SheepEffectPreferences.resetPlayer(id);
        SheepRuntimeUiState.lastOutOfEggWarnings().remove(id);
        SheepQuestState.resetPlayer(id);
        stopQueuedShearAllTask(id);
        EGG_MODULE.clearRuntimeState(id);
        SheepRuntimeUiState.lastSpawnLimitWarnings().remove(id);
        SheepComboState.resetPlayer(id);
        SheepAutomationState.resetPlayer(id);
        SheepUiPreferences.resetPlayer(id);
        SheepRuntimeUiState.lastPointsScoreboardUpdates().remove(id);
        SheepRuntimeUiState.socialsPages().remove(id);
        SheepSacrificeProgression.resetPlayer(id);
        SheepRebirthState.resetPlayer(id);
        SheepRuntimeUiState.lastPointsOverlays().remove(id);
        SheepRuntimeUiState.pointsOverlayExpirations().remove(id);
        SheepLifetimeProgressState.resetPlayer(id);
        SheepAchievementState.resetPlayer(id);
        removeComboBossBar(id);
        SheepEntityRuntimeState.removeCarriedSheep(id);
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
        SheepEconomyState.setPoints(id,
                getPlayerPointsBig(player).add(BigInteger.valueOf(amount)).max(BigInteger.ZERO));
        refreshTopPointsDisplays();
        saveData();
    }

    public static void adminSetPoints(Player player, long amount) {
        if (player == null) {
            return;
        }
        SheepEconomyState.setPoints(player.getUniqueId(), BigInteger.valueOf(Math.max(0L, amount)));
        refreshTopPointsDisplays();
        saveData();
    }

    public static void adminGiveQuestPoints(Player player, int amount) {
        if (player == null || amount == 0) {
            return;
        }
        UUID id = player.getUniqueId();
        SheepQuestState.questPoints().put(id, addSaturated(SheepQuestState.questPoints().getOrDefault(id, 0), amount));
        saveData();
    }

    public static void adminGiveAutomationPoints(Player player, int amount) {
        if (player == null || amount == 0) {
            return;
        }
        UUID id = player.getUniqueId();
        SheepAutomationState.setPoints(id, addSaturated(SheepAutomationState.getPoints(id), amount));
        saveData();
    }

    public static void adminGiveSacrificePoints(Player player, BigInteger amount) {
        SheepSacrificeProgression.adminGivePoints(player, amount, SheepMergeManager::saveData);
    }

    public static void adminSetQuestPoints(Player player, int amount) {
        if (player == null) {
            return;
        }
        SheepQuestState.questPoints().put(player.getUniqueId(), Math.max(0, amount));
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

        SheepPrestigeState.setLevel(playerId, targetLevel);
        clearPrestigeReminder(player);

        if (availablePoints < 0) {
            resetPrestigeUpgrades(playerId, true);
            availablePoints = totalEarnedPoints;
        }

        SheepPrestigeState.setPoints(playerId, Math.max(0, availablePoints));
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
        multiplier /= getAchievementWoolRegenSpeedMultiplier(ownerId);
        if (hasActiveRebirthSkill(ownerId, REBIRTH_SKILL_WOOL_REGEN_X10)) {
            multiplier *= 0.1D;
        }
        if (isAbilityActive(SheepQuestState.activeWoolRushUntil(), ownerId)) {
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
        spawnParticle(sheep.getWorld(),
                org.bukkit.Particle.CLOUD,
                mouth,
                10,
                0.18D,
                0.08D,
                0.18D,
                0.01D);
        playSheepSound(sheep.getWorld(), mouth, Sound.ENTITY_SHEEP_AMBIENT, 0.75f, 1.05f);

        sheep.setSheared(false);
        updateSheepName(sheep);
    }

    private static boolean applySheepRescueMotionIfNeeded(Sheep sheep) {
        if (sheep == null || !sheep.isValid()) {
            return false;
        }

        UUID sheepId = sheep.getUniqueId();
        org.bukkit.Location location = sheep.getLocation();
        boolean rescueInProgress = SheepEntityRuntimeState.isRescueInProgress(sheepId);
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
        long started = SheepEntityRuntimeState.getOrStartRescue(sheepId, now);
        org.bukkit.Location origin = SheepEntityRuntimeState.getOrSetRescueOrigin(sheepId, location);
        SheepEntityRuntimeState.ensureRescueCorrectionAt(sheepId, now);

        if (now - started >= SHEEP_RESCUE_TIMEOUT_MS) {
            teleportSheepToFarmCenter(sheep);
            clearSheepRescueState(sheepId);
            sheep.setCollidable(true);
            return false;
        }

        org.bukkit.Location desired = getRescuePathTargetLocation(sheep, origin, started, now);
        Vector steeringVelocity = getRescueSteeringVelocity(location, desired);
        sheep.setVelocity(steeringVelocity);

        long nextCorrectionAt = SheepEntityRuntimeState.getRescueCorrectionAt(sheepId, now);
        if (now >= nextCorrectionAt) {
            if (location.distanceSquared(desired) >= SHEEP_RESCUE_POSITION_CORRECTION_DISTANCE
                    * SHEEP_RESCUE_POSITION_CORRECTION_DISTANCE) {
                sheep.teleport(desired);
            }
            SheepEntityRuntimeState.setRescueCorrectionAt(sheepId, now + SHEEP_RESCUE_CORRECTION_INTERVAL_MS);
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
        SheepEntityRuntimeState.clearRescue(sheepId);
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

    public static void recoverPlayerIfFallenFromPlatform(Player player) {
        if (player == null || !player.isOnline() || !isSheepFarmWorld(player.getWorld())) {
            return;
        }
        org.bukkit.Location location = player.getLocation();
        if (location == null || location.getY() > getPlayerFallRecoveryTriggerY(player.getWorld())) {
            return;
        }

        org.bukkit.Location target = new org.bukkit.Location(
                player.getWorld(),
                FARM_CENTER_X,
                FARM_BASE_Y + 1.0D,
                FARM_CENTER_Z,
                location.getYaw(),
                location.getPitch());
        player.teleport(target);
        player.setVelocity(new Vector(0.0D, 0.0D, 0.0D));
        player.setFallDistance(0.0F);
    }

    private static double getPlayerFallRecoveryTriggerY(World world) {
        if (world == null) {
            return FARM_BASE_Y - 2.0D;
        }
        return world.getMinHeight() + PLAYER_FALL_RECOVERY_MARGIN_ABOVE_VOID;
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
        return SheepEconomyState.getPoints(player.getUniqueId(), getStartingPointsBig());
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
        long combinedMultiplier = (long) Math.max(1, getShearPointMultiplier(player))
                * Math.max(1, getAchievementPointMultiplier(player));
        BigInteger points = base.multiply(BigInteger.valueOf(Math.max(1L, combinedMultiplier)));
        if (isAbilityActive(SheepQuestState.activeJackpotShearsUntil(), player == null ? null : player.getUniqueId())) {
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
        return SheepFormatting.formatRainbowTier(tier);
    }

    public static boolean shearSheepForPlayer(Player player, Sheep sheep) {
        if (player == null || sheep == null || sheep.getWorld() == null || !isSheepFarmWorld(sheep.getWorld())) {
            return false;
        }
        UUID playerId = player.getUniqueId();
        if (isCountAbilityActive(SheepQuestState.activeAutoShearUses(), SheepQuestState.autoShearEnabled(), playerId)) {
            return queueShearAllEligibleSheepForPlayer(player, sheep);
        }
        return shearSingleSheepForPlayer(player, sheep);
    }

    private static boolean queueShearAllEligibleSheepForPlayer(Player player, Sheep triggerSheep) {
        if (player == null || triggerSheep == null || triggerSheep.getWorld() == null) {
            return false;
        }
        World world = triggerSheep.getWorld();
        if (!isFarmOwner(player, world)) {
            return false;
        }

        UUID playerId = player.getUniqueId();
        BukkitTask existingTask = SheepEntityRuntimeState.getShearAllTask(playerId);
        if (existingTask != null && !existingTask.isCancelled()) {
            return true;
        }

        List<UUID> targetSheepIds = collectEligibleShearTargetIds(world, triggerSheep);
        if (targetSheepIds.isEmpty()) {
            return false;
        }

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            private int index = 0;

            @Override
            public void run() {
                Player onlinePlayer = Bukkit.getPlayer(playerId);
                if (onlinePlayer == null
                        || !onlinePlayer.isOnline()
                        || onlinePlayer.getWorld() == null
                        || !onlinePlayer.getWorld().getUID().equals(world.getUID())
                        || !isFarmOwner(onlinePlayer, world)
                        || !isCountAbilityActive(SheepQuestState.activeAutoShearUses(),
                                SheepQuestState.autoShearEnabled(), playerId)
                        || getCountAbilityRemainingUses(SheepQuestState.activeAutoShearUses(), playerId) <= 0
                        || index >= targetSheepIds.size()) {
                    stopQueuedShearAllTask(playerId);
                    return;
                }

                while (index < targetSheepIds.size()) {
                    UUID sheepId = targetSheepIds.get(index++);
                    Entity entity = Bukkit.getEntity(sheepId);
                    if (!(entity instanceof Sheep candidate)
                            || !candidate.isValid()
                            || candidate.isDead()
                            || candidate.getWorld() == null
                            || !candidate.getWorld().getUID().equals(world.getUID())) {
                        continue;
                    }

                    if (shearSingleSheepForPlayer(onlinePlayer, candidate)) {
                        consumeCountAbilityUse(SheepQuestState.activeAutoShearUses(), playerId);
                        if (getCountAbilityRemainingUses(SheepQuestState.activeAutoShearUses(), playerId) <= 0) {
                            saveData();
                            stopQueuedShearAllTask(playerId);
                        }
                        return;
                    }
                }

                saveData();
                stopQueuedShearAllTask(playerId);
            }
        }, 1L, 1L);

        SheepEntityRuntimeState.putShearAllTask(playerId, task);
        return true;
    }

    private static List<UUID> collectEligibleShearTargetIds(World world, Sheep triggerSheep) {
        List<UUID> targetSheepIds = new ArrayList<>();
        if (world == null || triggerSheep == null) {
            return targetSheepIds;
        }

        if (isEligibleShearAllTarget(triggerSheep)) {
            targetSheepIds.add(triggerSheep.getUniqueId());
        }

        for (Sheep candidate : new ArrayList<>(world.getEntitiesByClass(Sheep.class))) {
            if (candidate == null || candidate.getUniqueId().equals(triggerSheep.getUniqueId())) {
                continue;
            }
            if (isEligibleShearAllTarget(candidate)) {
                targetSheepIds.add(candidate.getUniqueId());
            }
        }
        return targetSheepIds;
    }

    private static boolean isEligibleShearAllTarget(Sheep sheep) {
        if (sheep == null || !sheep.isValid() || sheep.isDead() || !sheep.isAdult()) {
            return false;
        }
        if (sheep.isSheared()) {
            return false;
        }
        return getNextEatTimestamp(sheep) <= System.currentTimeMillis();
    }

    private static void stopQueuedShearAllTask(UUID playerId) {
        if (playerId == null) {
            return;
        }
        BukkitTask task = SheepEntityRuntimeState.removeShearAllTask(playerId);
        if (task != null) {
            task.cancel();
        }
    }

    private static void stopAllQueuedShearAllTasks() {
        for (UUID playerId : SheepEntityRuntimeState.shearAllTaskPlayerIds()) {
            stopQueuedShearAllTask(playerId);
        }
    }

    private static boolean shearSingleSheepForPlayer(Player player, Sheep sheep) {
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
        UUID playerId = player.getUniqueId();
        SheepLifetimeProgressState.incrementLifetimeShears(playerId);
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
        int upgradedRainbowTier = upgradedTier == SheepTier.RAINBOW ? getRainbowTier(sheep) : 0;
        if (shouldAnnounceTierUnlock(player, upgradedTier, upgradedRainbowTier)) {
            announceTierUnlock(player, upgradedTier, upgradedRainbowTier);
            markTierUnlockAnnounced(player, upgradedTier, upgradedRainbowTier);
        }
        spawnParticle(sheep.getWorld(),
                org.bukkit.Particle.VILLAGER_HAPPY,
                sheep.getLocation().add(0, 0.7, 0),
                12,
                0.25,
                0.2,
                0.25,
                0.02);
        showOverlay(player, accent("Tier Booster triggered: " + currentTier.getDisplayName()
                + color(" &7-> ") + upgradedTier.getDisplayName()));
        playTierBoostProcSound(player);
        return true;
    }

    private static void playTierBoostProcSound(Player player) {
        if (player == null) {
            return;
        }
        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();
        long lastPlayed = SheepQuestState.lastTierBoostSoundTimestamps().getOrDefault(playerId, 0L);
        if (now - lastPlayed < TIER_BOOST_SOUND_COOLDOWN_MS) {
            return;
        }
        SheepQuestState.lastTierBoostSoundTimestamps().put(playerId, now);
        playSound(player, Sound.BLOCK_NOTE_BLOCK_CHIME, 0.35f, 0.95f);
    }

    public static String buildTopPointsText(int maxEntries) {
        Map<UUID, BigInteger> pointsSnapshot = SheepEconomyState.getPointsSnapshot();
        return buildTopPointsText(pointsSnapshot, snapshotTopPointsPlayerNames(pointsSnapshot.keySet()), maxEntries);
    }

    private static String buildTopPointsText(Map<UUID, BigInteger> pointsSnapshot, Map<UUID, String> nameSnapshot,
            int maxEntries) {
        StringBuilder builder = new StringBuilder("Top Sheep Merge Coins");
        pointsSnapshot.entrySet().stream()
                .sorted((left, right) -> {
                    int pointsCompare = right.getValue().compareTo(left.getValue());
                    if (pointsCompare != 0) {
                        return pointsCompare;
                    }

                    String leftSafeName = getSafeTopPointsName(left.getKey(), nameSnapshot);
                    String rightSafeName = getSafeTopPointsName(right.getKey(), nameSnapshot);
                    int nameCompare = leftSafeName.compareToIgnoreCase(rightSafeName);
                    if (nameCompare != 0) {
                        return nameCompare;
                    }
                    return left.getKey().compareTo(right.getKey());
                })
                .limit(Math.max(1, maxEntries))
                .forEach(entry -> builder.append("\n")
                        .append(getSafeTopPointsName(entry.getKey(), nameSnapshot))
                        .append(": ")
                        .append(formatPoints(entry.getValue())));
        if (builder.toString().equals("Top Sheep Merge Coins")) {
            builder.append("\nNo scores yet");
        }
        return builder.toString();
    }

    private static Map<UUID, String> snapshotTopPointsPlayerNames(Collection<UUID> playerIds) {
        Map<UUID, String> names = new HashMap<>();
        if (plugin == null || plugin.getServer() == null) {
            return names;
        }
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online == null) {
                continue;
            }
            String name = online.getName();
            if (name != null && !name.isBlank()) {
                names.put(online.getUniqueId(), name);
            }
        }

        if (playerIds == null) {
            return names;
        }

        for (UUID playerId : playerIds) {
            if (playerId == null || names.containsKey(playerId)) {
                continue;
            }
            String offlineName = Bukkit.getOfflinePlayer(playerId).getName();
            if (offlineName != null && !offlineName.isBlank()) {
                names.put(playerId, offlineName);
            }
        }
        return names;
    }

    private static String getSafeTopPointsName(UUID playerId, Map<UUID, String> nameSnapshot) {
        if (playerId == null) {
            return "unknown";
        }
        String name = nameSnapshot == null ? null : nameSnapshot.get(playerId);
        return name == null || name.isBlank() ? playerId.toString().substring(0, 8) : name;
    }

    public static List<String> getTopPointsLines(int maxEntries) {
        List<String> lines = new ArrayList<>();
        final int limit = Math.max(1, maxEntries);
        List<Map.Entry<UUID, BigInteger>> entries = SheepEconomyState.getPointsSnapshot().entrySet().stream()
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

    public static int getTopPointsPageCount(int pageSize) {
        int safePageSize = Math.max(1, pageSize);
        int totalEntries = SheepEconomyState.getPointsSnapshot().size();
        if (totalEntries <= 0) {
            return 1;
        }
        return (int) Math.ceil(totalEntries / (double) safePageSize);
    }

    public static List<String> getTopPointsLines(int pageSize, int pageNumber) {
        List<String> lines = new ArrayList<>();
        final int safePageSize = Math.max(1, pageSize);
        final int safePageNumber = Math.max(1, pageNumber);

        List<Map.Entry<UUID, BigInteger>> entries = SheepEconomyState.getPointsSnapshot().entrySet().stream()
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
                .toList();

        if (entries.isEmpty()) {
            lines.add("No scores yet.");
            return lines;
        }

        int startIndex = (safePageNumber - 1) * safePageSize;
        if (startIndex >= entries.size()) {
            lines.add("No scores on this page.");
            return lines;
        }

        int endIndex = Math.min(startIndex + safePageSize, entries.size());
        for (int index = startIndex; index < endIndex; index++) {
            Map.Entry<UUID, BigInteger> entry = entries.get(index);
            String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
            if (name == null || name.isBlank()) {
                name = entry.getKey().toString().substring(0, 8);
            }
            int rank = index + 1;
            lines.add(rank + ". " + name + " - " + formatPoints(entry.getValue()));
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
        if (plugin == null || plugin.getServer() == null) {
            return;
        }

        Map<UUID, BigInteger> pointsSnapshot = SheepEconomyState.getPointsSnapshot();
        Map<UUID, String> nameSnapshot = snapshotTopPointsPlayerNames(pointsSnapshot.keySet());
        Location savedLocation = getSavedTopPointsDisplayLocation();
        long requestVersion;
        synchronized (TOP_POINTS_REFRESH_LOCK) {
            requestVersion = ++topPointsRefreshVersion;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String topPointsText = buildTopPointsText(pointsSnapshot, nameSnapshot, 10);
            Bukkit.getScheduler().runTask(plugin, () -> {
                synchronized (TOP_POINTS_REFRESH_LOCK) {
                    if (requestVersion != topPointsRefreshVersion) {
                        return;
                    }
                }
                applyTopPointsText(savedLocation, topPointsText);
            });
        });
    }

    private static void applyTopPointsText(Location savedLocation, String topPointsText) {
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
            if (stripped != null && stripped.toLowerCase(Locale.ROOT).contains("top sheep merge coins")) {
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
            if (stripped != null && stripped.toLowerCase(Locale.ROOT).contains("top sheep merge coins")) {
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

    static SheepMergePlugin getBackupPlugin() {
        return plugin;
    }

    static void saveSheepSnapshotForBackup(World world) {
        saveSheepSnapshotForWorld(world);
    }

    static boolean saveFarmLayoutForBackup() {
        return saveFarmLayout();
    }

    static void clearStateForBackupLoad() {
        clearStateBeforeDataLoad();
    }

    static void loadDataForBackupLoad() {
        loadData();
    }

    static void loadFarmLayoutForBackupLoad() {
        loadFarmLayout();
    }

    public static synchronized File createBackup(boolean permanent, String trigger) {
        return SheepBackupManager.createBackup(permanent, trigger);
    }

    public static synchronized boolean maybeCreateAutomaticBackup(String trigger) {
        return SheepBackupManager.maybeCreateAutomaticBackup(trigger);
    }

    public static long getAutomaticBackupIntervalTicks() {
        return SheepBackupManager.getAutomaticBackupIntervalTicks();
    }

    public static synchronized File createManualBackup() {
        return SheepBackupManager.createManualBackup();
    }

    public static synchronized List<String> listBackups() {
        return SheepBackupManager.listBackups();
    }

    public static synchronized boolean markBackupForDeletion(String backupName) {
        return SheepBackupManager.markBackupForDeletion(backupName);
    }

    public static synchronized boolean recoverBackupMarkedForDeletion(String backupName) {
        return SheepBackupManager.recoverBackupMarkedForDeletion(backupName);
    }

    public static synchronized boolean isBackupMarkedForDeletion(String backupName) {
        return SheepBackupManager.isBackupMarkedForDeletion(backupName);
    }

    public static synchronized int purgeMarkedBackupsIfEligibleOnStartup() {
        return SheepBackupManager.purgeMarkedBackupsIfEligibleOnStartup();
    }

    public static synchronized File loadBackup(String backupName) {
        return SheepBackupManager.loadBackup(backupName);
    }

    private static void clearStateBeforeDataLoad() {
        stopAllQueuedShearAllTasks();
        SheepEconomyState.clear();
        SheepPrestigeState.clearBeforeDataLoad();
        SheepUpgradeState.clear();
        SheepTutorialState.clear();
        SheepVisitAccessState.clear();
        SheepEffectPreferences.clear();
        SheepQuestState.clear();
        SheepComboState.clearPersisted();
        SheepAutomationState.clearPersisted();
        SheepRebirthState.clear();
        SheepUiPreferences.clear();
        SheepRuntimeUiState.lastPointsScoreboardUpdates().clear();
        SheepRuntimeUiState.socialsPages().clear();
        SheepSacrificeProgression.clear();
        SheepLiveUpdateState.reset();
        for (BossBar bar : SheepRuntimeUiState.visitFarmBossBars().values()) {
            if (bar != null) {
                bar.removeAll();
                bar.setVisible(false);
            }
        }
        SheepRuntimeUiState.visitFarmBossBars().clear();
        savedFarmSheepByPlayer.clear();
        savedTutorialSheepByPlayer.clear();
        SheepRuntimeUiState.savedInventories().clear();
        SheepRuntimeUiState.savedScoreboards().clear();
        SheepEntityRuntimeState.clearCarriedSheep();
        SheepEntityRuntimeState.clearLiveSheepCounts();
        SheepRuntimeUiState.lastOutOfEggWarnings().clear();
        SheepLifetimeProgressState.clear();
        SheepAchievementState.clear();
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
        BigInteger boosted = points.multiply(BigInteger.valueOf(getPointsGainMultiplierFromRebirthSkills(player)));
        UUID playerId = player.getUniqueId();
        SheepEconomyState.setPoints(playerId, getPlayerPointsBig(player).add(boosted));
        refreshTopPointsDisplays();
        queuePointsGainOverlay(player, boosted);
        saveData();
        tickPrestigeReminder(player);
    }

    private static void queuePointsGainOverlay(Player player, BigInteger points) {
        if (player == null || points == null || points.signum() <= 0) {
            return;
        }
        UUID playerId = player.getUniqueId();
        SheepRuntimeUiState.lastPointsOverlays().put(playerId, points);
        SheepRuntimeUiState.pointsOverlayExpirations().put(
                playerId, System.currentTimeMillis() + POINTS_OVERLAY_DISPLAY_DURATION_MS);
    }

    public static void tickPointsGainOverlay(Player player) {
        if (player == null || !isSheepFarmWorld(player.getWorld())) {
            return;
        }
        UUID playerId = player.getUniqueId();
        BigInteger lastPoints = SheepRuntimeUiState.lastPointsOverlays().get(playerId);
        if (lastPoints == null || lastPoints.signum() <= 0) {
            return;
        }

        long now = System.currentTimeMillis();
        long expiresAt = SheepRuntimeUiState.pointsOverlayExpirations().getOrDefault(playerId, 0L);
        if (expiresAt <= now) {
            SheepRuntimeUiState.lastPointsOverlays().remove(playerId);
            SheepRuntimeUiState.pointsOverlayExpirations().remove(playerId);
            return;
        }

        showOverlay(player, action("+" + formatPoints(lastPoints) + " Coins"));
    }

    public static void showOverlay(Player player, String message) {
        if (player == null || message == null || message.isBlank()) {
            return;
        }
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
    }

    public static String color(String message) {
        return SheepFormatting.color(message);
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
        SheepRuntimeUiState.lastMergeTimestamps().put(playerId, now);
        SheepRuntimeUiState.lastMergeReminderTimestamps().remove(playerId);
        SheepRuntimeUiState.mergeTitleReminderShown().remove(playerId);
    }

    public static void clearMergeReminder(Player player) {
        if (player == null) {
            return;
        }
        UUID playerId = player.getUniqueId();
        SheepRuntimeUiState.lastMergeTimestamps().remove(playerId);
        SheepRuntimeUiState.lastMergeReminderTimestamps().remove(playerId);
        SheepRuntimeUiState.mergeTitleReminderShown().remove(playerId);
    }

    public static void clearComboRuntime(Player player) {
        if (player == null) {
            return;
        }
        UUID playerId = player.getUniqueId();
        stopQueuedShearAllTask(playerId);
        SheepRuntimeUiState.lastPointsScoreboardUpdates().remove(playerId);
        SheepComboState.resetRuntime(playerId);
        SheepRuntimeUiState.lastPointsOverlays().remove(playerId);
        SheepRuntimeUiState.pointsOverlayExpirations().remove(playerId);
        SheepQuestState.lastAbilityAuraSoundTimestamps().remove(playerId);
        removeComboBossBar(playerId);
        clearVisitFarmBossBar(player);
    }

    public static void clearPrestigeReminder(Player player) {
        if (player == null) {
            return;
        }
        SheepPrestigeState.clearReminder(player.getUniqueId());
    }

    public static void clearRebirthReminder(Player player) {
        if (player == null) {
            return;
        }
        SheepRebirthState.clearReminder(player.getUniqueId());
    }

    public static void tickPrestigeReminder(Player player) {
        if (player == null || !isSheepFarmWorld(player.getWorld())) {
            return;
        }
        if (getRebirthLevel(player) > 0) {
            clearPrestigeReminder(player);
            return;
        }
        if (getPlayerPointsBig(player).compareTo(getPrestigeCostBig(player)) < 0) {
            clearPrestigeReminder(player);
            return;
        }

        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();
        long lastReminder = SheepPrestigeState.getLastReminderTimestamp(playerId);
        if (now - lastReminder < 20_000L) {
            return;
        }

        if (!SheepPrestigeState.isTitleReminderShown(playerId)) {
            player.sendTitle(
                    color("&ePrestige ready"),
                    color("&7Use /sheepmerge prestige"),
                    10,
                    60,
                    10);
            SheepPrestigeState.setTitleReminderShown(playerId, true);
        } else {
            player.sendMessage(hint("Prestige ready. Use /sheepmerge prestige"));
        }
        SheepPrestigeState.setLastReminderTimestamp(playerId, now);
    }

    public static void tickRebirthReminder(Player player) {
        if (player == null || !isSheepFarmWorld(player.getWorld())) {
            return;
        }
        int affordableRebirths = getAffordableRebirthLevels(player);
        if (affordableRebirths <= 0) {
            clearRebirthReminder(player);
            return;
        }

        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();
        long lastReminder = SheepRebirthState.getLastReminderTimestamp(playerId);
        if (now - lastReminder < 20_000L) {
            return;
        }

        if (!SheepRebirthState.isTitleReminderShown(playerId)) {
            player.sendTitle(
                    color("&dRebirth ready"),
                    color("&7Open the rebirth menu"),
                    10,
                    60,
                    10);
            SheepRebirthState.setTitleReminderShown(playerId, true);
        } else {
            player.sendMessage(hint("Rebirth ready. Open the rebirth menu from /sheepmerge upgrade."));
        }
        SheepRebirthState.setLastReminderTimestamp(playerId, now);
    }

    public static void recordSheepMerge(Player player, SheepTier mergedFromTier, int woolReadySourceSheep) {
        if (player == null || mergedFromTier == null) {
            return;
        }
        UUID playerId = player.getUniqueId();
        SheepLifetimeProgressState.incrementLifetimeMerges(playerId);
        long now = System.currentTimeMillis();
        SheepRuntimeUiState.lastMergeTimestamps().put(playerId, now);
        SheepRuntimeUiState.lastMergeReminderTimestamps().remove(playerId);
        SheepRuntimeUiState.mergeTitleReminderShown().remove(playerId);

        tickComboDecay(player, now);
        double comboGain = (mergedFromTier.getLevel() + 1)
                * (1.0D + (getComboGainUpgradeLevel(player) * (COMBO_GAIN_PERCENT_PER_LEVEL / 100.0D)));
        if (comboFrenzyEventEndsAtMs > now) {
            comboGain *= COMBO_FRENZY_MULTIPLIER;
        }
        double updatedScore = Math.min(getComboMaxScore(player), getComboScore(player) + comboGain);
        SheepComboState.setScore(playerId, updatedScore);
        SheepComboState.setLastUpdateTimestamp(playerId, now);

        showOverlay(player, accent("Merge combo x" + formatComboMultiplier(getComboMultiplier(player, updatedScore))));
        updateComboBossBar(player, updatedScore);
    }

    private static double getComboMultiplier(Player player, double comboScore) {
        return Math.max(1.0D, 1.0D + comboScore * COMBO_POINT_MULTIPLIER_PER_SCORE);
    }

    private static String formatComboMultiplier(double multiplier) {
        return SheepFormatting.formatComboMultiplier(multiplier);
    }

    private static double getComboScore(Player player) {
        if (player == null) {
            return 0.0D;
        }
        return Math.max(0.0D, SheepComboState.getScore(player.getUniqueId()));
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
        long lastTick = SheepComboState.getLastUpdateTimestamp(playerId, now);
        SheepComboState.setLastUpdateTimestamp(playerId, now);

        if (currentScore <= 0.0D || now <= lastTick) {
            if (currentScore <= 0.0D) {
                SheepComboState.removeScore(playerId);
            }
            return;
        }

        double elapsedSeconds = (now - lastTick) / 1000.0D;
        double maxScore = getComboMaxScore(player);
        double levelScaling = 1.0D + (currentScore / Math.max(1.0D, maxScore)) * COMBO_DECAY_HIGH_LEVEL_SCALING;
        double decayPerSecond = BASE_COMBO_DECAY_PER_SECOND * levelScaling * getComboDecayMultiplier(player);
        double updatedScore = Math.max(0.0D, currentScore - decayPerSecond * elapsedSeconds);

        if (updatedScore <= 0.01D) {
            SheepComboState.removeScore(playerId);
            return;
        }
        SheepComboState.setScore(playerId, Math.min(maxScore, updatedScore));
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

        BossBar bar = SheepRuntimeUiState.comboBossBars().get(playerId);
        if (bar == null) {
            bar = Bukkit.createBossBar("Combo", BarColor.YELLOW, BarStyle.SEGMENTED_10);
            SheepRuntimeUiState.comboBossBars().put(playerId, bar);
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
                + " &7| &eCoins x" + formatComboMultiplier(getComboMultiplier(player, comboScore)));
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
        BossBar bar = SheepRuntimeUiState.comboBossBars().remove(playerId);
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
        int highestAnnounced = SheepUpgradeState.getHighestAnnouncedTier(playerId, SheepTier.WHITE.getLevel());
        if (tier.getLevel() > highestAnnounced) {
            return true;
        }
        if (tier != SheepTier.RAINBOW) {
            return false;
        }
        int normalizedRainbowTier = Math.max(1, rainbowTier);
        int highestRainbowAnnounced = SheepUpgradeState.getHighestAnnouncedRainbowTier(playerId);
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
        int highestAnnounced = SheepUpgradeState.getHighestAnnouncedTier(playerId, SheepTier.WHITE.getLevel());
        if (tier.getLevel() > highestAnnounced) {
            SheepUpgradeState.setHighestAnnouncedTier(playerId, tier.getLevel());
        }
        if (tier == SheepTier.RAINBOW) {
            int normalizedRainbowTier = Math.max(1, rainbowTier);
            int highestRainbowAnnounced = SheepUpgradeState.getHighestAnnouncedRainbowTier(playerId);
            if (normalizedRainbowTier > highestRainbowAnnounced) {
                SheepUpgradeState.setHighestAnnouncedRainbowTier(playerId, normalizedRainbowTier);
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
        if (getRebirthLevel(player) > 0) {
            clearMergeReminder(player);
            return;
        }
        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();
        long lastMerge = SheepRuntimeUiState.lastMergeTimestamps().getOrDefault(playerId, now);
        if (now - lastMerge < MERGE_REMINDER_DELAY_MS) {
            return;
        }
        long lastReminder = SheepRuntimeUiState.lastMergeReminderTimestamps().getOrDefault(playerId, 0L);
        if (now - lastReminder < MERGE_REMINDER_REPEAT_MS) {
            return;
        }
        if (!SheepRuntimeUiState.mergeTitleReminderShown().getOrDefault(playerId, false)) {
            player.sendTitle(
                    color("&eMerge sheep"),
                    color("&7Sneak-right-click one sheep, then right-click the same tier"),
                    10,
                    60,
                    10);
            SheepRuntimeUiState.mergeTitleReminderShown().put(playerId, true);
        } else {
            player.sendMessage(hint("Merge sheep. Sneak-right-click one sheep, then right-click the same tier."));
        }
        SheepRuntimeUiState.lastMergeReminderTimestamps().put(playerId, now);
    }

    public static void enforceFarmLoadout(Player player) {
        if (player == null || !isSheepFarmWorld(player.getWorld())) {
            return;
        }
        boolean shouldClearNonLoadoutItems = !player.isOp() && !isFarmBuildWorld(player.getWorld());
        List<ItemStack> quickAccessItems = buildQuickAccessHotbarItems(player);

        var inventory = player.getInventory();
        ItemStack[] storageContents = inventory.getStorageContents();
        ItemStack offHand = inventory.getItemInOffHand();
        boolean shearsInOffHand = isSheepMergeShearsItem(offHand);
        boolean storageChanged = false;

        for (int slot = 0; slot < storageContents.length; slot++) {
            ItemStack itemStack = storageContents[slot];

            if (slot >= INVENTORY_QUICK_ACCESS_FIRST_SLOT && slot <= INVENTORY_QUICK_ACCESS_LAST_SLOT) {
                int quickIndex = slot - INVENTORY_QUICK_ACCESS_FIRST_SLOT;
                ItemStack desired = quickIndex < quickAccessItems.size() ? quickAccessItems.get(quickIndex) : null;
                if (desired == null) {
                    if (itemStack != null && (shouldClearNonLoadoutItems || isQuickAccessCommandItem(itemStack))) {
                        storageContents[slot] = null;
                        storageChanged = true;
                    }
                    continue;
                }
                if (itemStack == null || !itemStack.isSimilar(desired)
                        || itemStack.getAmount() != desired.getAmount()) {
                    storageContents[slot] = desired;
                    storageChanged = true;
                }
                continue;
            }

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

            if (slot == FARM_SHEARS_ITEM_SLOT) {
                if (shearsInOffHand) {
                    if (isSheepMergeShearsItem(itemStack)) {
                        storageContents[slot] = null;
                        storageChanged = true;
                    }
                } else if (itemStack == null || !isSheepMergeShearsItem(itemStack) || itemStack.getAmount() != 1) {
                    storageContents[slot] = getSheepMergeShears();
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

        if (shearsInOffHand && (offHand == null || !isSheepMergeShearsItem(offHand) || offHand.getAmount() != 1)) {
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
            meta.setDisplayName(color("&bSheep Merge Menu"));
            meta.setLore(List.of(
                    hint("Right-click to open menu"),
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
                        || isSheepMergeEggItem(itemStack)
                        || isQuickAccessCommandItem(itemStack));
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
        if (player == null || sound == null || !areSoundEffectsEnabled(player)) {
            return;
        }
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    private static void playSheepSound(Player player, Sound sound, float volume, float pitch) {
        if (player == null || sound == null || !areSoundEffectsEnabled(player) || !areSheepSoundsEnabled(player)) {
            return;
        }
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    private static void playSound(World world, Location location, Sound sound, float volume, float pitch) {
        if (world == null || location == null || sound == null) {
            return;
        }
        for (Player player : world.getPlayers()) {
            playSound(player, sound, volume, pitch);
        }
    }

    private static void playSheepSound(World world, Location location, Sound sound, float volume, float pitch) {
        if (world == null || location == null || sound == null) {
            return;
        }
        for (Player player : world.getPlayers()) {
            playSheepSound(player, sound, volume, pitch);
        }
    }

    private static void spawnParticle(Player player, org.bukkit.Particle particle, Location location, int count,
            double offsetX, double offsetY, double offsetZ, double extra) {
        if (player == null || particle == null || location == null || !areParticleEffectsEnabled(player)) {
            return;
        }
        player.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, extra);
    }

    private static void spawnParticle(World world, org.bukkit.Particle particle, Location location, int count,
            double offsetX, double offsetY, double offsetZ, double extra) {
        if (world == null || particle == null || location == null) {
            return;
        }
        for (Player player : world.getPlayers()) {
            spawnParticle(player, particle, location, count, offsetX, offsetY, offsetZ, extra);
        }
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
        SheepEconomyState.setPoints(uuid, current.subtract(points));
        refreshTopPointsDisplays();
        saveData();
        return true;
    }

    public static int getPlayerLimit(Player player) {
        if (player == null) {
            return BASE_SHEEP_LIMIT;
        }
        int maxLimit = getMaxSheepLimit(player.getUniqueId());
        return Math.min(
                maxLimit,
                BASE_SHEEP_LIMIT + Math.max(0, SheepEconomyState.getExtraLimit(player.getUniqueId())));
    }

    public static int getOwnerLimit(World world) {
        UUID ownerId = getOwnerId(world);
        if (ownerId == null) {
            return BASE_SHEEP_LIMIT;
        }
        int maxLimit = getMaxSheepLimit(ownerId);
        return Math.min(
                maxLimit,
                BASE_SHEEP_LIMIT + Math.max(0, SheepEconomyState.getExtraLimit(ownerId)));
    }

    public static BigInteger getUpgradeCost(Player player) {
        return getDoubledUpgradeCostBig(scaleRegularPointsUpgradeBaseCost(LIMIT_UPGRADE_COST),
                getLimitUpgradeLevel(player));
    }

    public static int getLimitUpgradeStep() {
        return LIMIT_UPGRADE_STEP;
    }

    public static boolean upgradeLimit(Player player) {
        if (player == null) {
            return false;
        }
        int maxLimit = getMaxSheepLimit(player.getUniqueId());
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
        int currentExtra = Math.max(0, SheepEconomyState.getExtraLimit(playerId));
        int maxExtra = maxLimit - BASE_SHEEP_LIMIT;
        SheepEconomyState.setExtraLimit(playerId, Math.min(maxExtra, currentExtra + LIMIT_UPGRADE_STEP));
        saveData();
        return true;
    }

    public static int getLimitUpgradeLevel(Player player) {
        if (player == null) {
            return 0;
        }
        int extra = Math.max(0, SheepEconomyState.getExtraLimit(player.getUniqueId()));
        int maxLimit = getMaxSheepLimit(player.getUniqueId());
        int maxExtra = maxLimit - BASE_SHEEP_LIMIT;
        return Math.min(maxExtra, extra) / LIMIT_UPGRADE_STEP;
    }

    public static int getEggSpeedLevel(Player player) {
        if (player == null) {
            return 0;
        }
        return SheepEconomyState.getEggSpeedLevel(player.getUniqueId());
    }

    public static int getEggIntervalSeconds(Player player) {
        if (player == null) {
            return BASE_EGG_INTERVAL_SECONDS;
        }
        int minEggInterval = hasSacrificeUnlock(player, SACRIFICE_UNLOCK_EGG_COOLDOWN_TO_1S)
                ? MIN_EGG_INTERVAL_SECONDS_WITH_SACRIFICE
                : MIN_EGG_INTERVAL_SECONDS;
        return Math.max(minEggInterval,
                BASE_EGG_INTERVAL_SECONDS - SheepEconomyState.getEggSpeedLevel(player.getUniqueId()));
    }

    public static int getEggSpeedMaxLevel(Player player) {
        return player == null ? 0 : getEggSpeedMaxLevel(player.getUniqueId());
    }

    private static int getEggSpeedMaxLevel(UUID playerId) {
        long computed = (long) EGG_SPEED_BASE_MAX_LEVEL
                + (long) getPrestigeHigherMaxLevel(playerId) * PRESTIGE_CAP_BONUS_PER_LEVEL;
        int hardCap = hasSacrificeUnlock(playerId, SACRIFICE_UNLOCK_EGG_COOLDOWN_TO_1S)
                ? EGG_SPEED_MAX_LEVEL + 1
                : EGG_SPEED_MAX_LEVEL;
        return computed >= hardCap ? hardCap : (int) computed;
    }

    public static int getWoolRegenMaxLevel(Player player) {
        return player == null ? 0 : getWoolRegenMaxLevel(player.getUniqueId());
    }

    private static int getWoolRegenMaxLevel(UUID playerId) {
        long computed = (long) WOOL_REGEN_BASE_MAX_LEVEL
                + (long) getPrestigeHigherMaxLevel(playerId) * PRESTIGE_CAP_BONUS_PER_LEVEL;
        return computed >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) computed;
    }

    public static int getHigherTierChanceMaxLevel(Player player) {
        return player == null ? 0 : getHigherTierChanceMaxLevel(player.getUniqueId());
    }

    private static int getHigherTierChanceMaxLevel(UUID playerId) {
        long computed = (long) HIGHER_TIER_CHANCE_BASE_MAX_LEVEL
                + (long) getPrestigeHigherMaxLevel(playerId) * PRESTIGE_CAP_BONUS_PER_LEVEL;
        long softCapped = Math.min(computed, HIGHER_TIER_CHANCE_MAX_LEVEL);
        return (int) Math.min(softCapped, HIGHER_TIER_CHANCE_HARD_MAX_LEVEL);
    }

    public static int getWoolRegenLevel(Player player) {
        if (player == null) {
            return 0;
        }
        return Math.max(0, SheepEconomyState.getWoolRegenLevel(player.getUniqueId()));
    }

    public static int getHigherTierChanceLevel(Player player) {
        if (player == null) {
            return 0;
        }
        return SheepEconomyState.getHigherTierChanceLevel(player.getUniqueId());
    }

    public static int getHigherTierChancePercent(Player player) {
        if (player == null) {
            return 0;
        }
        int base = Math.min(HIGHER_TIER_CHANCE_BASE_CAP_PERCENT,
                SheepEconomyState.getHigherTierChanceLevel(player.getUniqueId()) * 5);
        if (isCountAbilityActive(SheepQuestState.activeLuckyBurstUses(), SheepQuestState.luckyBurstEnabled(),
                player.getUniqueId())) {
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
                SheepEconomyState.getHigherTierChanceLevel(ownerId) * 5);
        if (isCountAbilityActive(SheepQuestState.activeLuckyBurstUses(), SheepQuestState.luckyBurstEnabled(),
                ownerId)) {
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
        return player == null ? 0 : SheepPrestigeState.getStartEggs(player.getUniqueId()) * 10;
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
        Sheep sheep = spawnOwnedSheep(player, spawnLocation);
        return sheep != null;
    }

    private static boolean spawnAutomationSheepFromSky(Player player) {
        if (player == null || player.getWorld() == null) {
            return false;
        }

        Sheep sheep = spawnOwnedSheep(player, createSkySheepSpawnLocation(player.getWorld()));
        if (sheep == null) {
            return false;
        }

        Location spawnLocation = sheep.getLocation();
        sheep.setVelocity(new Vector(0.0D, -0.1D, 0.0D));
        spawnParticle(sheep.getWorld(),
                org.bukkit.Particle.CLOUD,
                spawnLocation.clone().add(0.0D, 0.4D, 0.0D),
                10,
                0.2D,
                0.4D,
                0.2D,
                0.02D);
        playSheepSound(sheep.getWorld(), spawnLocation, Sound.ENTITY_SHEEP_AMBIENT, 0.6f, 1.45f);
        return true;
    }

    private static Sheep spawnOwnedSheep(Player player, Location spawnLocation) {
        if (player == null || spawnLocation == null || spawnLocation.getWorld() == null) {
            return null;
        }
        if (!isSheepFarmWorld(player.getWorld()) || !isFarmOwner(player, player.getWorld())) {
            return null;
        }
        if (isWorldAtLimit(player.getWorld())) {
            return null;
        }
        if (!tryConsumeEgg(player)) {
            return null;
        }

        Sheep sheep = player.getWorld().spawn(spawnLocation, Sheep.class);
        setSheepTier(sheep, rollSpawnTier(player.getWorld()));
        UUID playerId = player.getUniqueId();
        SheepLifetimeProgressState.incrementLifetimeSpawns(playerId);
        if (isCountAbilityActive(SheepQuestState.activeLuckyBurstUses(), SheepQuestState.luckyBurstEnabled(),
                playerId)) {
            consumeCountAbilityUse(SheepQuestState.activeLuckyBurstUses(), playerId);
            saveData();
        }
        return sheep;
    }

    public static int getPrestigeDoublePointsChanceLevel(Player player) {
        return player == null ? 0
                : Math.min(PRESTIGE_DOUBLE_POINTS_MAX_LEVEL,
                        Math.max(0, SheepPrestigeState.getDoublePointsChance(player.getUniqueId())));
    }

    public static int getDoublePointsChancePercent(Player player) {
        return Math.min(100, getPrestigeDoublePointsChanceLevel(player) * 5);
    }

    public static int getPrestigeHigherMaxLevel(Player player) {
        if (player == null) {
            return 0;
        }
        return getPrestigeHigherMaxLevel(player.getUniqueId());
    }

    private static int getPrestigeHigherMaxLevel(UUID playerId) {
        if (playerId == null) {
            return 0;
        }
        int raw = SheepPrestigeState.getHigherMaxLevel(playerId);
        return Math.max(0, raw);
    }

    private static int getMaxSheepLimit(UUID playerId) {
        return hasSacrificeUnlock(playerId, SACRIFICE_UNLOCK_MAX_SHEEP_100)
                ? MAX_SHEEP_LIMIT + SACRIFICE_UNLOCK_MAX_SHEEP_BONUS
                : MAX_SHEEP_LIMIT;
    }

    private static boolean clampUpgradeLevelsToCurrentCaps(UUID playerId) {
        if (playerId == null) {
            return false;
        }

        boolean changed = false;

        int maxExtra = Math.max(0, getMaxSheepLimit(playerId) - BASE_SHEEP_LIMIT);
        int rawExtra = Math.max(0, SheepEconomyState.getExtraLimit(playerId));
        int clampedExtra = Math.min(maxExtra, rawExtra);
        if (clampedExtra <= 0) {
            changed |= SheepEconomyState.removeExtraLimit(playerId);
        } else if (clampedExtra != rawExtra) {
            SheepEconomyState.setExtraLimit(playerId, clampedExtra);
            changed = true;
        }

        int rawEggSpeed = Math.max(0, SheepEconomyState.getEggSpeedLevel(playerId));
        int clampedEggSpeed = Math.min(rawEggSpeed, getEggSpeedMaxLevel(playerId));
        if (clampedEggSpeed <= 0) {
            changed |= SheepEconomyState.removeEggSpeedLevel(playerId);
        } else if (clampedEggSpeed != rawEggSpeed) {
            SheepEconomyState.setEggSpeedLevel(playerId, clampedEggSpeed);
            changed = true;
        }

        int rawWool = Math.max(0, SheepEconomyState.getWoolRegenLevel(playerId));
        int clampedWool = Math.min(rawWool, getWoolRegenMaxLevel(playerId));
        if (clampedWool <= 0) {
            changed |= SheepEconomyState.removeWoolRegenLevel(playerId);
        } else if (clampedWool != rawWool) {
            SheepEconomyState.setWoolRegenLevel(playerId, clampedWool);
            changed = true;
        }

        int rawChance = Math.max(0, SheepEconomyState.getHigherTierChanceLevel(playerId));
        int clampedChance = Math.min(rawChance, getHigherTierChanceMaxLevel(playerId));
        if (clampedChance <= 0) {
            changed |= SheepEconomyState.removeHigherTierChanceLevel(playerId);
        } else if (clampedChance != rawChance) {
            SheepEconomyState.setHigherTierChanceLevel(playerId, clampedChance);
            changed = true;
        }

        return changed;
    }

    public static int getPrestigeStartEggsLevel(Player player) {
        return player == null ? 0 : SheepPrestigeState.getStartEggs(player.getUniqueId());
    }

    public static int getPrestigeQuestRewardLevel(Player player) {
        return player == null ? 0 : SheepPrestigeState.getQuestReward(player.getUniqueId());
    }

    public static int getPrestigeEggCapLevel(Player player) {
        return player == null ? 0 : SheepPrestigeState.getEggCap(player.getUniqueId());
    }

    public static int getBaseSpawnTierLevel(Player player) {
        return player == null ? 0
                : Math.min(
                        SheepTier.RAINBOW.getLevel(),
                        SheepPrestigeState.getBaseSpawnTier(player.getUniqueId()));
    }

    public static int getBaseSpawnTierLevel(World world) {
        UUID ownerId = getOwnerId(world);
        if (ownerId == null) {
            return 0;
        }
        return Math.min(
                SheepTier.RAINBOW.getLevel(),
                SheepPrestigeState.getBaseSpawnTier(ownerId));
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
        return player == null ? 0 : SheepQuestState.questUpgradeDurations().getOrDefault(player.getUniqueId(), 0);
    }

    public static int getQuestUpgradePowerLevel(Player player) {
        return player == null ? 0 : SheepQuestState.questUpgradePowers().getOrDefault(player.getUniqueId(), 0);
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
        return true;
    }

    private static void consumeCountAbilityUse(Map<UUID, Integer> remainingUsesByPlayer, UUID playerId) {
        consumeCountAbilityUses(remainingUsesByPlayer, playerId, 1);
    }

    private static void consumeCountAbilityUses(Map<UUID, Integer> remainingUsesByPlayer, UUID playerId,
            int useCount) {
        if (remainingUsesByPlayer == null || playerId == null) {
            return;
        }
        int usesToConsume = Math.max(0, useCount);
        if (usesToConsume <= 0) {
            return;
        }
        int current = Math.max(0, remainingUsesByPlayer.getOrDefault(playerId, 0));
        if (current <= usesToConsume) {
            remainingUsesByPlayer.remove(playerId);
        } else {
            remainingUsesByPlayer.put(playerId, current - usesToConsume);
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
        if (sound == Sound.ENTITY_SHEEP_SHEAR) {
            playSheepSound(player, sound, 1.0f, 1.2f);
        } else {
            playSound(player, sound, 1.0f, 1.2f);
        }
        spawnParticle(player, particle, player.getLocation().add(0, 2.0, 0), 25, 0.35, 0.5, 0.35, 0.02);
        return true;
    }

    private static boolean extendQuestAbility(Player player, Map<UUID, Long> activeUntil, int questPointCost,
            long durationMs, Sound sound, org.bukkit.Particle particle) {
        if (player == null || activeUntil == null) {
            return false;
        }
        if (!trySpendQuestPoints(player, questPointCost)) {
            return false;
        }
        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();
        long currentRemaining = Math.max(0L, activeUntil.getOrDefault(playerId, 0L) - now);
        long nextUntil = now + currentRemaining + durationMs;
        activeUntil.put(playerId, nextUntil);
        if (sound == Sound.ENTITY_SHEEP_SHEAR) {
            playSheepSound(player, sound, 1.0f, 1.2f);
        } else {
            playSound(player, sound, 1.0f, 1.2f);
        }
        spawnParticle(player, particle, player.getLocation().add(0, 2.0, 0), 25, 0.35, 0.5, 0.35, 0.02);
        saveData();
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
        if (sound == Sound.ENTITY_SHEEP_SHEAR) {
            playSheepSound(player, sound, 1.0f, 1.2f);
        } else {
            playSound(player, sound, 1.0f, 1.2f);
        }
        spawnParticle(player, particle, player.getLocation().add(0, 2.0, 0), 25, 0.35, 0.5, 0.35, 0.02);
        saveData();
        return true;
    }

    private static boolean upgradeQuestDuration(Player player) {
        int cost = getQuestUpgradeDurationCost(player);
        if (!trySpendQuestPoints(player, cost)) {
            return false;
        }
        SheepQuestState.questUpgradeDurations().put(player.getUniqueId(), getQuestUpgradeDurationLevel(player) + 1);
        saveData();
        return true;
    }

    private static boolean upgradeQuestPower(Player player) {
        int cost = getQuestUpgradePowerCost(player);
        if (!trySpendQuestPoints(player, cost)) {
            return false;
        }
        SheepQuestState.questUpgradePowers().put(player.getUniqueId(), getQuestUpgradePowerLevel(player) + 1);
        saveData();
        return true;
    }

    public static long getPrestigeRefundRemainingMs(Player player) {
        if (player == null) {
            return 0L;
        }
        long nextRefund = SheepPrestigeState.getNextRefundTimestamp(player.getUniqueId());
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
        SheepPrestigeState.resetUpgrades(playerId, clearRefundCooldown);
        SheepComboState.resetMaxUpgrade(playerId, COMBO_BASE_MAX_SCORE);
        clampUpgradeLevelsToCurrentCaps(playerId);
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
        long nextRefund = SheepPrestigeState.getNextRefundTimestamp(player.getUniqueId());
        if (now < nextRefund) {
            return false;
        }

        int refund = getPrestigeRefundAmount(player);
        if (refund <= 0) {
            return false;
        }

        UUID playerId = player.getUniqueId();
        SheepPrestigeState.setPoints(playerId, addSaturated(getPrestigePoints(player), refund));
        resetPrestigeUpgrades(playerId, false);
        SheepPrestigeState.setNextRefundTimestamp(playerId, now + PRESTIGE_REFUND_COOLDOWN_MS);
        saveData();
        return true;
    }

    private static int getRebirthRespecRefundAmount(Player player) {
        if (player == null) {
            return 0;
        }
        int spent = 0;
        int mask = getRebirthSkillUnlockMask(player.getUniqueId());
        for (RebirthSkillNode node : REBIRTH_SKILL_NODES) {
            if ((mask & getRebirthSkillBit(node.id)) != 0) {
                spent += getRebirthSkillCost(node);
            }
        }
        return Math.max(0, spent);
    }

    private static boolean tryRespecRebirthSkills(Player player) {
        if (player == null) {
            return false;
        }
        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();
        long nextRespec = SheepRebirthState.getNextRespecTimestamp(playerId);
        if (now < nextRespec) {
            return false;
        }
        if (getRebirthSkillUnlockMask(playerId) == 0) {
            return false;
        }
        SheepRebirthState.clearSkillUnlockMask(playerId);
        SheepRebirthState.clearSkillPendingMask(playerId);
        SheepRebirthState.setNextRespecTimestamp(playerId, now + REBIRTH_RESPEC_COOLDOWN_MS);
        saveData();
        return true;
    }

    private static String formatDuration(long durationMs) {
        return SheepFormatting.formatDuration(durationMs);
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
        return remaining > 0L ? "&aStatus: ON &7(" + formatDuration(remaining) + " left)" : "&8Status: OFF";
    }

    private static String getAbilityScoreLine(String label, Map<UUID, Long> activeUntil,
            Map<UUID, Long> pausedRemainingMsByPlayer, UUID playerId) {
        long remaining = getAbilityRemainingMs(activeUntil, playerId);
        return color((remaining > 0L ? "&d" : "&8") + label + "&8: &f"
                + (remaining > 0L ? formatDuration(remaining) : "inactive"));
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
            return color("&8" + label + "&8: &finactive");
        }
        return color("&d" + label + "&8: &f" + remaining + " uses "
                + (enabledByPlayer.getOrDefault(playerId, true) ? "&aON" : "&cOFF"));
    }

    private static long getQuestResetRemainingMs(Player player) {
        if (player == null) {
            return 0L;
        }
        long now = System.currentTimeMillis();
        long nextReset = SheepQuestState.nextQuestResetTimestamps().getOrDefault(player.getUniqueId(), 0L);
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
        SheepPrestigeState.setPoints(player.getUniqueId(), current - points);
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
                + " Coins for your prestige step."));
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
        SheepPrestigeState.setDoublePointsChance(player.getUniqueId(), getPrestigeDoublePointsChanceLevel(player) + 1);
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
        SheepPrestigeState.setHigherMaxLevel(player.getUniqueId(), current + 1);
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
        SheepPrestigeState.setStartEggs(player.getUniqueId(), getPrestigeStartEggsLevel(player) + 1);
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
        SheepPrestigeState.setEggCap(player.getUniqueId(), getPrestigeEggCapLevel(player) + 1);
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
        SheepPrestigeState.setBaseSpawnTier(player.getUniqueId(), currentLevel + 1);
        upgradeSheepBelowMinimumSpawnTier(player.getWorld());
        saveData();
        return true;
    }

    private static boolean upgradePrestigeQuestReward(Player player) {
        if (player == null) {
            return false;
        }
        int currentLevel = getPrestigeQuestRewardLevel(player);
        int cost = getPrestigeQuestRewardCost(player);
        if (!trySpendPrestigePoints(player, cost)) {
            return false;
        }
        SheepPrestigeState.setQuestReward(player.getUniqueId(), currentLevel + 1);
        saveData();
        return true;
    }

    public static void resetEggTimer(Player player) {
        EGG_MODULE.resetEggTimer(player);
    }

    public static void clearEggTimer(Player player) {
        EGG_MODULE.clearEggTimer(player);
    }

    public static void preserveEggCountForDeath(Player player) {
        EGG_MODULE.preserveEggCountForDeath(player);
    }

    public static void restoreEggCountAfterDeath(Player player) {
        EGG_MODULE.restoreEggCountAfterDeath(player);
    }

    public static void openUpgradeMenu(Player player) {
        if (player == null) {
            return;
        }
        markTutorialUpgradeOpened(player);

        Inventory inventory = Bukkit.createInventory(null, 27, UPGRADE_MENU_TITLE);
        inventory.setItem(LAYOUTS_MENU_OPEN_SLOT, MenuItemFactory.create(
                Material.ENDER_CHEST,
                "Settings",
                List.of(
                        "Scoreboard, inventory, sounds, particles, and visits",
                        "Configure settings for your farm and UI",
                        "Click to open")));
        int limitLevel = getLimitUpgradeLevel(player);
        int currentLimit = getPlayerLimit(player);
        int maxLimit = getMaxSheepLimit(player.getUniqueId());
        boolean limitMaxed = currentLimit >= maxLimit;
        BigInteger limitCost = getUpgradeCost(player);
        inventory.setItem(LIMIT_UPGRADE_SLOT, MenuItemFactory.create(
                Material.OAK_FENCE,
                "Sheep Limit",
                List.of(
                        "Level: " + limitLevel + " / " + ((maxLimit - BASE_SHEEP_LIMIT) / LIMIT_UPGRADE_STEP),
                        "Current limit: " + currentLimit + " / " + maxLimit,
                        "Next: " + currentLimit + " -> "
                                + Math.min(maxLimit, currentLimit + getLimitUpgradeStep()),
                        limitMaxed ? "MAXED" : "Cost: " + formatPoints(limitCost) + " Coins",
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
                                : "Cost: " + formatPoints(eggCost) + " Coins",
                        "Click to purchase")));

        int woolLevel = getWoolRegenLevel(player);
        int woolMaxLevel = getWoolRegenMaxLevel(player);
        String woolCurrentCooldownPercent = getWoolCooldownPercentDisplayAtLevel(player, woolLevel);
        String woolCurrentReductionPercent = getWoolCooldownReductionPercentDisplayAtLevel(player, woolLevel);
        String woolCurrentFactor = getWoolCooldownFactorDisplayAtLevel(player, woolLevel);
        int woolNextLevel = Math.min(woolMaxLevel, woolLevel + 1);
        String woolNextCooldownPercent = getWoolCooldownPercentDisplayAtLevel(player, woolNextLevel);
        String woolNextReductionPercent = getWoolCooldownReductionPercentDisplayAtLevel(player, woolNextLevel);
        String woolNextFactor = getWoolCooldownFactorDisplayAtLevel(player, woolNextLevel);
        BigInteger woolCost = getWoolRegenUpgradeCost(player);
        inventory.setItem(WOOL_REGEN_UPGRADE_SLOT, MenuItemFactory.create(
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
                                : "Cost: " + formatPoints(woolCost) + " Coins",
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
                                : "Cost: " + formatPoints(chanceCost) + " Coins",
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
                        "Shear " + SheepQuestState.questShears().getOrDefault(player.getUniqueId(), 0) + "/"
                                + getQuestTarget(player, QUEST_SHEARS_TARGET),
                        "Spawn " + SheepQuestState.questSpawns().getOrDefault(player.getUniqueId(), 0) + "/"
                                + getQuestTarget(player, QUEST_SPAWNS_TARGET),
                        "Merge " + SheepQuestState.questMerges().getOrDefault(player.getUniqueId(), 0) + "/"
                                + getQuestTarget(player, QUEST_MERGES_TARGET),
                        "Click to open")));

        inventory.setItem(SHOP_MENU_OPEN_SLOT, MenuItemFactory.create(
                Material.SHEARS,
                "Shear Shop",
                List.of(
                        "Shear level: " + getShearPointGainUpgradeLevel(player),
                        "Coin multiplier: x" + getShearPointMultiplier(player),
                        "Click to open")));

        inventory.setItem(COMBO_MENU_OPEN_SLOT, MenuItemFactory.create(
                Material.BLAZE_POWDER,
                "Combo Upgrades",
                List.of(
                        "Combo score: " + (int) Math.floor(getComboScore(player))
                                + " / " + (int) Math.floor(getComboMaxScore(player)),
                        "Coins x" + formatComboMultiplier(getComboMultiplier(player, getComboScore(player))),
                        "Click to open")));

        inventory.setItem(AUTOMATION_MENU_OPEN_SLOT, MenuItemFactory.create(
                Material.REDSTONE,
                "Automation",
                List.of(
                        "Automation points: " + formatPoints(getAutomationPoints(player)),
                        "Gain: +1 per " + formatDuration(AUTOMATION_POINT_INTERVAL_MS),
                        "Click to open")));

        inventory.setItem(ACHIEVEMENTS_MENU_OPEN_SLOT, createAchievementsMenuOpenItem());

        inventory.setItem(SACRIFICE_MENU_OPEN_SLOT, MenuItemFactory.create(
                Material.TOTEM_OF_UNDYING,
                "Sacrifice",
                List.of(
                        "Sacrifice points: " + formatPoints(getSacrificePoints(player)),
                        "Unlocks bought: " + getSacrificeUnlocksBought(player) + " / "
                                + SACRIFICE_UNLOCK_MAX_SHEEP_100,
                        "Click to open")));

        int rebirthLevel = getRebirthLevel(player);
        int affordableRebirths = getAffordableRebirthLevels(player);
        int rebirthReward = getRebirthPointsRewardForNextLevels(rebirthLevel, affordableRebirths);
        inventory.setItem(REBIRTH_MENU_OPEN_SLOT, MenuItemFactory.create(
                Material.DRAGON_EGG,
                "Rebirth",
                List.of(
                        "Rebirth level: " + rebirthLevel,
                        "Rebirth points: " + formatPoints(getRebirthPoints(player)),
                        "Next cost: " + getRebirthCostInPrestigeLevels(rebirthLevel) + " prestige levels",
                        "Consumes prestige levels and resets prestige progress",
                        affordableRebirths > 0
                                ? "Buy now: +" + affordableRebirths + " rebirth level(s), +"
                                        + formatPoints(rebirthReward) + " rebirth points"
                                : "Buy now: +0 rebirth level(s)",
                        "Click to open")));

        inventory.setItem(SOCIALS_MENU_OPEN_SLOT, createSocialsMenuOpenItem());

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

    public static boolean isAchievementsMenuTitle(String title) {
        return ACHIEVEMENTS_MENU_TITLE.equals(title);
    }

    public static boolean isAchievementsViewMenuTitle(String title) {
        return ACHIEVEMENTS_VIEW_MENU_TITLE.equals(title);
    }

    public static boolean isAchievementsUpgradesMenuTitle(String title) {
        return ACHIEVEMENTS_UPGRADES_MENU_TITLE.equals(title);
    }

    public static boolean isSacrificeMenuTitle(String title) {
        return SACRIFICE_MENU_TITLE.equals(title);
    }

    public static boolean isRebirthMenuTitle(String title) {
        return REBIRTH_MENU_TITLE.equals(title);
    }

    public static boolean isRebirthTreeMenuTitle(String title) {
        return REBIRTH_TREE_MENU_TITLE.equals(title);
    }

    public static boolean isScoreboardMenuTitle(String title) {
        return SCOREBOARD_MENU_TITLE.equals(title);
    }

    public static boolean isSocialsMenuTitle(String title) {
        return SOCIALS_MENU_TITLE.equals(title);
    }

    public static void tickOpenMenuStatRefresh(Player player) {
        if (player == null || player.getOpenInventory() == null) {
            return;
        }
        String title = player.getOpenInventory().getTitle();
        Inventory openInventory = player.getOpenInventory().getTopInventory();
        if (openInventory == null) {
            return;
        }

        if (isUpgradeMenuTitle(title)) {
            refreshOpenUpgradeMenuItems(player, openInventory);
        } else if (isPrestigeMenuTitle(title)) {
            refreshOpenPrestigeMenuItems(player, openInventory);
        } else if (isQuestMenuTitle(title)) {
            refreshOpenQuestMenuItems(player, openInventory);
        } else if (isShopMenuTitle(title)) {
            refreshOpenShopMenuItems(player, openInventory);
        } else if (isComboShopMenuTitle(title)) {
            refreshOpenComboMenuItems(player, openInventory);
        } else if (isAutomationMenuTitle(title)) {
            refreshOpenAutomationMenuItems(player, openInventory);
        } else if (isSocialsMenuTitle(title)) {
            refreshOpenSocialsMenuItems(player, openInventory);
        }
    }

    private static void refreshOpenUpgradeMenuItems(Player player, Inventory inventory) {
        if (player == null || inventory == null) {
            return;
        }
        setMenuItemIfChanged(inventory, LAYOUTS_MENU_OPEN_SLOT, MenuItemFactory.create(
                Material.ENDER_CHEST,
                "Settings",
                List.of(
                        "Scoreboard, inventory, sounds, particles, and visits",
                        "Configure settings for your farm and UI",
                        "Click to open")));

        int limitLevel = getLimitUpgradeLevel(player);
        int currentLimit = getPlayerLimit(player);
        int maxLimit = getMaxSheepLimit(player.getUniqueId());
        boolean limitMaxed = currentLimit >= maxLimit;
        BigInteger limitCost = getUpgradeCost(player);
        setMenuItemIfChanged(inventory, LIMIT_UPGRADE_SLOT, MenuItemFactory.create(
                Material.OAK_FENCE,
                "Sheep Limit",
                List.of(
                        "Level: " + limitLevel + " / " + ((maxLimit - BASE_SHEEP_LIMIT) / LIMIT_UPGRADE_STEP),
                        "Current limit: " + currentLimit + " / " + maxLimit,
                        "Next: " + currentLimit + " -> "
                                + Math.min(maxLimit, currentLimit + getLimitUpgradeStep()),
                        limitMaxed ? "MAXED" : "Cost: " + formatPoints(limitCost) + " Coins",
                        limitMaxed ? "Limit cap reached" : "Click to purchase")));

        int eggLevel = getEggSpeedLevel(player);
        int eggMaxLevel = getEggSpeedMaxLevel(player);
        int eggCurrentSeconds = getEggIntervalSeconds(player);
        int eggNextLevel = Math.min(eggMaxLevel, eggLevel + 1);
        int eggNextSeconds = Math.max(MIN_EGG_INTERVAL_SECONDS, BASE_EGG_INTERVAL_SECONDS - eggNextLevel);
        BigInteger eggCost = getEggSpeedUpgradeCost(player);
        setMenuItemIfChanged(inventory, EGG_SPEED_UPGRADE_SLOT, MenuItemFactory.create(
                Material.CLOCK,
                "Faster Egg Spawn",
                List.of(
                        "Level: " + eggLevel + " / " + eggMaxLevel,
                        "Current: " + eggCurrentSeconds + "s per egg",
                        eggLevel >= eggMaxLevel
                                ? "Next: MAXED"
                                : "Next: " + eggCurrentSeconds + "s -> " + eggNextSeconds + "s",
                        eggLevel >= eggMaxLevel ? "MAXED"
                                : "Cost: " + formatPoints(eggCost) + " Coins",
                        "Click to purchase")));

        int woolLevel = getWoolRegenLevel(player);
        int woolMaxLevel = getWoolRegenMaxLevel(player);
        String woolCurrentCooldownPercent = getWoolCooldownPercentDisplayAtLevel(player, woolLevel);
        String woolCurrentReductionPercent = getWoolCooldownReductionPercentDisplayAtLevel(player, woolLevel);
        String woolCurrentFactor = getWoolCooldownFactorDisplayAtLevel(player, woolLevel);
        int woolNextLevel = Math.min(woolMaxLevel, woolLevel + 1);
        String woolNextCooldownPercent = getWoolCooldownPercentDisplayAtLevel(player, woolNextLevel);
        String woolNextReductionPercent = getWoolCooldownReductionPercentDisplayAtLevel(player, woolNextLevel);
        String woolNextFactor = getWoolCooldownFactorDisplayAtLevel(player, woolNextLevel);
        BigInteger woolCost = getWoolRegenUpgradeCost(player);
        setMenuItemIfChanged(inventory, WOOL_REGEN_UPGRADE_SLOT, MenuItemFactory.create(
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
                                : "Cost: " + formatPoints(woolCost) + " Coins",
                        "Click to purchase")));

        int chanceLevel = getHigherTierChanceLevel(player);
        int chanceMaxLevel = getHigherTierChanceMaxLevel(player);
        int chanceCurrentPercent = Math.min(HIGHER_TIER_CHANCE_BASE_CAP_PERCENT, chanceLevel * 5);
        int chanceNextLevel = Math.min(chanceMaxLevel, chanceLevel + 1);
        int chanceNextPercent = Math.min(HIGHER_TIER_CHANCE_BASE_CAP_PERCENT, chanceNextLevel * 5);
        BigInteger chanceCost = getHigherTierChanceUpgradeCost(player);
        setMenuItemIfChanged(inventory, HIGHER_TIER_CHANCE_UPGRADE_SLOT, MenuItemFactory.create(
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
                                : "Cost: " + formatPoints(chanceCost) + " Coins",
                        "Click to purchase")));

        setMenuItemIfChanged(inventory, PRESTIGE_MENU_OPEN_SLOT, MenuItemFactory.create(
                Material.NETHER_STAR,
                "Prestige Upgrades",
                List.of(
                        "Prestige level: " + getPrestigeLevel(player),
                        "Prestige points: " + formatPoints(getPrestigePoints(player)),
                        "Click to open")));

        setMenuItemIfChanged(inventory, QUEST_MENU_OPEN_SLOT, MenuItemFactory.create(
                Material.BOOK,
                "Quests",
                List.of(
                        "Quest points: " + formatPoints(getQuestPoints(player)),
                        "Next reset: " + formatDuration(getQuestResetRemainingMs(player)),
                        "Shear " + SheepQuestState.questShears().getOrDefault(player.getUniqueId(), 0) + "/"
                                + getQuestTarget(player, QUEST_SHEARS_TARGET),
                        "Spawn " + SheepQuestState.questSpawns().getOrDefault(player.getUniqueId(), 0) + "/"
                                + getQuestTarget(player, QUEST_SPAWNS_TARGET),
                        "Merge " + SheepQuestState.questMerges().getOrDefault(player.getUniqueId(), 0) + "/"
                                + getQuestTarget(player, QUEST_MERGES_TARGET),
                        "Click to open")));

        setMenuItemIfChanged(inventory, SHOP_MENU_OPEN_SLOT, MenuItemFactory.create(
                Material.SHEARS,
                "Shear Shop",
                List.of(
                        "Shear level: " + getShearPointGainUpgradeLevel(player),
                        "Coin multiplier: x" + getShearPointMultiplier(player),
                        "Click to open")));

        setMenuItemIfChanged(inventory, COMBO_MENU_OPEN_SLOT, MenuItemFactory.create(
                Material.BLAZE_POWDER,
                "Combo Upgrades",
                List.of(
                        "Combo score: " + (int) Math.floor(getComboScore(player))
                                + " / " + (int) Math.floor(getComboMaxScore(player)),
                        "Coins x" + formatComboMultiplier(getComboMultiplier(player, getComboScore(player))),
                        "Click to open")));

        setMenuItemIfChanged(inventory, AUTOMATION_MENU_OPEN_SLOT, MenuItemFactory.create(
                Material.REDSTONE,
                "Automation",
                List.of(
                        "Automation points: " + formatPoints(getAutomationPoints(player)),
                        "Gain: +1 per " + formatDuration(AUTOMATION_POINT_INTERVAL_MS),
                        "Click to open")));

        setMenuItemIfChanged(inventory, ACHIEVEMENTS_MENU_OPEN_SLOT, createAchievementsMenuOpenItem());

        setMenuItemIfChanged(inventory, SACRIFICE_MENU_OPEN_SLOT, MenuItemFactory.create(
                Material.TOTEM_OF_UNDYING,
                "Sacrifice",
                List.of(
                        "Sacrifice points: " + formatPoints(getSacrificePoints(player)),
                        "Unlocks bought: " + getSacrificeUnlocksBought(player) + " / "
                                + SACRIFICE_UNLOCK_MAX_SHEEP_100,
                        "Click to open")));

        int rebirthLevel = getRebirthLevel(player);
        int affordableRebirths = getAffordableRebirthLevels(player);
        int rebirthReward = getRebirthPointsRewardForNextLevels(rebirthLevel, affordableRebirths);
        setMenuItemIfChanged(inventory, REBIRTH_MENU_OPEN_SLOT, MenuItemFactory.create(
                Material.DRAGON_EGG,
                "Rebirth",
                List.of(
                        "Rebirth level: " + rebirthLevel,
                        "Rebirth points: " + formatPoints(getRebirthPoints(player)),
                        "Next cost: " + getRebirthCostInPrestigeLevels(rebirthLevel) + " prestige levels",
                        "Consumes prestige levels and resets prestige progress",
                        affordableRebirths > 0
                                ? "Buy now: +" + affordableRebirths + " rebirth level(s), +"
                                        + formatPoints(rebirthReward) + " rebirth points"
                                : "Buy now: +0 rebirth level(s)",
                        "Click to open")));

        setMenuItemIfChanged(inventory, SOCIALS_MENU_OPEN_SLOT, createSocialsMenuOpenItem());
    }

    private static void refreshOpenSocialsMenuItems(Player player, Inventory inventory) {
        if (player == null || inventory == null) {
            return;
        }
        populateSocialsMenuItems(player, inventory, getCurrentSocialsPage(player));
    }

    private static void refreshOpenPrestigeMenuItems(Player player, Inventory inventory) {
        if (player == null || inventory == null) {
            return;
        }
        int affordablePrestiges = getAffordablePrestigeLevels(player);
        BigInteger totalCostForAffordable = getTotalPrestigeCostForNextLevels(getPrestigeLevel(player),
                affordablePrestiges);
        int rewardForAffordable = getPrestigePointsRewardForNextLevels(getPrestigeLevel(player), affordablePrestiges);
        setMenuItemIfChanged(inventory, PRESTIGE_UPGRADE_SLOT, MenuItemFactory.create(
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
                        "Next prestige cost: " + formatPoints(getPrestigeCostBig(player)) + " Coins",
                        affordablePrestiges > 0
                                ? "Total cost now: " + formatPoints(totalCostForAffordable) + " Coins"
                                : "Need more Coins for next prestige",
                        "Resets Coin upgrades",
                        "Click to prestige multiple")));

        setMenuItemIfChanged(inventory, PRESTIGE_DOUBLE_POINTS_SLOT, MenuItemFactory.create(
                Material.EMERALD,
                "Double Coins Chance",
                List.of(
                        "Level: " + getPrestigeDoublePointsChanceLevel(player) + " / "
                                + PRESTIGE_DOUBLE_POINTS_MAX_LEVEL,
                        "Chance: " + getDoublePointsChancePercent(player) + "%",
                        getPrestigeDoublePointsChanceLevel(player) >= PRESTIGE_DOUBLE_POINTS_MAX_LEVEL
                                ? "MAXED"
                                : "Cost: " + formatPoints(getPrestigeDoublePointsCost(player))
                                        + " prestige points",
                        "Click to purchase")));

        int baseSpawnTierLevel = getBaseSpawnTierLevel(player);
        SheepTier baseSpawnTier = SheepTier.byLevel(baseSpawnTierLevel);
        setMenuItemIfChanged(inventory, PRESTIGE_BASE_SPAWN_TIER_SLOT, MenuItemFactory.create(
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
        setMenuItemIfChanged(inventory, PRESTIGE_QUEST_REWARD_SLOT, MenuItemFactory.create(
                Material.BOOK,
                "Quest Reward Boost",
                List.of(
                        "Level: " + questRewardLevel,
                        "Quest rewards: +"
                                + (int) Math.round(questRewardLevel * PRESTIGE_QUEST_REWARD_BONUS_PER_LEVEL * 100)
                                + "%",
                        "Cost: " + formatPoints(getPrestigeQuestRewardCost(player)) + " prestige points",
                        "Click to purchase")));

        long refundRemaining = getPrestigeRefundRemainingMs(player);
        int refundAmount = getPrestigeRefundAmount(player);
        setMenuItemIfChanged(inventory, PRESTIGE_REFUND_SLOT, MenuItemFactory.create(
                Material.BARRIER,
                "Refund Prestige Upgrades",
                List.of(
                        "Refund amount: " + formatPoints(refundAmount) + " prestige points",
                        refundRemaining > 0L ? "Cooldown: " + formatDuration(refundRemaining) : "Cooldown: ready",
                        "Resets prestige shop upgrades",
                        "Click to refund")));
    }

    private static void refreshOpenQuestMenuItems(Player player, Inventory inventory) {
        if (player == null || inventory == null) {
            return;
        }
        UUID playerId = player.getUniqueId();
        long remaining = getQuestResetRemainingMs(player);
        boolean shearsComplete = SheepQuestState.questShearsComplete().getOrDefault(playerId, false);
        boolean spawnsComplete = SheepQuestState.questSpawnsComplete().getOrDefault(playerId, false);
        boolean mergesComplete = SheepQuestState.questMergesComplete().getOrDefault(playerId, false);
        int currentQuestPoints = getQuestPoints(player);
        int luckyCost = getQuestLuckyBurstCost(player);
        int woolRushCost = getQuestWoolRushCost(player);
        int jackpotCost = getQuestJackpotCost(player);
        int autoMergeCost = getQuestAutoMergeCost(player);
        int autoShearCost = getQuestAutoShearCost(player);
        boolean luckyBurstGlint = isCountAbilityActive(SheepQuestState.activeLuckyBurstUses(),
                SheepQuestState.luckyBurstEnabled(),
                playerId);
        boolean woolRushGlint = isAbilityActive(SheepQuestState.activeWoolRushUntil(), playerId);
        boolean jackpotGlint = isAbilityActive(SheepQuestState.activeJackpotShearsUntil(), playerId);
        boolean autoMergeGlint = isCountAbilityActive(SheepQuestState.activeAutoMergeUses(),
                SheepQuestState.autoMergeEnabled(),
                playerId);
        boolean autoShearGlint = isCountAbilityActive(SheepQuestState.activeAutoShearUses(),
                SheepQuestState.autoShearEnabled(),
                playerId);

        setMenuItemIfChanged(inventory, QUEST_BOARD_SLOT, MenuItemFactory.create(
                Material.BOOK,
                "Quest Board",
                List.of(
                        "Quest points: " + formatPoints(getQuestPoints(player)),
                        remaining > 0L ? "Next reset: " + formatDuration(remaining) : "Next reset: incoming",
                        (shearsComplete ? "DONE " : "TODO ")
                                + "Shear " + SheepQuestState.questShears().getOrDefault(playerId, 0) + "/"
                                + getQuestTarget(player, QUEST_SHEARS_TARGET)
                                + " (" + formatPoints(getQuestReward(player, QUEST_SHEARS_REWARD)) + " pts)",
                        (spawnsComplete ? "DONE " : "TODO ")
                                + "Spawn " + SheepQuestState.questSpawns().getOrDefault(playerId, 0) + "/"
                                + getQuestTarget(player, QUEST_SPAWNS_TARGET)
                                + " (" + formatPoints(getQuestReward(player, QUEST_SPAWNS_REWARD)) + " pts)",
                        (mergesComplete ? "DONE " : "TODO ")
                                + "Merge " + SheepQuestState.questMerges().getOrDefault(playerId, 0) + "/"
                                + getQuestTarget(player, QUEST_MERGES_TARGET)
                                + " (" + formatPoints(getQuestReward(player, QUEST_MERGES_REWARD)) + " pts)")));

        setMenuItemIfChanged(inventory, QUEST_ABILITY_LUCKY_BURST_SLOT, MenuItemFactory.create(
                Material.ENDER_EYE,
                "Lucky Burst",
                List.of(
                        "&6Cost: &f" + formatPoints(luckyCost) + " qp",
                        "&bBoost: &f+" + QUEST_LUCKY_BURST_SPAWN_CHANCE_BONUS_PERCENT + "% tier chance",
                        "&7Uses: &f" + getAbilityUseCount(player, QUEST_LUCKY_BURST_BASE_DURATION_MS),
                        currentQuestPoints >= luckyCost ? "&aReady to buy" : "&cNeed more quest points",
                        getCountAbilityMenuStatus(SheepQuestState.activeLuckyBurstUses(),
                                SheepQuestState.luckyBurstEnabled(), playerId),
                        getCountAbilityToggleActionLine(SheepQuestState.activeLuckyBurstUses(),
                                SheepQuestState.luckyBurstEnabled(),
                                playerId)),
                luckyBurstGlint));

        setMenuItemIfChanged(inventory, QUEST_ABILITY_WOOL_RUSH_SLOT, MenuItemFactory.create(
                Material.WHITE_WOOL,
                "Wool Rush",
                List.of(
                        "&6Cost: &f" + formatPoints(woolRushCost) + " qp",
                        "&bBoost: &fwool grows 90% faster",
                        "&7Time: &f" + formatDuration(getAbilityDurationMs(player, QUEST_WOOL_RUSH_BASE_DURATION_MS)),
                        currentQuestPoints >= woolRushCost ? "&aReady to buy" : "&cNeed more quest points",
                        getAbilityMenuStatus(SheepQuestState.activeWoolRushUntil(), null, playerId),
                        isAbilityActive(SheepQuestState.activeWoolRushUntil(), playerId)
                                ? "&eClick: Extend"
                                : "&aClick: Activate"),
                woolRushGlint));

        setMenuItemIfChanged(inventory, QUEST_ABILITY_JACKPOT_SHEARS_SLOT, MenuItemFactory.create(
                Material.GOLD_INGOT,
                "Jackpot Shears",
                List.of(
                        "&6Cost: &f" + formatPoints(jackpotCost) + " qp",
                        "&bBoost: &fx" + (2 + getQuestUpgradePowerLevel(player)) + " shear Coins",
                        "&7Time: &f"
                                + formatDuration(getAbilityDurationMs(player, QUEST_JACKPOT_SHEARS_BASE_DURATION_MS)),
                        currentQuestPoints >= jackpotCost ? "&aReady to buy" : "&cNeed more quest points",
                        getAbilityMenuStatus(SheepQuestState.activeJackpotShearsUntil(), null, playerId),
                        isAbilityActive(SheepQuestState.activeJackpotShearsUntil(), playerId)
                                ? "&eClick: Extend"
                                : "&aClick: Activate"),
                jackpotGlint));

        setMenuItemIfChanged(inventory, QUEST_ABILITY_AUTO_MERGE_SLOT, MenuItemFactory.create(
                Material.ANVIL,
                "Merge Assist",
                List.of(
                        "&6Cost: &f" + formatPoints(autoMergeCost) + " qp",
                        "&bBoost: &fauto-merges carried sheep",
                        "&7Uses: &f" + getAbilityUseCount(player, QUEST_AUTO_MERGE_BASE_DURATION_MS),
                        currentQuestPoints >= autoMergeCost ? "&aReady to buy" : "&cNeed more quest points",
                        getCountAbilityMenuStatus(SheepQuestState.activeAutoMergeUses(),
                                SheepQuestState.autoMergeEnabled(), playerId),
                        getCountAbilityToggleActionLine(SheepQuestState.activeAutoMergeUses(),
                                SheepQuestState.autoMergeEnabled(),
                                playerId)),
                autoMergeGlint));

        setMenuItemIfChanged(inventory, QUEST_ABILITY_AUTO_SHEAR_SLOT, MenuItemFactory.create(
                Material.SHEARS,
                "Shear All Sheep",
                List.of(
                        "&6Cost: &f" + formatPoints(autoShearCost) + " qp",
                        "&bBoost: &fshears every ready sheep",
                        "&7Uses: &f" + getAbilityUseCount(player, QUEST_AUTO_SHEAR_BASE_DURATION_MS),
                        currentQuestPoints >= autoShearCost ? "&aReady to buy" : "&cNeed more quest points",
                        getCountAbilityMenuStatus(SheepQuestState.activeAutoShearUses(),
                                SheepQuestState.autoShearEnabled(), playerId),
                        getCountAbilityToggleActionLine(SheepQuestState.activeAutoShearUses(),
                                SheepQuestState.autoShearEnabled(),
                                playerId)),
                autoShearGlint));

        setMenuItemIfChanged(inventory, QUEST_OPEN_UPGRADES_SLOT, MenuItemFactory.create(
                Material.ENCHANTED_BOOK,
                "Quest Upgrades",
                List.of(
                        "&7Duration Lv: &e" + getQuestUpgradeDurationLevel(player),
                        "&7Power Lv: &e" + getQuestUpgradePowerLevel(player),
                        "&aClick: Open")));

        setMenuItemIfChanged(inventory, QUEST_BACK_TO_UPGRADES_SLOT, MenuItemFactory.create(
                Material.ARROW,
                "Back To Upgrades",
                List.of(
                        "&7Quest Points: &e" + formatPoints(getQuestPoints(player)),
                        remaining > 0L ? "&7Reset: &b" + formatDuration(remaining) : "&7Reset: &bincoming",
                        "&aClick: Back")));
    }

    private static void refreshOpenShopMenuItems(Player player, Inventory inventory) {
        if (player == null || inventory == null) {
            return;
        }
        int woolSaveLevel = getShearWoolSaveLevel(player);
        int tierBoostLevel = getShearTierBoostLevel(player);
        setMenuItemIfChanged(inventory, SHOP_SHEAR_SLOT, MenuItemFactory.create(
                Material.SHEARS,
                "Shear Value",
                List.of(
                        "Level: " + getShearPointGainUpgradeLevel(player),
                        "Cost: " + formatPoints(getShearUpgradeCost(player)) + " Coins",
                        "Coins: base x" + getShearPointMultiplier(player),
                        "Wool reward scales with level",
                        "Click to purchase")));
        setMenuItemIfChanged(inventory, SHOP_SHEAR_KEEP_WOOL_SLOT, MenuItemFactory.create(
                Material.WHITE_WOOL,
                "Wool Keeper",
                List.of(
                        "Level: " + woolSaveLevel + " / " + SHEAR_WOOL_SAVE_MAX_LEVEL,
                        "Chance: " + getShearWoolSaveChancePercent(player) + "%",
                        woolSaveLevel >= SHEAR_WOOL_SAVE_MAX_LEVEL
                                ? "MAXED"
                                : "Cost: " + formatPoints(getShearWoolSaveUpgradeCost(player)) + " Coins",
                        "Chance for sheep to keep wool when sheared")));
        setMenuItemIfChanged(inventory, SHOP_SHEAR_TIER_BOOST_SLOT, MenuItemFactory.create(
                Material.GLOWSTONE_DUST,
                "Tier Booster",
                List.of(
                        "Level: " + tierBoostLevel + " / " + SHEAR_TIER_BOOST_MAX_LEVEL,
                        "Chance: " + getShearTierBoostChancePercent(player) + "%",
                        tierBoostLevel >= SHEAR_TIER_BOOST_MAX_LEVEL
                                ? "MAXED"
                                : "Cost: " + formatPoints(getShearTierBoostUpgradeCost(player)) + " Coins",
                        "Chance for shearing to upgrade sheep by one tier")));
        setMenuItemIfChanged(inventory, SHOP_BACK_TO_UPGRADES_SLOT, MenuItemFactory.create(
                Material.ARROW,
                "Back To Upgrades",
                List.of("Click to go back")));
    }

    private static void refreshOpenComboMenuItems(Player player, Inventory inventory) {
        if (player == null || inventory == null) {
            return;
        }
        int decayLevel = getComboDecayUpgradeLevel(player);
        int gainLevel = getComboGainUpgradeLevel(player);
        int maxLevel = getComboMaxUpgradeLevel(player);

        setMenuItemIfChanged(inventory, COMBO_DECAY_SLOT, MenuItemFactory.create(
                Material.CLOCK,
                "Slower Combo Decay",
                List.of(
                        "Level: " + decayLevel + " / " + COMBO_DECAY_MAX_LEVEL,
                        "Decay speed: " + (int) Math.round(getComboDecayMultiplier(player) * 100) + "%",
                        decayLevel >= COMBO_DECAY_MAX_LEVEL
                                ? "MAXED"
                                : "Cost: " + formatPoints(getComboDecayUpgradeCost(player)) + " Coins",
                        "Click to purchase")));

        setMenuItemIfChanged(inventory, COMBO_MAX_SLOT, MenuItemFactory.create(
                Material.NETHER_STAR,
                "Maximum Combo",
                List.of(
                        "Level: " + maxLevel,
                        "Max score: " + (int) Math.floor(getComboMaxScore(player)),
                        "Cost: " + formatPoints(getComboMaxUpgradePrestigeCost(player)) + " prestige points",
                        "Click to purchase")));

        setMenuItemIfChanged(inventory, COMBO_GAIN_SLOT, MenuItemFactory.create(
                Material.EMERALD,
                "Combo Gain Percentage",
                List.of(
                        "Level: " + gainLevel + " / " + COMBO_GAIN_MAX_LEVEL,
                        "Combo gain boost: +" + (int) Math.round(gainLevel * COMBO_GAIN_PERCENT_PER_LEVEL) + "%",
                        gainLevel >= COMBO_GAIN_MAX_LEVEL
                                ? "MAXED"
                                : "Cost: " + formatPoints(getComboGainUpgradeCost(player)) + " Coins",
                        "Click to purchase")));
    }

    private static void refreshOpenAutomationMenuItems(Player player, Inventory inventory) {
        if (player == null || inventory == null) {
            return;
        }
        setMenuItemIfChanged(inventory, 4, MenuItemFactory.create(
                Material.EXPERIENCE_BOTTLE,
                "Automation Points",
                List.of(
                        "&7Current: &e" + formatPoints(getAutomationPoints(player)),
                        "&bEarned while farming")));

        setMenuItemIfChanged(inventory, AUTOMATION_AUTO_BUY_SLOT, MenuItemFactory.create(
                Material.HOPPER,
                "Auto Buy Upgrades",
                List.of(
                        "&7Level: &e" + getAutomationAutoBuyUpgradeLevel(player) + " / "
                                + AUTOMATION_AUTO_BUY_MAX_LEVEL,
                        isAutomationAutoBuyEnabled(player) ? "&aStatus: ON" : "&cStatus: OFF",
                        "&7Rate: &b" + (getAutomationAutoBuyIntervalMs(player) <= 0L
                                ? "instant"
                                : formatDuration(getAutomationAutoBuyIntervalMs(player))),
                        getAutomationAutoBuyUpgradeLevel(player) >= AUTOMATION_AUTO_BUY_MAX_LEVEL
                                ? "&6Cost: &aMAXED"
                                : "&6Cost: &f" + formatPoints(getAutomationAutoBuyUpgradeCost(player)) + " AP",
                        "&fBuys cheap upgrades",
                        getAutomationAutoBuyUpgradeLevel(player) >= AUTOMATION_AUTO_BUY_MAX_LEVEL
                                ? "&aClick: Maxed"
                                : "&aClick: Upgrade")));

        setMenuItemIfChanged(inventory, AUTOMATION_AUTO_ABILITY_SLOT, MenuItemFactory.create(
                Material.BREWING_STAND,
                "Auto Activate Abilities",
                List.of(
                        "&7Level: &e" + getAutomationAutoAbilityUpgradeLevel(player) + " / "
                                + AUTOMATION_AUTO_ABILITY_MAX_LEVEL,
                        isAutomationAutoAbilityEnabled(player) ? "&aStatus: ON" : "&cStatus: OFF",
                        "&7Rate: &b"
                                + (getAutomationAutoAbilityUpgradeLevel(player) >= AUTOMATION_AUTO_ABILITY_MAX_LEVEL
                                        ? "instant"
                                        : formatDuration(AUTOMATION_AUTO_ABILITY_INTERVAL_MS)),
                        getAutomationAutoAbilityUpgradeLevel(player) >= AUTOMATION_AUTO_ABILITY_MAX_LEVEL
                                ? "&6Cost: &aMAXED"
                                : "&6Cost: &f" + formatPoints(getAutomationAutoAbilityUpgradeCost(player)) + " AP",
                        getAutomationAutoAbilityUpgradeLevel(player) >= AUTOMATION_AUTO_ABILITY_MAX_LEVEL
                                ? "&fInstant ability refill"
                                : getAutomationAutoAbilityUpgradeLevel(player) >= 2
                                        ? "&fBuys every missing ability"
                                        : "&fBuys one missing ability",
                        getAutomationAutoAbilityUpgradeLevel(player) >= AUTOMATION_AUTO_ABILITY_MAX_LEVEL
                                ? "&bFully automatic"
                                : "&7Upgrade for instant refill",
                        getAutomationAutoAbilityUpgradeLevel(player) >= AUTOMATION_AUTO_ABILITY_MAX_LEVEL
                                ? "&aClick: Maxed"
                                : "&aClick: Upgrade")));

        setMenuItemIfChanged(inventory, AUTOMATION_SLOW_AUTO_MERGE_SLOT, MenuItemFactory.create(
                Material.ANVIL,
                "Auto Merge",
                List.of(
                        "&7Level: &e" + getAutomationSlowAutoMergeUpgradeLevel(player) + " / "
                                + AUTOMATION_SLOW_AUTO_MERGE_MAX_LEVEL,
                        isAutomationSlowAutoMergeEnabled(player) ? "&aStatus: ON" : "&cStatus: OFF",
                        "&7Rate: &b" + formatDuration(getAutomationSlowAutoMergeIntervalMs(player)),
                        getAutomationSlowAutoMergeUpgradeLevel(player) >= AUTOMATION_SLOW_AUTO_MERGE_MAX_LEVEL
                                ? "&6Cost: &aMAXED"
                                : "&6Cost: &f" + formatPoints(getAutomationSlowAutoMergeUpgradeCost(player))
                                        + " AP",
                        "&fMerges one pair each cycle",
                        getAutomationSlowAutoMergeUpgradeLevel(player) >= AUTOMATION_SLOW_AUTO_MERGE_MAX_LEVEL
                                ? "&aClick: Maxed"
                                : "&aClick: Upgrade")));

        setMenuItemIfChanged(inventory, AUTOMATION_AUTO_PRESTIGE_SLOT, MenuItemFactory.create(
                Material.NETHER_STAR,
                "Auto Prestige",
                List.of(
                        "&7Level: &e" + getAutomationAutoPrestigeUpgradeLevel(player) + " / 1",
                        isAutomationAutoPrestigeEnabled(player) ? "&aStatus: ON" : "&cStatus: OFF",
                        "&7Rate: &b" + formatDuration(AUTOMATION_AUTO_PRESTIGE_INTERVAL_MS),
                        getAutomationAutoPrestigeUpgradeLevel(player) > 0
                                ? "&6Cost: &aMAXED"
                                : "&6Cost: &f" + formatPoints(getAutomationAutoPrestigeUpgradeCost(player))
                                        + " AP",
                        "&fPrestiges when affordable",
                        getAutomationAutoPrestigeUpgradeLevel(player) > 0 ? "&aClick: Maxed" : "&aClick: Unlock")));

        setMenuItemIfChanged(inventory, AUTOMATION_SLOW_AUTO_SHEAR_SLOT, MenuItemFactory.create(
                Material.SHEARS,
                "Auto Shear",
                List.of(
                        "&7Level: &e" + getAutomationSlowAutoShearUpgradeLevel(player) + " / "
                                + AUTOMATION_SLOW_AUTO_SHEAR_MAX_LEVEL,
                        isAutomationSlowAutoShearEnabled(player) ? "&aStatus: ON" : "&cStatus: OFF",
                        "&7Rate: &b" + formatDuration(getAutomationSlowAutoShearIntervalMs(player)),
                        getAutomationSlowAutoShearUpgradeLevel(player) >= AUTOMATION_SLOW_AUTO_SHEAR_MAX_LEVEL
                                ? "&6Cost: &aMAXED"
                                : "&6Cost: &f" + formatPoints(getAutomationSlowAutoShearUpgradeCost(player))
                                        + " AP",
                        "&fShears ready sheep each cycle",
                        getAutomationSlowAutoShearUpgradeLevel(player) >= AUTOMATION_SLOW_AUTO_SHEAR_MAX_LEVEL
                                ? "&aClick: Maxed"
                                : "&aClick: Upgrade")));

        long autoSpawnInterval = getAutomationAutoSpawnIntervalMs(player);
        setMenuItemIfChanged(inventory, AUTOMATION_AUTO_SPAWN_SLOT, MenuItemFactory.create(
                Material.SHEEP_SPAWN_EGG,
                "Auto Spawn Sheep",
                List.of(
                        "&7Level: &e" + getAutomationAutoSpawnUpgradeLevel(player) + " / "
                                + AUTOMATION_AUTO_SPAWN_MAX_LEVEL,
                        isAutomationAutoSpawnEnabled(player) ? "&aStatus: ON" : "&cStatus: OFF",
                        "&7Rate: &b" + (autoSpawnInterval <= 0L ? "instant" : formatDuration(autoSpawnInterval)),
                        "&fDrops sheep from the sky",
                        getAutomationAutoSpawnUpgradeLevel(player) >= AUTOMATION_AUTO_SPAWN_MAX_LEVEL
                                ? "&6Cost: &aMAXED"
                                : "&6Cost: &f" + formatPoints(getAutomationAutoSpawnUpgradeCost(player)) + " AP",
                        "&7Uses eggs automatically",
                        getAutomationAutoSpawnUpgradeLevel(player) >= AUTOMATION_AUTO_SPAWN_MAX_LEVEL
                                ? "&aClick: Maxed"
                                : "&aClick: Upgrade")));

        setMenuItemIfChanged(inventory, AUTOMATION_AUTO_BUY_TOGGLE_SLOT, MenuItemFactory.create(
                Material.LEVER,
                "Toggle Auto Buy",
                List.of(
                        isAutomationAutoBuyEnabled(player) ? "&aCurrent: ON" : "&cCurrent: OFF",
                        getAutomationAutoBuyUpgradeLevel(player) > 0 ? "&aClick: Toggle" : "&cBuy level 1 first")));

        setMenuItemIfChanged(inventory, AUTOMATION_AUTO_ABILITY_TOGGLE_SLOT, MenuItemFactory.create(
                Material.LEVER,
                "Toggle Auto Ability",
                List.of(
                        isAutomationAutoAbilityEnabled(player) ? "&aCurrent: ON" : "&cCurrent: OFF",
                        getAutomationAutoAbilityUpgradeLevel(player) > 0 ? "&aClick: Toggle"
                                : "&cBuy level 1 first")));

        setMenuItemIfChanged(inventory, AUTOMATION_AUTO_SPAWN_TOGGLE_SLOT, MenuItemFactory.create(
                Material.LEVER,
                "Toggle Auto Spawn",
                List.of(
                        isAutomationAutoSpawnEnabled(player) ? "&aCurrent: ON" : "&cCurrent: OFF",
                        getAutomationAutoSpawnUpgradeLevel(player) > 0 ? "&aClick: Toggle"
                                : "&cBuy level 1 first")));

        setMenuItemIfChanged(inventory, AUTOMATION_SLOW_AUTO_MERGE_TOGGLE_SLOT, MenuItemFactory.create(
                Material.LEVER,
                "Toggle Auto Merge",
                List.of(
                        isAutomationSlowAutoMergeEnabled(player) ? "&aCurrent: ON" : "&cCurrent: OFF",
                        getAutomationSlowAutoMergeUpgradeLevel(player) > 0 ? "&aClick: Toggle"
                                : "&cBuy level 1 first")));

        setMenuItemIfChanged(inventory, AUTOMATION_SLOW_AUTO_SHEAR_TOGGLE_SLOT, MenuItemFactory.create(
                Material.LEVER,
                "Toggle Auto Shear",
                List.of(
                        isAutomationSlowAutoShearEnabled(player) ? "&aCurrent: ON" : "&cCurrent: OFF",
                        getAutomationSlowAutoShearUpgradeLevel(player) > 0 ? "&aClick: Toggle"
                                : "&cBuy level 1 first")));

        setMenuItemIfChanged(inventory, AUTOMATION_AUTO_PRESTIGE_TOGGLE_SLOT, MenuItemFactory.create(
                Material.LEVER,
                "Toggle Auto Prestige",
                List.of(
                        isAutomationAutoPrestigeEnabled(player) ? "&aCurrent: ON" : "&cCurrent: OFF",
                        getAutomationAutoPrestigeUpgradeLevel(player) > 0 ? "&aClick: Toggle"
                                : "&cBuy level 1 first")));

        int unlockedAutomations = getUnlockedAutomationCount(player);
        setMenuItemIfChanged(inventory, AUTOMATION_ENABLE_ALL_SLOT, MenuItemFactory.create(
                Material.LIME_DYE,
                "Enable All",
                List.of(
                        "&7Unlocked: &e" + unlockedAutomations + " / 6",
                        unlockedAutomations > 0 ? "&aClick: Enable unlocked tracks"
                                : "&cUnlock an automation first")));

        setMenuItemIfChanged(inventory, AUTOMATION_DISABLE_ALL_SLOT, MenuItemFactory.create(
                Material.GRAY_DYE,
                "Disable All",
                List.of(
                        "&7Unlocked: &e" + unlockedAutomations + " / 6",
                        unlockedAutomations > 0 ? "&cClick: Disable unlocked tracks"
                                : "&cUnlock an automation first")));

        setMenuItemIfChanged(inventory, AUTOMATION_BACK_TO_UPGRADES_SLOT, MenuItemFactory.create(
                Material.ARROW,
                "Back To Upgrades",
                List.of("Click to go back")));
    }

    private static void setMenuItemIfChanged(Inventory inventory, int slot, ItemStack next) {
        if (inventory == null || slot < 0 || slot >= inventory.getSize()) {
            return;
        }
        ItemStack current = inventory.getItem(slot);
        if (current == null && next == null) {
            return;
        }
        if (current != null && next != null && current.isSimilar(next) && current.getAmount() == next.getAmount()) {
            return;
        }
        inventory.setItem(slot, next);
    }

    private static int getScoreboardLayoutMode(Player player) {
        if (player == null) {
            return 0;
        }
        return SheepUiPreferences.getScoreboardLayoutMode(player.getUniqueId());
    }

    private static boolean shouldShowScoreboardQuestPoints(Player player) {
        return player != null && SheepUiPreferences.shouldShowScoreboardQuestPoints(player.getUniqueId());
    }

    private static boolean shouldShowScoreboardAchievementPoints(Player player) {
        return player != null && SheepUiPreferences.shouldShowScoreboardAchievementPoints(player.getUniqueId());
    }

    private static boolean shouldShowScoreboardAutomationPoints(Player player) {
        return player != null && SheepUiPreferences.shouldShowScoreboardAutomationPoints(player.getUniqueId());
    }

    private static boolean shouldShowScoreboardSacrificePoints(Player player) {
        return player != null && SheepUiPreferences.shouldShowScoreboardSacrificePoints(player.getUniqueId());
    }

    private static boolean shouldShowScoreboardPrestigeStats(Player player) {
        return player != null && SheepUiPreferences.shouldShowScoreboardPrestigeStats(player.getUniqueId());
    }

    private static boolean shouldShowScoreboardQuestProgress(Player player) {
        return player != null && SheepUiPreferences.shouldShowScoreboardQuestProgress(player.getUniqueId());
    }

    private static boolean shouldShowScoreboardAbilityStatus(Player player) {
        return player != null && SheepUiPreferences.shouldShowScoreboardAbilityStatus(player.getUniqueId());
    }

    public static void openScoreboardMenu(Player player) {
        if (player == null) {
            return;
        }
        Inventory inventory = Bukkit.createInventory(null, 27, SCOREBOARD_MENU_TITLE);
        int layoutMode = getScoreboardLayoutMode(player);

        inventory.setItem(SCOREBOARD_QUEST_POINTS_SLOT, MenuItemFactory.create(
                Material.BOOK,
                "Quest Points",
                List.of(
                        "Status: " + (shouldShowScoreboardQuestPoints(player) ? "Shown" : "Hidden"),
                        shouldShowScoreboardQuestPoints(player) ? "Click: Hide" : "Click: Show")));

        inventory.setItem(SCOREBOARD_ACHIEVEMENT_POINTS_SLOT, MenuItemFactory.create(
                Material.ENCHANTED_BOOK,
                "Achievement Points",
                List.of(
                        "Status: " + (shouldShowScoreboardAchievementPoints(player) ? "Shown" : "Hidden"),
                        shouldShowScoreboardAchievementPoints(player) ? "Click: Hide" : "Click: Show")));

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

        inventory.setItem(SCOREBOARD_PRESTIGE_STATS_SLOT, MenuItemFactory.create(
                Material.NETHER_STAR,
                "Prestige Stats",
                List.of(
                        "Status: " + (shouldShowScoreboardPrestigeStats(player) ? "Shown" : "Hidden"),
                        shouldShowScoreboardPrestigeStats(player) ? "Click: Hide" : "Click: Show")));

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

        inventory.setItem(SCOREBOARD_LAYOUT_SLOT, MenuItemFactory.create(
                Material.MAP,
                "Compact Layout",
                List.of(
                        "Status: " + (layoutMode == 1 ? "Enabled" : "Disabled"),
                        "Mode: " + (layoutMode == 1 ? "Compact" : "Detailed"),
                        "Click: Toggle")));

        inventory.setItem(SCOREBOARD_BACK_SLOT, MenuItemFactory.create(
                Material.ARROW,
                "Back",
                List.of("Click: Open layouts")));

        player.openInventory(inventory);
    }

    public static void handleScoreboardMenuClick(Player player, int slot) {
        if (player == null) {
            return;
        }
        UUID playerId = player.getUniqueId();
        switch (slot) {
            case SCOREBOARD_ACHIEVEMENT_POINTS_SLOT -> SheepUiPreferences.setShowScoreboardAchievementPoints(playerId,
                    !shouldShowScoreboardAchievementPoints(player));
            case SCOREBOARD_QUEST_POINTS_SLOT -> SheepUiPreferences.setShowScoreboardQuestPoints(playerId,
                    !shouldShowScoreboardQuestPoints(player));
            case SCOREBOARD_AUTOMATION_POINTS_SLOT -> SheepUiPreferences.setShowScoreboardAutomationPoints(playerId,
                    !shouldShowScoreboardAutomationPoints(player));
            case SCOREBOARD_SACRIFICE_POINTS_SLOT -> SheepUiPreferences.setShowScoreboardSacrificePoints(playerId,
                    !shouldShowScoreboardSacrificePoints(player));
            case SCOREBOARD_PRESTIGE_STATS_SLOT -> SheepUiPreferences.setShowScoreboardPrestigeStats(playerId,
                    !shouldShowScoreboardPrestigeStats(player));
            case SCOREBOARD_QUEST_PROGRESS_SLOT -> SheepUiPreferences.setShowScoreboardQuestProgress(playerId,
                    !shouldShowScoreboardQuestProgress(player));
            case SCOREBOARD_ABILITIES_SLOT -> SheepUiPreferences.setShowScoreboardAbilityStatus(playerId,
                    !shouldShowScoreboardAbilityStatus(player));
            case SCOREBOARD_LAYOUT_SLOT -> SheepUiPreferences.setScoreboardLayoutMode(playerId,
                    getScoreboardLayoutMode(player) == 1 ? 0 : 1);
            case SCOREBOARD_BACK_SLOT -> {
                openUniversalLayoutMenu(player);
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

    public static boolean isUniversalLayoutMenuTitle(String title) {
        return SETTINGS_MENU_TITLE.equals(title);
    }

    public static boolean isScoreboardLayoutMenuTitle(String title) {
        return SCOREBOARD_LAYOUT_MENU_TITLE.equals(title);
    }

    public static boolean isInventoryLayoutMenuTitle(String title) {
        return INVENTORY_LAYOUT_MENU_TITLE.equals(title);
    }

    public static boolean isSoundEffectsMenuTitle(String title) {
        return SOUND_EFFECTS_MENU_TITLE.equals(title);
    }

    public static boolean isParticleEffectsMenuTitle(String title) {
        return PARTICLE_EFFECTS_MENU_TITLE.equals(title);
    }

    public static boolean isVisitAccessMenuTitle(String title) {
        return VISIT_ACCESS_MENU_TITLE.equals(title);
    }

    public static void openUniversalLayoutMenu(Player player) {
        if (player == null) {
            return;
        }
        Inventory inventory = Bukkit.createInventory(null, 27, SETTINGS_MENU_TITLE);
        inventory.setItem(4, MenuItemFactory.create(
                Material.NETHER_STAR,
                "Settings Hub",
                List.of(
                        "Inventory, scoreboard, visits, sound, and particles",
                        "Use the items below to open a section")));
        inventory.setItem(UNIVERSAL_LAYOUT_INVENTORY_SLOT, MenuItemFactory.create(
                Material.CHEST,
                "Inventory Layout",
                List.of(
                        "Quick access selected: " + getInventoryQuickAccessActions(player.getUniqueId()).size()
                                + " / " + INVENTORY_QUICK_ACCESS_MAX_ITEMS,
                        "Click: Open")));
        inventory.setItem(UNIVERSAL_LAYOUT_SCOREBOARD_SLOT, MenuItemFactory.create(
                Material.BOOK,
                "Scoreboard Settings",
                List.of(
                        "Sections: points, quests, automation, sacrifice",
                        "Layout: " + (getScoreboardLayoutMode(player) == 0 ? "Detailed" : "Compact"),
                        "Click: Open")));
        inventory.setItem(UNIVERSAL_LAYOUT_VISIT_SLOT, MenuItemFactory.create(
                Material.OAK_DOOR,
                "Visit Access & Blocks",
                List.of(
                        "Visit access: " + (isFarmVisitable(player.getUniqueId()) ? "Open" : "Closed"),
                        "Blocked visitors: " + getBlockedFarmVisitorCount(player.getUniqueId()),
                        "Click: Open")));
        inventory.setItem(UNIVERSAL_LAYOUT_SOUND_SLOT, MenuItemFactory.create(
                Material.MUSIC_DISC_PIGSTEP,
                "Sound Effects",
                List.of(
                        "Status: " + (areSoundEffectsEnabled(player) ? "Enabled" : "Disabled"),
                        "Click: Open")));
        inventory.setItem(UNIVERSAL_LAYOUT_PARTICLE_SLOT, MenuItemFactory.create(
                Material.FIRE_CHARGE,
                "Particle Effects",
                List.of(
                        "Status: " + (areParticleEffectsEnabled(player) ? "Enabled" : "Disabled"),
                        "Click: Open")));
        inventory.setItem(UNIVERSAL_LAYOUT_BACK_SLOT, MenuItemFactory.create(
                Material.ARROW,
                "Back",
                List.of("Click: Sheep Merge menu")));
        player.openInventory(inventory);
    }

    public static void handleUniversalLayoutMenuClick(Player player, int slot) {
        if (player == null) {
            return;
        }
        switch (slot) {
            case UNIVERSAL_LAYOUT_SCOREBOARD_SLOT -> openScoreboardMenu(player);
            case UNIVERSAL_LAYOUT_INVENTORY_SLOT -> openInventoryLayoutMenu(player);
            case UNIVERSAL_LAYOUT_SOUND_SLOT -> openSoundEffectsMenu(player);
            case UNIVERSAL_LAYOUT_PARTICLE_SLOT -> openParticleEffectsMenu(player);
            case UNIVERSAL_LAYOUT_VISIT_SLOT -> openVisitAccessMenu(player);
            case UNIVERSAL_LAYOUT_BACK_SLOT -> openUpgradeMenu(player);
            default -> {
                return;
            }
        }
    }

    public static void openSoundEffectsMenu(Player player) {
        if (player == null) {
            return;
        }
        Inventory inventory = Bukkit.createInventory(null, 27, SOUND_EFFECTS_MENU_TITLE);
        boolean enabled = areSoundEffectsEnabled(player);
        inventory.setItem(SOUND_EFFECTS_TOGGLE_SLOT, MenuItemFactory.create(
                enabled ? Material.LIME_DYE : Material.GRAY_DYE,
                "Sound Effects",
                List.of(
                        "Status: " + (enabled ? "Enabled" : "Disabled"),
                        enabled ? "Click: Disable" : "Click: Enable")));
        boolean sheepSoundsEnabled = areSheepSoundsEnabled(player);
        inventory.setItem(SHEEP_SOUNDS_TOGGLE_SLOT, MenuItemFactory.create(
                sheepSoundsEnabled ? Material.WHITE_WOOL : Material.GRAY_DYE,
                "Sheep Sounds",
                List.of(
                        "Status: " + (sheepSoundsEnabled ? "Enabled" : "Disabled"),
                        sheepSoundsEnabled ? "Click: Disable" : "Click: Enable")));
        inventory.setItem(SOUND_EFFECTS_BACK_SLOT, MenuItemFactory.create(
                Material.ARROW,
                "Back",
                List.of("Click: Settings")));
        player.openInventory(inventory);
    }

    public static void handleSoundEffectsMenuClick(Player player, int slot) {
        if (player == null) {
            return;
        }
        if (slot == SOUND_EFFECTS_TOGGLE_SLOT) {
            boolean enabled = toggleSoundEffects(player);
            player.sendMessage(action("Sound effects " + (enabled ? "enabled." : "disabled.")));
            openSoundEffectsMenu(player);
            return;
        }
        if (slot == SHEEP_SOUNDS_TOGGLE_SLOT) {
            boolean enabled = toggleSheepSounds(player);
            player.sendMessage(action("Sheep sounds " + (enabled ? "enabled." : "disabled.")));
            openSoundEffectsMenu(player);
            return;
        }
        if (slot == SOUND_EFFECTS_BACK_SLOT) {
            openUniversalLayoutMenu(player);
        }
    }

    public static void openParticleEffectsMenu(Player player) {
        if (player == null) {
            return;
        }
        Inventory inventory = Bukkit.createInventory(null, 27, PARTICLE_EFFECTS_MENU_TITLE);
        boolean enabled = areParticleEffectsEnabled(player);
        inventory.setItem(PARTICLE_EFFECTS_TOGGLE_SLOT, MenuItemFactory.create(
                enabled ? Material.LIME_DYE : Material.GRAY_DYE,
                "Particle Effects",
                List.of(
                        "Status: " + (enabled ? "Enabled" : "Disabled"),
                        enabled ? "Click: Disable" : "Click: Enable")));
        inventory.setItem(PARTICLE_EFFECTS_BACK_SLOT, MenuItemFactory.create(
                Material.ARROW,
                "Back",
                List.of("Click: Settings")));
        player.openInventory(inventory);
    }

    public static void handleParticleEffectsMenuClick(Player player, int slot) {
        if (player == null) {
            return;
        }
        if (slot == PARTICLE_EFFECTS_TOGGLE_SLOT) {
            boolean enabled = toggleParticleEffects(player);
            player.sendMessage(action("Particle effects " + (enabled ? "enabled." : "disabled.")));
            openParticleEffectsMenu(player);
            return;
        }
        if (slot == PARTICLE_EFFECTS_BACK_SLOT) {
            openUniversalLayoutMenu(player);
        }
    }

    public static void openVisitAccessMenu(Player player) {
        openVisitAccessMenu(player, 0);
    }

    private static void openVisitAccessMenu(Player player, int requestedPage) {
        if (player == null) {
            return;
        }
        Inventory inventory = Bukkit.createInventory(null, 54, VISIT_ACCESS_MENU_TITLE);
        populateVisitAccessMenuItems(player, inventory, requestedPage);
        player.openInventory(inventory);
    }

    private static void populateVisitAccessMenuItems(Player player, Inventory inventory, int requestedPage) {
        if (player == null || inventory == null) {
            return;
        }

        UUID ownerId = player.getUniqueId();
        List<Player> managedPlayers = getManagedVisitPlayers(player);
        int totalPages = Math.max(1, (int) Math.ceil(managedPlayers.size() / (double) SOCIALS_VISIT_PAGE_SIZE));
        int page = Math.max(0, Math.min(totalPages - 1, requestedPage));
        SheepVisitAccessState.setVisitAccessPage(ownerId, page);

        setMenuItemIfChanged(inventory, VISIT_ACCESS_TOGGLE_SLOT, MenuItemFactory.create(
                isFarmVisitable(ownerId) ? Material.OAK_DOOR : Material.IRON_DOOR,
                "Farm Visit Access",
                List.of(
                        "Status: " + (isFarmVisitable(ownerId) ? "Open" : "Closed"),
                        "Blocked visitors: " + getBlockedFarmVisitorCount(ownerId),
                        isFarmVisitable(ownerId) ? "Click: Close farm" : "Click: Open farm")));

        setMenuItemIfChanged(inventory, VISIT_ACCESS_SUMMARY_SLOT, MenuItemFactory.create(
                Material.PLAYER_HEAD,
                "Blocked Visitors",
                List.of(
                        "Page " + (page + 1) + " / " + totalPages,
                        "Click player heads to block or unblock")));

        setMenuItemIfChanged(inventory, VISIT_ACCESS_PREVIOUS_PAGE_SLOT, MenuItemFactory.create(
                Material.ARROW,
                "Previous Page",
                List.of(
                        "Page " + (page + 1) + " / " + totalPages,
                        page > 0 ? "Click to go back" : "Already at first page")));

        setMenuItemIfChanged(inventory, VISIT_ACCESS_NEXT_PAGE_SLOT, MenuItemFactory.create(
                Material.ARROW,
                "Next Page",
                List.of(
                        "Page " + (page + 1) + " / " + totalPages,
                        page + 1 < totalPages ? "Click to advance" : "Already at last page")));

        setMenuItemIfChanged(inventory, VISIT_ACCESS_BACK_SLOT, MenuItemFactory.create(
                Material.ARROW,
                "Back",
                List.of("Click: Settings")));

        clearSocialVisitEntries(inventory);
        List<Integer> displaySlots = getSocialVisitDisplaySlots();
        int startIndex = page * SOCIALS_VISIT_PAGE_SIZE;
        for (int offset = 0; offset < SOCIALS_VISIT_PAGE_SIZE; offset++) {
            int playerIndex = startIndex + offset;
            if (playerIndex >= managedPlayers.size() || offset >= displaySlots.size()) {
                break;
            }
            Player target = managedPlayers.get(playerIndex);
            if (target == null) {
                continue;
            }
            ItemStack visitItem = createVisitAccessItem(player, target);
            if (visitItem != null) {
                setMenuItemIfChanged(inventory, displaySlots.get(offset), visitItem);
            }
        }
    }

    private static int getCurrentVisitAccessPage(Player player) {
        if (player == null) {
            return 0;
        }
        return SheepVisitAccessState.getVisitAccessPage(player.getUniqueId());
    }

    private static List<Player> getManagedVisitPlayers(Player viewer) {
        if (viewer == null) {
            return List.of();
        }

        UUID viewerId = viewer.getUniqueId();
        List<Player> players = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online == null || !online.isOnline()) {
                continue;
            }
            if (viewerId.equals(online.getUniqueId())) {
                continue;
            }
            players.add(online);
        }
        players.sort((left, right) -> {
            boolean leftBlocked = isFarmVisitorBlocked(viewerId, left.getUniqueId());
            boolean rightBlocked = isFarmVisitorBlocked(viewerId, right.getUniqueId());
            if (leftBlocked != rightBlocked) {
                return leftBlocked ? -1 : 1;
            }
            return left.getName().compareToIgnoreCase(right.getName());
        });
        return players;
    }

    private static ItemStack createVisitAccessItem(Player owner, Player target) {
        if (owner == null || target == null) {
            return null;
        }

        boolean blocked = isFarmVisitorBlocked(owner, target);
        ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);
        ItemMeta rawMeta = head.getItemMeta();
        if (!(rawMeta instanceof SkullMeta skullMeta)) {
            return MenuItemFactory.create(Material.PLAYER_HEAD,
                    (blocked ? "Unblock " : "Block ") + target.getName(),
                    List.of(
                            "Status: " + (blocked ? "Blocked" : "Allowed"),
                            "Click to " + (blocked ? "allow" : "block")));
        }

        skullMeta.setOwningPlayer(target);
        skullMeta.setDisplayName((blocked ? "Unblock " : "Block ") + target.getName());
        skullMeta.setLore(List.of(
                "Player: " + target.getName(),
                "Status: " + (blocked ? "Blocked" : "Allowed"),
                "Click to " + (blocked ? "allow visits" : "block visits")));
        NamespacedKey key = getSocialVisitOwnerKey();
        if (key != null) {
            skullMeta.getPersistentDataContainer().set(key, PersistentDataType.STRING, target.getUniqueId().toString());
        }
        head.setItemMeta(skullMeta);
        return head;
    }

    public static void handleVisitAccessMenuClick(Player player, int slot, ItemStack clickedItem) {
        if (player == null) {
            return;
        }
        if (slot == VISIT_ACCESS_TOGGLE_SLOT) {
            boolean open = toggleFarmVisitable(player);
            player.sendMessage(action("Farm visit access " + (open ? "opened." : "closed.")));
            openVisitAccessMenu(player, getCurrentVisitAccessPage(player));
            return;
        }
        if (slot == VISIT_ACCESS_PREVIOUS_PAGE_SLOT) {
            openVisitAccessMenu(player, getCurrentVisitAccessPage(player) - 1);
            return;
        }
        if (slot == VISIT_ACCESS_NEXT_PAGE_SLOT) {
            openVisitAccessMenu(player, getCurrentVisitAccessPage(player) + 1);
            return;
        }
        if (slot == VISIT_ACCESS_BACK_SLOT) {
            openUniversalLayoutMenu(player);
            return;
        }

        UUID targetId = getSocialVisitOwnerId(clickedItem);
        if (targetId == null) {
            return;
        }
        if (targetId.equals(player.getUniqueId())) {
            player.sendMessage(hint("You cannot block yourself."));
            return;
        }

        boolean blocked = toggleFarmVisitorBlocked(player, targetId);
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetId);
        String targetName = target == null || target.getName() == null ? targetId.toString() : target.getName();
        player.sendMessage(action((blocked ? "Blocked: " : "Unblocked: ") + targetName));
        openVisitAccessMenu(player, getCurrentVisitAccessPage(player));
    }

    public static void openScoreboardLayoutMenu(Player player) {
        if (player == null) {
            return;
        }
        int layoutMode = getScoreboardLayoutMode(player);
        Inventory inventory = Bukkit.createInventory(null, 27, SCOREBOARD_LAYOUT_MENU_TITLE);
        inventory.setItem(SCOREBOARD_LAYOUT_DETAILED_SLOT, MenuItemFactory.create(
                Material.BOOK,
                "Detailed Layout",
                List.of(
                        "Status: " + (layoutMode == 0 ? "Selected" : "Not selected"),
                        "Shows all sections",
                        "Click: Select")));
        inventory.setItem(SCOREBOARD_LAYOUT_COMPACT_SLOT, MenuItemFactory.create(
                Material.MAP,
                "Compact Layout",
                List.of(
                        "Status: " + (layoutMode == 1 ? "Selected" : "Not selected"),
                        "Shows summary sections",
                        "Click: Select")));
        inventory.setItem(SCOREBOARD_LAYOUT_BACK_SLOT, MenuItemFactory.create(
                Material.ARROW,
                "Back",
                List.of("Click: Universal Layout")));
        player.openInventory(inventory);
    }

    public static void handleScoreboardLayoutMenuClick(Player player, int slot) {
        if (player == null) {
            return;
        }
        UUID playerId = player.getUniqueId();
        switch (slot) {
            case SCOREBOARD_LAYOUT_DETAILED_SLOT -> SheepUiPreferences.setScoreboardLayoutMode(playerId, 0);
            case SCOREBOARD_LAYOUT_COMPACT_SLOT -> SheepUiPreferences.setScoreboardLayoutMode(playerId, 1);
            case SCOREBOARD_LAYOUT_BACK_SLOT -> {
                openUniversalLayoutMenu(player);
                return;
            }
            default -> {
                return;
            }
        }
        saveData();
        updatePointsScoreboard(player);
        openScoreboardLayoutMenu(player);
    }

    public static void openInventoryLayoutMenu(Player player) {
        if (player == null) {
            return;
        }
        Inventory inventory = Bukkit.createInventory(null, 54, INVENTORY_LAYOUT_MENU_TITLE);
        List<String> selected = getInventoryQuickAccessActions(player.getUniqueId());
        boolean castingEnabled = isInventoryQuickAccessCastingEnabled(player);
        inventory.setItem(INVENTORY_LAYOUT_SELECTED_SLOT, MenuItemFactory.create(
                Material.CHEST,
                "Quick Access Slots",
                List.of(
                        "Selected: " + selected.size() + " / " + INVENTORY_QUICK_ACCESS_MAX_ITEMS,
                        "Hotbar slots used: 1-6",
                        "Casting: " + (castingEnabled ? "Enabled" : "Disabled"),
                        "Right-click quick item to cast")));

        inventory.setItem(INVENTORY_LAYOUT_CASTING_TOGGLE_SLOT, MenuItemFactory.create(
                Material.LEVER,
                "Quick Access Casting",
                List.of(
                        "Status: " + (castingEnabled ? "Enabled" : "Disabled"),
                        castingEnabled
                                ? "Click: Disable cast to inventory"
                                : "Click: Enable cast to inventory")));

        int slot = 10;
        for (QuickAccessDefinition definition : QUICK_ACCESS_DEFINITIONS) {
            if (slot >= 44) {
                break;
            }

            boolean enabled = selected.contains(definition.id);
            ItemStack item = MenuItemFactory.create(
                    definition.material,
                    definition.name,
                    List.of(
                            definition.description,
                            "Status: " + (enabled ? "Selected" : "Not selected"),
                            enabled ? "Click: Remove" : "Click: Add"));
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                NamespacedKey key = getInventoryLayoutOptionKey();
                if (key != null) {
                    meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, definition.id);
                }
                item.setItemMeta(meta);
            }

            inventory.setItem(slot, item);
            slot++;
            if (slot % 9 == 8) {
                slot += 2;
            }
        }

        inventory.setItem(INVENTORY_LAYOUT_BACK_SLOT, MenuItemFactory.create(
                Material.ARROW,
                "Back",
                List.of("Click: Universal Layout")));
        player.openInventory(inventory);
    }

    public static void handleInventoryLayoutMenuClick(Player player, int slot, ItemStack clickedItem) {
        if (player == null) {
            return;
        }
        if (slot == INVENTORY_LAYOUT_CASTING_TOGGLE_SLOT) {
            boolean enabled = toggleInventoryQuickAccessCasting(player);
            player.sendMessage(action("Quick-access inventory casting " + (enabled ? "enabled." : "disabled.")));
            enforceFarmLoadout(player);
            openInventoryLayoutMenu(player);
            return;
        }
        if (slot == INVENTORY_LAYOUT_BACK_SLOT) {
            openUniversalLayoutMenu(player);
            return;
        }

        String actionId = getInventoryLayoutOptionId(clickedItem);
        if (actionId == null) {
            return;
        }

        List<String> current = getInventoryQuickAccessActions(player.getUniqueId());
        boolean removing = current.contains(actionId);
        boolean changed = toggleInventoryQuickAccessAction(player, actionId);
        if (!changed) {
            player.sendMessage(warning("Quick access limit reached (" + INVENTORY_QUICK_ACCESS_MAX_ITEMS + ")."));
            return;
        }

        QuickAccessDefinition definition = getQuickAccessDefinition(actionId);
        if (definition != null) {
            player.sendMessage(action((removing ? "Removed: " : "Added: ") + definition.name));
        }
        enforceFarmLoadout(player);
        openInventoryLayoutMenu(player);
    }

    public static void openAchievementsMenu(Player player) {
        if (player == null) {
            return;
        }

        evaluateAchievementProgress(player, true);
        UUID playerId = player.getUniqueId();
        int unlockedCount = getUnlockedAchievementCount(playerId);
        int totalAchievements = ACHIEVEMENT_DEFINITIONS.size();
        int achievementPoints = getAchievementPoints(playerId);
        int nextMilestoneTarget = getNextAchievementMilestoneTarget(achievementPoints);

        Inventory inventory = Bukkit.createInventory(null, 27, ACHIEVEMENTS_MENU_TITLE);
        inventory.setItem(4, MenuItemFactory.create(
                Material.BOOK,
                "Achievements Hub",
                List.of(
                        "Unlocked: " + unlockedCount + "/" + totalAchievements,
                        "Achievement points: " + achievementPoints,
                        "Milestone line: unlock bonuses automatically",
                        nextMilestoneTarget > 0
                                ? "Next milestone: " + nextMilestoneTarget + " AP"
                                : "Milestone line complete")));

        inventory.setItem(ACHIEVEMENTS_VIEW_SLOT, MenuItemFactory.create(
                Material.MAP,
                "View Achievements",
                List.of(
                        "Browse goals and rewards",
                        "Click to open")));

        inventory.setItem(ACHIEVEMENTS_UPGRADES_SLOT, MenuItemFactory.create(
                Material.ENCHANTED_BOOK,
                "Achievement Milestones",
                List.of(
                        "Unlock milestone bonuses with achievement points",
                        "Click to open")));

        inventory.setItem(ACHIEVEMENTS_BACK_SLOT, MenuItemFactory.create(
                Material.ARROW,
                "Back",
                List.of("Click: Open upgrades")));

        player.openInventory(inventory);
    }

    public static void openAchievementsViewMenu(Player player) {
        if (player == null) {
            return;
        }

        Inventory inventory = Bukkit.createInventory(null, 54, ACHIEVEMENTS_VIEW_MENU_TITLE);
        UUID playerId = player.getUniqueId();
        Set<String> unlocked = getUnlockedAchievementIds(playerId);
        List<Integer> slots = getAchievementGridSlots();
        int slotIndex = 0;
        for (int index = 0; index < ACHIEVEMENT_DEFINITIONS.size(); index++) {
            AchievementDefinition achievement = ACHIEVEMENT_DEFINITIONS.get(index);
            if (isSecretAchievementId(achievement.id)) {
                continue;
            }
            if (slotIndex >= slots.size()) {
                break;
            }
            boolean unlockedAchievement = unlocked.contains(achievement.id);
            ItemStack item = "wool_guardian".equals(achievement.id)
                    ? MenuItemFactory.createShieldWithWhiteBanner(achievement.name,
                            List.of(
                                    "Objective: " + achievement.objective,
                                    achievement.reward,
                                    "Achievement points: +" + achievement.achievementPoints,
                                    "Status: " + (unlockedAchievement ? "UNLOCKED" : "LOCKED"),
                                    "Key: " + achievement.id),
                            unlockedAchievement)
                    : MenuItemFactory.create(
                            achievement.material,
                            achievement.name,
                            List.of(
                                    "Objective: " + achievement.objective,
                                    achievement.reward,
                                    "Achievement points: +" + achievement.achievementPoints,
                                    "Status: " + (unlockedAchievement ? "UNLOCKED" : "LOCKED"),
                                    "Key: " + achievement.id),
                            unlockedAchievement);
            if (unlockedAchievement && "socials_explorer".equals(achievement.id)) {
                ItemMeta itemMeta = item.getItemMeta();
                if (itemMeta instanceof SkullMeta skullMeta) {
                    skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(SOCIALS_AUTHOR_UUID));
                    item.setItemMeta(skullMeta);
                }
            }
            inventory.setItem(slots.get(slotIndex), item);
            slotIndex++;
        }

        for (AchievementDefinition achievement : ACHIEVEMENT_DEFINITIONS) {
            if (!isSecretAchievementId(achievement.id) || !unlocked.contains(achievement.id)) {
                continue;
            }
            ItemStack secretItem = MenuItemFactory.create(
                    achievement.material,
                    achievement.name,
                    List.of(
                            "Objective: " + achievement.objective,
                            achievement.reward,
                            "Achievement points: +" + achievement.achievementPoints,
                            "Status: UNLOCKED",
                            "Key: " + achievement.id),
                    true);
            if ("secret_owner_farm".equals(achievement.id)) {
                ItemMeta itemMeta = secretItem.getItemMeta();
                if (itemMeta instanceof SkullMeta skullMeta) {
                    skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(SOCIALS_AUTHOR_UUID));
                    secretItem.setItemMeta(skullMeta);
                }
            }
            int secretSlot = "secret_author_online".equals(achievement.id)
                    ? SECRET_AUTHOR_ONLINE_SLOT
                    : SECRET_OWNER_FARM_SLOT;
            inventory.setItem(secretSlot, secretItem);
        }

        inventory.setItem(ACHIEVEMENTS_VIEW_BACK_SLOT, MenuItemFactory.create(
                Material.ARROW,
                "Back",
                List.of("Click: Achievements hub")));
        player.openInventory(inventory);
    }

    public static void openAchievementsUpgradesMenu(Player player) {
        if (player == null) {
            return;
        }

        Inventory inventory = Bukkit.createInventory(null, 54, ACHIEVEMENTS_UPGRADES_MENU_TITLE);
        UUID playerId = player.getUniqueId();
        Set<String> unlockedMilestones = getUnlockedAchievementMilestoneIds(playerId);
        int achievementPoints = getAchievementPoints(playerId);
        int nextTarget = getNextAchievementMilestoneTarget(achievementPoints);

        inventory.setItem(4, MenuItemFactory.create(
                Material.NETHER_STAR,
                "Achievement Milestones",
                List.of(
                        "Current points: " + achievementPoints,
                        "Unlocked milestones: " + unlockedMilestones.size() + "/"
                                + ACHIEVEMENT_MILESTONE_DEFINITIONS.size(),
                        nextTarget > 0
                                ? "Next unlock: " + nextTarget + " AP"
                                : "All milestones unlocked")));

        List<Integer> slots = getAchievementMilestoneGridSlots();
        for (int index = 0; index < ACHIEVEMENT_MILESTONE_DEFINITIONS.size(); index++) {
            if (index >= slots.size()) {
                break;
            }
            AchievementMilestoneDefinition milestone = ACHIEVEMENT_MILESTONE_DEFINITIONS.get(index);
            boolean unlockedMilestone = unlockedMilestones.contains(milestone.id);
            inventory.setItem(slots.get(index), MenuItemFactory.create(
                    milestone.material,
                    milestone.name,
                    List.of(
                            "Target: " + milestone.requiredPoints + " achievement points",
                            milestone.reward,
                            "Status: " + (unlockedMilestone ? "UNLOCKED" : "LOCKED")),
                    unlockedMilestone));
        }

        inventory.setItem(ACHIEVEMENTS_UPGRADES_BACK_SLOT, MenuItemFactory.create(
                Material.ARROW,
                "Back",
                List.of("Click: Achievements hub")));
        player.openInventory(inventory);
    }

    public static void handleAchievementsMenuClick(Player player, int slot) {
        if (player == null) {
            return;
        }
        switch (slot) {
            case ACHIEVEMENTS_VIEW_SLOT -> {
                openAchievementsViewMenu(player);
                return;
            }
            case ACHIEVEMENTS_UPGRADES_SLOT -> {
                openAchievementsUpgradesMenu(player);
                return;
            }
            case ACHIEVEMENTS_BACK_SLOT -> {
                openUpgradeMenu(player);
                return;
            }
            default -> {
                return;
            }
        }
    }

    public static void handleAchievementsViewMenuClick(Player player, int slot) {
        if (player == null) {
            return;
        }
        if (slot == ACHIEVEMENTS_VIEW_BACK_SLOT) {
            openAchievementsMenu(player);
        }
    }

    public static void handleAchievementsUpgradesMenuClick(Player player, int slot) {
        if (player == null) {
            return;
        }
        if (slot == ACHIEVEMENTS_UPGRADES_BACK_SLOT) {
            openAchievementsMenu(player);
        }
    }

    public static void openSocialsMenu(Player player) {
        openSocialsMenu(player, 0);
    }

    private static void openSocialsMenu(Player player, int requestedPage) {
        if (player == null) {
            return;
        }
        Inventory inventory = Bukkit.createInventory(null, 54, SOCIALS_MENU_TITLE);
        populateSocialsMenuItems(player, inventory, requestedPage);
        player.openInventory(inventory);
    }

    private static void populateSocialsMenuItems(Player player, Inventory inventory, int requestedPage) {
        if (player == null || inventory == null) {
            return;
        }

        List<Player> visitableOwners = getVisitableFarmOwners(player);
        int totalPages = Math.max(1, (int) Math.ceil(visitableOwners.size() / (double) SOCIALS_VISIT_PAGE_SIZE));
        int page = Math.max(0, Math.min(totalPages - 1, requestedPage));
        SheepRuntimeUiState.socialsPages().put(player.getUniqueId(), page);

        setMenuItemIfChanged(inventory, SOCIALS_PREVIOUS_PAGE_SLOT, MenuItemFactory.create(
                Material.ARROW,
                "Previous Page",
                List.of(
                        "Page " + (page + 1) + " / " + totalPages,
                        page > 0 ? "Click to go back" : "Already at first page")));

        setMenuItemIfChanged(inventory, SOCIALS_NEXT_PAGE_SLOT, MenuItemFactory.create(
                Material.ARROW,
                "Next Page",
                List.of(
                        "Page " + (page + 1) + " / " + totalPages,
                        page + 1 < totalPages ? "Click to advance" : "Already at last page")));

        List<String> topLines = getTopPointsLines(5);
        List<String> topLore = new ArrayList<>();
        topLore.add("Top players by Coins:");
        topLore.addAll(topLines);
        setMenuItemIfChanged(inventory, SOCIALS_TOP_POINTS_SLOT, MenuItemFactory.create(
                Material.GOLD_INGOT,
                "Top Coins",
                topLore));

        boolean visitingAnotherFarm = isSheepFarmWorld(player.getWorld()) && !isFarmOwner(player, player.getWorld());
        setMenuItemIfChanged(inventory, SOCIALS_RETURN_HOME_SLOT, MenuItemFactory.create(
                Material.COMPASS,
                "Return To Your Farm",
                List.of(
                        visitingAnotherFarm ? "You are currently visiting." : "You are already in your own world.",
                        "Click to return home")));

        setMenuItemIfChanged(inventory, SOCIALS_BACK_SLOT, MenuItemFactory.create(
                Material.ARROW,
                "Back",
                List.of("Click: Open menu")));

        clearSocialVisitEntries(inventory);
        List<Integer> displaySlots = getSocialVisitDisplaySlots();
        int startIndex = page * SOCIALS_VISIT_PAGE_SIZE;
        for (int offset = 0; offset < SOCIALS_VISIT_PAGE_SIZE; offset++) {
            int ownerIndex = startIndex + offset;
            if (ownerIndex >= visitableOwners.size() || offset >= displaySlots.size()) {
                break;
            }
            Player owner = visitableOwners.get(ownerIndex);
            if (owner == null) {
                continue;
            }
            ItemStack visitItem = createSocialVisitItem(owner);
            if (visitItem != null) {
                setMenuItemIfChanged(inventory, displaySlots.get(offset), visitItem);
            }
        }
    }

    private static int getCurrentSocialsPage(Player player) {
        if (player == null) {
            return 0;
        }
        return Math.max(0, SheepRuntimeUiState.socialsPages().getOrDefault(player.getUniqueId(), 0));
    }

    private static List<Integer> getSocialVisitDisplaySlots() {
        List<Integer> slots = new ArrayList<>();
        for (int slot = 10; slot < 44; slot++) {
            if (slot % 9 == 8) {
                continue;
            }
            slots.add(slot);
        }
        return slots;
    }

    private static void clearSocialVisitEntries(Inventory inventory) {
        if (inventory == null) {
            return;
        }
        for (int slot : getSocialVisitDisplaySlots()) {
            inventory.setItem(slot, null);
        }
    }

    private static List<Player> getVisitableFarmOwners(Player viewer) {
        if (viewer == null) {
            return List.of();
        }

        UUID viewerId = viewer.getUniqueId();
        List<Player> owners = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online == null || !online.isOnline()) {
                continue;
            }
            UUID ownerId = online.getUniqueId();
            if (ownerId == null || ownerId.equals(viewerId)) {
                continue;
            }
            if (!viewer.isOp() && !isFarmVisitable(ownerId)) {
                continue;
            }
            if (!viewer.isOp() && isFarmVisitorBlocked(ownerId, viewerId)) {
                continue;
            }
            owners.add(online);
        }
        owners.sort((left, right) -> left.getName().compareToIgnoreCase(right.getName()));
        return owners;
    }

    private static ItemStack createSocialVisitItem(Player owner) {
        if (owner == null) {
            return null;
        }

        ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);
        ItemMeta rawMeta = head.getItemMeta();
        if (!(rawMeta instanceof SkullMeta skullMeta)) {
            return MenuItemFactory.create(Material.PLAYER_HEAD, "Visit " + owner.getName(), List.of("Click to visit"));
        }

        skullMeta.setOwningPlayer(owner);
        skullMeta.setDisplayName("Visit " + owner.getName());
        skullMeta.setLore(List.of(
                "Farm owner: " + owner.getName(),
                "Click to visit"));
        NamespacedKey key = getSocialVisitOwnerKey();
        if (key != null) {
            skullMeta.getPersistentDataContainer().set(key, PersistentDataType.STRING, owner.getUniqueId().toString());
        }
        head.setItemMeta(skullMeta);
        return head;
    }

    private static ItemStack createAchievementsMenuOpenItem() {
        return MenuItemFactory.create(
                Material.RED_BANNER,
                "Achievements",
                List.of(
                        "Track milestones and claim bonuses",
                        "Click to open"));
    }

    private static ItemStack createSocialsMenuOpenItem() {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);
        ItemMeta rawMeta = head.getItemMeta();
        if (!(rawMeta instanceof SkullMeta skullMeta)) {
            return MenuItemFactory.create(
                    Material.PLAYER_HEAD,
                    ChatColor.RESET + "" + ChatColor.YELLOW + "Socials",
                    List.of(
                            ChatColor.RED + "" + ChatColor.BOLD + "Author:",
                            ChatColor.GREEN + "" + ChatColor.ITALIC + "0x208u16 (unknown)"));
        }

        OfflinePlayer author = Bukkit.getOfflinePlayer(SOCIALS_AUTHOR_UUID);
        skullMeta.setOwningPlayer(author);
        skullMeta.setDisplayName(ChatColor.RESET + "" + ChatColor.YELLOW + "Socials");
        skullMeta.setLore(List.of(
                ChatColor.RED + "" + ChatColor.BOLD + "Author:",
                ChatColor.GREEN + "" + ChatColor.ITALIC + getAuthorCredentialsText(author)));
        head.setItemMeta(skullMeta);
        return head;
    }

    private static String getAuthorCredentialsText(OfflinePlayer author) {
        String minecraftUsername = author == null ? null : author.getName();
        if (minecraftUsername == null || minecraftUsername.isBlank()) {
            minecraftUsername = "unknown";
        }
        return "0x208u16 (" + minecraftUsername + ")";
    }

    private static UUID getSocialVisitOwnerId(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() != Material.PLAYER_HEAD) {
            return null;
        }
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return null;
        }
        NamespacedKey key = getSocialVisitOwnerKey();
        if (key == null) {
            return null;
        }
        String uuidRaw = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (uuidRaw == null || uuidRaw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(uuidRaw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public static void handleSocialsMenuClick(Player player, int slot, ItemStack clickedItem) {
        if (player == null) {
            return;
        }
        if (slot == SOCIALS_PREVIOUS_PAGE_SLOT) {
            openSocialsMenu(player, getCurrentSocialsPage(player) - 1);
            return;
        }
        if (slot == SOCIALS_NEXT_PAGE_SLOT) {
            openSocialsMenu(player, getCurrentSocialsPage(player) + 1);
            return;
        }
        if (slot == SOCIALS_BACK_SLOT) {
            openUpgradeMenu(player);
            return;
        }
        if (slot == SOCIALS_RETURN_HOME_SLOT) {
            if (isSheepFarmWorld(player.getWorld()) && !isFarmOwner(player, player.getWorld())) {
                player.closeInventory();
                if (plugin != null) {
                    Bukkit.getScheduler().runTask(plugin, () -> player.performCommand("sheepmerge"));
                }
            } else {
                player.sendMessage(hint("You are already in your own world."));
            }
            return;
        }

        UUID ownerId = getSocialVisitOwnerId(clickedItem);
        if (ownerId == null) {
            return;
        }

        Player owner = Bukkit.getPlayer(ownerId);
        if (owner == null || !owner.isOnline()) {
            player.sendMessage(warning("That player is no longer online."));
            openSocialsMenu(player, getCurrentSocialsPage(player));
            return;
        }
        if (!player.isOp() && !isFarmVisitable(ownerId)) {
            player.sendMessage(warning("That farm is closed to visitors."));
            openSocialsMenu(player, getCurrentSocialsPage(player));
            return;
        }

        player.closeInventory();
        if (plugin != null) {
            String targetName = owner.getName();
            Bukkit.getScheduler().runTask(plugin, () -> player.performCommand("sheepmerge visit " + targetName));
        }
    }

    public static void handleUpgradeMenuClick(Player player, int slot) {
        if (player == null) {
            return;
        }
        switch (slot) {
            case LAYOUTS_MENU_OPEN_SLOT -> {
                openUniversalLayoutMenu(player);
                return;
            }
            case LIMIT_UPGRADE_SLOT -> {
                if (blockTutorialMenuPurchase(player, TutorialStep.BUY_REGULAR_UPGRADE,
                        "Hotbar Slot 9 -> Upgrade Menu -> Buy one regular upgrade")) {
                    break;
                }
                if (getPlayerLimit(player) >= getMaxSheepLimit(player.getUniqueId())) {
                    player.sendMessage(warning("Sheep limit maxed."));
                } else if (upgradeLimit(player)) {
                    playUpgradeSound(player);
                    player.sendMessage(action("Limit up: " + getPlayerLimit(player)));
                    markTutorialRegularUpgradesIfComplete(player);
                } else {
                    player.sendMessage(warning("Not enough Coins."));
                }
            }
            case EGG_SPEED_UPGRADE_SLOT -> {
                if (blockTutorialMenuPurchase(player, TutorialStep.BUY_REGULAR_UPGRADE,
                        "Hotbar Slot 9 -> Upgrade Menu -> Buy one regular upgrade")) {
                    break;
                }
                if (upgradeEggSpeed(player)) {
                    playUpgradeSound(player);
                    player.sendMessage(action("Eggs: every " + getEggIntervalSeconds(player) + "s"));
                    markTutorialRegularUpgradesIfComplete(player);
                } else {
                    player.sendMessage(warning("Not enough Coins."));
                }
            }
            case WOOL_REGEN_UPGRADE_SLOT -> {
                if (blockTutorialMenuPurchase(player, TutorialStep.BUY_REGULAR_UPGRADE,
                        "Hotbar Slot 9 -> Upgrade Menu -> Buy one regular upgrade")) {
                    break;
                }
                if (upgradeWoolRegen(player)) {
                    playUpgradeSound(player);
                    player.sendMessage(action("Wool regen up"));
                    markTutorialRegularUpgradesIfComplete(player);
                } else {
                    player.sendMessage(warning("Not enough Coins."));
                }
            }
            case HIGHER_TIER_CHANCE_UPGRADE_SLOT -> {
                if (blockTutorialMenuPurchase(player, TutorialStep.BUY_REGULAR_UPGRADE,
                        "Hotbar Slot 9 -> Upgrade Menu -> Buy one regular upgrade")) {
                    break;
                }
                int level = SheepEconomyState.getHigherTierChanceLevel(player.getUniqueId());
                if (level >= getHigherTierChanceMaxLevel(player)) {
                    player.sendMessage(warning("Spawn chance maxed."));
                    break;
                }
                if (upgradeHigherTierChance(player)) {
                    playUpgradeSound(player);
                    player.sendMessage(action("Spawn chance: " + getHigherTierChancePercent(player) + "%"));
                    markTutorialRegularUpgradesIfComplete(player);
                } else {
                    player.sendMessage(warning("Not enough Coins."));
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
            case ACHIEVEMENTS_MENU_OPEN_SLOT -> {
                openAchievementsMenu(player);
                return;
            }
            case SACRIFICE_MENU_OPEN_SLOT -> {
                openSacrificeMenu(player);
                return;
            }
            case REBIRTH_MENU_OPEN_SLOT -> {
                openRebirthMenu(player);
                return;
            }
            case SOCIALS_MENU_OPEN_SLOT -> {
                openSocialsMenu(player);
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
                        "Next prestige cost: " + formatPoints(getPrestigeCostBig(player)) + " Coins",
                        affordablePrestiges > 0
                                ? "Total cost now: " + formatPoints(totalCostForAffordable) + " Coins"
                                : "Need more Coins for next prestige",
                        "Resets Coin upgrades",
                        "Click to prestige multiple")));

        inventory.setItem(PRESTIGE_DOUBLE_POINTS_SLOT, MenuItemFactory.create(
                Material.EMERALD,
                "Double Coins Chance",
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
                        "Level: " + questRewardLevel,
                        "Quest rewards: +"
                                + (int) Math.round(questRewardLevel * PRESTIGE_QUEST_REWARD_BONUS_PER_LEVEL * 100)
                                + "%",
                        "Cost: " + formatPoints(getPrestigeQuestRewardCost(player)) + " prestige points",
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
                        "Upgrade Menu -> Prestige Menu -> Prestige Reset")) {
                    break;
                }
                int gained = prestige(player);
                if (gained > 0) {
                    playPrestigeSound(player);
                    player.sendMessage(action("Prestige +" + gained));
                } else {
                    player.sendMessage(warning("Not enough Coins."));
                }
            }
            case PRESTIGE_DOUBLE_POINTS_SLOT -> {
                if (blockTutorialMenuPurchase(player, TutorialStep.PRESTIGE_ONCE,
                        "Upgrade Menu -> Prestige Menu -> Prestige Reset")) {
                    break;
                }
                if (getPrestigeDoublePointsChanceLevel(player) >= PRESTIGE_DOUBLE_POINTS_MAX_LEVEL) {
                    player.sendMessage(warning("Double Coins chance is maxed."));
                    break;
                }
                if (upgradePrestigeDoublePoints(player)) {
                    playUpgradeSound(player);
                    player.sendMessage(action("Double Coins: " + getDoublePointsChancePercent(player) + "%"));
                } else {
                    player.sendMessage(warning("Not enough prestige points."));
                }
            }
            case PRESTIGE_HIGHER_MAX_LEVEL_SLOT -> {
                if (blockTutorialMenuPurchase(player, TutorialStep.PRESTIGE_ONCE,
                        "Upgrade Menu -> Prestige Menu -> Prestige Reset")) {
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
                        "Upgrade Menu -> Prestige Menu -> Prestige Reset")) {
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
                        "Upgrade Menu -> Prestige Menu -> Prestige Reset")) {
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
                        "Upgrade Menu -> Prestige Menu -> Prestige Reset")) {
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
                        "Upgrade Menu -> Prestige Menu -> Prestige Reset")) {
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
                        "Upgrade Menu -> Prestige Menu -> Prestige Reset")) {
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
                        "Level: " + maxLevel,
                        "Max score: " + (int) Math.floor(getComboMaxScore(player)),
                        "Cost: " + formatPoints(getComboMaxUpgradePrestigeCost(player)) + " prestige points",
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
                        "&7Current: &e" + formatPoints(getAutomationPoints(player)),
                        "&bEarned while farming")));

        inventory.setItem(AUTOMATION_AUTO_BUY_SLOT, MenuItemFactory.create(
                Material.HOPPER,
                "Auto Buy Upgrades",
                List.of(
                        "&7Level: &e" + getAutomationAutoBuyUpgradeLevel(player) + " / "
                                + AUTOMATION_AUTO_BUY_MAX_LEVEL,
                        isAutomationAutoBuyEnabled(player) ? "&aStatus: ON" : "&cStatus: OFF",
                        "&7Rate: &b" + (getAutomationAutoBuyIntervalMs(player) <= 0L
                                ? "instant"
                                : formatDuration(getAutomationAutoBuyIntervalMs(player))),
                        getAutomationAutoBuyUpgradeLevel(player) >= AUTOMATION_AUTO_BUY_MAX_LEVEL
                                ? "&6Cost: &aMAXED"
                                : "&6Cost: &f" + formatPoints(getAutomationAutoBuyUpgradeCost(player)) + " AP",
                        "&fBuys cheap upgrades",
                        getAutomationAutoBuyUpgradeLevel(player) >= AUTOMATION_AUTO_BUY_MAX_LEVEL
                                ? "&aClick: Maxed"
                                : "&aClick: Upgrade")));

        inventory.setItem(AUTOMATION_AUTO_ABILITY_SLOT, MenuItemFactory.create(
                Material.BREWING_STAND,
                "Auto Activate Abilities",
                List.of(
                        "&7Level: &e" + getAutomationAutoAbilityUpgradeLevel(player) + " / "
                                + AUTOMATION_AUTO_ABILITY_MAX_LEVEL,
                        isAutomationAutoAbilityEnabled(player) ? "&aStatus: ON" : "&cStatus: OFF",
                        "&7Rate: &b"
                                + (getAutomationAutoAbilityUpgradeLevel(player) >= AUTOMATION_AUTO_ABILITY_MAX_LEVEL
                                        ? "instant"
                                        : formatDuration(AUTOMATION_AUTO_ABILITY_INTERVAL_MS)),
                        getAutomationAutoAbilityUpgradeLevel(player) >= AUTOMATION_AUTO_ABILITY_MAX_LEVEL
                                ? "&6Cost: &aMAXED"
                                : "&6Cost: &f" + formatPoints(getAutomationAutoAbilityUpgradeCost(player)) + " AP",
                        getAutomationAutoAbilityUpgradeLevel(player) >= AUTOMATION_AUTO_ABILITY_MAX_LEVEL
                                ? "&fInstant ability refill"
                                : getAutomationAutoAbilityUpgradeLevel(player) >= 2
                                        ? "&fBuys every missing ability"
                                        : "&fBuys one missing ability",
                        getAutomationAutoAbilityUpgradeLevel(player) >= AUTOMATION_AUTO_ABILITY_MAX_LEVEL
                                ? "&bFully automatic"
                                : "&7Upgrade for instant refill",
                        getAutomationAutoAbilityUpgradeLevel(player) >= AUTOMATION_AUTO_ABILITY_MAX_LEVEL
                                ? "&aClick: Maxed"
                                : "&aClick: Upgrade")));

        inventory.setItem(AUTOMATION_SLOW_AUTO_MERGE_SLOT, MenuItemFactory.create(
                Material.ANVIL,
                "Auto Merge",
                List.of(
                        "&7Level: &e" + getAutomationSlowAutoMergeUpgradeLevel(player) + " / "
                                + AUTOMATION_SLOW_AUTO_MERGE_MAX_LEVEL,
                        isAutomationSlowAutoMergeEnabled(player) ? "&aStatus: ON" : "&cStatus: OFF",
                        "&7Rate: &b" + formatDuration(getAutomationSlowAutoMergeIntervalMs(player)),
                        getAutomationSlowAutoMergeUpgradeLevel(player) >= AUTOMATION_SLOW_AUTO_MERGE_MAX_LEVEL
                                ? "&6Cost: &aMAXED"
                                : "&6Cost: &f" + formatPoints(getAutomationSlowAutoMergeUpgradeCost(player)) + " AP",
                        "&fMerges one pair each cycle",
                        getAutomationSlowAutoMergeUpgradeLevel(player) >= AUTOMATION_SLOW_AUTO_MERGE_MAX_LEVEL
                                ? "&aClick: Maxed"
                                : "&aClick: Upgrade")));

        inventory.setItem(AUTOMATION_AUTO_PRESTIGE_SLOT, MenuItemFactory.create(
                Material.NETHER_STAR,
                "Auto Prestige",
                List.of(
                        "&7Level: &e" + getAutomationAutoPrestigeUpgradeLevel(player) + " / 1",
                        isAutomationAutoPrestigeEnabled(player) ? "&aStatus: ON" : "&cStatus: OFF",
                        "&7Rate: &b" + formatDuration(AUTOMATION_AUTO_PRESTIGE_INTERVAL_MS),
                        getAutomationAutoPrestigeUpgradeLevel(player) > 0
                                ? "&6Cost: &aMAXED"
                                : "&6Cost: &f" + formatPoints(getAutomationAutoPrestigeUpgradeCost(player)) + " AP",
                        "&fPrestiges when affordable",
                        getAutomationAutoPrestigeUpgradeLevel(player) > 0 ? "&aClick: Maxed" : "&aClick: Unlock")));

        inventory.setItem(AUTOMATION_SLOW_AUTO_SHEAR_SLOT, MenuItemFactory.create(
                Material.SHEARS,
                "Auto Shear",
                List.of(
                        "&7Level: &e" + getAutomationSlowAutoShearUpgradeLevel(player) + " / "
                                + AUTOMATION_SLOW_AUTO_SHEAR_MAX_LEVEL,
                        isAutomationSlowAutoShearEnabled(player) ? "&aStatus: ON" : "&cStatus: OFF",
                        "&7Rate: &b" + formatDuration(getAutomationSlowAutoShearIntervalMs(player)),
                        getAutomationSlowAutoShearUpgradeLevel(player) >= AUTOMATION_SLOW_AUTO_SHEAR_MAX_LEVEL
                                ? "&6Cost: &aMAXED"
                                : "&6Cost: &f" + formatPoints(getAutomationSlowAutoShearUpgradeCost(player)) + " AP",
                        "&fShears ready sheep each cycle",
                        getAutomationSlowAutoShearUpgradeLevel(player) >= AUTOMATION_SLOW_AUTO_SHEAR_MAX_LEVEL
                                ? "&aClick: Maxed"
                                : "&aClick: Upgrade")));

        long autoSpawnInterval = getAutomationAutoSpawnIntervalMs(player);
        inventory.setItem(AUTOMATION_AUTO_SPAWN_SLOT, MenuItemFactory.create(
                Material.SHEEP_SPAWN_EGG,
                "Auto Spawn Sheep",
                List.of(
                        "&7Level: &e" + getAutomationAutoSpawnUpgradeLevel(player) + " / "
                                + AUTOMATION_AUTO_SPAWN_MAX_LEVEL,
                        isAutomationAutoSpawnEnabled(player) ? "&aStatus: ON" : "&cStatus: OFF",
                        "&7Rate: &b" + (autoSpawnInterval <= 0L ? "instant" : formatDuration(autoSpawnInterval)),
                        "&fDrops sheep from the sky",
                        getAutomationAutoSpawnUpgradeLevel(player) >= AUTOMATION_AUTO_SPAWN_MAX_LEVEL
                                ? "&6Cost: &aMAXED"
                                : "&6Cost: &f" + formatPoints(getAutomationAutoSpawnUpgradeCost(player)) + " AP",
                        "&7Uses eggs automatically",
                        getAutomationAutoSpawnUpgradeLevel(player) >= AUTOMATION_AUTO_SPAWN_MAX_LEVEL
                                ? "&aClick: Maxed"
                                : "&aClick: Upgrade")));

        inventory.setItem(AUTOMATION_AUTO_BUY_TOGGLE_SLOT, MenuItemFactory.create(
                Material.LEVER,
                "Toggle Auto Buy",
                List.of(
                        isAutomationAutoBuyEnabled(player) ? "&aCurrent: ON" : "&cCurrent: OFF",
                        getAutomationAutoBuyUpgradeLevel(player) > 0 ? "&aClick: Toggle" : "&cBuy level 1 first")));

        inventory.setItem(AUTOMATION_AUTO_ABILITY_TOGGLE_SLOT, MenuItemFactory.create(
                Material.LEVER,
                "Toggle Auto Ability",
                List.of(
                        isAutomationAutoAbilityEnabled(player) ? "&aCurrent: ON" : "&cCurrent: OFF",
                        getAutomationAutoAbilityUpgradeLevel(player) > 0 ? "&aClick: Toggle" : "&cBuy level 1 first")));

        inventory.setItem(AUTOMATION_AUTO_SPAWN_TOGGLE_SLOT, MenuItemFactory.create(
                Material.LEVER,
                "Toggle Auto Spawn",
                List.of(
                        isAutomationAutoSpawnEnabled(player) ? "&aCurrent: ON" : "&cCurrent: OFF",
                        getAutomationAutoSpawnUpgradeLevel(player) > 0 ? "&aClick: Toggle" : "&cBuy level 1 first")));

        inventory.setItem(AUTOMATION_SLOW_AUTO_MERGE_TOGGLE_SLOT, MenuItemFactory.create(
                Material.LEVER,
                "Toggle Auto Merge",
                List.of(
                        isAutomationSlowAutoMergeEnabled(player) ? "&aCurrent: ON" : "&cCurrent: OFF",
                        getAutomationSlowAutoMergeUpgradeLevel(player) > 0 ? "&aClick: Toggle"
                                : "&cBuy level 1 first")));

        inventory.setItem(AUTOMATION_SLOW_AUTO_SHEAR_TOGGLE_SLOT, MenuItemFactory.create(
                Material.LEVER,
                "Toggle Auto Shear",
                List.of(
                        isAutomationSlowAutoShearEnabled(player) ? "&aCurrent: ON" : "&cCurrent: OFF",
                        getAutomationSlowAutoShearUpgradeLevel(player) > 0 ? "&aClick: Toggle"
                                : "&cBuy level 1 first")));

        inventory.setItem(AUTOMATION_AUTO_PRESTIGE_TOGGLE_SLOT, MenuItemFactory.create(
                Material.LEVER,
                "Toggle Auto Prestige",
                List.of(
                        isAutomationAutoPrestigeEnabled(player) ? "&aCurrent: ON" : "&cCurrent: OFF",
                        getAutomationAutoPrestigeUpgradeLevel(player) > 0 ? "&aClick: Toggle"
                                : "&cBuy level 1 first")));

        int unlockedAutomations = getUnlockedAutomationCount(player);
        inventory.setItem(AUTOMATION_ENABLE_ALL_SLOT, MenuItemFactory.create(
                Material.LIME_DYE,
                "Enable All",
                List.of(
                        "&7Unlocked: &e" + unlockedAutomations + " / 6",
                        unlockedAutomations > 0 ? "&aClick: Enable unlocked tracks"
                                : "&cUnlock an automation first")));

        inventory.setItem(AUTOMATION_DISABLE_ALL_SLOT, MenuItemFactory.create(
                Material.GRAY_DYE,
                "Disable All",
                List.of(
                        "&7Unlocked: &e" + unlockedAutomations + " / 6",
                        unlockedAutomations > 0 ? "&cClick: Disable unlocked tracks"
                                : "&cUnlock an automation first")));

        inventory.setItem(AUTOMATION_BACK_TO_UPGRADES_SLOT, MenuItemFactory.create(
                Material.ARROW,
                "Back To Upgrades",
                List.of("Click to go back")));
        player.openInventory(inventory);
    }

    public static void handleAutomationMenuClick(Player player, int slot) {
        handleAutomationMenuClick(player, slot, true);
    }

    private static void handleAutomationMenuClick(Player player, int slot, boolean reopenMenu) {
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
                if (getAutomationAutoAbilityUpgradeLevel(player) >= AUTOMATION_AUTO_ABILITY_MAX_LEVEL) {
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
                    player.sendMessage(warning("Auto Merge is already maxed."));
                    break;
                }
                if (upgradeAutomationSlowAutoMerge(player)) {
                    playUpgradeSound(player);
                    player.sendMessage(action("Auto Merge upgraded."));
                } else {
                    player.sendMessage(warning("Not enough automation points."));
                }
            }
            case AUTOMATION_SLOW_AUTO_SHEAR_SLOT -> {
                if (getAutomationSlowAutoShearUpgradeLevel(player) >= AUTOMATION_SLOW_AUTO_SHEAR_MAX_LEVEL) {
                    player.sendMessage(warning("Auto Shear is already maxed."));
                    break;
                }
                if (upgradeAutomationSlowAutoShear(player)) {
                    playUpgradeSound(player);
                    player.sendMessage(action("Auto Shear upgraded."));
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
                boolean enabled = SheepAutomationState.toggleAutoBuyEnabled(player.getUniqueId());
                saveData();
                player.sendMessage(action("Auto Buy " + (enabled ? "enabled" : "disabled") + "."));
            }
            case AUTOMATION_AUTO_ABILITY_TOGGLE_SLOT -> {
                if (getAutomationAutoAbilityUpgradeLevel(player) <= 0) {
                    player.sendMessage(warning("Buy Auto Ability level 1 first."));
                    break;
                }
                boolean enabled = SheepAutomationState.toggleAutoAbilityEnabled(player.getUniqueId());
                saveData();
                player.sendMessage(action("Auto Ability " + (enabled ? "enabled" : "disabled") + "."));
            }
            case AUTOMATION_SLOW_AUTO_MERGE_TOGGLE_SLOT -> {
                if (getAutomationSlowAutoMergeUpgradeLevel(player) <= 0) {
                    player.sendMessage(warning("Buy Auto Merge level 1 first."));
                    break;
                }
                boolean enabled = SheepAutomationState.toggleSlowAutoMergeEnabled(player.getUniqueId());
                saveData();
                player.sendMessage(action("Auto Merge " + (enabled ? "enabled" : "disabled") + "."));
            }
            case AUTOMATION_SLOW_AUTO_SHEAR_TOGGLE_SLOT -> {
                if (getAutomationSlowAutoShearUpgradeLevel(player) <= 0) {
                    player.sendMessage(warning("Buy Auto Shear level 1 first."));
                    break;
                }
                boolean enabled = SheepAutomationState.toggleSlowAutoShearEnabled(player.getUniqueId());
                saveData();
                player.sendMessage(action("Auto Shear " + (enabled ? "enabled" : "disabled") + "."));
            }
            case AUTOMATION_AUTO_PRESTIGE_TOGGLE_SLOT -> {
                if (getAutomationAutoPrestigeUpgradeLevel(player) <= 0) {
                    player.sendMessage(warning("Buy Auto Prestige first."));
                    break;
                }
                boolean enabled = SheepAutomationState.toggleAutoPrestigeEnabled(player.getUniqueId());
                saveData();
                player.sendMessage(action("Auto Prestige " + (enabled ? "enabled" : "disabled") + "."));
            }
            case AUTOMATION_AUTO_SPAWN_TOGGLE_SLOT -> {
                if (getAutomationAutoSpawnUpgradeLevel(player) <= 0) {
                    player.sendMessage(warning("Buy Auto Spawn level 1 first."));
                    break;
                }
                boolean enabled = SheepAutomationState.toggleAutoSpawnEnabled(player.getUniqueId());
                saveData();
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
        if (reopenMenu) {
            openAutomationMenu(player);
        }
    }

    public static void openSacrificeMenu(Player player) {
        if (player == null) {
            return;
        }
        Inventory inventory = Bukkit.createInventory(null, 27, SACRIFICE_MENU_TITLE);
        UUID playerId = player.getUniqueId();
        int unlocksBought = getSacrificeUnlocksBought(playerId);
        BigInteger nextCost = getSacrificeUnlockCost(playerId);
        boolean regularUnlocked = hasSacrificeUnlock(player, SACRIFICE_UNLOCK_NO_REGULAR_RESETS);
        boolean comboUnlocked = hasSacrificeUnlock(player, SACRIFICE_UNLOCK_NO_COMBO_RESETS);
        boolean shearUnlocked = hasSacrificeUnlock(player, SACRIFICE_UNLOCK_NO_SHEAR_RESETS);
        boolean eggCooldownUnlocked = hasSacrificeUnlock(player, SACRIFICE_UNLOCK_EGG_COOLDOWN_TO_1S);
        boolean maxSheepUnlocked = hasSacrificeUnlock(player, SACRIFICE_UNLOCK_MAX_SHEEP_100);

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
                        "Status: " + sacrificeUnlockStatusLine(player, SACRIFICE_UNLOCK_NO_REGULAR_RESETS),
                        "Keeps regular upgrades on prestige",
                        "Applies immediately"),
                regularUnlocked));

        inventory.setItem(SACRIFICE_UNLOCK_COMBO_RESETS_SLOT, MenuItemFactory.create(
                Material.BLAZE_POWDER,
                "Unlock 2: Keep Combo Upgrades",
                List.of(
                        "Status: " + sacrificeUnlockStatusLine(player, SACRIFICE_UNLOCK_NO_COMBO_RESETS),
                        "Keeps combo upgrades on prestige",
                        "Applies immediately"),
                comboUnlocked));

        inventory.setItem(SACRIFICE_UNLOCK_SHEAR_RESETS_SLOT, MenuItemFactory.create(
                Material.SHEARS,
                "Unlock 3: Keep Shear Upgrades",
                List.of(
                        "Status: " + sacrificeUnlockStatusLine(player, SACRIFICE_UNLOCK_NO_SHEAR_RESETS),
                        "Keeps shear shop on prestige",
                        "Applies immediately"),
                shearUnlocked));

        inventory.setItem(SACRIFICE_UNLOCK_EGG_COOLDOWN_SLOT, MenuItemFactory.create(
                Material.CLOCK,
                "Unlock 4: 1s Egg Cooldown Cap",
                List.of(
                        "Status: " + (eggCooldownUnlocked
                                ? "UNLOCKED"
                                : "LOCKED"),
                        eggCooldownUnlocked ? "MAXED" : "Not unlocked",
                        "Adds +1 egg speed max level",
                        "Allows 1 egg per second"),
                eggCooldownUnlocked));

        inventory.setItem(SACRIFICE_UNLOCK_MAX_SHEEP_SLOT, MenuItemFactory.create(
                Material.OAK_FENCE,
                "Unlock 5: +50 Sheep Cap",
                List.of(
                        "Status: " + (maxSheepUnlocked
                                ? "UNLOCKED"
                                : "LOCKED"),
                        maxSheepUnlocked ? "MAXED" : "Not unlocked",
                        "Raises max sheep limit by +50"),
                maxSheepUnlocked));

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
                int unlockId = switch (slot) {
                    case SACRIFICE_UNLOCK_REGULAR_RESETS_SLOT -> SACRIFICE_UNLOCK_NO_REGULAR_RESETS;
                    case SACRIFICE_UNLOCK_COMBO_RESETS_SLOT -> SACRIFICE_UNLOCK_NO_COMBO_RESETS;
                    case SACRIFICE_UNLOCK_SHEAR_RESETS_SLOT -> SACRIFICE_UNLOCK_NO_SHEAR_RESETS;
                    case SACRIFICE_UNLOCK_EGG_COOLDOWN_SLOT -> SACRIFICE_UNLOCK_EGG_COOLDOWN_TO_1S;
                    default -> SACRIFICE_UNLOCK_MAX_SHEEP_100;
                };
                if (hasSacrificeUnlock(player, unlockId)) {
                    player.sendMessage(warning("That sacrifice unlock is already purchased."));
                    break;
                }
                int nextRequiredUnlockId = unlocksBought + 1;
                if (unlockId != nextRequiredUnlockId) {
                    player.sendMessage(warning("Buy sacrifice unlocks in order. Next unlock: "
                            + nextRequiredUnlockId + " / " + SACRIFICE_UNLOCK_MAX + "."));
                    break;
                }
                if (tryBuySacrificeUnlock(player, unlockId)) {
                    playUpgradeSound(player);
                    player.sendMessage(action("Sacrifice unlock purchased."));
                } else {
                    player.sendMessage(warning("Not enough sacrifice points."));
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

    public static void openRebirthMenu(Player player) {
        if (player == null) {
            return;
        }
        Inventory inventory = Bukkit.createInventory(null, 27, REBIRTH_MENU_TITLE);
        int rebirthLevel = getRebirthLevel(player);
        int rebirthPoints = getRebirthPoints(player);
        int unspent = getUnspentRebirthPoints(player);
        int affordable = getAffordableRebirthLevels(player);
        int nextCost = getRebirthCostInPrestigeLevels(rebirthLevel);
        int reward = getRebirthPointsRewardForNextLevels(rebirthLevel, affordable);

        inventory.setItem(REBIRTH_PROGRESS_SLOT, MenuItemFactory.create(
                Material.DRAGON_EGG,
                "Rebirth Progress",
                List.of(
                        "&bLevel: &f" + rebirthLevel,
                        "&dRebirth points: &f" + formatPoints(rebirthPoints),
                        "&aUnspent: &f" + formatPoints(unspent),
                        "&6Next cost: &f" + nextCost + " prestige levels")));

        inventory.setItem(REBIRTH_ACTION_SLOT, MenuItemFactory.create(
                Material.NETHER_STAR,
                "Rebirth Reset",
                List.of(
                        affordable > 0
                                ? "&aReady: +" + affordable + " rebirth level(s)"
                                : "&cReady: +0 rebirth level(s)",
                        affordable > 0
                                ? "&dReward: +" + formatPoints(reward) + " rebirth points"
                                : "&dReward: +0 rebirth points",
                        "&6Required prestige levels: &f" + nextCost,
                        "&7Cost scaling: +10 prestige levels per rebirth",
                        "&cConsumes prestige levels and prestige points",
                        "&cResets prestige upgrades",
                        hasActiveRebirthSkill(player.getUniqueId(), REBIRTH_SKILL_KEEP_SACRIFICE_AFTER_REBIRTH)
                                ? "&aKeeps sacrifice points"
                                : "&cSacrifice points reset",
                        "&aClick: Rebirth")));

        inventory.setItem(REBIRTH_OPEN_TREE_SLOT, MenuItemFactory.create(
                Material.ENCHANTED_BOOK,
                "Rebirth Skill Tree",
                List.of(
                        "&7Spend rebirth points here",
                        "&aUnlocks apply immediately",
                        "&aUnlocks stay active until refunded",
                        "&7Cost starts at 1 RP",
                        "&7Builds upward from the root",
                        "&aClick: Open")));

        inventory.setItem(REBIRTH_BACK_TO_UPGRADES_SLOT, MenuItemFactory.create(
                Material.ARROW,
                "Back To Upgrades",
                List.of("Click to go back")));
        player.openInventory(inventory);
    }

    public static void handleRebirthMenuClick(Player player, int slot) {
        if (player == null) {
            return;
        }
        switch (slot) {
            case REBIRTH_ACTION_SLOT -> {
                int gained = rebirth(player);
                if (gained > 0) {
                    playPrestigeSound(player);
                    player.sendMessage(action("Rebirth +" + gained));
                } else {
                    player.sendMessage(warning("Not enough prestige levels."));
                }
            }
            case REBIRTH_OPEN_TREE_SLOT -> {
                openRebirthTreeMenu(player);
                return;
            }
            case REBIRTH_BACK_TO_UPGRADES_SLOT -> {
                openUpgradeMenu(player);
                return;
            }
            default -> {
                return;
            }
        }
        updatePointsScoreboard(player);
        openRebirthMenu(player);
    }

    public static void openRebirthTreeMenu(Player player) {
        if (player == null) {
            return;
        }
        Inventory inventory = Bukkit.createInventory(null, 54, REBIRTH_TREE_MENU_TITLE);
        int unspent = getUnspentRebirthPoints(player);
        int refundAmount = getRebirthRespecRefundAmount(player);
        long respecRemaining = getRebirthRespecRemainingMs(player);
        inventory.setItem(4, MenuItemFactory.create(
                Material.BOOK,
                "Tree Overview",
                List.of(
                        "&bLevel: &f" + getRebirthLevel(player),
                        "&dRebirth points: &f" + formatPoints(getRebirthPoints(player)),
                        "&aUnspent: &f" + formatPoints(unspent),
                        "&aAll unlocked skills are active immediately",
                        "&7Respec available every 30 minutes",
                        "&7Root starts at the bottom",
                        "&7Unlock nodes upward from the root")));

        for (RebirthSkillNode node : REBIRTH_SKILL_NODES) {
            boolean unlocked = hasRebirthSkill(player, node.id);
            int cost = getRebirthSkillCost(node);
            boolean parentUnlocked = node.parentId <= 0 || hasRebirthSkill(player, node.parentId);
            boolean inStock = !unlocked && parentUnlocked && unspent >= cost;
            List<String> lore = new ArrayList<>();
            lore.add("&6Cost: &f" + cost + " RP");
            lore.add("&e" + node.effectLine);
            if (node.parentId > 0) {
                RebirthSkillNode parent = getRebirthSkillNode(node.parentId);
                lore.add("&7Requires: " + (parent == null ? "Root" : parent.name));
            }
            if (unlocked) {
                lore.add("&aUnlocked");
                lore.add("&aActive now");
                lore.add("&aPermanent unlock");
            } else if (!parentUnlocked) {
                lore.add("&cLocked: need parent first");
            } else if (!inStock) {
                lore.add("&cNeed " + (cost - unspent) + " more RP");
            } else {
                lore.add("&aReady");
                lore.add("&aClick to unlock");
            }
            inventory.setItem(node.slot, MenuItemFactory.create(node.material, node.name, lore));
        }

        inventory.setItem(REBIRTH_TREE_RESPEC_SLOT, MenuItemFactory.create(
                Material.BARRIER,
                "Respec Rebirth Skills",
                List.of(
                        "&dRefund amount: &f" + formatPoints(refundAmount) + " rebirth points",
                        respecRemaining > 0L
                                ? "&7Cooldown: &f" + formatDuration(respecRemaining)
                                : "&7Cooldown: &aready",
                        "&cResets all rebirth skill unlocks",
                        "&7Rebirth level and rebirth points are kept",
                        "&aClick to respec")));

        inventory.setItem(REBIRTH_TREE_BACK_SLOT, MenuItemFactory.create(
                Material.ARROW,
                "Back To Rebirth",
                List.of("Click to go back")));
        player.openInventory(inventory);
    }

    public static void handleRebirthTreeMenuClick(Player player, int slot) {
        if (player == null) {
            return;
        }
        if (slot == REBIRTH_TREE_BACK_SLOT) {
            openRebirthMenu(player);
            return;
        }
        if (slot == REBIRTH_TREE_RESPEC_SLOT) {
            long respecRemaining = getRebirthRespecRemainingMs(player);
            if (respecRemaining > 0L) {
                player.sendMessage(warning("Respec cooldown: " + formatDuration(respecRemaining)));
                openRebirthTreeMenu(player);
                return;
            }
            int refundAmount = getRebirthRespecRefundAmount(player);
            if (refundAmount <= 0) {
                player.sendMessage(warning("No rebirth skills to respec."));
                openRebirthTreeMenu(player);
                return;
            }
            if (tryRespecRebirthSkills(player)) {
                playUpgradeSound(player);
                player.sendMessage(action("Refunded " + formatPoints(refundAmount) + " rebirth points."));
            } else {
                player.sendMessage(warning("Respec is not available right now."));
            }
            openRebirthTreeMenu(player);
            return;
        }
        RebirthSkillNode clicked = null;
        for (RebirthSkillNode node : REBIRTH_SKILL_NODES) {
            if (node.slot == slot) {
                clicked = node;
                break;
            }
        }
        if (clicked == null) {
            return;
        }

        if (hasRebirthSkill(player, clicked.id)) {
            player.sendMessage(warning("That rebirth skill is already unlocked permanently."));
            openRebirthTreeMenu(player);
            return;
        }

        if (tryUnlockRebirthSkill(player, clicked.id)) {
            playUpgradeSound(player);
            player.sendMessage(action("Rebirth skill unlocked."));
        } else {
            player.sendMessage(warning("Cannot unlock this skill yet."));
        }
        openRebirthTreeMenu(player);
    }

    private static boolean tryUnlockRebirthSkill(Player player, int skillId) {
        if (player == null) {
            return false;
        }
        RebirthSkillNode node = getRebirthSkillNode(skillId);
        if (node == null || hasRebirthSkill(player, skillId)) {
            return false;
        }
        if (node.parentId > 0 && !hasRebirthSkill(player, node.parentId)) {
            return false;
        }
        int cost = getRebirthSkillCost(node);
        if (getUnspentRebirthPoints(player) < cost) {
            return false;
        }
        UUID playerId = player.getUniqueId();
        int updatedMask = getRebirthSkillUnlockMask(playerId) | getRebirthSkillBit(skillId);
        SheepRebirthState.setSkillUnlockMask(playerId, updatedMask);
        SheepRebirthState.clearSkillPendingMask(playerId);
        saveData();
        return true;
    }

    public static void openQuestMenu(Player player) {
        if (player == null) {
            return;
        }
        markTutorialQuestOpened(player);
        Inventory inventory = Bukkit.createInventory(null, 27, QUEST_MENU_TITLE);
        UUID playerId = player.getUniqueId();
        long remaining = getQuestResetRemainingMs(player);
        boolean shearsComplete = SheepQuestState.questShearsComplete().getOrDefault(playerId, false);
        boolean spawnsComplete = SheepQuestState.questSpawnsComplete().getOrDefault(playerId, false);
        boolean mergesComplete = SheepQuestState.questMergesComplete().getOrDefault(playerId, false);
        int currentQuestPoints = getQuestPoints(player);
        int luckyCost = getQuestLuckyBurstCost(player);
        int woolRushCost = getQuestWoolRushCost(player);
        int jackpotCost = getQuestJackpotCost(player);
        int autoMergeCost = getQuestAutoMergeCost(player);
        int autoShearCost = getQuestAutoShearCost(player);
        boolean luckyBurstGlint = isCountAbilityActive(SheepQuestState.activeLuckyBurstUses(),
                SheepQuestState.luckyBurstEnabled(),
                playerId);
        boolean woolRushGlint = isAbilityActive(SheepQuestState.activeWoolRushUntil(), playerId);
        boolean jackpotGlint = isAbilityActive(SheepQuestState.activeJackpotShearsUntil(), playerId);
        boolean autoMergeGlint = isCountAbilityActive(SheepQuestState.activeAutoMergeUses(),
                SheepQuestState.autoMergeEnabled(),
                playerId);
        boolean autoShearGlint = isCountAbilityActive(SheepQuestState.activeAutoShearUses(),
                SheepQuestState.autoShearEnabled(),
                playerId);

        inventory.setItem(QUEST_BOARD_SLOT, MenuItemFactory.create(
                Material.BOOK,
                "Quest Board",
                List.of(
                        "Quest points: " + formatPoints(getQuestPoints(player)),
                        remaining > 0L ? "Next reset: " + formatDuration(remaining) : "Next reset: incoming",
                        (shearsComplete ? "DONE " : "TODO ")
                                + "Shear " + SheepQuestState.questShears().getOrDefault(playerId, 0) + "/"
                                + getQuestTarget(player, QUEST_SHEARS_TARGET)
                                + " (" + formatPoints(getQuestReward(player, QUEST_SHEARS_REWARD)) + " pts)",
                        (spawnsComplete ? "DONE " : "TODO ")
                                + "Spawn " + SheepQuestState.questSpawns().getOrDefault(playerId, 0) + "/"
                                + getQuestTarget(player, QUEST_SPAWNS_TARGET)
                                + " (" + formatPoints(getQuestReward(player, QUEST_SPAWNS_REWARD)) + " pts)",
                        (mergesComplete ? "DONE " : "TODO ")
                                + "Merge " + SheepQuestState.questMerges().getOrDefault(playerId, 0) + "/"
                                + getQuestTarget(player, QUEST_MERGES_TARGET)
                                + " (" + formatPoints(getQuestReward(player, QUEST_MERGES_REWARD)) + " pts)")));

        inventory.setItem(QUEST_ABILITY_LUCKY_BURST_SLOT, MenuItemFactory.create(
                Material.ENDER_EYE,
                "Lucky Burst",
                List.of(
                        "&6Cost: &f" + formatPoints(luckyCost) + " qp",
                        "&bBoost: &f+" + QUEST_LUCKY_BURST_SPAWN_CHANCE_BONUS_PERCENT + "% tier chance",
                        "&7Uses: &f" + getAbilityUseCount(player, QUEST_LUCKY_BURST_BASE_DURATION_MS),
                        currentQuestPoints >= luckyCost ? "&aReady to buy" : "&cNeed more quest points",
                        getCountAbilityMenuStatus(SheepQuestState.activeLuckyBurstUses(),
                                SheepQuestState.luckyBurstEnabled(), playerId),
                        getCountAbilityToggleActionLine(SheepQuestState.activeLuckyBurstUses(),
                                SheepQuestState.luckyBurstEnabled(),
                                playerId)),
                luckyBurstGlint));

        inventory.setItem(QUEST_ABILITY_WOOL_RUSH_SLOT, MenuItemFactory.create(
                Material.WHITE_WOOL,
                "Wool Rush",
                List.of(
                        "&6Cost: &f" + formatPoints(woolRushCost) + " qp",
                        "&bBoost: &fwool grows 90% faster",
                        "&7Time: &f" + formatDuration(getAbilityDurationMs(player, QUEST_WOOL_RUSH_BASE_DURATION_MS)),
                        currentQuestPoints >= woolRushCost ? "&aReady to buy" : "&cNeed more quest points",
                        getAbilityMenuStatus(SheepQuestState.activeWoolRushUntil(), null, playerId),
                        isAbilityActive(SheepQuestState.activeWoolRushUntil(), playerId)
                                ? "&eClick: Extend"
                                : "&aClick: Activate"),
                woolRushGlint));

        inventory.setItem(QUEST_ABILITY_JACKPOT_SHEARS_SLOT, MenuItemFactory.create(
                Material.GOLD_INGOT,
                "Jackpot Shears",
                List.of(
                        "&6Cost: &f" + formatPoints(jackpotCost) + " qp",
                        "&bBoost: &fx" + (2 + getQuestUpgradePowerLevel(player)) + " shear Coins",
                        "&7Time: &f"
                                + formatDuration(getAbilityDurationMs(player, QUEST_JACKPOT_SHEARS_BASE_DURATION_MS)),
                        currentQuestPoints >= jackpotCost ? "&aReady to buy" : "&cNeed more quest points",
                        getAbilityMenuStatus(SheepQuestState.activeJackpotShearsUntil(), null, playerId),
                        isAbilityActive(SheepQuestState.activeJackpotShearsUntil(), playerId)
                                ? "&eClick: Extend"
                                : "&aClick: Activate"),
                jackpotGlint));

        inventory.setItem(QUEST_ABILITY_AUTO_MERGE_SLOT, MenuItemFactory.create(
                Material.ANVIL,
                "Merge Assist",
                List.of(
                        "&6Cost: &f" + formatPoints(autoMergeCost) + " qp",
                        "&bBoost: &fauto-merges carried sheep",
                        "&7Uses: &f" + getAbilityUseCount(player, QUEST_AUTO_MERGE_BASE_DURATION_MS),
                        currentQuestPoints >= autoMergeCost ? "&aReady to buy" : "&cNeed more quest points",
                        getCountAbilityMenuStatus(SheepQuestState.activeAutoMergeUses(),
                                SheepQuestState.autoMergeEnabled(), playerId),
                        getCountAbilityToggleActionLine(SheepQuestState.activeAutoMergeUses(),
                                SheepQuestState.autoMergeEnabled(),
                                playerId)),
                autoMergeGlint));

        inventory.setItem(QUEST_ABILITY_AUTO_SHEAR_SLOT, MenuItemFactory.create(
                Material.SHEARS,
                "Shear All Sheep",
                List.of(
                        "&6Cost: &f" + formatPoints(autoShearCost) + " qp",
                        "&bBoost: &fshears every ready sheep",
                        "&7Uses: &f" + getAbilityUseCount(player, QUEST_AUTO_SHEAR_BASE_DURATION_MS),
                        currentQuestPoints >= autoShearCost ? "&aReady to buy" : "&cNeed more quest points",
                        getCountAbilityMenuStatus(SheepQuestState.activeAutoShearUses(),
                                SheepQuestState.autoShearEnabled(), playerId),
                        getCountAbilityToggleActionLine(SheepQuestState.activeAutoShearUses(),
                                SheepQuestState.autoShearEnabled(),
                                playerId)),
                autoShearGlint));

        inventory.setItem(QUEST_OPEN_UPGRADES_SLOT, MenuItemFactory.create(
                Material.ENCHANTED_BOOK,
                "Quest Upgrades",
                List.of(
                        "&7Duration Lv: &e" + getQuestUpgradeDurationLevel(player),
                        "&7Power Lv: &e" + getQuestUpgradePowerLevel(player),
                        "&aClick: Open")));

        inventory.setItem(QUEST_BACK_TO_UPGRADES_SLOT, MenuItemFactory.create(
                Material.ARROW,
                "Back To Upgrades",
                List.of(
                        "&7Quest Points: &e" + formatPoints(getQuestPoints(player)),
                        remaining > 0L ? "&7Reset: &b" + formatDuration(remaining) : "&7Reset: &bincoming",
                        "&aClick: Back")));
        player.openInventory(inventory);
    }

    public static void handleQuestMenuClick(Player player, int slot) {
        if (player == null) {
            return;
        }
        switch (slot) {
            case QUEST_ABILITY_LUCKY_BURST_SLOT -> {
                if (toggleCountAbilityEnabled(player, SheepQuestState.activeLuckyBurstUses(),
                        SheepQuestState.luckyBurstEnabled())) {
                    boolean enabled = SheepQuestState.luckyBurstEnabled().getOrDefault(player.getUniqueId(), true);
                    player.sendMessage(action("Lucky Burst "
                            + (enabled ? "enabled." : "disabled.")));
                    break;
                }
                if (blockTutorialMenuPurchase(player, TutorialStep.USE_ABILITY,
                        "Activate any quest ability")) {
                    break;
                }
                if (activateCountQuestAbility(
                        player,
                        SheepQuestState.activeLuckyBurstUses(),
                        SheepQuestState.luckyBurstEnabled(),
                        getQuestLuckyBurstCost(player),
                        getAbilityUseCount(player, QUEST_LUCKY_BURST_BASE_DURATION_MS),
                        Sound.BLOCK_BEACON_POWER_SELECT,
                        org.bukkit.Particle.END_ROD)) {
                    markTutorialAbilityUsed(player);
                    spawnParticle(player,
                            org.bukkit.Particle.TOTEM,
                            player.getLocation().add(0, 2.1, 0),
                            18,
                            0.45,
                            0.45,
                            0.45,
                            0.0);
                    playSound(player, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 0.9f, 1.5f);
                    player.sendMessage(action("Lucky Burst active."));
                } else {
                    player.sendMessage(warning("Not enough quest points."));
                }
            }
            case QUEST_ABILITY_WOOL_RUSH_SLOT -> {
                if (blockTutorialMenuPurchase(player, TutorialStep.USE_ABILITY,
                        "Activate any quest ability")) {
                    break;
                }
                boolean active = isAbilityActive(SheepQuestState.activeWoolRushUntil(), player.getUniqueId());
                boolean applied = active
                        ? extendQuestAbility(
                                player,
                                SheepQuestState.activeWoolRushUntil(),
                                getQuestWoolRushCost(player),
                                getAbilityDurationMs(player, QUEST_WOOL_RUSH_BASE_DURATION_MS),
                                Sound.ENTITY_ENDER_DRAGON_FLAP,
                                org.bukkit.Particle.CLOUD)
                        : activateQuestAbility(
                                player,
                                SheepQuestState.activeWoolRushUntil(),
                                getQuestWoolRushCost(player),
                                getAbilityDurationMs(player, QUEST_WOOL_RUSH_BASE_DURATION_MS),
                                Sound.ENTITY_ENDER_DRAGON_FLAP,
                                org.bukkit.Particle.CLOUD);
                if (applied) {
                    markTutorialAbilityUsed(player);
                    applyWoolRushToShearedSheep(player);
                    spawnParticle(player,
                            org.bukkit.Particle.SPORE_BLOSSOM_AIR,
                            player.getLocation().add(0, 2.0, 0),
                            28,
                            0.5,
                            0.35,
                            0.5,
                            0.01);
                    playSound(player, Sound.BLOCK_MOSS_CARPET_PLACE, 1.0f, 0.8f);
                    player.sendMessage(action(active ? "Wool Rush extended." : "Wool Rush active."));
                } else {
                    player.sendMessage(warning("Not enough quest points."));
                }
            }
            case QUEST_ABILITY_JACKPOT_SHEARS_SLOT -> {
                if (blockTutorialMenuPurchase(player, TutorialStep.USE_ABILITY,
                        "Activate any quest ability")) {
                    break;
                }
                boolean active = isAbilityActive(SheepQuestState.activeJackpotShearsUntil(), player.getUniqueId());
                boolean applied = active
                        ? extendQuestAbility(
                                player,
                                SheepQuestState.activeJackpotShearsUntil(),
                                getQuestJackpotCost(player),
                                getAbilityDurationMs(player, QUEST_JACKPOT_SHEARS_BASE_DURATION_MS),
                                Sound.ENTITY_PLAYER_LEVELUP,
                                org.bukkit.Particle.CRIT)
                        : activateQuestAbility(
                                player,
                                SheepQuestState.activeJackpotShearsUntil(),
                                getQuestJackpotCost(player),
                                getAbilityDurationMs(player, QUEST_JACKPOT_SHEARS_BASE_DURATION_MS),
                                Sound.ENTITY_PLAYER_LEVELUP,
                                org.bukkit.Particle.CRIT);
                if (applied) {
                    markTutorialAbilityUsed(player);
                    spawnParticle(player,
                            org.bukkit.Particle.FIREWORKS_SPARK,
                            player.getLocation().add(0, 2.1, 0),
                            22,
                            0.45,
                            0.45,
                            0.45,
                            0.02);
                    playSound(player, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.8f, 1.6f);
                    player.sendMessage(action(active ? "Jackpot Shears extended." : "Jackpot Shears active."));
                } else {
                    player.sendMessage(warning("Not enough quest points."));
                }
            }
            case QUEST_ABILITY_AUTO_MERGE_SLOT -> {
                if (toggleCountAbilityEnabled(player, SheepQuestState.activeAutoMergeUses(),
                        SheepQuestState.autoMergeEnabled())) {
                    boolean enabled = SheepQuestState.autoMergeEnabled().getOrDefault(player.getUniqueId(), true);
                    player.sendMessage(action("Merge Assist "
                            + (enabled ? "enabled." : "disabled.")));
                    break;
                }
                if (blockTutorialMenuPurchase(player, TutorialStep.USE_ABILITY,
                        "Activate any quest ability")) {
                    break;
                }
                if (activateCountQuestAbility(
                        player,
                        SheepQuestState.activeAutoMergeUses(),
                        SheepQuestState.autoMergeEnabled(),
                        getQuestAutoMergeCost(player),
                        getAbilityUseCount(player, QUEST_AUTO_MERGE_BASE_DURATION_MS),
                        Sound.BLOCK_PISTON_EXTEND,
                        org.bukkit.Particle.ENCHANTMENT_TABLE)) {
                    markTutorialAbilityUsed(player);
                    SheepQuestState.nextAutoMergeAt().put(player.getUniqueId(), 0L);
                    spawnParticle(player,
                            org.bukkit.Particle.WAX_ON,
                            player.getLocation().add(0, 2.0, 0),
                            26,
                            0.5,
                            0.4,
                            0.5,
                            0.03);
                    playSound(player, Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.3f);
                    player.sendMessage(action("Merge Assist active."));
                } else {
                    player.sendMessage(warning("Not enough quest points."));
                }
            }
            case QUEST_ABILITY_AUTO_SHEAR_SLOT -> {
                if (toggleCountAbilityEnabled(player, SheepQuestState.activeAutoShearUses(),
                        SheepQuestState.autoShearEnabled())) {
                    boolean enabled = SheepQuestState.autoShearEnabled().getOrDefault(player.getUniqueId(), true);
                    player.sendMessage(action("Shear All Sheep "
                            + (enabled ? "enabled." : "disabled.")));
                    break;
                }
                if (blockTutorialMenuPurchase(player, TutorialStep.USE_ABILITY,
                        "Activate any quest ability")) {
                    break;
                }
                if (activateCountQuestAbility(
                        player,
                        SheepQuestState.activeAutoShearUses(),
                        SheepQuestState.autoShearEnabled(),
                        getQuestAutoShearCost(player),
                        getAbilityUseCount(player, QUEST_AUTO_SHEAR_BASE_DURATION_MS),
                        Sound.ENTITY_SHEEP_SHEAR,
                        org.bukkit.Particle.WAX_OFF)) {
                    markTutorialAbilityUsed(player);
                    SheepQuestState.nextAutoShearAt().put(player.getUniqueId(), 0L);
                    spawnParticle(player,
                            org.bukkit.Particle.WAX_OFF,
                            player.getLocation().add(0, 2.0, 0),
                            26,
                            0.5,
                            0.4,
                            0.5,
                            0.03);
                    playSound(player, Sound.ITEM_TRIDENT_RETURN, 0.8f, 1.4f);
                    player.sendMessage(action("Shear All Sheep active."));
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
                        "Complete the tutorial before buying Quest Upgrades")) {
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
                        "Complete the tutorial before buying Quest Upgrades")) {
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
                        "Level: " + getShearPointGainUpgradeLevel(player),
                        "Cost: " + formatPoints(getShearUpgradeCost(player)) + " Coins",
                        "Coins: base x" + getShearPointMultiplier(player),
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
                                : "Cost: " + formatPoints(getShearWoolSaveUpgradeCost(player)) + " Coins",
                        "Chance for sheep to keep wool when sheared")));
        inventory.setItem(SHOP_SHEAR_TIER_BOOST_SLOT, MenuItemFactory.create(
                Material.GLOWSTONE_DUST,
                "Tier Booster",
                List.of(
                        "Level: " + tierBoostLevel + " / " + SHEAR_TIER_BOOST_MAX_LEVEL,
                        "Chance: " + getShearTierBoostChancePercent(player) + "%",
                        tierBoostLevel >= SHEAR_TIER_BOOST_MAX_LEVEL
                                ? "MAXED"
                                : "Cost: " + formatPoints(getShearTierBoostUpgradeCost(player)) + " Coins",
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
                    player.sendMessage(warning("Not enough Coins."));
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
        return getDoubledUpgradeCostBig(scaleRegularPointsUpgradeBaseCost(EGG_SPEED_UPGRADE_BASE_COST), level);
    }

    private static BigInteger getWoolRegenUpgradeCost(Player player) {
        int level = getWoolRegenLevel(player);
        return getDoubledUpgradeCostBig(scaleRegularPointsUpgradeBaseCost(WOOL_REGEN_UPGRADE_BASE_COST), level);
    }

    private static BigInteger getHigherTierChanceUpgradeCost(Player player) {
        int level = getHigherTierChanceLevel(player);
        return getDoubledUpgradeCostBig(scaleRegularPointsUpgradeBaseCost(HIGHER_TIER_CHANCE_UPGRADE_BASE_COST),
                level);
    }

    private static int scaleRegularPointsUpgradeBaseCost(int baseCost) {
        long normalized = Math.max(1L, baseCost);
        long scaled = normalized * REGULAR_POINTS_UPGRADE_COST_MULTIPLIER;
        if ((scaled & 1L) != 0L) {
            scaled++;
        }
        return (int) Math.min(Integer.MAX_VALUE, scaled);
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
        SheepEconomyState.setEggSpeedLevel(player.getUniqueId(), currentLevel + 1);
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
        SheepEconomyState.setWoolRegenLevel(player.getUniqueId(), newLevel);
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
        SheepEconomyState.setHigherTierChanceLevel(player.getUniqueId(), currentLevel + 1);
        saveData();
        return true;
    }

    private static int getWoolRegenLevel(World world) {
        UUID ownerId = getOwnerId(world);
        if (ownerId == null) {
            return 0;
        }
        return SheepEconomyState.getWoolRegenLevel(ownerId);
    }

    private static int getWoolCooldownPercentAtLevel(Player player, int level) {
        int normalizedLevel = Math.max(0, level);
        return Math.max(1, (int) Math.ceil(getWoolCooldownPercentRawAtLevel(player, normalizedLevel)));
    }

    private static double getWoolCooldownFactorAtLevel(Player player, int level) {
        double factor = Math.pow(WOOL_REGEN_PER_LEVEL_MULTIPLIER, Math.max(0, level));
        factor /= getAchievementWoolRegenSpeedMultiplier(player);
        if (!Double.isFinite(factor) || factor <= 0.0D) {
            return (100.0D - WOOL_REGEN_MAX_REDUCTION_PERCENT) / 100.0D;
        }
        return Math.max((100.0D - WOOL_REGEN_MAX_REDUCTION_PERCENT) / 100.0D, factor);
    }

    private static double getWoolCooldownPercentRawAtLevel(Player player, int level) {
        return getWoolCooldownFactorAtLevel(player, level) * 100.0D;
    }

    private static double getWoolCooldownReductionPercentRawAtLevel(Player player, int level) {
        return Math.min(WOOL_REGEN_MAX_REDUCTION_PERCENT, 100.0D - getWoolCooldownPercentRawAtLevel(player, level));
    }

    private static String getWoolCooldownPercentDisplayAtLevel(Player player, int level) {
        return String.format(Locale.ROOT, "%1$." + WOOL_REGEN_PERCENT_DISPLAY_DECIMALS + "f",
                getWoolCooldownPercentRawAtLevel(player, level));
    }

    private static String getWoolCooldownReductionPercentDisplayAtLevel(Player player, int level) {
        return String.format(Locale.ROOT, "%1$." + WOOL_REGEN_PERCENT_DISPLAY_DECIMALS + "f",
                getWoolCooldownReductionPercentRawAtLevel(player, level));
    }

    private static String getWoolCooldownFactorDisplayAtLevel(Player player, int level) {
        return String.format(Locale.ROOT, "%1$." + WOOL_REGEN_FACTOR_DISPLAY_DECIMALS + "f",
                getWoolCooldownFactorAtLevel(player, level));
    }

    private static int getWoolCooldownReductionPercentAtLevel(int level) {
        return Math.min(99, Math.max(0, 100 - getWoolCooldownPercentAtLevel(null, level)));
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
        return SheepEntityRuntimeState.getLiveSheepCount(world.getUID(), countLiveSheep(world));
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
        SheepEntityRuntimeState.retainLiveSheepCounts(knownFarmWorlds);
    }

    public static void refreshLiveSheepCount(World world) {
        if (world == null || !isSheepFarmWorld(world)) {
            return;
        }
        SheepEntityRuntimeState.setLiveSheepCount(world.getUID(), countLiveSheep(world));
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
        if (player == null || SheepRuntimeUiState.savedInventories().containsKey(player.getUniqueId())) {
            return;
        }
        ItemStack[] contents = InventoryDataUtils.cloneItemStackArray(player.getInventory().getContents());
        ItemStack[] armor = InventoryDataUtils.cloneItemStackArray(player.getInventory().getArmorContents());
        ItemStack offhand = player.getInventory().getItemInOffHand() == null ? null
                : player.getInventory().getItemInOffHand().clone();
        SheepRuntimeUiState.savedInventories().put(
                player.getUniqueId(), new InventoryDataUtils.Snapshot(contents, armor, offhand));
        saveData();
    }

    public static void restorePlayerInventory(Player player) {
        if (player == null) {
            return;
        }
        InventoryDataUtils.Snapshot snapshot = SheepRuntimeUiState.savedInventories().remove(player.getUniqueId());
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
        return player != null && SheepRuntimeUiState.savedInventories().containsKey(player.getUniqueId());
    }

    public static void restoreSavedStateOutsideFarm(Player player) {
        if (player == null || isSheepFarmWorld(player.getWorld())) {
            return;
        }
        maybeRestorePlayerInventoryOutsideFarm(player);
        maybeRestorePlayerScoreboardOutsideFarm(player);
        player.setPlayerListName(null);
        clearEggTimer(player);
    }

    private static void maybeRestorePlayerInventoryOutsideFarm(Player player) {
        if (player == null) {
            return;
        }
        UUID playerId = player.getUniqueId();
        if (!SheepRuntimeUiState.savedInventories().containsKey(playerId)) {
            return;
        }

        if (hasAnyForcedFarmLoadoutItem(player) || isInventoryCompletelyEmpty(player)) {
            restorePlayerInventory(player);
            return;
        }

        // The player already has non-farm items; discard stale pending snapshot.
        SheepRuntimeUiState.savedInventories().remove(playerId);
        saveData();
    }

    private static void maybeRestorePlayerScoreboardOutsideFarm(Player player) {
        if (player == null) {
            return;
        }
        UUID playerId = player.getUniqueId();
        if (!SheepRuntimeUiState.savedScoreboards().containsKey(playerId)) {
            return;
        }

        Scoreboard current = player.getScoreboard();
        if (current == null) {
            restorePlayerScoreboard(player);
            return;
        }
        Objective objective = current == null ? null : current.getObjective("sheepmerge_points");
        if (objective != null) {
            restorePlayerScoreboard(player);
            return;
        }

        if (current.getObjectives().isEmpty()) {
            restorePlayerScoreboard(player);
            return;
        }

        // Current scoreboard is not the SheepMerge sidebar; keep it and discard stale
        // pending snapshot.
        SheepRuntimeUiState.savedScoreboards().remove(playerId);
    }

    private static boolean hasAnyForcedFarmLoadoutItem(Player player) {
        if (player == null) {
            return false;
        }
        var inventory = player.getInventory();
        for (ItemStack itemStack : inventory.getContents()) {
            if (isForcedFarmLoadoutItem(itemStack)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isInventoryCompletelyEmpty(Player player) {
        if (player == null) {
            return true;
        }
        var inventory = player.getInventory();
        for (ItemStack itemStack : inventory.getContents()) {
            if (itemStack != null && itemStack.getType() != Material.AIR) {
                return false;
            }
        }
        for (ItemStack itemStack : inventory.getArmorContents()) {
            if (itemStack != null && itemStack.getType() != Material.AIR) {
                return false;
            }
        }
        ItemStack offHand = inventory.getItemInOffHand();
        return offHand == null || offHand.getType() == Material.AIR;
    }

    public static void showPointsScoreboard(Player player) {
        if (player == null) {
            return;
        }
        if (!SheepRuntimeUiState.savedScoreboards().containsKey(player.getUniqueId())) {
            SheepRuntimeUiState.savedScoreboards().put(player.getUniqueId(), player.getScoreboard());
        }

        Scoreboard scoreboard = player.getServer().getScoreboardManager().getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("sheepmerge_points", "dummy", "Sheep Merge Coins");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        renderPointsScoreboard(player, scoreboard, objective);
        SheepRuntimeUiState.lastPointsScoreboardUpdates().put(player.getUniqueId(), System.currentTimeMillis());
        player.setScoreboard(scoreboard);
    }

    public static void updatePointsScoreboard(Player player) {
        if (player == null) {
            return;
        }
        long now = System.currentTimeMillis();
        UUID playerId = player.getUniqueId();
        long lastUpdatedAt = SheepRuntimeUiState.lastPointsScoreboardUpdates().getOrDefault(playerId, 0L);
        if (now - lastUpdatedAt < SCOREBOARD_UPDATE_INTERVAL_MS) {
            return;
        }
        Scoreboard scoreboard = player.getScoreboard();
        Objective objective = scoreboard == null ? null : scoreboard.getObjective("sheepmerge_points");
        if (objective == null) {
            showPointsScoreboard(player);
            return;
        }
        renderPointsScoreboard(player, scoreboard, objective);
        SheepRuntimeUiState.lastPointsScoreboardUpdates().put(playerId, now);
    }

    public static void recordVisitedOtherFarm(Player visitor, UUID ownerId) {
        if (visitor == null) {
            return;
        }
        UUID visitorId = visitor.getUniqueId();
        if (ownerId == null || ownerId.equals(visitorId)) {
            return;
        }
        SheepLifetimeProgressState.incrementLifetimeOtherFarmVisits(visitorId);
        if (SOCIALS_AUTHOR_UUID.equals(ownerId)) {
            SheepLifetimeProgressState.setVisitedOwnerFarm(visitorId, true);
        }
        evaluateAchievementProgress(visitor, true);
        saveData();
    }

    private static String getQuestScoreLine(String label, int progress, int target, boolean complete) {
        return color((complete ? "&a" : "&b") + label + "&8: &f"
                + (complete ? "done" : (progress + "/" + target)));
    }

    private static String makeScoreboardSpacer(int index) {
        return " ".repeat(Math.max(1, index));
    }

    private static void renderPointsScoreboard(Player player, Scoreboard scoreboard, Objective objective) {
        if (player == null || scoreboard == null || objective == null) {
            return;
        }

        objective.setDisplayName(color("&6&lSheepMerge &f&lStats"));

        for (String entry : new HashSet<>(scoreboard.getEntries())) {
            scoreboard.resetScores(entry);
        }

        UUID playerId = player.getUniqueId();
        List<String> lines = new ArrayList<>();
        lines.add(color("&6Coins&8: &f" + formatPoints(getPlayerPointsBig(player))));
        if (shouldShowScoreboardAchievementPoints(player)) {
            lines.add(color("&dAchv&8: &f" + getAchievementPoints(player)));
        }
        if (shouldShowScoreboardPrestigeStats(player)) {
            lines.add(color("&5Prestige&8: &fLv " + getPrestigeLevel(player)));
            lines.add(color("&5P.Pts&8: &f" + formatPoints(getPrestigePoints(player))));
        }

        if (shouldShowScoreboardQuestPoints(player)) {
            lines.add(color("&aQuest&8: &f" + formatPoints(getQuestPoints(player))));
        }
        if (shouldShowScoreboardAutomationPoints(player)) {
            lines.add(color("&cAuto&8: &f" + formatPoints(getAutomationPoints(player))));
        }
        if (shouldShowScoreboardSacrificePoints(player)) {
            lines.add(color("&4Sac&8: &f" + formatPoints(getSacrificePoints(player))));
        }

        boolean compact = getScoreboardLayoutMode(player) == 1;

        if (!compact && shouldShowScoreboardQuestProgress(player)) {
            long questResetSeconds = Math.max(0L, (getQuestResetRemainingMs(player) + 999L) / 1000L);
            lines.add(makeScoreboardSpacer(lines.size() + 1));
            lines.add(color("&a&lQuests &8(&f" + questResetSeconds + "s&8)"));
            lines.add(getQuestScoreLine("Shear", SheepQuestState.questShears().getOrDefault(playerId, 0),
                    getQuestTarget(player, QUEST_SHEARS_TARGET),
                    SheepQuestState.questShearsComplete().getOrDefault(playerId, false)));
            lines.add(getQuestScoreLine("Spawn", SheepQuestState.questSpawns().getOrDefault(playerId, 0),
                    getQuestTarget(player, QUEST_SPAWNS_TARGET),
                    SheepQuestState.questSpawnsComplete().getOrDefault(playerId, false)));
            lines.add(getQuestScoreLine("Merge", SheepQuestState.questMerges().getOrDefault(playerId, 0),
                    getQuestTarget(player, QUEST_MERGES_TARGET),
                    SheepQuestState.questMergesComplete().getOrDefault(playerId, false)));
        }

        if (!compact && shouldShowScoreboardAbilityStatus(player)) {
            lines.add(makeScoreboardSpacer(lines.size() + 1));
            lines.add(color("&d&lAbilities"));
            lines.add(getCountAbilityScoreLine("Lucky", SheepQuestState.activeLuckyBurstUses(),
                    SheepQuestState.luckyBurstEnabled(), playerId));
            lines.add(getAbilityScoreLine("Wool", SheepQuestState.activeWoolRushUntil(),
                    SheepQuestState.pausedWoolRushRemaining(), playerId));
            lines.add(getAbilityScoreLine("Jackpot", SheepQuestState.activeJackpotShearsUntil(),
                    SheepQuestState.pausedJackpotShearsRemaining(), playerId));
            lines.add(getCountAbilityScoreLine("Merge", SheepQuestState.activeAutoMergeUses(),
                    SheepQuestState.autoMergeEnabled(), playerId));
            lines.add(getCountAbilityScoreLine("Shear", SheepQuestState.activeAutoShearUses(),
                    SheepQuestState.autoShearEnabled(), playerId));
        }

        int score = Math.min(15, lines.size());
        for (int index = 0; index < lines.size() && score > 0; index++) {
            objective.getScore(lines.get(index)).setScore(score);
            score--;
        }

        updateTabListPointsVisibility(player);
    }

    public static void updateTabListPointsVisibility(Player player) {
        if (player == null) {
            return;
        }
        if (!isSheepFarmWorld(player.getWorld())) {
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
        Scoreboard previous = SheepRuntimeUiState.savedScoreboards().remove(player.getUniqueId());
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
            UUID playerId = player.getUniqueId();
            boolean wasInSheepWorld = isSheepFarmWorld(player.getWorld());
            boolean hadSavedInventory = SheepRuntimeUiState.savedInventories().containsKey(playerId);
            boolean hadSavedScoreboard = SheepRuntimeUiState.savedScoreboards().containsKey(playerId);

            if (hadSavedInventory) {
                restorePlayerInventory(player);
            }
            if (hadSavedScoreboard) {
                restorePlayerScoreboard(player);
            }
            clearEggTimer(player);
            clearPickedUpSheep(player);
            clearComboRuntime(player);
            if (fallbackWorld != null && wasInSheepWorld) {
                Location fallbackSpawn = fallbackWorld.getSpawnLocation().clone().add(0.5D, 0.0D, 0.5D);
                player.teleport(fallbackSpawn);
            }
            if (wasInSheepWorld && !hadSavedInventory) {
                clearForcedFarmLoadoutWithoutSnapshot(player);
            }
            if (wasInSheepWorld && !hadSavedScoreboard) {
                clearSheepMergeSidebarWithoutSnapshot(player);
            }
            player.setPlayerListName(null);
        }
        SheepRuntimeUiState.savedScoreboards().clear();
        EGG_MODULE.clearSavedExperienceCache();
    }

    private static void clearForcedFarmLoadoutWithoutSnapshot(Player player) {
        if (player == null) {
            return;
        }
        var inventory = player.getInventory();
        ItemStack[] storage = inventory.getStorageContents();
        boolean changed = false;

        if (FARM_UPGRADE_COMMAND_SLOT >= 0
                && FARM_UPGRADE_COMMAND_SLOT < storage.length
                && isSheepMergeUpgradeCommandItem(storage[FARM_UPGRADE_COMMAND_SLOT])) {
            storage[FARM_UPGRADE_COMMAND_SLOT] = null;
            changed = true;
        }
        if (FARM_EGG_ITEM_SLOT >= 0
                && FARM_EGG_ITEM_SLOT < storage.length
                && isSheepMergeEggItem(storage[FARM_EGG_ITEM_SLOT])) {
            storage[FARM_EGG_ITEM_SLOT] = null;
            changed = true;
        }
        if (FARM_SHEARS_ITEM_SLOT >= 0
                && FARM_SHEARS_ITEM_SLOT < storage.length
                && isSheepMergeShearsItem(storage[FARM_SHEARS_ITEM_SLOT])) {
            storage[FARM_SHEARS_ITEM_SLOT] = null;
            changed = true;
        }

        int quickStart = Math.max(0, INVENTORY_QUICK_ACCESS_FIRST_SLOT);
        int quickEnd = Math.min(storage.length - 1, INVENTORY_QUICK_ACCESS_LAST_SLOT);
        for (int slot = quickStart; slot <= quickEnd; slot++) {
            if (isQuickAccessCommandItem(storage[slot])) {
                storage[slot] = null;
                changed = true;
            }
        }

        if (changed) {
            inventory.setStorageContents(storage);
        }

        ItemStack offHand = inventory.getItemInOffHand();
        if (isSheepMergeShearsItem(offHand)) {
            inventory.setItemInOffHand(null);
        }
    }

    private static void clearSheepMergeSidebarWithoutSnapshot(Player player) {
        if (player == null) {
            return;
        }
        Scoreboard current = player.getScoreboard();
        Objective objective = current == null ? null : current.getObjective("sheepmerge_points");
        if (objective == null) {
            return;
        }
        if (Bukkit.getScoreboardManager() != null) {
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }

    public static boolean isSheepMergeShearsItem(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() != Material.SHEARS) {
            return false;
        }
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null || !meta.isUnbreakable()) {
            return false;
        }
        return "Sheep Merge Shears".equals(meta.getDisplayName());
    }

    public static boolean isManagedShearsHotbarSlot(int slot) {
        return slot == FARM_SHEARS_ITEM_SLOT;
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
            SheepEconomyState.clearPersistedKeys(dataConfig);
            SheepPrestigeState.clearPersistedKeys(dataConfig);
            dataConfig.set("prestigeExpandFarm", null);
            SheepUpgradeState.clearPersistedKeys(dataConfig);
            SheepVisitAccessState.clearPersistedKeys(dataConfig);
            SheepEffectPreferences.clearPersistedKeys(dataConfig);
            SheepQuestState.clearPersistedKeys(dataConfig);
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
            SheepUiPreferences.clearPersistedKeys(dataConfig);
            dataConfig.set("liveUpdate", null);
            dataConfig.set("dataSchemaVersion", null);
            SheepSacrificeProgression.clearPersistedKeys(dataConfig);
            SheepRebirthState.clearPersistedKeys(dataConfig);
            SheepLifetimeProgressState.clearPersistedKeys(dataConfig);
            SheepAchievementState.clearPersistedKeys(dataConfig);
            dataConfig.set("farmSheep", null);
            dataConfig.set("tutorialSheep", null);
            dataConfig.set("pendingInventory", null);
            SheepEconomyState.saveTo(dataConfig);
            SheepPrestigeState.saveTo(dataConfig);
            SheepUpgradeState.saveTo(dataConfig);
            SheepTutorialState.saveTo(dataConfig);
            SheepVisitAccessState.saveTo(dataConfig);
            SheepEffectPreferences.saveTo(dataConfig);
            SheepQuestState.saveTo(dataConfig);
            SheepComboState.saveTo(dataConfig);
            SheepAutomationState.saveTo(dataConfig);
            SheepUiPreferences.saveTo(dataConfig);
            SheepSacrificeProgression.saveTo(dataConfig);
            int rebirthTreeMask = (1 << REBIRTH_SKILL_NODES.size()) - 1;
            SheepRebirthState.saveTo(dataConfig, rebirthTreeMask);
            SheepLifetimeProgressState.saveTo(dataConfig);
            SheepAchievementState.saveTo(dataConfig);
            dataConfig.set("liveUpdate.enabled", SheepLiveUpdateState.isLiveUpdateEnabled());
            dataConfig.set("liveUpdate.stagedVersion", SheepLiveUpdateState.getStagedLiveUpdateVersion());
            dataConfig.set("liveUpdate.lastStatus", SheepLiveUpdateState.getLastLiveUpdateStatus());
            dataConfig.set("liveUpdate.lastCheckAt", Math.max(0L, SheepLiveUpdateState.getLastLiveUpdateCheckAt()));
            dataConfig.set("dataSchemaVersion", Math.max(0, SheepLiveUpdateState.getDataSchemaVersion()));
            saveSheepSnapshots("farmSheep", savedFarmSheepByPlayer);
            saveSheepSnapshots("tutorialSheep", savedTutorialSheepByPlayer);
            for (Map.Entry<UUID, InventoryDataUtils.Snapshot> entry : SheepRuntimeUiState.savedInventories()
                    .entrySet()) {
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
        SheepEconomyState.loadFrom(dataConfig);
        SheepPrestigeState.loadFrom(
                dataConfig,
                PRESTIGE_DOUBLE_POINTS_MAX_LEVEL,
                SheepTier.RAINBOW.getLevel());
        loadSheepSnapshots("farmSheep", savedFarmSheepByPlayer);
        loadSheepSnapshots("tutorialSheep", savedTutorialSheepByPlayer);
        SheepUpgradeState.loadFrom(
                dataConfig,
                SheepTier.WHITE.getLevel(),
                SheepTier.RAINBOW.getLevel());
        SheepTutorialState.loadFrom(dataConfig);
        SheepVisitAccessState.loadFrom(dataConfig);
        SheepEffectPreferences.loadFrom(dataConfig);
        SheepQuestState.loadFrom(dataConfig);
        SheepComboState.loadFrom(dataConfig, COMBO_DECAY_MAX_LEVEL, COMBO_GAIN_MAX_LEVEL);
        SheepAutomationState.loadFrom(
                dataConfig,
                AUTOMATION_AUTO_BUY_MAX_LEVEL,
                AUTOMATION_AUTO_ABILITY_MAX_LEVEL,
                AUTOMATION_SLOW_AUTO_MERGE_MAX_LEVEL,
                AUTOMATION_SLOW_AUTO_SHEAR_MAX_LEVEL,
                AUTOMATION_AUTO_SPAWN_MAX_LEVEL,
                AUTOMATION_SINGLE_LEVEL_MAX);
        SheepUiPreferences.loadFrom(dataConfig, INVENTORY_QUICK_ACCESS_MAX_ITEMS,
                actionId -> getQuickAccessDefinition(actionId) != null);
        SheepSacrificeProgression.loadFrom(dataConfig);
        int rebirthTreeMask = (1 << REBIRTH_SKILL_NODES.size()) - 1;
        SheepRebirthState.loadFrom(dataConfig, rebirthTreeMask);
        SheepLifetimeProgressState.loadFrom(dataConfig);
        SheepAchievementState.loadFrom(dataConfig,
                id -> getAchievementDefinition(id) != null,
                id -> getAchievementMilestoneDefinition(id) != null);
        SheepLiveUpdateState.loadPersistedState(
                !dataConfig.contains("liveUpdate.enabled") || dataConfig.getBoolean("liveUpdate.enabled", true),
                dataConfig.getString("liveUpdate.stagedVersion", ""),
                dataConfig.getString("liveUpdate.lastStatus", "Not checked yet."),
                dataConfig.getLong("liveUpdate.lastCheckAt", 0L),
                dataConfig.getInt("dataSchemaVersion", 0));
        Set<UUID> playersToClamp = new HashSet<>();
        playersToClamp.addAll(SheepEconomyState.getUpgradeTrackedPlayerIds());
        playersToClamp.addAll(SheepPrestigeState.getHigherMaxTrackedPlayerIds());
        playersToClamp.addAll(SheepSacrificeProgression.getUnlockTrackedPlayerIds());
        for (UUID playerId : playersToClamp) {
            clampUpgradeLevelsToCurrentCaps(playerId);
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
                    SheepRuntimeUiState.savedInventories().put(uuid,
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
        SheepEntityRuntimeState.putCarriedSheep(player.getUniqueId(), sheep);
        updateCarriedSheepPosition(player);
    }

    public static boolean hasPickedUpSheep(Player player) {
        return player != null && SheepEntityRuntimeState.hasCarriedSheep(player.getUniqueId());
    }

    public static Sheep getPickedUpSheep(Player player) {
        if (player == null) {
            return null;
        }
        Sheep sheep = SheepEntityRuntimeState.getCarriedSheep(player.getUniqueId());
        if (sheep != null && !sheep.isValid()) {
            SheepEntityRuntimeState.removeCarriedSheep(player.getUniqueId());
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
        SheepEntityRuntimeState.removeCarriedSheep(player.getUniqueId());
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

        Location carryLocation = player.getLocation().clone().add(0.0D, 1.8D, 0.0D);
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
        Sheep sheep = SheepEntityRuntimeState.removeCarriedSheep(player.getUniqueId());
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
