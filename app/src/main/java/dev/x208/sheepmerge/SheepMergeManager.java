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
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public final class SheepMergeManager {

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
    private static final int PRESTIGE_DOUBLE_POINTS_MAX_LEVEL = 20;
    private static final int BASE_EGG_CAP = 10;
    private static final int PRESTIGE_EGG_CAP_STEP = 10;
    private static final double PRESTIGE_QUEST_REWARD_BONUS_PER_LEVEL = 0.25D;
    private static final String FARM_BUILD_WORLD_NAME = "sheepfarm_build";
    private static final int FARM_WORLD_RADIUS_CHUNKS = 5;
    private static final int FARM_LAYOUT_SAVE_CHUNK_SPAN = 4;
    private static final int FARM_LAYOUT_SAVE_CHUNK_HALF_SPAN = FARM_LAYOUT_SAVE_CHUNK_SPAN / 2;
    private static final int FARM_MIN_XZ = -5;
    private static final int FARM_MAX_XZ = 6;
    private static final int FARM_RADIUS = Math.max(Math.abs(FARM_MIN_XZ), Math.abs(FARM_MAX_XZ));
    private static final int FARM_BASE_Y = 100;
    private static int SHEAR_SHOP_BASE_COST = 20;
    private static int SHEAR_WOOL_SAVE_BASE_COST = 30;
    private static int SHEAR_TIER_BOOST_BASE_COST = 45;
    private static final int SHEAR_WOOL_SAVE_CHANCE_PER_LEVEL = 5;
    private static final int SHEAR_WOOL_SAVE_CHANCE_CAP = 50;
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
    private static final long POINTS_OVERLAY_DISPLAY_DURATION_MS = 1_400L;
    private static final int COMBO_DECAY_MAX_LEVEL = 20;
    private static final int COMBO_GAIN_MAX_LEVEL = 20;
    private static int COMBO_DECAY_BASE_COST = 75;
    private static int COMBO_GAIN_BASE_COST = 90;
    private static int COMBO_MAX_BASE_PRESTIGE_COST = 3;
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
    private static final int INVENTORY_QUICK_ACCESS_MAX_ITEMS = 6;
    private static final UUID SOCIALS_AUTHOR_UUID = UUID.fromString("27268675-a9b7-4abd-9628-e6c4515a5cf6");

    public static boolean isAuthor(Player player) {
        return player != null && SOCIALS_AUTHOR_UUID.equals(player.getUniqueId());
    }

    private static SheepMergePlugin plugin;
    private static final SheepEggModule EGG_MODULE = new SheepEggModule();
    private static FileConfiguration dataConfig;
    private static File dataFile;

    static SheepMergePlugin leaderboardPlugin() {
        return plugin;
    }

    static FileConfiguration leaderboardDataConfig() {
        return dataConfig;
    }

    static FileConfiguration ensureLeaderboardDataConfig() {
        if (dataConfig == null && dataFile != null) {
            dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        }
        return dataConfig;
    }

    private static File farmStructureCacheDirectory;
    private static final Object FARM_STRUCTURE_CACHE_REFRESH_LOCK = new Object();
    private static long lastFarmStructureCacheRefreshAtMs = 0L;
    private static boolean farmCommitInProgress = false;

    static record AchievementMenuEntry(String id, Material material, String name, String objective, String reward,
            int achievementPoints) {
    }

    static record AchievementMilestoneMenuEntry(String id, int requiredPoints, Material material, String name,
            String reward) {
    }

    static record RebirthSkillMenuEntry(int id, int parentId, int slot, Material material, String name,
            String effectLine, int cost) {
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

    private SheepMergeManager() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void initialize(SheepMergePlugin plugin) {
        SheepMergeManager.plugin = plugin;
        SheepEntityRuntime.initialize(plugin);
        dataFile = new File(plugin.getDataFolder(), "scores.yml");
        SheepFarmLayoutManager.initialize(plugin);
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
        SheepQuestRuntime.configure(
                configuration.getQuestShearsTarget(),
                configuration.getQuestSpawnsTarget(),
                configuration.getQuestMergesTarget(),
                configuration.getQuestShearsReward(),
                configuration.getQuestSpawnsReward(),
                configuration.getQuestMergesReward(),
                configuration.getAbilityLuckyBurstBaseCost(),
                configuration.getAbilityWoolRushBaseCost(),
                configuration.getAbilityJackpotShearsBaseCost(),
                configuration.getAbilityAutoMergeBaseCost(),
                configuration.getAbilityAutoShearBaseCost(),
                configuration.getAbilityLuckyBurstBaseDurationMs(),
                configuration.getAbilityWoolRushBaseDurationMs(),
                configuration.getAbilityJackpotShearsBaseDurationMs(),
                configuration.getAbilityAutoMergeBaseDurationMs(),
                configuration.getAbilityAutoShearBaseDurationMs(),
                configuration.getQuestUpgradeDurationBaseCost(),
                configuration.getQuestUpgradePowerBaseCost());
        SHEAR_SHOP_BASE_COST = configuration.getShearShopBaseCost();
        SHEAR_WOOL_SAVE_BASE_COST = configuration.getShearWoolSaveBaseCost();
        SHEAR_TIER_BOOST_BASE_COST = configuration.getShearTierBoostBaseCost();
        COMBO_DECAY_BASE_COST = configuration.getComboDecayBaseCost();
        COMBO_GAIN_BASE_COST = configuration.getComboGainBaseCost();
        COMBO_MAX_BASE_PRESTIGE_COST = configuration.getComboMaxBasePrestigeCost();
        SheepAutomationRuntime.configure(
                configuration.getAutomationAutoBuyBaseCost(),
                configuration.getAutomationAutoAbilityBaseCost(),
                configuration.getAutomationSlowAutoMergeBaseCost(),
                configuration.getAutomationSlowAutoShearBaseCost(),
                Math.max(1, configuration.getAutomationAutoSpawnBaseCost() / 2),
                configuration.getAutomationPointIntervalMs(),
                configuration.getAutomationAutoBuyIntervalMs(),
                configuration.getAutomationAutoAbilityIntervalMs(),
                configuration.getAutomationSlowAutoMergeIntervalMs(),
                configuration.getAutomationSlowAutoShearIntervalMs(),
                configuration.getAutomationAutoSpawnBaseIntervalMs(),
                configuration.getAutomationAutoSpawnIntervalStepMs(),
                configuration.getAutomationAutoSpawnMinIntervalMs(),
                configuration.getAutomationConditionMinPointsReserve(),
                configuration.getAutomationConditionMinQuestPoints(),
                configuration.getAutomationConditionMinSheepForMerge(),
                configuration.getAutomationConditionMinReadySheepForShear());
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
        SheepTutorialRuntime.configure(
                TUTORIAL_SHEAR_TARGET,
                TUTORIAL_SPAWN_TARGET,
                TUTORIAL_MERGE_TARGET,
                TUTORIAL_MENU_SECTION_TARGET,
                TUTORIAL_REMINDER_DELAY_MS,
                TUTORIAL_REMINDER_REPEAT_MS,
                TUTORIAL_TASK_TITLE_REPEAT_MS,
                TUTORIAL_STATUS_FEED_REPEAT_MS,
                TUTORIAL_FOCUS_NOTIFICATION_COOLDOWN_MS,
                TUTORIAL_MERGE_POINTS_REMINDER_REPEAT_MS);
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
        return SheepLiveUpdateState.getLiveUpdateStatusLines(
                CURRENT_DATA_SCHEMA_VERSION,
                getCurrentPluginVersion());
    }

    public static String getCurrentPluginVersion() {
        return plugin == null ? "unknown" : plugin.getDescription().getVersion();
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
        return SheepFarmLayoutManager.hasSavedLayout();
    }

    public static String getFarmBuildWorldName() {
        return FARM_BUILD_WORLD_NAME;
    }

    public static boolean isFarmBuildWorld(World world) {
        return world != null && FARM_BUILD_WORLD_NAME.equals(world.getName());
    }

    public static boolean saveBuildWorldToLayoutFile() {
        return SheepFarmLayoutManager.saveBuildWorldToLayoutFile();
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
        return SheepFarmLayoutManager.capture(sourceWorld);
        /*
         * if (sourceWorld == null || (!isSheepFarmWorld(sourceWorld) &&
         * !isFarmBuildWorld(sourceWorld))) {
         * return false;
         * }
         * if (farmLayoutConfig == null) {
         * farmLayoutConfig = new YamlConfiguration();
         * }
         * int minChunkX = Integer.MAX_VALUE;
         * int maxChunkX = Integer.MIN_VALUE;
         * int minChunkZ = Integer.MAX_VALUE;
         * int maxChunkZ = Integer.MIN_VALUE;
         * for (Chunk loadedChunk : sourceWorld.getLoadedChunks()) {
         * if (loadedChunk == null) {
         * continue;
         * }
         * minChunkX = Math.min(minChunkX, loadedChunk.getX());
         * maxChunkX = Math.max(maxChunkX, loadedChunk.getX());
         * minChunkZ = Math.min(minChunkZ, loadedChunk.getZ());
         * maxChunkZ = Math.max(maxChunkZ, loadedChunk.getZ());
         * }
         * if (minChunkX > maxChunkX || minChunkZ > maxChunkZ) {
         * minChunkX = -FARM_LAYOUT_SAVE_CHUNK_HALF_SPAN;
         * maxChunkX = minChunkX + FARM_LAYOUT_SAVE_CHUNK_SPAN - 1;
         * minChunkZ = -FARM_LAYOUT_SAVE_CHUNK_HALF_SPAN;
         * maxChunkZ = minChunkZ + FARM_LAYOUT_SAVE_CHUNK_SPAN - 1;
         * }
         * int minAllowedChunkX = -FARM_LAYOUT_SAVE_CHUNK_HALF_SPAN;
         * int maxAllowedChunkX = minAllowedChunkX + FARM_LAYOUT_SAVE_CHUNK_SPAN - 1;
         * int minAllowedChunkZ = -FARM_LAYOUT_SAVE_CHUNK_HALF_SPAN;
         * int maxAllowedChunkZ = minAllowedChunkZ + FARM_LAYOUT_SAVE_CHUNK_SPAN - 1;
         * minChunkX = Math.max(minChunkX, minAllowedChunkX);
         * maxChunkX = Math.min(maxChunkX, maxAllowedChunkX);
         * minChunkZ = Math.max(minChunkZ, minAllowedChunkZ);
         * maxChunkZ = Math.min(maxChunkZ, maxAllowedChunkZ);
         * if (minChunkX > maxChunkX || minChunkZ > maxChunkZ) {
         * minChunkX = minAllowedChunkX;
         * maxChunkX = maxAllowedChunkX;
         * minChunkZ = minAllowedChunkZ;
         * maxChunkZ = maxAllowedChunkZ;
         * }
         * int minX = minChunkX << 4;
         * int maxX = (maxChunkX << 4) + 15;
         * int minZ = minChunkZ << 4;
         * int maxZ = (maxChunkZ << 4) + 15;
         *
         * farmLayoutConfig.set("version", 3);
         * farmLayoutConfig.set("world.minY", sourceWorld.getMinHeight());
         * farmLayoutConfig.set("world.maxY", sourceWorld.getMaxHeight());
         * farmLayoutConfig.set("world.minX", minX);
         * farmLayoutConfig.set("world.maxX", maxX);
         * farmLayoutConfig.set("world.minZ", minZ);
         * farmLayoutConfig.set("world.maxZ", maxZ);
         * farmLayoutConfig.set("world.name", sourceWorld.getName());
         * farmLayoutConfig.set("world.savedAt", System.currentTimeMillis());
         * farmLayoutConfig.set("chunks", null);
         * farmLayoutConfig.set("blocks", null);
         *
         * int minY = sourceWorld.getMinHeight();
         * int maxY = sourceWorld.getMaxHeight();
         * for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
         * for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
         * String chunkPath = "chunks." + chunkKeyFor(chunkX, chunkZ);
         * farmLayoutConfig.set(chunkPath + ".x", chunkX);
         * farmLayoutConfig.set(chunkPath + ".z", chunkZ);
         *
         * List<String> palette = new ArrayList<>();
         * Map<String, Integer> paletteIndices = new HashMap<>();
         * StringBuilder encodedRuns = new StringBuilder();
         * int previousPaletteIndex = -1;
         * int runLength = 0;
         *
         * for (int y = minY; y < maxY; y++) {
         * for (int localX = 0; localX < 16; localX++) {
         * for (int localZ = 0; localZ < 16; localZ++) {
         * int worldX = (chunkX << 4) + localX;
         * int worldZ = (chunkZ << 4) + localZ;
         * String serializedBlockData = sourceWorld.getBlockAt(worldX, y, worldZ)
         * .getBlockData()
         * .getAsString();
         *
         * Integer paletteIndex = paletteIndices.get(serializedBlockData);
         * if (paletteIndex == null) {
         * paletteIndex = palette.size();
         * palette.add(serializedBlockData);
         * paletteIndices.put(serializedBlockData, paletteIndex);
         * }
         *
         * if (paletteIndex == previousPaletteIndex) {
         * runLength++;
         * } else {
         * appendChunkRun(encodedRuns, previousPaletteIndex, runLength);
         * previousPaletteIndex = paletteIndex;
         * runLength = 1;
         * }
         * }
         * }
         * }
         *
         * appendChunkRun(encodedRuns, previousPaletteIndex, runLength);
         * farmLayoutConfig.set(chunkPath + ".format", "rle-v1");
         * farmLayoutConfig.set(chunkPath + ".palette", palette);
         * farmLayoutConfig.set(chunkPath + ".data", encodedRuns.toString());
         * farmLayoutConfig.set(chunkPath + ".height", maxY - minY);
         * }
         * }
         * return saveFarmLayout();
         */
    }

    public static int applySharedFarmLayoutToAllFarmWorlds() {
        return SheepFarmLayoutManager.applyToAllFarmWorlds();
    }

    public static void applyFarmLayout(World world) {
        SheepFarmLayoutManager.apply(world);
    }

    public static void applyFarmLayoutAsync(World world, Runnable onComplete) {
        SheepFarmLayoutManager.applyAsync(world, onComplete);
    }

    public static void saveSheepSnapshotForWorld(World world) {
        SheepSnapshotState.saveForWorld(world);
    }

    public static void restoreSavedSheepForWorld(World world) {
        SheepSnapshotState.restoreForWorld(world);
    }

    public static void restoreSavedSheepForWorldAsync(World world) {
        restoreSavedSheepForWorldAsync(world, null);
    }

    public static void restoreSavedSheepForWorldAsync(World world, Runnable onComplete) {
        SheepSnapshotState.restoreForWorldAsync(world, onComplete);
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
        addKnownFarmOwners(ownerIds, SheepSnapshotState.farmOwnerIds());
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
            SheepEntityRuntimeState.clearRescue(sheepId);
        }
        refreshLiveSheepCount(world);
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

    private static void loadFarmLayout() {
        if (plugin == null) {
            return;
        }
        SheepFarmLayoutManager.load();
    }

    private static boolean saveFarmLayout() {
        return SheepFarmLayoutManager.save();
    }

    public static NamespacedKey getTierKey() {
        return SheepEntityRuntime.getTierKey();
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

    static List<SheepSettingsMenus.QuickAccessOption> getSettingsQuickAccessOptions() {
        List<SheepSettingsMenus.QuickAccessOption> options = new ArrayList<>();
        for (QuickAccessDefinition definition : QUICK_ACCESS_DEFINITIONS) {
            options.add(new SheepSettingsMenus.QuickAccessOption(
                    definition.id,
                    definition.material,
                    definition.name,
                    definition.description));
        }
        return options;
    }

    static SheepSettingsMenus.QuickAccessOption getSettingsQuickAccessOption(String actionId) {
        QuickAccessDefinition definition = getQuickAccessDefinition(actionId);
        if (definition == null) {
            return null;
        }
        return new SheepSettingsMenus.QuickAccessOption(
                definition.id,
                definition.material,
                definition.name,
                definition.description);
    }

    static List<String> getSettingsQuickAccessActions(UUID playerId) {
        return getInventoryQuickAccessActions(playerId);
    }

    static boolean isSettingsQuickAccessCastingEnabled(Player player) {
        return isInventoryQuickAccessCastingEnabled(player);
    }

    static boolean toggleSettingsQuickAccessCasting(Player player) {
        return toggleInventoryQuickAccessCasting(player);
    }

    static boolean toggleSettingsQuickAccessAction(Player player, String actionId) {
        return toggleInventoryQuickAccessAction(player, actionId);
    }

    static int getInventoryQuickAccessMaxItems() {
        return INVENTORY_QUICK_ACCESS_MAX_ITEMS;
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

    static List<ItemStack> buildQuickAccessHotbarItemsForRuntime(Player player) {
        return buildQuickAccessHotbarItems(player);
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
        return SheepFarmLayoutManager.needsBootstrap(world);
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

    private static void resetTutorialProgress(UUID playerId) {
        SheepTutorialRuntime.resetProgress(playerId);
    }

    public static int getPrestigeLevel(Player player) {
        return SheepPrestigeRuntime.getLevel(player);
    }

    public static int getPrestigePoints(Player player) {
        return SheepPrestigeRuntime.getPoints(player);
    }

    public static int getPrestigeMaxLevel() {
        return SheepPrestigeRuntime.getMaxLevel();
    }

    public static String formatPoints(long points) {
        return SheepFormatting.formatPoints(points);
    }

    public static String formatPoints(BigInteger points) {
        return SheepFormatting.formatPoints(points);
    }

    public static int getQuestPoints(Player player) {
        return SheepQuestRuntime.getPoints(player);
    }

    public static int getRebirthLevel(Player player) {
        return SheepRebirthRuntime.getLevel(player);
    }

    public static int getRebirthPoints(Player player) {
        return SheepRebirthRuntime.getPoints(player);
    }

    public static int getUnspentRebirthPointsDisplay(Player player) {
        return SheepRebirthRuntime.getUnspentPoints(player);
    }

    public static int getRebirthNextCostInPrestigeLevels(Player player) {
        return SheepRebirthRuntime.getNextCostInPrestigeLevels(getRebirthLevel(player));
    }

    public static int getAffordableRebirthLevelsDisplay(Player player) {
        return SheepRebirthRuntime.getAffordableLevels(player, getPrestigeLevel(player));
    }

    public static long getRebirthRespecRemainingMs(Player player) {
        return SheepRebirthRuntime.getRespecRemainingMs(player);
    }

    private static int getPointsGainMultiplierFromRebirthSkills(Player player) {
        return SheepRebirthRuntime.getPointsGainMultiplier(player);
    }

    private static int getQuestPointsGainMultiplierFromRebirthSkills(Player player) {
        return SheepRebirthRuntime.getQuestPointsGainMultiplier(player);
    }

    private static int getSacrificePointsGainMultiplierFromRebirthSkills(Player player) {
        return SheepRebirthRuntime.getSacrificePointsGainMultiplier(player);
    }

    private static boolean hasQuestMaster(Player player) {
        return SheepRebirthRuntime.hasQuestMaster(player);
    }

    public static void tickQuestSystem(Player player) {
        SheepQuestRuntime.tick(player);
    }

    public static void tickTutorialReminder(Player player) {
        SheepTutorialRuntime.tickReminder(player);
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
        SheepQuestRuntime.recordShear(player);
    }

    public static void recordQuestSpawn(Player player) {
        SheepQuestRuntime.recordSpawn(player);
    }

    public static void recordQuestMerge(Player player) {
        SheepQuestRuntime.recordMerge(player);
    }

    public static List<String> getAchievementIds() {
        return SheepAchievementRuntime.ids();
    }

    public static String getAchievementDisplayName(String achievementId) {
        return SheepAchievementRuntime.displayName(achievementId);
    }

    public static boolean isAchievementUnlocked(Player player, String achievementId) {
        return SheepAchievementRuntime.isUnlocked(player, achievementId);
    }

    public static boolean adminCompleteAchievement(Player player, String achievementId, boolean notify) {
        return SheepAchievementRuntime.adminComplete(player, achievementId, notify);
    }

    public static int adminCompleteAllAchievements(Player player, boolean notify) {
        return SheepAchievementRuntime.adminCompleteAll(player, notify);
    }

    private static Set<String> getUnlockedAchievementIds(UUID playerId) {
        return SheepAchievementState.getUnlockedAchievementIds(playerId);
    }

    private static Set<String> getUnlockedAchievementMilestoneIds(UUID playerId) {
        return SheepAchievementState.getUnlockedAchievementMilestoneIds(playerId);
    }

    public static int getAchievementPoints(Player player) {
        return player == null ? 0 : SheepAchievementRuntime.points(player.getUniqueId());
    }

    private static int getAchievementPointMultiplier(Player player) {
        return player == null ? 1 : SheepAchievementRuntime.pointMultiplier(player.getUniqueId());
    }

    private static double getAchievementWoolRegenSpeedMultiplier(Player player) {
        return player == null ? 1.0D : getAchievementWoolRegenSpeedMultiplier(player.getUniqueId());
    }

    private static double getAchievementWoolRegenSpeedMultiplier(UUID playerId) {
        return SheepAchievementRuntime.woolRegenMultiplier(playerId);
    }

    private static void reconcileAchievementAutomationPointGrants() {
        SheepAchievementRuntime.reconcileAutomationPointGrants();
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

    private static void evaluateAchievementProgress(Player player, boolean notify) {
        SheepAchievementRuntime.evaluate(player, notify);
    }

    public static void evaluateAuthorOnlineSecretForOnlinePlayers() {
        SheepAchievementRuntime.evaluateAuthorOnlineSecretForOnlinePlayers();
    }

    public static void tickActiveAbilities(Player player) {
        if (player == null || !isSheepFarmWorld(player.getWorld())) {
            return;
        }
        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();

        SheepQuestRuntime.tickAbilities(player, now);
        tickAutomationSystems(player, playerId, now);
        updatePointsScoreboard(player);
    }

    private static void tickAutomationSystems(Player player, UUID playerId, long now) {
        SheepAutomationRuntime.tickSystems(player, now);
    }

    public static void tickAutomationAutoSpawnRealtime(Player player) {
        SheepAutomationRuntime.tickAutoSpawnRealtime(player);
    }

    public static void tickAutomationPlaytimePoints(Player player) {
        SheepAutomationRuntime.tickPlaytimePoints(player);
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

    private static boolean canAutoBuyUpgradeNow(Player player, BigInteger availablePoints, BigInteger cost) {
        return player != null
                && availablePoints != null
                && cost != null
                && cost.signum() > 0
                && availablePoints.compareTo(cost) >= 0
                && canSpendUpgradePointsDuringTutorial(player, cost);
    }

    static BigInteger automationPlayerPoints(Player player) {
        return getPlayerPointsBig(player);
    }

    static BigInteger automationAutoBuyCost(Player player, SheepAutomationRuntime.AutoBuyUpgrade upgrade,
            BigInteger availablePoints) {
        if (player == null || upgrade == null || availablePoints == null) {
            return null;
        }
        BigInteger cost = switch (upgrade) {
            case SHEEP_LIMIT -> getPlayerLimit(player) < getMaxSheepLimit(player.getUniqueId())
                    ? getUpgradeCost(player)
                    : null;
            case EGG_SPEED -> getEggSpeedLevel(player) < getEggSpeedMaxLevel(player)
                    ? getEggSpeedUpgradeCost(player)
                    : null;
            case WOOL_REGEN -> getWoolRegenLevel(player) < getWoolRegenMaxLevel(player)
                    ? getWoolRegenUpgradeCost(player)
                    : null;
            case HIGHER_TIER_CHANCE -> getHigherTierChanceLevel(player) < getHigherTierChanceMaxLevel(player)
                    ? getHigherTierChanceUpgradeCost(player)
                    : null;
            case COMBO_DECAY -> getComboDecayUpgradeLevel(player) < COMBO_DECAY_MAX_LEVEL
                    ? getComboDecayUpgradeCost(player)
                    : null;
            case COMBO_GAIN -> getComboGainUpgradeLevel(player) < COMBO_GAIN_MAX_LEVEL
                    ? getComboGainUpgradeCost(player)
                    : null;
            case SHEAR_WOOL_SAVE -> getShearWoolSaveLevel(player) < SHEAR_WOOL_SAVE_MAX_LEVEL
                    ? getShearWoolSaveUpgradeCost(player)
                    : null;
            case SHEAR_TIER_BOOST -> getShearTierBoostLevel(player) < SHEAR_TIER_BOOST_MAX_LEVEL
                    ? getShearTierBoostUpgradeCost(player)
                    : null;
            case SHEAR_VALUE -> getShearUpgradeCost(player);
        };
        return canAutoBuyUpgradeNow(player, availablePoints, cost) ? cost : null;
    }

    static boolean automationBuyUpgrade(Player player, SheepAutomationRuntime.AutoBuyUpgrade upgrade) {
        if (player == null || upgrade == null) {
            return false;
        }
        return switch (upgrade) {
            case SHEEP_LIMIT -> upgradeLimit(player);
            case EGG_SPEED -> upgradeEggSpeed(player);
            case WOOL_REGEN -> upgradeWoolRegen(player);
            case HIGHER_TIER_CHANCE -> upgradeHigherTierChance(player);
            case COMBO_DECAY -> upgradeComboDecay(player);
            case COMBO_GAIN -> upgradeComboGain(player);
            case SHEAR_WOOL_SAVE -> upgradeShearWoolSave(player);
            case SHEAR_TIER_BOOST -> upgradeShearTierBoost(player);
            case SHEAR_VALUE -> upgradeShearShop(player);
        };
    }

    static boolean automationHasMergeCandidates(Player player, int minimumCount) {
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
            if (count >= minimumCount) {
                return true;
            }
            byTier.put(key, count);
        }
        return false;
    }

    static int automationReadySheepCount(Player player) {
        return countReadySheep(player);
    }

    static boolean automationCanUseOwnedFarm(Player player) {
        return player != null && player.getWorld() != null && isSheepFarmWorld(player.getWorld())
                && isFarmOwner(player, player.getWorld());
    }

    static boolean automationWorldAtLimit(Player player) {
        return player == null || player.getWorld() == null || isWorldAtLimit(player.getWorld());
    }

    static boolean automationSpawnSheepFromSky(Player player) {
        return spawnAutomationSheepFromSky(player);
    }

    static void automationRecordSpawn(Player player) {
        recordQuestSpawn(player);
        recordTutorialSpawn(player);
    }

    static void automationPrestige(Player player) {
        prestige(player);
    }

    static void automationSaveData() {
        saveData();
    }

    public static void tickRandomFarmEvents() {
        SheepRandomEventRuntime.tickRandomFarmEvents();
    }

    public static boolean triggerSheepStormEvent() {
        return SheepRandomEventRuntime.triggerSheepStormEvent();
    }

    public static boolean isSheepStormActive() {
        return SheepRandomEventRuntime.isSheepStormActive();
    }

    public static boolean triggerComboFrenzyEvent() {
        return SheepRandomEventRuntime.triggerComboFrenzyEvent();
    }

    public static void broadcastRandomGameplayTip() {
        SheepRandomEventRuntime.broadcastRandomGameplayTip();
    }

    public static boolean tryAutoMergeOnPickup(Player player, Sheep pickedSheep) {
        return SheepQuestRuntime.tryAutoMergeOnPickup(player, pickedSheep);
    }

    static long getRemainingWoolRegenMs(Sheep sheep) {
        return SheepEntityRuntime.getRemainingWoolRegenMs(sheep);
    }

    static long getCombinedRemainingWoolRegenMs(Sheep first, Sheep second) {
        return SheepEntityRuntime.getCombinedRemainingWoolRegenMs(first, second);
    }

    public static void initializeMergedSheepAfterMerge(Sheep sheep, SheepTier tier, long remainingWoolRegenMs) {
        SheepEntityRuntime.initializeMergedSheep(sheep, tier, remainingWoolRegenMs);
    }

    public static int getShearShopLevel(Player player) {
        return SheepEconomyRuntime.getShearShopLevel(player);
    }

    public static int getShearWoolSaveLevel(Player player) {
        return SheepEconomyRuntime.getShearWoolSaveLevel(player);
    }

    public static int getShearTierBoostLevel(Player player) {
        return SheepEconomyRuntime.getShearTierBoostLevel(player);
    }

    public static int getShearFlatBonus(Player player) {
        return 0;
    }

    public static int getShearPointGainUpgradeLevel(Player player) {
        return SheepEconomyRuntime.getShearPointGainUpgradeLevel(player);
    }

    public static int getShearPointMultiplier(Player player) {
        return SheepEconomyRuntime.getShearPointMultiplier(player);
    }

    public static BigInteger getShearUpgradeCost(Player player) {
        return SheepEconomyRuntime.getShearUpgradeCost(player, SHEAR_SHOP_BASE_COST);
    }

    public static int getShearWoolSaveChancePercent(Player player) {
        return SheepEconomyRuntime.getShearWoolSaveChancePercent(player);
    }

    public static int getShearTierBoostChancePercent(Player player) {
        return SheepEconomyRuntime.getShearTierBoostChancePercent(player);
    }

    public static BigInteger getShearWoolSaveUpgradeCost(Player player) {
        return SheepEconomyRuntime.getShearWoolSaveUpgradeCost(player, SHEAR_WOOL_SAVE_BASE_COST);
    }

    public static BigInteger getShearTierBoostUpgradeCost(Player player) {
        return SheepEconomyRuntime.getShearTierBoostUpgradeCost(player, SHEAR_TIER_BOOST_BASE_COST);
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
        return SheepAutomationRuntime.getPoints(player);
    }

    public static int getAutomationAutoBuyUpgradeLevel(Player player) {
        return SheepAutomationRuntime.getAutoBuyUpgradeLevel(player);
    }

    public static int getAutomationAutoAbilityUpgradeLevel(Player player) {
        return SheepAutomationRuntime.getAutoAbilityUpgradeLevel(player);
    }

    public static int getAutomationSlowAutoMergeUpgradeLevel(Player player) {
        return SheepAutomationRuntime.getSlowAutoMergeUpgradeLevel(player);
    }

    public static int getAutomationSlowAutoShearUpgradeLevel(Player player) {
        return SheepAutomationRuntime.getSlowAutoShearUpgradeLevel(player);
    }

    public static int getAutomationAutoSpawnUpgradeLevel(Player player) {
        return SheepAutomationRuntime.getAutoSpawnUpgradeLevel(player);
    }

    public static int getAutomationAutoPrestigeUpgradeLevel(Player player) {
        return SheepAutomationRuntime.getAutoPrestigeUpgradeLevel(player);
    }

    public static BigInteger getSacrificePoints(Player player) {
        return SheepSacrificeProgression.getPoints(player);
    }

    public static int getSacrificeUnlocksBought(Player player) {
        return SheepSacrificeProgression.getUnlocksBought(player);
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

    private static void addSacrificePoints(UUID playerId, BigInteger amount) {
        SheepSacrificeProgression.addPoints(playerId, amount,
                SheepMergeManager::getSacrificePointsGainMultiplierFromRebirthSkills);
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
        return SheepAutomationRuntime.isAutoBuyEnabled(player);
    }

    public static boolean isAutomationAutoAbilityEnabled(Player player) {
        return SheepAutomationRuntime.isAutoAbilityEnabled(player);
    }

    public static boolean isAutomationSlowAutoMergeEnabled(Player player) {
        return SheepAutomationRuntime.isSlowAutoMergeEnabled(player);
    }

    public static boolean isAutomationSlowAutoShearEnabled(Player player) {
        return SheepAutomationRuntime.isSlowAutoShearEnabled(player);
    }

    public static boolean isAutomationAutoSpawnEnabled(Player player) {
        return SheepAutomationRuntime.isAutoSpawnEnabled(player);
    }

    public static boolean isAutomationAutoPrestigeEnabled(Player player) {
        return SheepAutomationRuntime.isAutoPrestigeEnabled(player);
    }

    private static int getUnlockedAutomationCount(Player player) {
        return SheepAutomationRuntime.getUnlockedCount(player);
    }

    private static int setAllAutomationsEnabled(Player player, boolean enabled) {
        return SheepAutomationRuntime.setAllEnabled(player, enabled);
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
        return SheepAutomationRuntime.getAutoBuyUpgradeCost(player);
    }

    private static int getAutomationAutoAbilityUpgradeCost(Player player) {
        return SheepAutomationRuntime.getAutoAbilityUpgradeCost(player);
    }

    private static int getAutomationSlowAutoMergeUpgradeCost(Player player) {
        return SheepAutomationRuntime.getSlowAutoMergeUpgradeCost(player);
    }

    private static int getAutomationSlowAutoShearUpgradeCost(Player player) {
        return SheepAutomationRuntime.getSlowAutoShearUpgradeCost(player);
    }

    private static int getAutomationAutoSpawnUpgradeCost(Player player) {
        return SheepAutomationRuntime.getAutoSpawnUpgradeCost(player);
    }

    private static int getAutomationAutoPrestigeUpgradeCost(Player player) {
        return SheepAutomationRuntime.getAutoPrestigeUpgradeCost(player);
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
        SheepComboRuntime.clampScoreToMax(player);
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
        return SheepAutomationRuntime.upgradeAutoBuy(player);
    }

    private static boolean upgradeAutomationAutoAbility(Player player) {
        return SheepAutomationRuntime.upgradeAutoAbility(player);
    }

    private static boolean upgradeAutomationSlowAutoMerge(Player player) {
        return SheepAutomationRuntime.upgradeSlowAutoMerge(player);
    }

    private static boolean upgradeAutomationSlowAutoShear(Player player) {
        return SheepAutomationRuntime.upgradeSlowAutoShear(player);
    }

    private static boolean upgradeAutomationAutoSpawn(Player player) {
        return SheepAutomationRuntime.upgradeAutoSpawn(player);
    }

    private static boolean upgradeAutomationAutoPrestige(Player player) {
        return SheepAutomationRuntime.upgradeAutoPrestige(player);
    }

    public static boolean upgradeShearShop(Player player) {
        return SheepEconomyRuntime.upgradeShearShop(player, SHEAR_SHOP_BASE_COST);
    }

    public static boolean upgradeShearWoolSave(Player player) {
        return SheepEconomyRuntime.upgradeShearWoolSave(player, SHEAR_WOOL_SAVE_BASE_COST);
    }

    public static boolean upgradeShearTierBoost(Player player) {
        return SheepEconomyRuntime.upgradeShearTierBoost(player, SHEAR_TIER_BOOST_BASE_COST);
    }

    public static int prestige(Player player) {
        return SheepPrestigeRuntime.prestige(player, PRESTIGE_LEVEL_BASE_COST);
    }

    public static int rebirth(Player player) {
        return SheepRebirthRuntime.rebirth(player, getPrestigeLevel(player));
    }

    private static void runPrestigeResetEffects(Player player, boolean forRebirth) {
        SheepPrestigeRuntime.runResetEffects(player, forRebirth);
    }

    static void prestigeClearReminder(Player player) {
        clearPrestigeReminder(player);
    }

    static void prestigeRunResetEffects(Player player, boolean forRebirth) {
        runPrestigeResetEffects(player, forRebirth);
    }

    static void prestigeSaveData() {
        saveData();
    }

    static void prestigeEvaluateAchievements(Player player) {
        evaluateAchievementProgress(player, true);
    }

    static void prestigeMarkTutorialComplete(Player player) {
        markTutorialPrestigedOnce(player);
    }

    static void prestigeAddEggs(Player player, int amount) {
        addEggs(player, amount);
    }

    static void prestigeUpgradeSheepBelowMinimum(Player player) {
        if (player != null) {
            upgradeSheepBelowMinimumSpawnTier(player.getWorld());
        }
    }

    static void prestigeResetUpgrades(UUID playerId, boolean clearRefundCooldown) {
        resetPrestigeUpgrades(playerId, clearRefundCooldown);
    }

    static boolean prestigeKeepsPoints(UUID playerId) {
        return SheepRebirthRuntime.keepsPointsAfterPrestige(playerId);
    }

    static boolean prestigeKeepsSheep(UUID playerId) {
        return SheepRebirthRuntime.keepsSheepAfterPrestige(playerId);
    }

    static BigInteger prestigeRemoveSheepAndGetSacrifice(Player player) {
        if (player == null) {
            return BigInteger.ZERO;
        }
        World world = player.getWorld();
        if (!isSheepFarmWorld(world) || !isFarmOwner(player, world)) {
            return BigInteger.ZERO;
        }
        BigInteger sacrificeGained = BigInteger.ZERO;
        for (Sheep sheep : world.getEntitiesByClass(Sheep.class)) {
            sacrificeGained = sacrificeGained.add(getSacrificeValueForSheep(sheep));
            if (sheep != null && sheep.isValid()) {
                sheep.remove();
            }
        }
        refreshLiveSheepCount(world);
        return sacrificeGained;
    }

    static void prestigeAddSacrificePoints(UUID playerId, BigInteger amount) {
        addSacrificePoints(playerId, amount);
    }

    static void prestigeRefreshTopPoints() {
        SheepLeaderboardRuntime.refreshTopPointsDisplays();
    }

    static boolean prestigeKeepsRegularUpgrades(UUID playerId) {
        return isSacrificeUnlockActive(playerId, SACRIFICE_UNLOCK_NO_REGULAR_RESETS);
    }

    static boolean prestigeKeepsShearUpgrades(UUID playerId) {
        return isSacrificeUnlockActive(playerId, SACRIFICE_UNLOCK_NO_SHEAR_RESETS);
    }

    static boolean prestigeKeepsComboUpgrades(UUID playerId) {
        return isSacrificeUnlockActive(playerId, SACRIFICE_UNLOCK_NO_COMBO_RESETS);
    }

    static void prestigeResetComboUpgrades(UUID playerId) {
        SheepComboState.resetRegularUpgrades(playerId);
    }

    static void prestigeClearMergeReminder(Player player) {
        clearMergeReminder(player);
    }

    static void prestigeClearEggRuntime(UUID playerId) {
        EGG_MODULE.clearRuntimeState(playerId);
    }

    static void prestigeClearComboRuntime(Player player) {
        clearComboRuntime(player);
    }

    public static int getPrestigeCost(Player player) {
        return toIntClamped(getPrestigeCostBig(player));
    }

    private static BigInteger getPrestigeCostBig(Player player) {
        return SheepPrestigeRuntime.getCost(player, PRESTIGE_LEVEL_BASE_COST);
    }

    private static int getAffordablePrestigeLevels(Player player) {
        return SheepPrestigeRuntime.getAffordableLevels(player, PRESTIGE_LEVEL_BASE_COST);
    }

    private static BigInteger getTotalPrestigeCostForNextLevels(int currentLevel, int levelsToBuy) {
        return SheepPrestigeRuntime.getTotalCost(currentLevel, levelsToBuy, PRESTIGE_LEVEL_BASE_COST);
    }

    private static int getPrestigePointsRewardForNextLevels(int currentLevel, int levelsToBuy) {
        return SheepPrestigeRuntime.getPointsReward(currentLevel, levelsToBuy);
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
        return SheepTutorialRuntime.shearCount(player);
    }

    public static int getTutorialSpawnCount(Player player) {
        return SheepTutorialRuntime.spawnCount(player);
    }

    public static int getTutorialMergeCount(Player player) {
        return SheepTutorialRuntime.mergeCount(player);
    }

    public static void startTutorial(Player player, boolean resetProgress) {
        SheepTutorialRuntime.start(player, resetProgress);
    }

    public static void markTutorialUpgradeOpened(Player player) {
        SheepTutorialRuntime.markUpgradeOpened(player);
    }

    public static void markTutorialQuestOpened(Player player) {
        SheepTutorialRuntime.markQuestOpened(player);
    }

    public static void markTutorialPrestigeOpened(Player player) {
        SheepTutorialRuntime.markPrestigeOpened(player);
    }

    public static void markTutorialQuestUpgradesOpened(Player player) {
        SheepTutorialRuntime.markQuestUpgradesOpened(player);
    }

    public static void markTutorialAbilityUsed(Player player) {
        SheepTutorialRuntime.markAbilityUsed(player);
    }

    public static void markTutorialShearUpgraded(Player player) {
        SheepTutorialRuntime.markShearUpgraded(player);
    }

    public static void markTutorialPrestigedOnce(Player player) {
        SheepTutorialRuntime.markPrestigedOnce(player);
    }

    public static void markTutorialRegularUpgradesIfComplete(Player player) {
        SheepTutorialRuntime.markRegularUpgradesIfComplete(player);
    }

    public static void markTutorialShearShopOpened(Player player) {
        SheepTutorialRuntime.markShearShopOpened(player);
    }

    public static void recordTutorialShear(Player player) {
        SheepTutorialRuntime.recordShear(player);
    }

    public static void recordTutorialSpawn(Player player) {
        SheepTutorialRuntime.recordSpawn(player);
    }

    public static void recordTutorialMerge(Player player) {
        SheepTutorialRuntime.recordMerge(player);
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

    public static boolean shouldRestrictTutorialActions(Player player) {
        return SheepTutorialRuntime.shouldRestrictActions(player);
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

    private static BigInteger minPositive(BigInteger current, BigInteger candidate) {
        if (candidate == null || candidate.signum() <= 0) {
            return current;
        }
        if (current == null || candidate.compareTo(current) < 0) {
            return candidate;
        }
        return current;
    }

    public static boolean blockTutorialAction(Player player, TutorialAction action, String attemptedAction) {
        SheepTutorialRuntime.Action runtimeAction = action == null
                ? null
                : SheepTutorialRuntime.Action.valueOf(action.name());
        return SheepTutorialRuntime.blockAction(player, runtimeAction, attemptedAction);
    }

    public static String getTutorialProgressLine(Player player) {
        return SheepTutorialRuntime.progressLine(player);
    }

    private static void migrateTutorialSheepToFarmWorld(UUID playerId) {
        if (playerId == null) {
            return;
        }

        World tutorialWorld = Bukkit.getWorld(getTutorialWorldName(playerId));
        if (tutorialWorld == null) {
            return;
        }
        SheepSnapshotState.migrateTutorialToFarm(playerId, tutorialWorld);
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
        SheepSnapshotState.resetPlayer(id);
        SheepEconomyState.resetAdminPlayer(id);
        SheepLeaderboardRuntime.refreshTopPointsDisplays();
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
        SheepRebirthRuntime.resetPlayer(id);
        SheepRuntimeUiState.lastPointsOverlays().remove(id);
        SheepRuntimeUiState.pointsOverlayExpirations().remove(id);
        SheepLifetimeProgressState.resetPlayer(id);
        SheepAchievementState.resetPlayer(id);
        SheepComboRuntime.removeBossBar(id);
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
        SheepEconomyRuntime.adminGivePoints(player, amount, getStartingPointsBig());
    }

    public static void adminSetPoints(Player player, long amount) {
        SheepEconomyRuntime.adminSetPoints(player, amount);
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
        return SheepPrestigeRuntime.adminSetLevel(player, targetLevel, COMBO_MAX_BASE_PRESTIGE_COST,
                getComboMaxUpgradeLevel(player), SheepTier.RAINBOW.getLevel());
    }

    public static SheepTier getSheepTier(Sheep sheep) {
        return SheepEntityRuntime.getTier(sheep);
    }

    public static void setSheepTier(Sheep sheep, SheepTier tier) {
        SheepEntityRuntime.setTier(sheep, tier);
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
        if (SheepRebirthRuntime.hasWoolRegenBoost(ownerId)) {
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
        SheepEntityRuntime.processEatTimer(sheep);
    }

    public static long getNextEatTimestamp(Sheep sheep) {
        return SheepEntityRuntime.getNextEatTimestamp(sheep);
    }

    public static void setNextEatTimestamp(Sheep sheep, long timestamp) {
        SheepEntityRuntime.setNextEatTimestamp(sheep, timestamp);
    }

    public static void recoverPlayerIfFallenFromPlatform(Player player) {
        SheepEntityRuntime.recoverPlayerIfFallen(player);
    }

    public static void updateSheepName(Sheep sheep) {
        SheepEntityRuntime.updateName(sheep);
    }

    private static BigInteger getStartingPointsBig() {
        return BigInteger.valueOf(Math.max(0L, STARTING_PLAYER_POINTS));
    }

    public static BigInteger getPlayerPointsBig(Player player) {
        return SheepEconomyRuntime.getPoints(player, getStartingPointsBig());
    }

    public static long getPlayerPoints(Player player) {
        return SheepEconomyRuntime.getPointsLong(player, getStartingPointsBig());
    }

    public static int calculateShearPoints(Player player, SheepTier tier) {
        BigInteger points = calculateShearPointsBig(player, tier, null);
        if (points.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0) {
            return Integer.MAX_VALUE;
        }
        return Math.max(1, points.intValue());
    }

    public static BigInteger calculateShearPointsBig(Player player, SheepTier tier, Sheep sheep) {
        return SheepShearingRuntime.calculatePoints(player, tier, sheep);
    }

    public static int getRainbowTier(Sheep sheep) {
        return SheepEntityRuntime.getRainbowTier(sheep);
    }

    public static void setRainbowTier(Sheep sheep, int tier) {
        SheepEntityRuntime.setRainbowTier(sheep, tier);
    }

    public static String formatRainbowTier(int tier) {
        return SheepFormatting.formatRainbowTier(tier);
    }

    public static boolean shearSheepForPlayer(Player player, Sheep sheep) {
        return SheepShearingRuntime.shearForPlayer(player, sheep);
    }

    static int entityGetAchievementPointMultiplier(Player player) {
        return getAchievementPointMultiplier(player);
    }

    static boolean entityIsJackpotShearsActive(Player player) {
        return isAbilityActive(SheepQuestState.activeJackpotShearsUntil(),
                player == null ? null : player.getUniqueId());
    }

    static int entityRandomNextInt(int bound) {
        return RANDOM.nextInt(bound);
    }

    static boolean entityIsAutoShearActive(Player player) {
        UUID playerId = player == null ? null : player.getUniqueId();
        return isCountAbilityActive(SheepQuestState.activeAutoShearUses(), SheepQuestState.autoShearEnabled(),
                playerId);
    }

    static boolean entityQueueShearAllEligibleSheep(Player player, Sheep sheep) {
        return queueShearAllEligibleSheepForPlayer(player, sheep);
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

                    if (SheepShearingRuntime.shearSingle(onlinePlayer, candidate)) {
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

    static void comboStopQueuedShearAllTask(UUID playerId) {
        stopQueuedShearAllTask(playerId);
    }

    private static void stopAllQueuedShearAllTasks() {
        for (UUID playerId : SheepEntityRuntimeState.shearAllTaskPlayerIds()) {
            stopQueuedShearAllTask(playerId);
        }
    }

    public static int getWoolDropAmount(Player player) {
        return 1 + getShearShopLevel(player);
    }

    public static boolean tryTriggerShearWoolSave(Player player, Sheep sheep) {
        return SheepShearingRuntime.tryWoolSave(player, sheep);
    }

    public static boolean tryTriggerShearTierBoost(Player player, Sheep sheep) {
        return SheepShearingRuntime.tryTierBoost(player, sheep);
    }

    static void entityPlaySound(Player player, Sound sound, float volume, float pitch) {
        playSound(player, sound, volume, pitch);
    }

    static void entityPlayTierBoostProcSound(Player player) {
        playTierBoostProcSound(player);
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
        return SheepLeaderboardRuntime.buildTopPointsText(maxEntries);
    }

    public static List<String> getTopPointsLines(int maxEntries) {
        return SheepLeaderboardRuntime.getTopPointsLines(maxEntries);
    }

    public static int getTopPointsPageCount(int pageSize) {
        return SheepLeaderboardRuntime.getTopPointsPageCount(pageSize);
    }

    public static List<String> getTopPointsLines(int pageSize, int pageNumber) {
        return SheepLeaderboardRuntime.getTopPointsLines(pageSize, pageNumber);
    }

    public static boolean spawnOrMoveTopPointsDisplay(Player player) {
        return SheepLeaderboardRuntime.spawnOrMoveTopPointsDisplay(player);
    }

    public static boolean spawnOrMoveTopPointsDisplay(Location location) {
        return SheepLeaderboardRuntime.spawnOrMoveTopPointsDisplay(location);
    }

    public static boolean removeTopPointsDisplay() {
        return SheepLeaderboardRuntime.removeTopPointsDisplay();
    }

    public static void restoreTopPointsDisplayIfPossible() {
        SheepLeaderboardRuntime.restoreTopPointsDisplayIfPossible();
    }

    public static void restoreTopPointsDisplayAfterRestart(World loadedWorld) {
        SheepLeaderboardRuntime.restoreTopPointsDisplayAfterRestart(loadedWorld);
    }

    public static void reconcileTopPointsDisplayForChunk(World world, int chunkX, int chunkZ) {
        SheepLeaderboardRuntime.reconcileTopPointsDisplayForChunk(world, chunkX, chunkZ);
    }

    public static void reconcileTopPointsDisplayForLocation(Location location) {
        SheepLeaderboardRuntime.reconcileTopPointsDisplayForLocation(location);
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
        SheepRebirthRuntime.clear();
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
        SheepSnapshotState.clear();
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
        SheepEconomyRuntime.addPoints(player, points, getStartingPointsBig(),
                getPointsGainMultiplierFromRebirthSkills(player));
    }

    static void economyAfterPointsAdded(Player player, BigInteger boosted) {
        SheepLeaderboardRuntime.refreshTopPointsDisplays();
        queuePointsGainOverlay(player, boosted);
        saveData();
        tickPrestigeReminder(player);
    }

    static void economyAfterPointsChanged() {
        SheepLeaderboardRuntime.refreshTopPointsDisplays();
        saveData();
    }

    static void economySaveData() {
        saveData();
    }

    static BigInteger economyStartingPoints() {
        return getStartingPointsBig();
    }

    static int economyBaseSheepLimit() {
        return BASE_SHEEP_LIMIT;
    }

    static int economyMaxSheepLimit(UUID playerId) {
        return getMaxSheepLimit(playerId);
    }

    static int economyPrestigeHigherMaxLevel(UUID playerId) {
        return getPrestigeHigherMaxLevel(playerId);
    }

    static boolean economyCanSpendUpgradePointsDuringTutorial(Player player, BigInteger points) {
        return canSpendUpgradePointsDuringTutorial(player, points);
    }

    static void economyResetEggTimer(Player player) {
        resetEggTimer(player);
    }

    static void economyApplyWoolRegenReduction(Player player, int oldLevel, int newLevel) {
        applyWoolRegenReductionToActiveCooldowns(player, oldLevel, newLevel);
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
        SheepComboRuntime.clearRuntime(player);
    }

    public static void clearPrestigeReminder(Player player) {
        if (player == null) {
            return;
        }
        SheepPrestigeState.clearReminder(player.getUniqueId());
    }

    public static void clearRebirthReminder(Player player) {
        SheepRebirthRuntime.clearReminder(player);
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
        SheepRebirthRuntime.tickReminder(player, player != null && isSheepFarmWorld(player.getWorld()),
                getPrestigeLevel(player));
    }

    static String rebirthColor(String message) {
        return color(message);
    }

    static String rebirthHint(String message) {
        return hint(message);
    }

    public static void recordSheepMerge(Player player, SheepTier mergedFromTier, int woolReadySourceSheep) {
        SheepComboRuntime.recordSheepMerge(player, mergedFromTier, woolReadySourceSheep);
    }

    public static void tickCombo(Player player) {
        SheepComboRuntime.tick(player);
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
        SheepInventoryRuntime.enforceFarmLoadout(player);
    }

    public static void applyFarmSaturation(Player player) {
        SheepInventoryRuntime.applyFarmSaturation(player);
    }

    public static ItemStack getSheepMergeUpgradeCommandItem() {
        return SheepInventoryRuntime.getSheepMergeUpgradeCommandItem();
    }

    public static ItemStack getSheepMergeEggItem() {
        return SheepInventoryRuntime.getSheepMergeEggItem();
    }

    public static boolean isSheepMergeUpgradeCommandItem(ItemStack itemStack) {
        return SheepInventoryRuntime.isSheepMergeUpgradeCommandItem(itemStack);
    }

    public static boolean isSheepMergeEggItem(ItemStack itemStack) {
        return SheepInventoryRuntime.isSheepMergeEggItem(itemStack);
    }

    public static boolean isForcedFarmLoadoutItem(ItemStack itemStack) {
        return SheepInventoryRuntime.isForcedFarmLoadoutItem(itemStack);
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

    static void randomEventPlaySound(Player player, Sound sound, float volume, float pitch) {
        playSound(player, sound, volume, pitch);
    }

    static int randomEventFarmMinXz() {
        return FARM_MIN_XZ;
    }

    static int randomEventFarmMaxXz() {
        return FARM_MAX_XZ;
    }

    static int randomEventFarmBaseY() {
        return FARM_BASE_Y;
    }

    private static void playSheepSound(Player player, Sound sound, float volume, float pitch) {
        if (player == null || sound == null || !areSoundEffectsEnabled(player) || !areSheepSoundsEnabled(player)) {
            return;
        }
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    private static void playSheepSound(World world, Location location, Sound sound, float volume, float pitch) {
        if (world == null || location == null || sound == null) {
            return;
        }
        for (Player player : world.getPlayers()) {
            playSheepSound(player, sound, volume, pitch);
        }
    }

    static void entityPlaySheepSound(World world, Location location, Sound sound, float volume, float pitch) {
        playSheepSound(world, location, sound, volume, pitch);
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

    static void entitySpawnParticle(World world, org.bukkit.Particle particle, Location location, int count,
            double offsetX, double offsetY, double offsetZ, double extra) {
        spawnParticle(world, particle, location, count, offsetX, offsetY, offsetZ, extra);
    }

    public static boolean trySpendPoints(Player player, long points) {
        if (points <= 0L) {
            return false;
        }
        return trySpendPoints(player, BigInteger.valueOf(points));
    }

    public static boolean trySpendPoints(Player player, BigInteger points) {
        return SheepEconomyRuntime.trySpendPoints(player, points, getStartingPointsBig());
    }

    public static int getPlayerLimit(Player player) {
        return SheepEconomyRuntime.getPlayerLimit(player, BASE_SHEEP_LIMIT);
    }

    public static int getOwnerLimit(World world) {
        UUID ownerId = getOwnerId(world);
        return SheepEconomyRuntime.getOwnerLimit(ownerId, BASE_SHEEP_LIMIT);
    }

    public static BigInteger getUpgradeCost(Player player) {
        return SheepEconomyRuntime.getLimitUpgradeCost(player, LIMIT_UPGRADE_COST);
    }

    public static int getLimitUpgradeStep() {
        return LIMIT_UPGRADE_STEP;
    }

    public static boolean upgradeLimit(Player player) {
        return SheepEconomyRuntime.upgradeLimit(player, LIMIT_UPGRADE_COST);
    }

    public static int getLimitUpgradeLevel(Player player) {
        return SheepEconomyRuntime.getLimitUpgradeLevel(player);
    }

    public static int getEggSpeedLevel(Player player) {
        return SheepEconomyRuntime.getEggSpeedLevel(player);
    }

    public static int getEggIntervalSeconds(Player player) {
        int minEggInterval = hasSacrificeUnlock(player, SACRIFICE_UNLOCK_EGG_COOLDOWN_TO_1S)
                ? MIN_EGG_INTERVAL_SECONDS_WITH_SACRIFICE
                : MIN_EGG_INTERVAL_SECONDS;
        return SheepEconomyRuntime.getEggIntervalSeconds(player, BASE_EGG_INTERVAL_SECONDS, minEggInterval);
    }

    public static int getEggSpeedMaxLevel(Player player) {
        return player == null ? 0 : getEggSpeedMaxLevel(player.getUniqueId());
    }

    private static int getEggSpeedMaxLevel(UUID playerId) {
        int hardCap = hasSacrificeUnlock(playerId, SACRIFICE_UNLOCK_EGG_COOLDOWN_TO_1S)
                ? EGG_SPEED_MAX_LEVEL + 1
                : EGG_SPEED_MAX_LEVEL;
        return SheepEconomyRuntime.getEggSpeedMaxLevel(playerId, EGG_SPEED_BASE_MAX_LEVEL,
                PRESTIGE_CAP_BONUS_PER_LEVEL, hardCap);
    }

    public static int getWoolRegenMaxLevel(Player player) {
        return player == null ? 0 : getWoolRegenMaxLevel(player.getUniqueId());
    }

    private static int getWoolRegenMaxLevel(UUID playerId) {
        return SheepEconomyRuntime.getWoolRegenMaxLevel(playerId, WOOL_REGEN_BASE_MAX_LEVEL,
                PRESTIGE_CAP_BONUS_PER_LEVEL);
    }

    public static int getHigherTierChanceMaxLevel(Player player) {
        return player == null ? 0 : getHigherTierChanceMaxLevel(player.getUniqueId());
    }

    private static int getHigherTierChanceMaxLevel(UUID playerId) {
        return SheepEconomyRuntime.getHigherTierChanceMaxLevel(playerId, HIGHER_TIER_CHANCE_BASE_MAX_LEVEL,
                PRESTIGE_CAP_BONUS_PER_LEVEL, HIGHER_TIER_CHANCE_MAX_LEVEL, HIGHER_TIER_CHANCE_HARD_MAX_LEVEL);
    }

    public static int getWoolRegenLevel(Player player) {
        return SheepEconomyRuntime.getWoolRegenLevel(player);
    }

    public static int getHigherTierChanceLevel(Player player) {
        return SheepEconomyRuntime.getHigherTierChanceLevel(player);
    }

    public static int getHigherTierChancePercent(Player player) {
        UUID playerId = player == null ? null : player.getUniqueId();
        int abilityBonus = 0;
        if (isCountAbilityActive(SheepQuestState.activeLuckyBurstUses(), SheepQuestState.luckyBurstEnabled(),
                playerId)) {
            abilityBonus = SheepQuestRuntime.getLuckyBurstBonusPercent();
        }
        return SheepEconomyRuntime.getHigherTierChancePercent(playerId, abilityBonus,
                HIGHER_TIER_CHANCE_BASE_CAP_PERCENT);
    }

    public static int getHigherTierChancePercent(World world) {
        UUID ownerId = getOwnerId(world);
        int abilityBonus = 0;
        if (isCountAbilityActive(SheepQuestState.activeLuckyBurstUses(), SheepQuestState.luckyBurstEnabled(),
                ownerId)) {
            abilityBonus = SheepQuestRuntime.getLuckyBurstBonusPercent();
        }
        return SheepEconomyRuntime.getHigherTierChancePercent(ownerId, abilityBonus,
                HIGHER_TIER_CHANCE_BASE_CAP_PERCENT);
    }

    public static SheepTier rollSpawnTier(World world) {
        return SheepEntityRuntime.rollSpawnTier(world);
    }

    public static void upgradeSheepBelowMinimumSpawnTier(World world) {
        SheepEntityRuntime.upgradeBelowMinimumSpawnTier(world);
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

    static boolean entityTryConsumeEgg(Player player) {
        return tryConsumeEgg(player);
    }

    public static boolean spawnSheepFromEgg(Player player, Location spawnLocation) {
        return SheepEntityRuntime.spawnFromEgg(player, spawnLocation);
    }

    static void entityAfterOwnedSheepSpawn(Player player) {
        UUID playerId = player.getUniqueId();
        SheepAchievementRuntime.recordSpawn(player);
        if (isCountAbilityActive(SheepQuestState.activeLuckyBurstUses(), SheepQuestState.luckyBurstEnabled(),
                playerId)) {
            consumeCountAbilityUse(SheepQuestState.activeLuckyBurstUses(), playerId);
            saveData();
        }
    }

    private static boolean spawnAutomationSheepFromSky(Player player) {
        if (player == null || player.getWorld() == null) {
            return false;
        }

        Sheep sheep = spawnOwnedSheep(player,
                SheepRandomEventRuntime.createSkySheepSpawnLocation(player.getWorld()));
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
        SheepAchievementRuntime.recordSpawn(player);
        if (isCountAbilityActive(SheepQuestState.activeLuckyBurstUses(), SheepQuestState.luckyBurstEnabled(),
                playerId)) {
            consumeCountAbilityUse(SheepQuestState.activeLuckyBurstUses(), playerId);
            saveData();
        }
        return sheep;
    }

    public static int getPrestigeDoublePointsChanceLevel(Player player) {
        return SheepPrestigeRuntime.getDoublePointsLevel(player);
    }

    public static int getDoublePointsChancePercent(Player player) {
        return SheepPrestigeRuntime.getDoublePointsChancePercent(player);
    }

    public static int getPrestigeHigherMaxLevel(Player player) {
        return SheepPrestigeRuntime.getHigherMaxLevel(player);
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
        return SheepPrestigeRuntime.getStartEggsLevel(player);
    }

    public static int getPrestigeQuestRewardLevel(Player player) {
        return SheepPrestigeRuntime.getQuestRewardLevel(player);
    }

    public static int getPrestigeEggCapLevel(Player player) {
        return SheepPrestigeRuntime.getEggCapLevel(player);
    }

    public static int getBaseSpawnTierLevel(Player player) {
        return SheepPrestigeRuntime.getBaseSpawnTierLevel(player, SheepTier.RAINBOW.getLevel());
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
        return SheepPrestigeRuntime.getDoublePointsCost(player);
    }

    public static int getPrestigeHigherMaxLevelCost(Player player) {
        return SheepPrestigeRuntime.getHigherMaxCost(player);
    }

    public static int getPrestigeStartEggsCost(Player player) {
        return SheepPrestigeRuntime.getStartEggsCost(player);
    }

    public static int getPrestigeEggCapCost(Player player) {
        return SheepPrestigeRuntime.getEggCapCost(player);
    }

    public static int getPrestigeBaseSpawnTierCost(Player player) {
        return SheepPrestigeRuntime.getBaseSpawnTierCost(player, SheepTier.RAINBOW.getLevel());
    }

    public static int getPrestigeQuestRewardCost(Player player) {
        return SheepPrestigeRuntime.getQuestRewardCost(player);
    }

    private static double getPrestigeQuestRewardMultiplier(Player player) {
        return 1.0D + getPrestigeQuestRewardLevel(player) * PRESTIGE_QUEST_REWARD_BONUS_PER_LEVEL;
    }

    public static int getQuestUpgradeDurationLevel(Player player) {
        return SheepQuestRuntime.getUpgradeDurationLevel(player);
    }

    public static int getQuestUpgradePowerLevel(Player player) {
        return SheepQuestRuntime.getUpgradePowerLevel(player);
    }

    public static int getQuestUpgradeDurationCost(Player player) {
        return SheepQuestRuntime.getUpgradeDurationCost(player);
    }

    public static int getQuestUpgradePowerCost(Player player) {
        return SheepQuestRuntime.getUpgradePowerCost(player);
    }

    private static int getQuestLuckyBurstCost(Player player) {
        return SheepQuestRuntime.getLuckyBurstCost(player);
    }

    private static int getQuestWoolRushCost(Player player) {
        return SheepQuestRuntime.getWoolRushCost(player);
    }

    private static int getQuestJackpotCost(Player player) {
        return SheepQuestRuntime.getJackpotCost(player);
    }

    private static int getQuestAutoMergeCost(Player player) {
        return SheepQuestRuntime.getAutoMergeCost(player);
    }

    private static int getQuestAutoShearCost(Player player) {
        return SheepQuestRuntime.getAutoShearCost(player);
    }

    private static boolean isAbilityActive(Map<UUID, Long> activeUntil, UUID playerId) {
        return SheepQuestRuntime.isAbilityActive(activeUntil, playerId);
    }

    private static boolean isCountAbilityActive(Map<UUID, Integer> remainingUsesByPlayer,
            Map<UUID, Boolean> enabledByPlayer,
            UUID playerId) {
        return SheepQuestRuntime.isCountAbilityActive(remainingUsesByPlayer, enabledByPlayer, playerId);
    }

    private static int getCountAbilityRemainingUses(Map<UUID, Integer> remainingUsesByPlayer, UUID playerId) {
        return SheepQuestRuntime.getCountAbilityRemainingUses(remainingUsesByPlayer, playerId);
    }

    private static void consumeCountAbilityUse(Map<UUID, Integer> remainingUsesByPlayer, UUID playerId) {
        SheepQuestRuntime.consumeCountAbilityUse(remainingUsesByPlayer, playerId);
    }

    private static boolean upgradeQuestDuration(Player player) {
        return SheepQuestRuntime.upgradeDuration(player);
    }

    private static boolean upgradeQuestPower(Player player) {
        return SheepQuestRuntime.upgradePower(player);
    }

    public static long getPrestigeRefundRemainingMs(Player player) {
        return SheepPrestigeRuntime.refundRemainingMs(player);
    }

    private static int getPrestigeUpgradeCost(int baseCost, int level) {
        return SheepPrestigeRuntime.upgradeCost(baseCost, level);
    }

    private static int addSaturated(int current, int delta) {
        long total = (long) current + Math.max(0L, delta);
        return total >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;
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

    private static void resetPrestigeUpgrades(UUID playerId, boolean clearRefundCooldown) {
        if (playerId == null) {
            return;
        }
        SheepPrestigeState.resetUpgrades(playerId, clearRefundCooldown);
        SheepComboRuntime.resetMaxUpgrade(playerId);
        clampUpgradeLevelsToCurrentCaps(playerId);
    }

    private static int getPrestigeRefundAmount(Player player) {
        return SheepPrestigeRuntime.getRefundAmount(player, COMBO_MAX_BASE_PRESTIGE_COST,
                getComboMaxUpgradeLevel(player), SheepTier.RAINBOW.getLevel());
    }

    private static boolean tryRefundPrestigePoints(Player player) {
        return SheepPrestigeRuntime.tryRefund(player, COMBO_MAX_BASE_PRESTIGE_COST,
                getComboMaxUpgradeLevel(player), SheepTier.RAINBOW.getLevel());
    }

    private static String formatDuration(long durationMs) {
        return SheepFormatting.formatDuration(durationMs);
    }

    private static String getAbilityMenuStatus(Map<UUID, Long> activeUntil, Map<UUID, Long> pausedRemainingMsByPlayer,
            UUID playerId) {
        return SheepQuestRuntime.getAbilityMenuStatus(activeUntil, playerId);
    }

    private static String getCountAbilityMenuStatus(Map<UUID, Integer> remainingUsesByPlayer,
            Map<UUID, Boolean> enabledByPlayer,
            UUID playerId) {
        return SheepQuestRuntime.getCountAbilityMenuStatus(remainingUsesByPlayer, enabledByPlayer, playerId);
    }

    private static String getCountAbilityToggleActionLine(Map<UUID, Integer> remainingUsesByPlayer,
            Map<UUID, Boolean> enabledByPlayer,
            UUID playerId) {
        return SheepQuestRuntime.getCountAbilityToggleActionLine(remainingUsesByPlayer, enabledByPlayer, playerId);
    }

    private static long getQuestResetRemainingMs(Player player) {
        return SheepQuestRuntime.getResetRemainingMs(player);
    }

    private static boolean trySpendPrestigePoints(Player player, int points) {
        return SheepPrestigeRuntime.trySpendPoints(player, points);
    }

    private static boolean canSpendUpgradePointsDuringTutorial(Player player, BigInteger spendPoints) {
        return SheepTutorialRuntime.canSpendUpgradePoints(player, spendPoints);
    }

    private static boolean upgradePrestigeDoublePoints(Player player) {
        return SheepPrestigeRuntime.upgradeDoublePoints(player);
    }

    private static boolean upgradePrestigeHigherMaxLevel(Player player) {
        return SheepPrestigeRuntime.upgradeHigherMax(player);
    }

    private static boolean upgradePrestigeStartEggs(Player player) {
        return SheepPrestigeRuntime.upgradeStartEggs(player);
    }

    private static boolean upgradePrestigeEggCap(Player player) {
        return SheepPrestigeRuntime.upgradeEggCap(player);
    }

    private static boolean upgradePrestigeBaseSpawnTier(Player player) {
        return SheepPrestigeRuntime.upgradeBaseSpawnTier(player, SheepTier.RAINBOW.getLevel());
    }

    private static boolean upgradePrestigeQuestReward(Player player) {
        return SheepPrestigeRuntime.upgradeQuestReward(player);
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
        SheepProgressionMenus.openUpgradeMenu(player);
    }

    public static boolean isUpgradeMenuTitle(String title) {
        return SheepProgressionMenus.isUpgradeMenuTitle(title);
    }

    public static boolean isPrestigeMenuTitle(String title) {
        return SheepProgressionMenus.isPrestigeMenuTitle(title);
    }

    public static boolean isQuestMenuTitle(String title) {
        return SheepProgressionMenus.isQuestMenuTitle(title);
    }

    public static boolean isQuestUpgradesMenuTitle(String title) {
        return SheepProgressionMenus.isQuestUpgradesMenuTitle(title);
    }

    public static boolean isShopMenuTitle(String title) {
        return SheepProgressionMenus.isShopMenuTitle(title);
    }

    public static boolean isComboShopMenuTitle(String title) {
        return SheepProgressionMenus.isComboShopMenuTitle(title);
    }

    public static boolean isAutomationMenuTitle(String title) {
        return SheepProgressionMenus.isAutomationMenuTitle(title);
    }

    public static boolean isAchievementsMenuTitle(String title) {
        return SheepAchievementMenus.isAchievementsMenuTitle(title);
    }

    public static boolean isAchievementsViewMenuTitle(String title) {
        return SheepAchievementMenus.isAchievementsViewMenuTitle(title);
    }

    public static boolean isAchievementsUpgradesMenuTitle(String title) {
        return SheepAchievementMenus.isAchievementsUpgradesMenuTitle(title);
    }

    public static boolean isSacrificeMenuTitle(String title) {
        return SheepSacrificeMenus.isSacrificeMenuTitle(title);
    }

    public static boolean isRebirthMenuTitle(String title) {
        return SheepRebirthMenus.isRebirthMenuTitle(title);
    }

    public static boolean isRebirthTreeMenuTitle(String title) {
        return SheepRebirthMenus.isRebirthTreeMenuTitle(title);
    }

    public static boolean isScoreboardMenuTitle(String title) {
        return SheepSettingsMenus.isScoreboardMenuTitle(title);
    }

    public static boolean isSocialsMenuTitle(String title) {
        return SheepSocialMenus.isSocialsMenuTitle(title);
    }

    public static void tickOpenMenuStatRefresh(Player player) {
        SheepProgressionMenus.tickOpenMenuStatRefresh(player);
    }

    static void refreshOpenSocialsMenuItems(Player player, Inventory inventory) {
        SheepSocialMenus.refreshOpenSocialsMenuItems(player, inventory);
    }

    private static int getScoreboardLayoutMode(Player player) {
        if (player == null) {
            return 0;
        }
        return SheepUiPreferences.getScoreboardLayoutMode(player.getUniqueId());
    }

    public static void openScoreboardMenu(Player player) {
        SheepSettingsMenus.openScoreboardMenu(player);
    }

    public static void handleScoreboardMenuClick(Player player, int slot) {
        SheepSettingsMenus.handleScoreboardMenuClick(player, slot);
    }

    public static boolean isUniversalLayoutMenuTitle(String title) {
        return SheepSettingsMenus.isUniversalLayoutMenuTitle(title);
    }

    public static boolean isScoreboardLayoutMenuTitle(String title) {
        return SheepSettingsMenus.isScoreboardLayoutMenuTitle(title);
    }

    public static boolean isInventoryLayoutMenuTitle(String title) {
        return SheepSettingsMenus.isInventoryLayoutMenuTitle(title);
    }

    public static boolean isSoundEffectsMenuTitle(String title) {
        return SheepSettingsMenus.isSoundEffectsMenuTitle(title);
    }

    public static boolean isParticleEffectsMenuTitle(String title) {
        return SheepSettingsMenus.isParticleEffectsMenuTitle(title);
    }

    public static boolean isVisitAccessMenuTitle(String title) {
        return SheepSettingsMenus.isVisitAccessMenuTitle(title);
    }

    public static void openUniversalLayoutMenu(Player player) {
        SheepSettingsMenus.openUniversalLayoutMenu(player);
    }

    public static void handleUniversalLayoutMenuClick(Player player, int slot) {
        SheepSettingsMenus.handleUniversalLayoutMenuClick(player, slot);
    }

    public static void openSoundEffectsMenu(Player player) {
        SheepSettingsMenus.openSoundEffectsMenu(player);
    }

    public static void handleSoundEffectsMenuClick(Player player, int slot) {
        SheepSettingsMenus.handleSoundEffectsMenuClick(player, slot);
    }

    public static void openParticleEffectsMenu(Player player) {
        SheepSettingsMenus.openParticleEffectsMenu(player);
    }

    public static void handleParticleEffectsMenuClick(Player player, int slot) {
        SheepSettingsMenus.handleParticleEffectsMenuClick(player, slot);
    }

    public static void openVisitAccessMenu(Player player) {
        SheepSettingsMenus.openVisitAccessMenu(player);
    }

    public static void handleVisitAccessMenuClick(Player player, int slot, ItemStack clickedItem) {
        SheepSettingsMenus.handleVisitAccessMenuClick(player, slot, clickedItem);
    }

    public static void openScoreboardLayoutMenu(Player player) {
        SheepSettingsMenus.openScoreboardLayoutMenu(player);
    }

    public static void handleScoreboardLayoutMenuClick(Player player, int slot) {
        SheepSettingsMenus.handleScoreboardLayoutMenuClick(player, slot);
    }

    public static void openInventoryLayoutMenu(Player player) {
        SheepSettingsMenus.openInventoryLayoutMenu(player);
    }

    public static void handleInventoryLayoutMenuClick(Player player, int slot, ItemStack clickedItem) {
        SheepSettingsMenus.handleInventoryLayoutMenuClick(player, slot, clickedItem);
    }

    public static void openAchievementsMenu(Player player) {
        SheepAchievementMenus.openAchievementsMenu(player);
    }

    public static void openAchievementsViewMenu(Player player) {
        SheepAchievementMenus.openAchievementsViewMenu(player);
    }

    public static void openAchievementsUpgradesMenu(Player player) {
        SheepAchievementMenus.openAchievementsUpgradesMenu(player);
    }

    public static void handleAchievementsMenuClick(Player player, int slot) {
        SheepAchievementMenus.handleAchievementsMenuClick(player, slot);
    }

    public static void handleAchievementsViewMenuClick(Player player, int slot) {
        SheepAchievementMenus.handleAchievementsViewMenuClick(player, slot);
    }

    public static void handleAchievementsUpgradesMenuClick(Player player, int slot) {
        SheepAchievementMenus.handleAchievementsUpgradesMenuClick(player, slot);
    }

    static void achievementMenuEvaluateProgress(Player player) {
        SheepAchievementRuntime.evaluate(player, true);
    }

    static List<AchievementMenuEntry> achievementMenuEntries() {
        return SheepAchievementRuntime.definitions().stream()
                .map(definition -> new AchievementMenuEntry(definition.getId(), definition.getMaterial(),
                        definition.getName(), definition.getObjective(), definition.getReward(),
                        definition.getAchievementPoints()))
                .toList();
    }

    static List<AchievementMilestoneMenuEntry> achievementMilestoneMenuEntries() {
        return SheepAchievementRuntime.milestones().stream()
                .map(definition -> new AchievementMilestoneMenuEntry(definition.getId(),
                        definition.getRequiredPoints(), definition.getMaterial(), definition.getName(),
                        definition.getReward()))
                .toList();
    }

    static Set<String> achievementMenuUnlockedIds(Player player) {
        return player == null ? Set.of() : getUnlockedAchievementIds(player.getUniqueId());
    }

    static Set<String> achievementMenuUnlockedMilestoneIds(Player player) {
        return player == null ? Set.of() : getUnlockedAchievementMilestoneIds(player.getUniqueId());
    }

    static int achievementMenuNextMilestoneTarget(int achievementPoints) {
        return SheepAchievementRuntime.nextMilestoneTarget(achievementPoints);
    }

    static void achievementSaveData() {
        saveData();
    }

    static void achievementApplyWoolRegenBonus(Player player, double oldMultiplier, double newMultiplier) {
        applyAchievementWoolRegenBonusToActiveCooldowns(player, oldMultiplier, newMultiplier);
    }

    static int achievementUnlockedAutomationCount(Player player) {
        return getUnlockedAutomationCount(player);
    }

    static int achievementScoreboardLayoutMode(Player player) {
        return getScoreboardLayoutMode(player);
    }

    static int achievementQuickAccessCount(UUID playerId) {
        return getInventoryQuickAccessActions(playerId).size();
    }

    static int achievementQuickAccessMaxItems() {
        return INVENTORY_QUICK_ACCESS_MAX_ITEMS;
    }

    static boolean achievementQuickAccessCastingEnabled(UUID playerId) {
        return isInventoryQuickAccessCastingEnabled(playerId);
    }

    static boolean achievementHasOpenedSocials(UUID playerId) {
        return SheepRuntimeUiState.socialsPages().containsKey(playerId);
    }

    static int achievementMaxSheepLimit(UUID playerId) {
        return getMaxSheepLimit(playerId);
    }

    static void tutorialSaveData() {
        saveData();
    }

    static BigInteger tutorialPlayerPoints(Player player) {
        return getPlayerPointsBig(player);
    }

    static BigInteger tutorialPrestigeCost(Player player) {
        return getPrestigeCostBig(player);
    }

    static BigInteger tutorialMinimumRegularUpgradeCost(Player player) {
        return getMinimumRegularUpgradeCost(player);
    }

    static BigInteger tutorialMinimumShearUpgradeCost(Player player) {
        return getMinimumShearUpgradeCost(player);
    }

    static String tutorialColor(String message) {
        return color(message);
    }

    static void tutorialCompleteWorldTransition(Player player) {
        if (player == null) {
            return;
        }
        UUID playerId = player.getUniqueId();
        migrateTutorialSheepToFarmWorld(playerId);
        String worldName = SheepFarmWorldCommand.getWorldName(playerId);
        SheepFarmWorldCommand.ensureFarmWorldAsync(worldName, world -> {
            if (!player.isOnline()) {
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
    }

    public static void openSocialsMenu(Player player) {
        SheepSocialMenus.openSocialsMenu(player);
    }

    private static String getAuthorCredentialsText(OfflinePlayer author) {
        String minecraftUsername = author == null ? null : author.getName();
        if (minecraftUsername == null || minecraftUsername.isBlank()) {
            minecraftUsername = "unknown";
        }
        return "0x208u16 (" + minecraftUsername + ")";
    }

    public static void handleSocialsMenuClick(Player player, int slot, ItemStack clickedItem) {
        SheepSocialMenus.handleSocialsMenuClick(player, slot, clickedItem);
    }

    public static void handleUpgradeMenuClick(Player player, int slot) {
        SheepProgressionMenus.handleUpgradeMenuClick(player, slot);
    }

    static boolean progressionBlockRegularUpgradePurchase(Player player) {
        return SheepTutorialRuntime.blockRegularUpgradePurchase(player);
    }

    static int progressionMaxSheepLimit(Player player) {
        return getMaxSheepLimit(player.getUniqueId());
    }

    static boolean progressionUpgradeLimit(Player player) {
        return upgradeLimit(player);
    }

    static boolean progressionUpgradeEggSpeed(Player player) {
        return upgradeEggSpeed(player);
    }

    static boolean progressionUpgradeWoolRegen(Player player) {
        return upgradeWoolRegen(player);
    }

    static boolean progressionUpgradeHigherTierChance(Player player) {
        return upgradeHigherTierChance(player);
    }

    static void progressionMarkTutorialRegularUpgradesIfComplete(Player player) {
        markTutorialRegularUpgradesIfComplete(player);
    }

    public static void openPrestigeMenu(Player player) {
        SheepProgressionMenus.openPrestigeMenu(player);
    }

    public static void handlePrestigeMenuClick(Player player, int slot) {
        SheepProgressionMenus.handlePrestigeMenuClick(player, slot);
    }

    static boolean progressionBlockPrestigePurchase(Player player) {
        return SheepTutorialRuntime.blockPrestigePurchase(player);
    }

    static void progressionMarkTutorialPrestigeOpened(Player player) {
        markTutorialPrestigeOpened(player);
    }

    static int progressionAffordablePrestigeLevels(Player player) {
        return getAffordablePrestigeLevels(player);
    }

    static BigInteger progressionPrestigeCost(Player player) {
        return getPrestigeCostBig(player);
    }

    static BigInteger progressionTotalPrestigeCost(Player player, int levels) {
        return getTotalPrestigeCostForNextLevels(getPrestigeLevel(player), levels);
    }

    static int progressionPrestigeRewardForLevels(Player player, int levels) {
        return getPrestigePointsRewardForNextLevels(getPrestigeLevel(player), levels);
    }

    static int progressionPrestigeDoublePointsMaxLevel() {
        return PRESTIGE_DOUBLE_POINTS_MAX_LEVEL;
    }

    static String progressionPrestigeNextCaps(Player player) {
        int nextLevel = getPrestigeHigherMaxLevel(player) + 1;
        int egg = Math.min(EGG_SPEED_MAX_LEVEL, EGG_SPEED_BASE_MAX_LEVEL + nextLevel * PRESTIGE_CAP_BONUS_PER_LEVEL);
        int wool = WOOL_REGEN_BASE_MAX_LEVEL + nextLevel * PRESTIGE_CAP_BONUS_PER_LEVEL;
        int chance = Math.min(HIGHER_TIER_CHANCE_HARD_MAX_LEVEL,
                Math.min(HIGHER_TIER_CHANCE_MAX_LEVEL,
                        HIGHER_TIER_CHANCE_BASE_MAX_LEVEL + nextLevel * PRESTIGE_CAP_BONUS_PER_LEVEL));
        return "Egg " + egg + ", Wool " + wool + ", Chance " + chance;
    }

    static String progressionPrestigeBaseCaps() {
        return "Egg " + EGG_SPEED_BASE_MAX_LEVEL + ", Wool " + WOOL_REGEN_BASE_MAX_LEVEL + ", Chance "
                + HIGHER_TIER_CHANCE_BASE_MAX_LEVEL;
    }

    static int progressionPrestigeEggCapStep() {
        return PRESTIGE_EGG_CAP_STEP;
    }

    static int progressionPrestige(Player player) {
        return prestige(player);
    }

    static void progressionPlayPrestigeSound(Player player) {
        playPrestigeSound(player);
    }

    static boolean progressionPrestigeDoublePointsMaxed(Player player) {
        return getPrestigeDoublePointsChanceLevel(player) >= PRESTIGE_DOUBLE_POINTS_MAX_LEVEL;
    }

    static boolean progressionUpgradePrestigeDoublePoints(Player player) {
        return upgradePrestigeDoublePoints(player);
    }

    static boolean progressionUpgradePrestigeHigherMaxLevel(Player player) {
        return upgradePrestigeHigherMaxLevel(player);
    }

    static boolean progressionUpgradePrestigeStartEggs(Player player) {
        return upgradePrestigeStartEggs(player);
    }

    static boolean progressionUpgradePrestigeEggCap(Player player) {
        return upgradePrestigeEggCap(player);
    }

    static boolean progressionUpgradePrestigeBaseSpawnTier(Player player) {
        return upgradePrestigeBaseSpawnTier(player);
    }

    static boolean progressionUpgradePrestigeQuestReward(Player player) {
        return upgradePrestigeQuestReward(player);
    }

    static int progressionPrestigeQuestRewardPercent(Player player) {
        return (int) Math.round(getPrestigeQuestRewardLevel(player) * PRESTIGE_QUEST_REWARD_BONUS_PER_LEVEL * 100);
    }

    static String progressionFormatDuration(long durationMs) {
        return formatDuration(durationMs);
    }

    static int progressionPrestigeRefundAmount(Player player) {
        return getPrestigeRefundAmount(player);
    }

    static boolean progressionTryRefundPrestigePoints(Player player) {
        return tryRefundPrestigePoints(player);
    }

    public static void openComboShopMenu(Player player) {
        SheepProgressionMenus.openComboShopMenu(player);
    }

    public static void handleComboShopMenuClick(Player player, int slot) {
        SheepProgressionMenus.handleComboShopMenuClick(player, slot);
    }

    public static void openAutomationMenu(Player player) {
        SheepProgressionMenus.openAutomationMenu(player);
    }

    public static void handleAutomationMenuClick(Player player, int slot) {
        SheepProgressionMenus.handleAutomationMenuClick(player, slot, true);
    }

    static void handleAutomationMenuClick(Player player, int slot, boolean reopenMenu) {
        SheepProgressionMenus.handleAutomationMenuClick(player, slot, reopenMenu);
    }

    public static void openSacrificeMenu(Player player) {
        SheepSacrificeMenus.openSacrificeMenu(player);
    }

    public static void handleSacrificeMenuClick(Player player, int slot) {
        SheepSacrificeMenus.handleSacrificeMenuClick(player, slot);
    }

    public static void openRebirthMenu(Player player) {
        SheepRebirthMenus.openRebirthMenu(player);
    }

    public static void handleRebirthMenuClick(Player player, int slot) {
        SheepRebirthMenus.handleRebirthMenuClick(player, slot);
    }

    public static void openRebirthTreeMenu(Player player) {
        SheepRebirthMenus.openRebirthTreeMenu(player);
    }

    public static void handleRebirthTreeMenuClick(Player player, int slot) {
        SheepRebirthMenus.handleRebirthTreeMenuClick(player, slot);
    }

    static NamespacedKey socialsVisitOwnerKey() {
        return getSocialVisitOwnerKey();
    }

    static boolean socialsCanListOwner(Player viewer, Player owner) {
        if (viewer == null || owner == null || !owner.isOnline() || viewer.getUniqueId().equals(owner.getUniqueId())) {
            return false;
        }
        return viewer.isOp() || (isFarmVisitable(owner.getUniqueId())
                && !isFarmVisitorBlocked(owner.getUniqueId(), viewer.getUniqueId()));
    }

    static boolean socialsIsVisitingAnotherFarm(Player player) {
        return player != null && isSheepFarmWorld(player.getWorld()) && !isFarmOwner(player, player.getWorld());
    }

    static boolean socialsCanVisit(Player viewer, UUID ownerId) {
        return viewer != null && ownerId != null && (viewer.isOp() || isFarmVisitable(ownerId));
    }

    static void socialsReturnHome(Player player) {
        if (player == null)
            return;
        player.closeInventory();
        if (plugin != null)
            Bukkit.getScheduler().runTask(plugin, () -> player.performCommand("sheepmerge"));
    }

    static void socialsVisit(Player player, Player owner) {
        if (player == null || owner == null)
            return;
        player.closeInventory();
        if (plugin != null) {
            String targetName = owner.getName();
            Bukkit.getScheduler().runTask(plugin, () -> player.performCommand("sheepmerge visit " + targetName));
        }
    }

    static BigInteger sacrificeMenuUnlockCost(Player player) {
        return player == null ? BigInteger.ZERO : getSacrificeUnlockCost(player.getUniqueId());
    }

    static boolean sacrificeMenuHasUnlock(Player player, int unlockId) {
        return hasSacrificeUnlock(player, unlockId);
    }

    static String sacrificeMenuUnlockStatus(Player player, int unlockId) {
        return sacrificeUnlockStatusLine(player, unlockId);
    }

    static BigInteger sacrificeMenuAllSheep(Player player) {
        return sacrificeAllSheepForPlayer(player);
    }

    static boolean sacrificeMenuTryBuyUnlock(Player player, int unlockId) {
        return tryBuySacrificeUnlock(player, unlockId);
    }

    static int rebirthMenuUnspentPoints(Player player) {
        return SheepRebirthRuntime.getUnspentPoints(player);
    }

    static boolean rebirthMenuKeepsSacrifice(Player player) {
        return player != null && SheepRebirthRuntime.keepsSacrificeAfterRebirth(player.getUniqueId());
    }

    static int rebirthMenuRebirth(Player player) {
        return rebirth(player);
    }

    static List<RebirthSkillMenuEntry> rebirthMenuSkillEntries() {
        return SheepRebirthRuntime.getSkillNodes().stream()
                .map(node -> new RebirthSkillMenuEntry(node.getId(), node.getParentId(), node.getSlot(),
                        node.getMaterial(), node.getName(), node.getEffectLine(),
                        SheepRebirthRuntime.getSkillCost(node)))
                .toList();
    }

    static boolean rebirthMenuHasSkill(Player player, int skillId) {
        return SheepRebirthRuntime.hasSkill(player, skillId);
    }

    static int rebirthMenuRespecRefundAmount(Player player) {
        return SheepRebirthRuntime.getRefundAmount(player);
    }

    static boolean rebirthMenuTryRespec(Player player) {
        return SheepRebirthRuntime.tryRespec(player);
    }

    static boolean rebirthMenuTryUnlock(Player player, int skillId) {
        return SheepRebirthRuntime.tryUnlock(player, skillId);
    }

    static void progressionMarkTutorialUpgradeOpened(Player player) {
        markTutorialUpgradeOpened(player);
    }

    static int progressionBaseSheepLimit() {
        return BASE_SHEEP_LIMIT;
    }

    static int progressionLimitUpgradeStep() {
        return getLimitUpgradeStep();
    }

    static int progressionEggIntervalSecondsAtLevel(int level) {
        return Math.max(MIN_EGG_INTERVAL_SECONDS, BASE_EGG_INTERVAL_SECONDS - level);
    }

    static BigInteger progressionEggSpeedUpgradeCost(Player player) {
        return getEggSpeedUpgradeCost(player);
    }

    static String progressionWoolCooldownPercentDisplayAtLevel(Player player, int level) {
        return getWoolCooldownPercentDisplayAtLevel(player, level);
    }

    static String progressionWoolCooldownReductionPercentDisplayAtLevel(Player player, int level) {
        return getWoolCooldownReductionPercentDisplayAtLevel(player, level);
    }

    static String progressionWoolCooldownFactorDisplayAtLevel(Player player, int level) {
        return getWoolCooldownFactorDisplayAtLevel(player, level);
    }

    static BigInteger progressionWoolRegenUpgradeCost(Player player) {
        return getWoolRegenUpgradeCost(player);
    }

    static int progressionHigherTierChancePercentAtLevel(int level) {
        return Math.min(HIGHER_TIER_CHANCE_BASE_CAP_PERCENT, level * 5);
    }

    static int progressionHigherTierChanceBaseCapPercent() {
        return HIGHER_TIER_CHANCE_BASE_CAP_PERCENT;
    }

    static BigInteger progressionHigherTierChanceUpgradeCost(Player player) {
        return getHigherTierChanceUpgradeCost(player);
    }

    static int progressionComboScoreDisplay(Player player) {
        return (int) Math.floor(SheepComboRuntime.getScore(player));
    }

    static int progressionComboMaxScoreDisplay(Player player) {
        return (int) Math.floor(SheepComboRuntime.getMaxScore(player));
    }

    static String progressionComboMultiplierDisplay(Player player) {
        return SheepFormatting.formatComboMultiplier(
                SheepComboRuntime.getMultiplier(player, SheepComboRuntime.getScore(player)));
    }

    static String progressionAutomationPointInterval() {
        return formatDuration(SheepAutomationRuntime.getPointIntervalMs());
    }

    static int progressionSacrificeUnlockMax() {
        return SACRIFICE_UNLOCK_MAX_SHEEP_100;
    }

    static int progressionRebirthRewardForLevels(int level, int levels) {
        return SheepRebirthRuntime.getPointsRewardForNextLevels(level, levels);
    }

    static int progressionRebirthCost(int level) {
        return SheepRebirthRuntime.getNextCostInPrestigeLevels(level);
    }

    static OfflinePlayer socialsAuthor() {
        return Bukkit.getOfflinePlayer(SOCIALS_AUTHOR_UUID);
    }

    static String socialsAuthorCredentialsText(OfflinePlayer author) {
        return getAuthorCredentialsText(author);
    }

    static int progressionAutomationAutoBuyMaxLevel() {
        return SheepAutomationRuntime.getAutoBuyMaxLevel();
    }

    static int progressionAutomationAutoAbilityMaxLevel() {
        return SheepAutomationRuntime.getAutoAbilityMaxLevel();
    }

    static int progressionAutomationSlowAutoMergeMaxLevel() {
        return SheepAutomationRuntime.getSlowAutoMergeMaxLevel();
    }

    static int progressionAutomationSlowAutoShearMaxLevel() {
        return SheepAutomationRuntime.getSlowAutoShearMaxLevel();
    }

    static int progressionAutomationAutoSpawnMaxLevel() {
        return SheepAutomationRuntime.getAutoSpawnMaxLevel();
    }

    static String progressionAutomationAutoBuyRate(Player player) {
        long interval = SheepAutomationRuntime.getAutoBuyIntervalMs(player);
        return interval <= 0L ? "instant" : formatDuration(interval);
    }

    static String progressionAutomationAutoAbilityRate() {
        return formatDuration(SheepAutomationRuntime.getAutoAbilityIntervalMs());
    }

    static String progressionAutomationSlowAutoMergeRate(Player player) {
        return formatDuration(SheepAutomationRuntime.getSlowAutoMergeIntervalMs(player));
    }

    static String progressionAutomationSlowAutoShearRate(Player player) {
        return formatDuration(SheepAutomationRuntime.getSlowAutoShearIntervalMs(player));
    }

    static String progressionAutomationAutoPrestigeRate() {
        return formatDuration(SheepAutomationRuntime.getAutoPrestigeIntervalMs());
    }

    static String progressionAutomationAutoSpawnRate(Player player) {
        long interval = SheepAutomationRuntime.getAutoSpawnIntervalMs(player);
        return interval <= 0L ? "instant" : formatDuration(interval);
    }

    static int progressionAutomationAutoBuyUpgradeCost(Player player) {
        return getAutomationAutoBuyUpgradeCost(player);
    }

    static int progressionAutomationAutoAbilityUpgradeCost(Player player) {
        return getAutomationAutoAbilityUpgradeCost(player);
    }

    static int progressionAutomationSlowAutoMergeUpgradeCost(Player player) {
        return getAutomationSlowAutoMergeUpgradeCost(player);
    }

    static int progressionAutomationSlowAutoShearUpgradeCost(Player player) {
        return getAutomationSlowAutoShearUpgradeCost(player);
    }

    static int progressionAutomationAutoPrestigeUpgradeCost(Player player) {
        return getAutomationAutoPrestigeUpgradeCost(player);
    }

    static int progressionAutomationAutoSpawnUpgradeCost(Player player) {
        return getAutomationAutoSpawnUpgradeCost(player);
    }

    static boolean progressionUpgradeAutomationAutoBuy(Player player) {
        return upgradeAutomationAutoBuy(player);
    }

    static boolean progressionUpgradeAutomationAutoAbility(Player player) {
        return upgradeAutomationAutoAbility(player);
    }

    static boolean progressionUpgradeAutomationSlowAutoMerge(Player player) {
        return upgradeAutomationSlowAutoMerge(player);
    }

    static boolean progressionUpgradeAutomationSlowAutoShear(Player player) {
        return upgradeAutomationSlowAutoShear(player);
    }

    static boolean progressionUpgradeAutomationAutoPrestige(Player player) {
        return upgradeAutomationAutoPrestige(player);
    }

    static boolean progressionUpgradeAutomationAutoSpawn(Player player) {
        return upgradeAutomationAutoSpawn(player);
    }

    static boolean progressionToggleAutomationAutoBuy(Player player) {
        return SheepAutomationRuntime.toggleAutoBuy(player);
    }

    static boolean progressionToggleAutomationAutoAbility(Player player) {
        return SheepAutomationRuntime.toggleAutoAbility(player);
    }

    static boolean progressionToggleAutomationSlowAutoMerge(Player player) {
        return SheepAutomationRuntime.toggleSlowAutoMerge(player);
    }

    static boolean progressionToggleAutomationSlowAutoShear(Player player) {
        return SheepAutomationRuntime.toggleSlowAutoShear(player);
    }

    static boolean progressionToggleAutomationAutoPrestige(Player player) {
        return SheepAutomationRuntime.toggleAutoPrestige(player);
    }

    static boolean progressionToggleAutomationAutoSpawn(Player player) {
        return SheepAutomationRuntime.toggleAutoSpawn(player);
    }

    static int progressionUnlockedAutomationCount(Player player) {
        return getUnlockedAutomationCount(player);
    }

    static int progressionSetAllAutomationsEnabled(Player player, boolean enabled) {
        return setAllAutomationsEnabled(player, enabled);
    }

    static int progressionComboDecayMaxLevel() {
        return COMBO_DECAY_MAX_LEVEL;
    }

    static int progressionComboGainMaxLevel() {
        return COMBO_GAIN_MAX_LEVEL;
    }

    static double progressionComboGainPercentPerLevel() {
        return SheepComboRuntime.getGainPercentPerLevel();
    }

    static double progressionComboDecayMultiplier(Player player) {
        return SheepComboRuntime.getDecayMultiplier(player);
    }

    static double progressionComboMaxScore(Player player) {
        return SheepComboRuntime.getMaxScore(player);
    }

    static BigInteger progressionComboDecayUpgradeCost(Player player) {
        return getComboDecayUpgradeCost(player);
    }

    static BigInteger progressionComboGainUpgradeCost(Player player) {
        return getComboGainUpgradeCost(player);
    }

    static int progressionComboMaxUpgradePrestigeCost(Player player) {
        return getComboMaxUpgradePrestigeCost(player);
    }

    static boolean progressionUpgradeComboDecay(Player player) {
        return upgradeComboDecay(player);
    }

    static boolean progressionUpgradeComboMax(Player player) {
        return upgradeComboMax(player);
    }

    static boolean progressionUpgradeComboGain(Player player) {
        return upgradeComboGain(player);
    }

    static void progressionUpdateComboBossBar(Player player) {
        SheepComboRuntime.updateBossBar(player, SheepComboRuntime.getScore(player));
    }

    static int progressionShearWoolSaveMaxLevel() {
        return SHEAR_WOOL_SAVE_MAX_LEVEL;
    }

    static int progressionShearTierBoostMaxLevel() {
        return SHEAR_TIER_BOOST_MAX_LEVEL;
    }

    static void progressionMarkTutorialShearShopOpened(Player player) {
        markTutorialShearShopOpened(player);
    }

    static void progressionMarkTutorialShearUpgraded(Player player) {
        markTutorialShearUpgraded(player);
    }

    static boolean progressionBlockShearUpgradePurchase(Player player) {
        return SheepTutorialRuntime.blockShearUpgradePurchase(player);
    }

    static boolean progressionUpgradeShearShop(Player player) {
        return upgradeShearShop(player);
    }

    static boolean progressionUpgradeShearWoolSave(Player player) {
        return upgradeShearWoolSave(player);
    }

    static boolean progressionUpgradeShearTierBoost(Player player) {
        return upgradeShearTierBoost(player);
    }

    static void progressionMarkTutorialQuestUpgradesOpened(Player player) {
        markTutorialQuestUpgradesOpened(player);
    }

    static boolean progressionBlockQuestUpgradePurchase(Player player) {
        return SheepTutorialRuntime.blockCompletedStepPurchase(player,
                "Complete the tutorial before buying Quest Upgrades");
    }

    static int progressionQuestUpgradeDurationCost(Player player) {
        return getQuestUpgradeDurationCost(player);
    }

    static int progressionQuestUpgradePowerCost(Player player) {
        return getQuestUpgradePowerCost(player);
    }

    static boolean progressionUpgradeQuestDuration(Player player) {
        return upgradeQuestDuration(player);
    }

    static boolean progressionUpgradeQuestPower(Player player) {
        return upgradeQuestPower(player);
    }

    static void progressionPlayUpgradeSound(Player player) {
        playUpgradeSound(player);
    }

    static void progressionMarkTutorialQuestOpened(Player player) {
        markTutorialQuestOpened(player);
    }

    static void progressionMarkTutorialAbilityUsed(Player player) {
        markTutorialAbilityUsed(player);
    }

    static boolean progressionBlockQuestAbilityPurchase(Player player) {
        return SheepTutorialRuntime.blockAbilityUse(player);
    }

    static int progressionQuestLuckyBurstCost(Player player) {
        return getQuestLuckyBurstCost(player);
    }

    static long progressionQuestResetRemainingMs(Player player) {
        return getQuestResetRemainingMs(player);
    }

    static int progressionQuestWoolRushCost(Player player) {
        return getQuestWoolRushCost(player);
    }

    static int progressionQuestJackpotCost(Player player) {
        return getQuestJackpotCost(player);
    }

    static int progressionQuestAutoMergeCost(Player player) {
        return getQuestAutoMergeCost(player);
    }

    static int progressionQuestAutoShearCost(Player player) {
        return getQuestAutoShearCost(player);
    }

    static int progressionQuestShearsTarget(Player player) {
        return SheepQuestRuntime.getShearsTarget(player);
    }

    static int progressionQuestSpawnsTarget(Player player) {
        return SheepQuestRuntime.getSpawnsTarget(player);
    }

    static int progressionQuestMergesTarget(Player player) {
        return SheepQuestRuntime.getMergesTarget(player);
    }

    static int progressionQuestShearsReward(Player player) {
        return SheepQuestRuntime.getShearsReward(player);
    }

    static int progressionQuestSpawnsReward(Player player) {
        return SheepQuestRuntime.getSpawnsReward(player);
    }

    static int progressionQuestMergesReward(Player player) {
        return SheepQuestRuntime.getMergesReward(player);
    }

    static int progressionQuestLuckyBurstBonusPercent() {
        return SheepQuestRuntime.getLuckyBurstBonusPercent();
    }

    static int progressionQuestLuckyBurstUseCount(Player player) {
        return SheepQuestRuntime.getLuckyBurstUseCount(player);
    }

    static int progressionQuestAutoMergeUseCount(Player player) {
        return SheepQuestRuntime.getAutoMergeUseCount(player);
    }

    static int progressionQuestAutoShearUseCount(Player player) {
        return SheepQuestRuntime.getAutoShearUseCount(player);
    }

    static String progressionQuestWoolRushDuration(Player player) {
        return formatDuration(SheepQuestRuntime.getWoolRushDurationMs(player));
    }

    static String progressionQuestJackpotDuration(Player player) {
        return formatDuration(SheepQuestRuntime.getJackpotDurationMs(player));
    }

    static boolean progressionQuestLuckyBurstActive(Player player) {
        return isCountAbilityActive(SheepQuestState.activeLuckyBurstUses(), SheepQuestState.luckyBurstEnabled(),
                player.getUniqueId());
    }

    static boolean progressionQuestWoolRushActive(Player player) {
        return isAbilityActive(SheepQuestState.activeWoolRushUntil(), player.getUniqueId());
    }

    static boolean progressionQuestJackpotActive(Player player) {
        return isAbilityActive(SheepQuestState.activeJackpotShearsUntil(), player.getUniqueId());
    }

    static boolean progressionQuestAutoMergeActive(Player player) {
        return isCountAbilityActive(SheepQuestState.activeAutoMergeUses(), SheepQuestState.autoMergeEnabled(),
                player.getUniqueId());
    }

    static boolean progressionQuestAutoShearActive(Player player) {
        return isCountAbilityActive(SheepQuestState.activeAutoShearUses(), SheepQuestState.autoShearEnabled(),
                player.getUniqueId());
    }

    static String progressionQuestLuckyBurstStatus(Player player) {
        return getCountAbilityMenuStatus(SheepQuestState.activeLuckyBurstUses(), SheepQuestState.luckyBurstEnabled(),
                player.getUniqueId());
    }

    static String progressionQuestLuckyBurstAction(Player player) {
        return getCountAbilityToggleActionLine(SheepQuestState.activeLuckyBurstUses(),
                SheepQuestState.luckyBurstEnabled(), player.getUniqueId());
    }

    static String progressionQuestWoolRushStatus(Player player) {
        return getAbilityMenuStatus(SheepQuestState.activeWoolRushUntil(), null, player.getUniqueId());
    }

    static String progressionQuestJackpotStatus(Player player) {
        return getAbilityMenuStatus(SheepQuestState.activeJackpotShearsUntil(), null, player.getUniqueId());
    }

    static String progressionQuestAutoMergeStatus(Player player) {
        return getCountAbilityMenuStatus(SheepQuestState.activeAutoMergeUses(), SheepQuestState.autoMergeEnabled(),
                player.getUniqueId());
    }

    static String progressionQuestAutoMergeAction(Player player) {
        return getCountAbilityToggleActionLine(SheepQuestState.activeAutoMergeUses(),
                SheepQuestState.autoMergeEnabled(), player.getUniqueId());
    }

    static String progressionQuestAutoShearStatus(Player player) {
        return getCountAbilityMenuStatus(SheepQuestState.activeAutoShearUses(), SheepQuestState.autoShearEnabled(),
                player.getUniqueId());
    }

    static String progressionQuestAutoShearAction(Player player) {
        return getCountAbilityToggleActionLine(SheepQuestState.activeAutoShearUses(),
                SheepQuestState.autoShearEnabled(), player.getUniqueId());
    }

    static boolean progressionToggleQuestLuckyBurst(Player player) {
        return SheepQuestRuntime.toggleLuckyBurst(player);
    }

    static boolean progressionToggleQuestAutoMerge(Player player) {
        return SheepQuestRuntime.toggleAutoMerge(player);
    }

    static boolean progressionToggleQuestAutoShear(Player player) {
        return SheepQuestRuntime.toggleAutoShear(player);
    }

    static boolean progressionActivateQuestLuckyBurst(Player player) {
        return SheepQuestRuntime.activateLuckyBurst(player);
    }

    static boolean progressionApplyQuestWoolRush(Player player, boolean extend) {
        return SheepQuestRuntime.applyWoolRush(player, extend);
    }

    static boolean progressionApplyQuestJackpot(Player player, boolean extend) {
        return SheepQuestRuntime.applyJackpot(player, extend);
    }

    static boolean progressionActivateQuestAutoMerge(Player player) {
        return SheepQuestRuntime.activateAutoMerge(player);
    }

    static boolean progressionActivateQuestAutoShear(Player player) {
        return SheepQuestRuntime.activateAutoShear(player);
    }

    static void progressionApplyWoolRushToShearedSheep(Player player) {
        applyWoolRushToShearedSheep(player);
    }

    static void progressionSpawnParticle(Player player, org.bukkit.Particle particle, Location location, int count,
            double offsetX, double offsetY, double offsetZ, double extra) {
        spawnParticle(player, particle, location, count, offsetX, offsetY, offsetZ, extra);
    }

    static void progressionPlaySound(Player player, Sound sound, float volume, float pitch) {
        playSound(player, sound, volume, pitch);
    }

    static boolean questHasQuestMaster(Player player) {
        return hasQuestMaster(player);
    }

    static int questPointsGainMultiplier(Player player) {
        return getQuestPointsGainMultiplierFromRebirthSkills(player);
    }

    static int questPrestigeLevel(Player player) {
        return getPrestigeLevel(player);
    }

    static double questPrestigeRewardMultiplier(Player player) {
        return getPrestigeQuestRewardMultiplier(player);
    }

    static int questAddSaturated(int current, int amount) {
        return addSaturated(current, amount);
    }

    static boolean questIsSheepFarmWorld(World world) {
        return isSheepFarmWorld(world);
    }

    static String questColor(String text) {
        return color(text);
    }

    static String questAction(String text) {
        return action(text);
    }

    static void questPlaySound(Player player, Sound sound, float volume, float pitch) {
        playSound(player, sound, volume, pitch);
    }

    static void questSpawnParticle(Player player, org.bukkit.Particle particle, Location location, int count,
            double offsetX, double offsetY, double offsetZ, double extra) {
        spawnParticle(player, particle, location, count, offsetX, offsetY, offsetZ, extra);
    }

    static void questSaveData() {
        saveData();
    }

    static int questDoubledUpgradeCost(int baseCost, int level) {
        return getDoubledUpgradeCost(baseCost, level);
    }

    static String questHint(String text) {
        return hint(text);
    }

    static String questFormatDuration(long durationMs) {
        return formatDuration(durationMs);
    }

    static void questPlayActivationEffect(Player player, Sound sound, org.bukkit.Particle particle) {
        if (sound == Sound.ENTITY_SHEEP_SHEAR) {
            playSheepSound(player, sound, 1.0f, 1.2f);
        } else {
            playSound(player, sound, 1.0f, 1.2f);
        }
        spawnParticle(player, particle, player.getLocation().add(0, 2.0, 0), 25, 0.35, 0.5, 0.35, 0.02);
    }

    static void questPlayRandomAuraSound(Player player) {
        Sound[] gentleAuraSounds = {
                Sound.BLOCK_NOTE_BLOCK_CHIME,
                Sound.BLOCK_NOTE_BLOCK_HARP,
                Sound.BLOCK_AMETHYST_BLOCK_CHIME
        };
        playSound(player, gentleAuraSounds[RANDOM.nextInt(gentleAuraSounds.length)], 0.16f, 1.0f);
    }

    static boolean questShearSheep(Player player, Sheep sheep) {
        return shearSheepForPlayer(player, sheep);
    }

    static boolean questMergeSheepPair(Player player, Sheep first, Sheep second, boolean recordTutorial) {
        return SheepEntityRuntime.mergePair(player, first, second, recordTutorial);
    }

    static Sheep questPickedUpSheep(Player player) {
        return getPickedUpSheep(player);
    }

    static long questNextEatTimestamp(Sheep sheep) {
        return getNextEatTimestamp(sheep);
    }

    static void questUpdateSheepName(Sheep sheep) {
        updateSheepName(sheep);
    }

    public static void openQuestMenu(Player player) {
        SheepProgressionMenus.openQuestMenu(player);
    }

    public static void handleQuestMenuClick(Player player, int slot) {
        SheepProgressionMenus.handleQuestMenuClick(player, slot);
    }

    public static void openQuestUpgradesMenu(Player player) {
        SheepProgressionMenus.openQuestUpgradesMenu(player);
    }

    public static void handleQuestUpgradeMenuClick(Player player, int slot) {
        SheepProgressionMenus.handleQuestUpgradeMenuClick(player, slot);
    }

    public static void openShopMenu(Player player) {
        SheepProgressionMenus.openShopMenu(player);
    }

    public static void handleShopMenuClick(Player player, int slot) {
        SheepProgressionMenus.handleShopMenuClick(player, slot);
    }

    private static BigInteger getEggSpeedUpgradeCost(Player player) {
        return SheepEconomyRuntime.getRegularUpgradeCost(EGG_SPEED_UPGRADE_BASE_COST, getEggSpeedLevel(player));
    }

    private static BigInteger getWoolRegenUpgradeCost(Player player) {
        return SheepEconomyRuntime.getRegularUpgradeCost(WOOL_REGEN_UPGRADE_BASE_COST, getWoolRegenLevel(player));
    }

    private static BigInteger getHigherTierChanceUpgradeCost(Player player) {
        return SheepEconomyRuntime.getRegularUpgradeCost(HIGHER_TIER_CHANCE_UPGRADE_BASE_COST,
                getHigherTierChanceLevel(player));
    }

    private static int scaleRegularPointsUpgradeBaseCost(int baseCost) {
        return SheepEconomyRuntime.scaleRegularUpgradeBaseCost(baseCost);
    }

    private static boolean upgradeEggSpeed(Player player) {
        return SheepEconomyRuntime.upgradeEggSpeed(player, EGG_SPEED_UPGRADE_BASE_COST);
    }

    private static boolean upgradeWoolRegen(Player player) {
        return SheepEconomyRuntime.upgradeWoolRegen(player, WOOL_REGEN_UPGRADE_BASE_COST);
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
        return SheepEconomyRuntime.upgradeHigherTierChance(player, HIGHER_TIER_CHANCE_UPGRADE_BASE_COST);
    }

    private static int getWoolRegenLevel(World world) {
        UUID ownerId = getOwnerId(world);
        if (ownerId == null) {
            return 0;
        }
        return SheepEconomyState.getWoolRegenLevel(ownerId);
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
        return SheepEntityRuntime.getSheepCount(world);
    }

    public static boolean isWorldAtLimit(World world) {
        return SheepEntityRuntime.isWorldAtLimit(world);
    }

    public static void refreshLiveSheepCounts(Iterable<World> worlds) {
        SheepEntityRuntime.refreshLiveSheepCounts(worlds);
    }

    public static void refreshLiveSheepCount(World world) {
        SheepEntityRuntime.refreshLiveSheepCount(world);
    }

    public static void savePlayerInventory(Player player) {
        SheepInventoryRuntime.savePlayerInventory(player);
    }

    public static void restorePlayerInventory(Player player) {
        SheepInventoryRuntime.restorePlayerInventory(player);
    }

    public static boolean hasSavedInventory(Player player) {
        return SheepInventoryRuntime.hasSavedInventory(player);
    }

    public static void restoreSavedStateOutsideFarm(Player player) {
        SheepInventoryRuntime.restoreSavedStateOutsideFarm(player);
    }

    public static void showPointsScoreboard(Player player) {
        SheepScoreboardRuntime.showPointsScoreboard(player);
    }

    public static void updatePointsScoreboard(Player player) {
        SheepScoreboardRuntime.updatePointsScoreboard(player);
    }

    public static void recordVisitedOtherFarm(Player visitor, UUID ownerId) {
        SheepAchievementRuntime.recordVisitedOtherFarm(visitor, ownerId);
    }

    public static void updateTabListPointsVisibility(Player player) {
        SheepScoreboardRuntime.updateTabListPointsVisibility(player);
    }

    public static void restorePlayerScoreboard(Player player) {
        SheepScoreboardRuntime.restorePlayerScoreboard(player);
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
                SheepInventoryRuntime.clearForcedFarmLoadoutWithoutSnapshot(player);
            }
            if (wasInSheepWorld && !hadSavedScoreboard) {
                SheepScoreboardRuntime.clearSheepMergeSidebarWithoutSnapshot(player);
            }
            player.setPlayerListName(null);
        }
        SheepRuntimeUiState.savedScoreboards().clear();
        EGG_MODULE.clearSavedExperienceCache();
    }

    public static boolean isSheepMergeShearsItem(ItemStack itemStack) {
        return SheepInventoryRuntime.isSheepMergeShearsItem(itemStack);
    }

    public static boolean isManagedShearsHotbarSlot(int slot) {
        return SheepInventoryRuntime.isManagedShearsHotbarSlot(slot);
    }

    public static ItemStack getSheepMergeShears() {
        return SheepInventoryRuntime.getSheepMergeShears();
    }

    public static void saveData() {
        if (plugin == null || dataFile == null) {
            return;
        }
        dataConfig = SheepDataPersistence.saveData(plugin, dataFile, dataConfig);
    }

    private static void loadData() {
        if (plugin == null || dataFile == null) {
            return;
        }
        dataConfig = SheepDataPersistence.loadData(plugin, dataFile);
    }

    static int persistencePrestigeDoublePointsMaxLevel() {
        return PRESTIGE_DOUBLE_POINTS_MAX_LEVEL;
    }

    static int persistenceRainbowSheepLevel() {
        return SheepTier.RAINBOW.getLevel();
    }

    static int persistenceComboDecayMaxLevel() {
        return COMBO_DECAY_MAX_LEVEL;
    }

    static int persistenceComboGainMaxLevel() {
        return COMBO_GAIN_MAX_LEVEL;
    }

    static int persistenceAutomationAutoBuyMaxLevel() {
        return SheepAutomationRuntime.getAutoBuyMaxLevel();
    }

    static int persistenceAutomationAutoAbilityMaxLevel() {
        return SheepAutomationRuntime.getAutoAbilityMaxLevel();
    }

    static int persistenceAutomationSlowAutoMergeMaxLevel() {
        return SheepAutomationRuntime.getSlowAutoMergeMaxLevel();
    }

    static int persistenceAutomationSlowAutoShearMaxLevel() {
        return SheepAutomationRuntime.getSlowAutoShearMaxLevel();
    }

    static int persistenceAutomationAutoSpawnMaxLevel() {
        return SheepAutomationRuntime.getAutoSpawnMaxLevel();
    }

    static int persistenceAutomationAutoPrestigeMaxLevel() {
        return 1;
    }

    static int persistenceQuickAccessMaxItems() {
        return INVENTORY_QUICK_ACCESS_MAX_ITEMS;
    }

    static boolean persistenceIsValidQuickAccessAction(String actionId) {
        return getQuickAccessDefinition(actionId) != null;
    }

    static boolean persistenceIsValidAchievementId(String achievementId) {
        return SheepAchievementRuntime.isValidId(achievementId);
    }

    static boolean persistenceIsValidAchievementMilestoneId(String milestoneId) {
        return SheepAchievementRuntime.isValidMilestoneId(milestoneId);
    }

    static boolean persistenceClampUpgradeLevelsToCurrentCaps(UUID playerId) {
        return clampUpgradeLevelsToCurrentCaps(playerId);
    }

    public static void storePickedUpSheep(Player player, Sheep sheep) {
        SheepEntityRuntime.storePickedUpSheep(player, sheep);
    }

    public static boolean hasPickedUpSheep(Player player) {
        return SheepEntityRuntime.hasPickedUpSheep(player);
    }

    public static Sheep getPickedUpSheep(Player player) {
        return SheepEntityRuntime.getPickedUpSheep(player);
    }

    public static boolean dropPickedUpSheep(Player player) {
        return SheepEntityRuntime.dropPickedUpSheep(player);
    }

    public static void updateCarriedSheepPosition(Player player) {
        SheepEntityRuntime.updateCarriedSheepPosition(player);
    }

    public static void clearPickedUpSheep(Player player) {
        SheepEntityRuntime.clearPickedUpSheep(player);
    }

    static UUID getOwnerId(World world) {
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
