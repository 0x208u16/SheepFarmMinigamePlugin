package dev.thehale.papermc_plugin_template;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
import org.bukkit.entity.Display;
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

    private static final Map<UUID, Integer> pointsByPlayer = new HashMap<>();
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
    private static final Map<UUID, Integer> highestAnnouncedTierByPlayer = new HashMap<>();
    private static final Map<UUID, Integer> shearShopLevelByPlayer = new HashMap<>();
    private static final Map<UUID, Boolean> tutorialCompletedByPlayer = new HashMap<>();
    private static final Map<UUID, Integer> tutorialShearsByPlayer = new HashMap<>();
    private static final Map<UUID, Integer> tutorialSpawnsByPlayer = new HashMap<>();
    private static final Map<UUID, Integer> tutorialMergesByPlayer = new HashMap<>();
    private static final Map<UUID, Boolean> tutorialUpgradeOpenedByPlayer = new HashMap<>();
    private static final Map<UUID, Boolean> tutorialQuestOpenedByPlayer = new HashMap<>();
    private static final Map<UUID, Boolean> tutorialQuestUpgradesOpenedByPlayer = new HashMap<>();
    private static final Map<UUID, Boolean> tutorialPrestigeOpenedByPlayer = new HashMap<>();
    private static final Map<UUID, Boolean> tutorialAbilityUsedByPlayer = new HashMap<>();
    private static final Map<UUID, Boolean> tutorialShearShopOpenedByPlayer = new HashMap<>();
    private static final Map<UUID, Boolean> tutorialBypassedByPlayer = new HashMap<>();
    private static final Map<UUID, Long> tutorialStartedAtByPlayer = new HashMap<>();
    private static final Map<UUID, Long> lastTutorialReminderTimestampByPlayer = new HashMap<>();
    private static final Map<UUID, Integer> questPointsByPlayer = new HashMap<>();
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
    private static final Map<UUID, Long> activeWoolRushUntilByPlayer = new HashMap<>();
    private static final Map<UUID, Long> activeJackpotShearsUntilByPlayer = new HashMap<>();
    private static final Map<UUID, Long> activeAutoMergeUntilByPlayer = new HashMap<>();
    private static final Map<UUID, Long> nextAutoMergeAtByPlayer = new HashMap<>();
    private static final Map<UUID, Long> nextEggTimestampByPlayer = new HashMap<>();
    private static final Map<UUID, Long> lastPrestigeReminderTimestampByPlayer = new HashMap<>();
    private static final Map<UUID, Long> nextPrestigeRefundTimestampByPlayer = new HashMap<>();
    private static final Map<UUID, Long> lastMergeTimestampByPlayer = new HashMap<>();
    private static final Map<UUID, Long> lastMergeReminderTimestampByPlayer = new HashMap<>();
    private static final Map<UUID, Sheep> carriedSheepByPlayer = new HashMap<>();
    private static final Map<UUID, Long> sheepRescueStartByEntity = new HashMap<>();
    private static final Map<UUID, InventoryDataUtils.Snapshot> savedInventories = new HashMap<>();
    private static final Map<UUID, Scoreboard> savedScoreboards = new HashMap<>();
    private static final Map<UUID, Integer> savedLevels = new HashMap<>();
    private static final Map<UUID, Float> savedExpProgress = new HashMap<>();
    private static final Map<UUID, Integer> liveSheepCountByWorld = new HashMap<>();
    private static final Map<UUID, Boolean> farmVisitEnabledByPlayer = new HashMap<>();
    private static final Map<UUID, Long> lastSpawnLimitWarningTimestampByPlayer = new HashMap<>();
    private static final Pattern OWNER_ID_PATTERN = Pattern.compile("^sheepfarm_([0-9a-fA-F]{32})$");
    private static final Pattern TUTORIAL_OWNER_ID_PATTERN = Pattern.compile("^sheeptutorial_([0-9a-fA-F]{32})$");
    private static final Random RANDOM = new Random();
    private static final int BASE_SHEEP_LIMIT = 10;
    private static final int LIMIT_UPGRADE_STEP = 5;
    private static final int LIMIT_UPGRADE_COST = 20;
    private static final int BASE_EGG_INTERVAL_SECONDS = 10;
    private static final int MIN_EGG_INTERVAL_SECONDS = 2;
    private static final int EGG_SPEED_MAX_LEVEL = BASE_EGG_INTERVAL_SECONDS - MIN_EGG_INTERVAL_SECONDS;
    private static final int EGG_SPEED_UPGRADE_BASE_COST = 15;
    private static final int WOOL_REGEN_UPGRADE_BASE_COST = 25;
    private static final int WOOL_REGEN_MAX_LEVEL = 8;
    private static final int HIGHER_TIER_CHANCE_UPGRADE_BASE_COST = 30;
    private static final int HIGHER_TIER_CHANCE_MAX_LEVEL = 10;
    private static final int HIGHER_TIER_CHANCE_HARD_MAX_LEVEL = 20;
    private static final int PRESTIGE_DOUBLE_POINTS_BASE_COST = 1;
    private static final int PRESTIGE_HIGHER_MAX_LEVEL_BASE_COST = 2;
    private static final int PRESTIGE_START_EGGS_BASE_COST = 1;
    private static final int PRESTIGE_EGG_CAP_BASE_COST = 2;
    private static final int PRESTIGE_BASE_SPAWN_TIER_BASE_COST = 10;
    private static final int QUEST_SHEARS_TARGET = 20;
    private static final int QUEST_SPAWNS_TARGET = 12;
    private static final int QUEST_MERGES_TARGET = 8;
    private static final int QUEST_SHEARS_REWARD = 4;
    private static final int QUEST_SPAWNS_REWARD = 5;
    private static final int QUEST_MERGES_REWARD = 7;
    private static final long BASE_QUEST_RESET_MS = 15L * 60L * 1000L;
    private static final long MIN_QUEST_RESET_MS = 5L * 60L * 1000L;
    private static final long QUEST_RESET_REDUCTION_PER_PRESTIGE_MS = 60L * 1000L;
    private static final int QUEST_LUCKY_BURST_BASE_COST = 8;
    private static final int QUEST_WOOL_RUSH_BASE_COST = 10;
    private static final int QUEST_JACKPOT_SHEARS_BASE_COST = 15;
    private static final int QUEST_AUTO_MERGE_BASE_COST = 18;
    private static final long QUEST_LUCKY_BURST_BASE_DURATION_MS = 3L * 60L * 1000L;
    private static final long QUEST_WOOL_RUSH_BASE_DURATION_MS = 4L * 60L * 1000L;
    private static final long QUEST_JACKPOT_SHEARS_BASE_DURATION_MS = 2L * 60L * 1000L;
    private static final long QUEST_AUTO_MERGE_BASE_DURATION_MS = 90L * 1000L;
    private static final long QUEST_AUTO_MERGE_INTERVAL_MS = 1000L;
    private static final int QUEST_UPGRADE_DURATION_BASE_COST = 12;
    private static final int QUEST_UPGRADE_POWER_BASE_COST = 15;
    private static final int BASE_EGG_CAP = 10;
    private static final int PRESTIGE_EGG_CAP_STEP = 10;
    private static final int PRESTIGE_MAX_LEVEL = 50;
    private static final int PRESTIGE_HIGHER_MAX_LEVEL_HARD_CAP = 8;
    private static final long PRESTIGE_REFUND_COOLDOWN_MS = 30L * 60L * 1000L;
    private static final int FARM_RADIUS = 5;
    private static final int FARM_BASE_Y = 100;
    private static final int FARM_MIN_Y = FARM_BASE_Y - 1;
    private static final int FARM_MAX_Y = FARM_BASE_Y + 4;
    private static final long SHEEP_RESCUE_TIMEOUT_MS = 10_000L;
    private static final long SHEEP_RESCUE_INITIAL_YEET_MS = 450L;
    private static final double SHEEP_RESCUE_UPWARD_VELOCITY = 0.95D;
    private static final double SHEEP_RESCUE_HORIZONTAL_VELOCITY = 0.45D;
    private static final double SHEEP_RESCUE_INITIAL_YEET_HORIZONTAL_VELOCITY = 0.70D;
    private static final double SHEEP_RESCUE_INITIAL_YEET_UPWARD_VELOCITY = 0.38D;
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
    private static final int SHEAR_SHOP_BASE_COST = 40;
    private static final long SPAWN_LIMIT_WARNING_COOLDOWN_MS = 5_000L;
    private static final long MERGE_REMINDER_DELAY_MS = 30_000L;
    private static final long MERGE_REMINDER_REPEAT_MS = 60_000L;
    private static final long TUTORIAL_REMINDER_DELAY_MS = 2L * 60L * 1000L;
    private static final long TUTORIAL_REMINDER_REPEAT_MS = 60_000L;
    private static final long TUTORIAL_FAIL_TIMEOUT_MS = 5L * 60L * 1000L;
    private static final long RANDOM_EVENT_ROLL_INTERVAL_MS = 60_000L;
    private static final int RANDOM_EVENT_TRIGGER_CHANCE_DENOMINATOR = 10;
    private static final long SHEEP_RAIN_EVENT_DURATION_MS = 60_000L;
    private static final long SHEEP_RAIN_MIN_INTERVAL_MS = 1_000L;
    private static final long SHEEP_RAIN_MAX_INTERVAL_MS = 5_000L;
    private static final int SHEEP_RAIN_SPAWN_HEIGHT = 12;
    private static final double SHEEP_RAIN_HORIZONTAL_PADDING = 1.5D;
    private static final int TUTORIAL_SHEAR_TARGET = 3;
    private static final int TUTORIAL_SPAWN_TARGET = 1;
    private static final int TUTORIAL_MERGE_TARGET = 1;
    private static final int TUTORIAL_MENU_SECTION_TARGET = 5;
    public static final String UPGRADE_MENU_TITLE = "Sheep Merge Upgrades";
    public static final String PRESTIGE_MENU_TITLE = "Prestige Upgrades";
    public static final String QUEST_MENU_TITLE = "Quest Abilities";
    public static final String QUEST_UPGRADES_MENU_TITLE = "Quest Upgrades";
    public static final String SHOP_MENU_TITLE = "Shear Shop";
    public static final int LIMIT_UPGRADE_SLOT = 10;
    public static final int EGG_SPEED_UPGRADE_SLOT = 12;
    public static final int WOOL_REGEN_UPGRADE_SLOT = 14;
    public static final int HIGHER_TIER_CHANCE_UPGRADE_SLOT = 16;
    public static final int PRESTIGE_MENU_OPEN_SLOT = 22;
    public static final int QUEST_MENU_OPEN_SLOT = 20;
    public static final int SHOP_MENU_OPEN_SLOT = 24;
    public static final int PRESTIGE_UPGRADE_SLOT = 10;
    public static final int PRESTIGE_DOUBLE_POINTS_SLOT = 12;
    public static final int PRESTIGE_HIGHER_MAX_LEVEL_SLOT = 14;
    public static final int PRESTIGE_START_EGGS_SLOT = 16;
    public static final int PRESTIGE_EGG_CAP_SLOT = 18;
    public static final int PRESTIGE_BASE_SPAWN_TIER_SLOT = 20;
    public static final int PRESTIGE_REFUND_SLOT = 24;
    public static final int PRESTIGE_BACK_TO_UPGRADES_SLOT = 26;
    public static final int QUEST_ABILITY_LUCKY_BURST_SLOT = 10;
    public static final int QUEST_ABILITY_WOOL_RUSH_SLOT = 13;
    public static final int QUEST_ABILITY_JACKPOT_SHEARS_SLOT = 16;
    public static final int QUEST_ABILITY_AUTO_MERGE_SLOT = 19;
    public static final int QUEST_OPEN_UPGRADES_SLOT = 22;
    public static final int QUEST_BACK_TO_UPGRADES_SLOT = 26;
    public static final int QUEST_BOARD_SLOT = 4;
    public static final int QUEST_UPGRADE_DURATION_SLOT = 11;
    public static final int QUEST_UPGRADE_POWER_SLOT = 15;
    public static final int QUEST_UPGRADE_BACK_SLOT = 26;
    public static final int SHOP_SHEAR_SLOT = 13;
    public static final int SHOP_BACK_TO_UPGRADES_SLOT = 26;

    private static SheepMergePlugin plugin;
    private static FileConfiguration dataConfig;
    private static File dataFile;
    private static FileConfiguration farmLayoutConfig;
    private static File farmLayoutFile;
    private static long nextRandomEventRollAtMs = 0L;
    private static long sheepRainEventEndsAtMs = 0L;
    private static long nextSheepRainSpawnAtMs = 0L;
    private static BossBar sheepRainBossBar;
    private static int lastGameplayTipIndex = -1;
    private static final List<String> GAMEPLAY_TIPS = List.of(
            "&7Use &e/sheepmerge &7to jump straight to your personal farm.",
            "&7Run &e/sheepmerge status &7to quickly check your core progression stats.",
            "&7Open &e/sheepmerge upgrade &7to improve limit, egg speed, wool regen, and spawn chance.",
            "&7Bigger &eSheep Limit &7means more sheep alive at once and more merge opportunities.",
            "&7Faster &eEgg Speed &7means spawn eggs are generated more often.",
            "&7Higher &eWool Regen &7levels regrow wool faster for more shearing.",
            "&7Upgrade &eHigher-Tier Chance &7to roll better sheep from eggs more often.",
            "&7Sneak-right-click a sheep to carry it, then right-click the same tier to merge.",
            "&7Top-tier sheep cannot merge further, so focus on spawning and supporting them.",
            "&7Shearing and merging are your main point engines, so keep both loops active.",
            "&7Run &e/sheepmerge shop &7to upgrade your shears and improve shear value.",
            "&7Run &e/sheepmerge prestige &7when ready to reset progress for permanent bonuses.",
            "&7Prestige points buy long-term boosts like double points chance and bigger egg cap.",
            "&7Use prestige upgrades to raise max levels, start with eggs, and improve base spawn tier.",
            "&7Prestige refund lets you respec spent prestige points when the cooldown is over.",
            "&7Quest board objectives reset over time. Completing quests awards quest points.",
            "&7Spend quest points on abilities: &eLucky Burst, Wool Rush, Jackpot Shears, Auto Merge&7.",
            "&7Quest upgrades boost ability &eDuration &7and &ePower&7 for stronger activations.",
            "&7Use &e/sheepmerge tutorial &7anytime if you want a guided refresher.",
            "&7Your farm can be opened or closed with &e/sheepmerge visit toggle&7.",
            "&7Visit another open farm with &e/sheepmerge visit <player>&7.",
            "&7If needed, remove visitors from your own farm using &e/sheepmerge kick <player>&7.",
            "&7Random &eSheep Storm &7events can happen and flood farms with sheep from above.",
            "&7Rainbow sheep are legendary. Keep merging to push your tier progression.");

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

    public static int getFarmRadius() {
        return FARM_RADIUS;
    }

    public static int getFarmBaseY() {
        return FARM_BASE_Y;
    }

    public static boolean hasSavedFarmLayout() {
        return farmLayoutConfig != null
                && farmLayoutConfig.isConfigurationSection("blocks")
                && !farmLayoutConfig.getConfigurationSection("blocks").getKeys(false).isEmpty();
    }

    public static boolean saveSharedFarmLayoutFromWorld(World sourceWorld) {
        if (sourceWorld == null || !isSheepFarmWorld(sourceWorld)) {
            return false;
        }
        if (farmLayoutConfig == null) {
            farmLayoutConfig = new YamlConfiguration();
        }
        farmLayoutConfig.set("blocks", null);
        for (int x = -FARM_RADIUS; x <= FARM_RADIUS; x++) {
            for (int y = FARM_MIN_Y; y <= FARM_MAX_Y; y++) {
                for (int z = -FARM_RADIUS; z <= FARM_RADIUS; z++) {
                    farmLayoutConfig.set("blocks." + keyFor(x, y, z),
                            sourceWorld.getBlockAt(x, y, z).getBlockData().getAsString());
                }
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
        if (hasSavedFarmLayout()) {
            applySavedFarmLayout(world);
        } else {
            applyDefaultFarmLayout(world);
        }
    }

    private static void applySavedFarmLayout(World world) {
        for (int x = -FARM_RADIUS; x <= FARM_RADIUS; x++) {
            for (int y = FARM_MIN_Y; y <= FARM_MAX_Y; y++) {
                for (int z = -FARM_RADIUS; z <= FARM_RADIUS; z++) {
                    String serialized = farmLayoutConfig.getString("blocks." + keyFor(x, y, z));
                    BlockData data = (serialized == null || serialized.isBlank())
                            ? Bukkit.createBlockData(getDefaultFarmMaterialAt(x, y, z))
                            : parseBlockData(serialized);
                    world.getBlockAt(x, y, z).setBlockData(data, true);
                }
            }
        }
    }

    private static void applyDefaultFarmLayout(World world) {
        for (int x = -FARM_RADIUS; x <= FARM_RADIUS; x++) {
            for (int y = FARM_MIN_Y; y <= FARM_MAX_Y; y++) {
                for (int z = -FARM_RADIUS; z <= FARM_RADIUS; z++) {
                    Material material = getDefaultFarmMaterialAt(x, y, z);
                    world.getBlockAt(x, y, z).setBlockData(Bukkit.createBlockData(material), true);
                }
            }
        }
    }

    private static Material getDefaultFarmMaterialAt(int x, int y, int z) {
        if (y == FARM_MIN_Y) {
            return Material.DIRT;
        }
        if (y == FARM_BASE_Y) {
            return Material.GRASS_BLOCK;
        }
        if (y == FARM_BASE_Y + 1 && (Math.abs(x) == FARM_RADIUS || Math.abs(z) == FARM_RADIUS)) {
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

    public static boolean isTutorialWorld(World world) {
        return world != null && world.getName().startsWith("sheeptutorial_");
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
        return tutorialCompletedByPlayer.getOrDefault(playerId, false)
                || tutorialBypassedByPlayer.getOrDefault(playerId, false);
    }

    private static void clearTutorialRuntimeState(UUID playerId) {
        if (playerId == null) {
            return;
        }
        tutorialStartedAtByPlayer.remove(playerId);
        lastTutorialReminderTimestampByPlayer.remove(playerId);
    }

    private static void resetTutorialProgress(UUID playerId) {
        if (playerId == null) {
            return;
        }
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

    public static int getQuestPoints(Player player) {
        return player == null ? 0 : questPointsByPlayer.getOrDefault(player.getUniqueId(), 10);
    }

    private static void addQuestPoints(Player player, int amount) {
        if (player == null || amount <= 0) {
            return;
        }
        UUID playerId = player.getUniqueId();
        questPointsByPlayer.put(playerId, getQuestPoints(player) + amount);
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

        long now = System.currentTimeMillis();
        long startedAt = tutorialStartedAtByPlayer.getOrDefault(playerId, now);
        tutorialStartedAtByPlayer.putIfAbsent(playerId, now);
        if (now - startedAt >= TUTORIAL_FAIL_TIMEOUT_MS && hasCompletedBasicTutorialTasks(player)) {
            tutorialBypassedByPlayer.put(playerId, true);
            clearTutorialRuntimeState(playerId);
            player.sendTitle(color("&cTutorial Failed"), color("&7Use /sheepmerge tutorial to retry"), 10, 70, 10);
            player.sendMessage(warning("You finished the basic tutorial, but ran out of time on the rest."));
            player.sendMessage(hint("You were sent to your farm. Use /sheepmerge tutorial to retry anytime."));
            SheepFarmWorldCommand.teleportToFarmWorld(player);
            saveData();
            return;
        }
        if (now - startedAt < TUTORIAL_REMINDER_DELAY_MS) {
            return;
        }

        long lastReminder = lastTutorialReminderTimestampByPlayer.getOrDefault(playerId, 0L);
        if (now - lastReminder < TUTORIAL_REMINDER_REPEAT_MS) {
            return;
        }

        lastTutorialReminderTimestampByPlayer.put(playerId, now);
        player.sendMessage(warning("Finish the tutorial to unlock your actual sheep farm."));
        player.sendMessage(hint(getTutorialNextStepLine(player)));
    }

    public static void recordQuestShear(Player player) {
        updateQuestProgress(player, questShearsByPlayer, questShearsCompleteByPlayer, QUEST_SHEARS_TARGET,
                QUEST_SHEARS_REWARD, "Shearing quest complete", Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
    }

    public static void recordQuestSpawn(Player player) {
        updateQuestProgress(player, questSpawnsByPlayer, questSpawnsCompleteByPlayer, QUEST_SPAWNS_TARGET,
                QUEST_SPAWNS_REWARD, "Spawning quest complete", Sound.BLOCK_NOTE_BLOCK_BELL);
    }

    public static void recordQuestMerge(Player player) {
        updateQuestProgress(player, questMergesByPlayer, questMergesCompleteByPlayer, QUEST_MERGES_TARGET,
                QUEST_MERGES_REWARD, "Merging quest complete", Sound.UI_TOAST_CHALLENGE_COMPLETE);
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
        addQuestPoints(player, reward);
        player.sendMessage(action(completionText + ": +" + reward + " quest points"));
        playSound(player, rewardSound, 1.0f, 1.1f);
        player.getWorld().spawnParticle(org.bukkit.Particle.VILLAGER_HAPPY,
                player.getLocation().add(0, 1.0, 0), 14, 0.35, 0.4, 0.35, 0.02);
    }

    public static void tickActiveAbilities(Player player) {
        if (player == null || !isSheepFarmWorld(player.getWorld())) {
            return;
        }
        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();

        tickAbilityVisual(player, playerId, now, activeLuckyBurstUntilByPlayer, org.bukkit.Particle.END_ROD,
                "Lucky Burst ended");
        tickAbilityVisual(player, playerId, now, activeWoolRushUntilByPlayer, org.bukkit.Particle.CLOUD,
                "Wool Rush ended");
        tickAbilityVisual(player, playerId, now, activeJackpotShearsUntilByPlayer, org.bukkit.Particle.CRIT,
                "Jackpot Shears ended");
        tickAbilityVisual(player, playerId, now, activeAutoMergeUntilByPlayer, org.bukkit.Particle.ENCHANTMENT_TABLE,
                "Auto Merge ended");
        emitAbilityAura(player, playerId, now);
        tickAutoMergeAbility(player, playerId, now);
        updatePointsScoreboard(player);
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

        if (nextRandomEventRollAtMs <= 0L) {
            nextRandomEventRollAtMs = now + RANDOM_EVENT_ROLL_INTERVAL_MS;
            return;
        }
        if (now < nextRandomEventRollAtMs || sheepRainEventEndsAtMs > now) {
            return;
        }

        nextRandomEventRollAtMs = now + RANDOM_EVENT_ROLL_INTERVAL_MS;
        if (RANDOM.nextInt(RANDOM_EVENT_TRIGGER_CHANCE_DENOMINATOR) != 0) {
            return;
        }

        startSheepRainEvent(now);
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

    public static void broadcastRandomGameplayTip() {
        if (plugin == null || plugin.getServer() == null || plugin.getServer().getOnlinePlayers().isEmpty()) {
            return;
        }

        String tip = getNextGameplayTip();
        String message = color("&8[&6SheepMerge Tip&8] &f" + tip);
        for (Player player : plugin.getServer().getOnlinePlayers()) {
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
        double min = -FARM_RADIUS + SHEEP_RAIN_HORIZONTAL_PADDING;
        double max = FARM_RADIUS - SHEEP_RAIN_HORIZONTAL_PADDING;
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
        long until = activeAutoMergeUntilByPlayer.getOrDefault(playerId, 0L);
        if (until <= now) {
            nextAutoMergeAtByPlayer.remove(playerId);
            return;
        }

        long nextAutoMergeAt = nextAutoMergeAtByPlayer.getOrDefault(playerId, 0L);
        if (now < nextAutoMergeAt) {
            return;
        }

        nextAutoMergeAtByPlayer.put(playerId, now + QUEST_AUTO_MERGE_INTERVAL_MS);
        tryAutoMergeOnce(player);
    }

    private static boolean tryAutoMergeOnce(Player player) {
        if (player == null || player.getWorld() == null || !isSheepFarmWorld(player.getWorld())) {
            return false;
        }

        World world = player.getWorld();
        Map<Integer, Sheep> firstByTier = new HashMap<>();
        for (Sheep sheep : world.getEntitiesByClass(Sheep.class)) {
            if (sheep == null || !sheep.isValid() || sheep.isDead()) {
                continue;
            }
            if (sheep.isInsideVehicle()) {
                continue;
            }

            SheepTier tier = getSheepTier(sheep);
            if (tier == null || !tier.hasNext()) {
                continue;
            }

            Sheep first = firstByTier.putIfAbsent(tier.getLevel(), sheep);
            if (first == null || !first.isValid() || first.getUniqueId().equals(sheep.getUniqueId())) {
                continue;
            }

            SheepTier mergedTier = tier.next();
            Location spawnLocation = sheep.getLocation().clone();
            first.remove();
            sheep.remove();

            Sheep mergedSheep = world.spawn(spawnLocation, Sheep.class);
            setSheepTier(mergedSheep, mergedTier);
            mergedSheep.setVelocity(new Vector(0.0D, 0.18D, 0.0D));
            world.spawnParticle(org.bukkit.Particle.VILLAGER_HAPPY,
                    spawnLocation.clone().add(0.0D, 0.5D, 0.0D),
                    10,
                    0.25D,
                    0.25D,
                    0.25D,
                    0.02D);

            if (shouldAnnounceTierUnlock(player, mergedTier)) {
                announceTierUnlock(player, mergedTier);
                markTierUnlockAnnounced(player, mergedTier);
            }
            recordSheepMerge(player);
            recordQuestMerge(player);
            return true;
        }
        return false;
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
        if (isAbilityActive(activeLuckyBurstUntilByPlayer, playerId)) {
            player.getWorld().spawnParticle(org.bukkit.Particle.TOTEM,
                    player.getLocation().add(0.0D, 1.1D, 0.0D),
                    2,
                    0.18D,
                    0.28D,
                    0.18D,
                    0.0D);
            if ((now / 3000L) % 2L == 0L) {
                playSound(player, Sound.BLOCK_AMETHYST_CLUSTER_HIT, 0.25f, 1.8f);
            }
        }

        if (isAbilityActive(activeWoolRushUntilByPlayer, playerId)) {
            player.getWorld().spawnParticle(org.bukkit.Particle.SPORE_BLOSSOM_AIR,
                    player.getLocation().add(0.0D, 0.9D, 0.0D),
                    4,
                    0.22D,
                    0.26D,
                    0.22D,
                    0.01D);
            if ((now / 4000L) % 2L == 0L) {
                playSound(player, Sound.BLOCK_WOOL_PLACE, 0.25f, 1.7f);
            }
        }

        if (isAbilityActive(activeJackpotShearsUntilByPlayer, playerId)) {
            player.getWorld().spawnParticle(org.bukkit.Particle.FIREWORKS_SPARK,
                    player.getLocation().add(0.0D, 1.25D, 0.0D),
                    3,
                    0.25D,
                    0.35D,
                    0.25D,
                    0.01D);
            if ((now / 3000L) % 2L == 1L) {
                playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.2f, 1.9f);
            }
        }

        if (isAbilityActive(activeAutoMergeUntilByPlayer, playerId)) {
            player.getWorld().spawnParticle(org.bukkit.Particle.WAX_ON,
                    player.getLocation().add(0.0D, 1.0D, 0.0D),
                    5,
                    0.22D,
                    0.28D,
                    0.22D,
                    0.02D);
            if ((now / 4000L) % 2L == 1L) {
                playSound(player, Sound.BLOCK_PISTON_CONTRACT, 0.2f, 1.6f);
            }
        }
    }

    public static int getShearShopLevel(Player player) {
        return player == null ? 0 : shearShopLevelByPlayer.getOrDefault(player.getUniqueId(), 0);
    }

    public static int getShearFlatBonus(Player player) {
        return Math.max(0, getShearShopLevel(player));
    }

    public static int getShearPointMultiplier(Player player) {
        int level = getShearShopLevel(player);
        return 1 + (level / 10);
    }

    public static int getShearUpgradeCost(Player player) {
        return getDoubledUpgradeCost(SHEAR_SHOP_BASE_COST, getShearShopLevel(player));
    }

    public static boolean upgradeShearShop(Player player) {
        if (player == null) {
            return false;
        }
        int cost = getShearUpgradeCost(player);
        if (!trySpendPoints(player, cost)) {
            return false;
        }
        shearShopLevelByPlayer.put(player.getUniqueId(), getShearShopLevel(player) + 1);
        saveData();
        return true;
    }

    public static boolean prestige(Player player) {
        if (player == null) {
            return false;
        }
        int current = getPrestigeLevel(player);
        if (current >= PRESTIGE_MAX_LEVEL) {
            return false;
        }
        if (getPlayerPoints(player) < getPrestigeCost(player)) {
            return false;
        }
        if (!trySpendPoints(player, getPrestigeCost(player))) {
            return false;
        }

        int nextPrestige = current + 1;
        prestigeLevelByPlayer.put(player.getUniqueId(), nextPrestige);
        prestigePointsByPlayer.put(player.getUniqueId(), getPrestigePoints(player) + nextPrestige);
        clearPrestigeReminder(player);

        // Reset regular progression purchased with normal points.
        pointsByPlayer.put(player.getUniqueId(), 0);
        extraLimitByPlayer.remove(player.getUniqueId());
        eggSpeedLevelByPlayer.remove(player.getUniqueId());
        woolRegenLevelByPlayer.remove(player.getUniqueId());
        higherTierChanceLevelByPlayer.remove(player.getUniqueId());
        shearShopLevelByPlayer.remove(player.getUniqueId());
        clearMergeReminder(player);
        nextEggTimestampByPlayer.remove(player.getUniqueId());

        World world = player.getWorld();
        if (isSheepFarmWorld(world)) {
            for (Sheep sheep : world.getEntitiesByClass(Sheep.class)) {
                sheep.remove();
            }
            refreshLiveSheepCount(world);
        }

        saveData();
        return true;
    }

    public static int getPrestigeCost(Player player) {
        return getDoubledUpgradeCost(500, getPrestigeLevel(player));
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
        return count;
    }

    private static String getTutorialNextStepLine(Player player) {
        if (player == null) {
            return "Step: Use /sheepmerge tutorial";
        }
        if (getTutorialShearCount(player) < TUTORIAL_SHEAR_TARGET) {
            return "Step: Shear sheep with shears (" + getTutorialShearCount(player) + "/" + TUTORIAL_SHEAR_TARGET
                    + ")";
        }
        if (getTutorialSpawnCount(player) < TUTORIAL_SPAWN_TARGET) {
            return "Step: Spawn sheep with eggs (" + getTutorialSpawnCount(player) + "/" + TUTORIAL_SPAWN_TARGET + ")";
        }
        if (getTutorialMergeCount(player) < TUTORIAL_MERGE_TARGET) {
            return "Step: Merge same-tier sheep <While looking at a close by sheep: SHIFT + RIGHT CLICK> ("
                    + getTutorialMergeCount(player) + "/" + TUTORIAL_MERGE_TARGET + ")";
        }

        UUID playerId = player.getUniqueId();
        if (!tutorialUpgradeOpenedByPlayer.getOrDefault(playerId, false)) {
            return "Step: Run /sheepmerge upgrade";
        }
        if (!tutorialQuestOpenedByPlayer.getOrDefault(playerId, false)) {
            return "Step: In Upgrades, click Quests";
        }
        if (!tutorialQuestUpgradesOpenedByPlayer.getOrDefault(playerId, false)) {
            return "Step: In Quests, click Quest Upgrades";
        }
        if (!tutorialPrestigeOpenedByPlayer.getOrDefault(playerId, false)) {
            return "Step: In Upgrades, click Prestige";
        }
        if (!tutorialAbilityUsedByPlayer.getOrDefault(playerId, false)) {
            return "Step: In Quests, activate any ability";
        }
        if (!tutorialShearShopOpenedByPlayer.getOrDefault(playerId, false)) {
            return "Bonus: In Upgrades, open Shear Shop to learn shear upgrades";
        }
        return "Step: Tutorial complete";
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

        player.sendTitle(color("&eSheepMerge Tutorial"), color("&7Learn the full game flow"), 10, 55, 10);
        player.sendMessage(hint("Tutorial steps:"));
        player.sendMessage(hint("1) Shear 3 sheep, spawn 1 sheep, merge 1 pair."));
        player.sendMessage(hint("2) Run /sheepmerge upgrade, then open Quests."));
        player.sendMessage(hint("3) In Quests, open Quest Upgrades (counts as section 5 fix)."));
        player.sendMessage(hint("4) Open Prestige from Upgrades."));
        player.sendMessage(hint("5) Activate one quest ability."));
        player.sendMessage(hint("Bonus: Open Shear Shop to learn shear upgrades."));
        player.sendMessage(hint(getTutorialProgressLine(player)));
        player.sendMessage(hint(getTutorialNextStepLine(player)));
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
        player.sendMessage(hint(getTutorialProgressLine(player)));
        player.sendMessage(hint(getTutorialNextStepLine(player)));
        checkTutorialCompletion(player);
    }

    public static void markTutorialUpgradeOpened(Player player) {
        markTutorialSection(player, tutorialUpgradeOpenedByPlayer, "Tutorial: Opened Upgrades.");
    }

    public static void markTutorialQuestOpened(Player player) {
        markTutorialSection(player, tutorialQuestOpenedByPlayer, "Tutorial: Opened Quests.");
    }

    public static void markTutorialPrestigeOpened(Player player) {
        markTutorialSection(player, tutorialPrestigeOpenedByPlayer, "Tutorial: Opened Prestige.");
    }

    public static void markTutorialQuestUpgradesOpened(Player player) {
        markTutorialSection(player, tutorialQuestUpgradesOpenedByPlayer, "Tutorial: Opened Quest Upgrades.");
    }

    public static void markTutorialAbilityUsed(Player player) {
        markTutorialSection(player, tutorialAbilityUsedByPlayer, "Tutorial: Activated an ability.");
    }

    public static void markTutorialShearShopOpened(Player player) {
        markTutorialSection(player, tutorialShearShopOpenedByPlayer, "Tutorial: Opened Shear Shop.");
    }

    public static void recordTutorialShear(Player player) {
        if (!isTutorialInProgress(player) || !isInTutorialWorld(player)) {
            return;
        }
        tutorialShearsByPlayer.put(player.getUniqueId(), getTutorialShearCount(player) + 1);
        player.sendMessage(hint(getTutorialProgressLine(player)));
        player.sendMessage(hint(getTutorialNextStepLine(player)));
        checkTutorialCompletion(player);
    }

    public static void recordTutorialSpawn(Player player) {
        if (!isTutorialInProgress(player) || !isInTutorialWorld(player)) {
            return;
        }
        tutorialSpawnsByPlayer.put(player.getUniqueId(), getTutorialSpawnCount(player) + 1);
        player.sendMessage(hint(getTutorialProgressLine(player)));
        player.sendMessage(hint(getTutorialNextStepLine(player)));
        checkTutorialCompletion(player);
    }

    public static void recordTutorialMerge(Player player) {
        if (!isTutorialInProgress(player) || !isInTutorialWorld(player)) {
            return;
        }
        tutorialMergesByPlayer.put(player.getUniqueId(), getTutorialMergeCount(player) + 1);
        player.sendMessage(hint(getTutorialProgressLine(player)));
        player.sendMessage(hint(getTutorialNextStepLine(player)));
        checkTutorialCompletion(player);
    }

    public static String getTutorialProgressLine(Player player) {
        return "Tutorial tasks: Shear " + getTutorialShearCount(player) + "/" + TUTORIAL_SHEAR_TARGET
                + ", Spawn " + getTutorialSpawnCount(player) + "/" + TUTORIAL_SPAWN_TARGET
                + ", Merge " + getTutorialMergeCount(player) + "/" + TUTORIAL_MERGE_TARGET
                + " | Sections " + getTutorialSectionCount(player) + "/" + TUTORIAL_MENU_SECTION_TARGET;
    }

    private static boolean hasCompletedBasicTutorialTasks(Player player) {
        return player != null
                && getTutorialShearCount(player) >= TUTORIAL_SHEAR_TARGET
                && getTutorialSpawnCount(player) >= TUTORIAL_SPAWN_TARGET
                && getTutorialMergeCount(player) >= TUTORIAL_MERGE_TARGET;
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
            player.sendTitle(color("&aTutorial Complete"), color("&7Sending you to your farm"), 10, 60, 10);
            SheepFarmWorldCommand.teleportToFarmWorld(player);
            player.sendMessage(action("Tutorial complete! Use /sheepmerge tutorial anytime to replay."));
            saveData();
        }
    }

    public static boolean adminResetPlayer(Player player) {
        if (player == null) {
            return false;
        }
        UUID id = player.getUniqueId();
        pointsByPlayer.remove(id);
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
        lastPrestigeReminderTimestampByPlayer.remove(id);
        shearShopLevelByPlayer.remove(id);
        resetTutorialProgress(id);
        farmVisitEnabledByPlayer.remove(id);
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
        nextAutoMergeAtByPlayer.remove(id);
        nextEggTimestampByPlayer.remove(id);
        lastSpawnLimitWarningTimestampByPlayer.remove(id);
        saveData();
        return true;
    }

    public static void adminGivePoints(Player player, int amount) {
        if (player == null || amount == 0) {
            return;
        }
        UUID id = player.getUniqueId();
        pointsByPlayer.put(id, Math.max(0, pointsByPlayer.getOrDefault(id, 0) + amount));
        saveData();
    }

    public static void adminSetPoints(Player player, int amount) {
        if (player == null) {
            return;
        }
        pointsByPlayer.put(player.getUniqueId(), Math.max(0, amount));
        saveData();
    }

    public static void adminGiveQuestPoints(Player player, int amount) {
        if (player == null || amount == 0) {
            return;
        }
        UUID id = player.getUniqueId();
        questPointsByPlayer.put(id, Math.max(0, questPointsByPlayer.getOrDefault(id, 0) + amount));
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
        if (player == null || targetLevel < 0 || targetLevel > PRESTIGE_MAX_LEVEL) {
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
        sheep.setColor(tier.getColor() == null ? org.bukkit.DyeColor.WHITE : tier.getColor());
        sheep.getPersistentDataContainer().set(getTierKey(), PersistentDataType.INTEGER, tier.getLevel());
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
        double multiplier = Math.pow(0.85, regenLevel);
        UUID ownerId = getOwnerId(sheep.getWorld());
        if (isAbilityActive(activeWoolRushUntilByPlayer, ownerId)) {
            int power = ownerId == null ? 0 : questUpgradePowerByPlayer.getOrDefault(ownerId, 0);
            multiplier *= Math.max(0.2, 0.55 - power * 0.05);
        }
        return Math.max(1, (int) Math.ceil(baseSeconds * multiplier));
    }

    public static void processSheepEatTimer(Sheep sheep) {
        if (sheep == null || !sheep.isValid() || sheep.getWorld() == null
                || !isSheepFarmWorld(sheep.getWorld())) {
            return;
        }

        boolean rescueActive = applySheepRescueMotionIfNeeded(sheep);
        applyRainbowColorAnimation(sheep, getSheepTier(sheep));

        if (!sheep.isSheared()) {
            updateSheepName(sheep);
            return;
        }

        long now = System.currentTimeMillis();
        long nextEat = getNextEatTimestamp(sheep);
        if (now >= nextEat && nextEat > 0L) {
            sheep.setSheared(false);
            sheep.setAI(true);
            setNextEatTimestamp(sheep, 0L);
        } else {
            sheep.setSheared(true);
            sheep.setGravity(true);
            boolean shouldKeepAiEnabled = rescueActive || !sheep.isOnGround();
            sheep.setAI(shouldKeepAiEnabled);
        }
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
            sheepRescueStartByEntity.remove(sheepId);
            sheep.setCollidable(true);
            return false;
        }

        // Let rescued sheep phase through collisions while returning to center.
        sheep.setCollidable(false);
        sheep.setAI(true);
        sheep.setGravity(true);

        long now = System.currentTimeMillis();
        long started = sheepRescueStartByEntity.getOrDefault(sheepId, now);
        sheepRescueStartByEntity.putIfAbsent(sheepId, now);
        if (now - started >= SHEEP_RESCUE_TIMEOUT_MS) {
            teleportSheepToFarmCenter(sheep);
            sheepRescueStartByEntity.remove(sheepId);
            sheep.setCollidable(true);
            return false;
        }

        if (now - started < SHEEP_RESCUE_INITIAL_YEET_MS) {
            Vector awayFromCenter = new Vector(location.getX() - 0.5D, 0.0D, location.getZ() - 0.5D);
            double horizontalDistance = Math.sqrt(awayFromCenter.getX() * awayFromCenter.getX()
                    + awayFromCenter.getZ() * awayFromCenter.getZ());
            if (horizontalDistance < 0.0001D) {
                awayFromCenter = sheep.getVelocity().clone().setY(0.0D);
                horizontalDistance = Math.sqrt(awayFromCenter.getX() * awayFromCenter.getX()
                        + awayFromCenter.getZ() * awayFromCenter.getZ());
            }
            if (horizontalDistance > 0.0001D) {
                awayFromCenter.normalize().multiply(SHEEP_RESCUE_INITIAL_YEET_HORIZONTAL_VELOCITY);
                sheep.setVelocity(new Vector(
                        awayFromCenter.getX(),
                        SHEEP_RESCUE_INITIAL_YEET_UPWARD_VELOCITY,
                        awayFromCenter.getZ()));
                sheep.setFallDistance(0.0F);
                return true;
            }
        }

        Vector toCenter = new Vector(0.5D - location.getX(), 0.0D, 0.5D - location.getZ());
        double horizontalDistance = Math.sqrt(toCenter.getX() * toCenter.getX() + toCenter.getZ() * toCenter.getZ());
        if (horizontalDistance > 0.0001D) {
            double horizontalSpeed = Math.min(SHEEP_RESCUE_HORIZONTAL_VELOCITY, 0.18D + horizontalDistance * 0.07D);
            toCenter.normalize().multiply(horizontalSpeed);
        }

        double belowTarget = Math.max(0.0D, (FARM_BASE_Y + 1.0D) - location.getY());
        double upwardVelocity = 0.20D + Math.min(0.55D, horizontalDistance * 0.08D + belowTarget * 0.22D);
        upwardVelocity = Math.min(SHEEP_RESCUE_UPWARD_VELOCITY, upwardVelocity);

        // Apply rescue steering every tick for smooth arching flight toward center.
        sheep.setVelocity(new Vector(toCenter.getX(), upwardVelocity, toCenter.getZ()));
        sheep.setFallDistance(0.0F);
        return true;
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
        return Math.abs(location.getX()) <= FARM_RADIUS - 0.20D
                && Math.abs(location.getZ()) <= FARM_RADIUS - 0.20D;
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
                0.5D,
                FARM_BASE_Y + 1.0D,
                0.5D,
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
        boolean nearEdge = Math.abs(location.getX()) > FARM_RADIUS - SHEEP_FALL_TRIGGER_EDGE_MARGIN
                || Math.abs(location.getZ()) > FARM_RADIUS - SHEEP_FALL_TRIGGER_EDGE_MARGIN;
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
        return Math.abs(location.getX()) > FARM_RADIUS + SHEEP_RESCUE_EDGE_MARGIN
                || Math.abs(location.getZ()) > FARM_RADIUS + SHEEP_RESCUE_EDGE_MARGIN;
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
        String name = tier.getDisplayName();
        if (sheep.isSheared()) {
            long remainingSeconds = Math.max(0L,
                    (getNextEatTimestamp(sheep) - System.currentTimeMillis() + 999L) / 1000L);
            name += " - " + remainingSeconds + "s";
        }
        sheep.setCustomName(name);
        sheep.setCustomNameVisible(true);
    }

    public static int getPlayerPoints(Player player) {
        return pointsByPlayer.getOrDefault(player.getUniqueId(), 0);
    }

    public static int calculateShearPoints(Player player, SheepTier tier) {
        int base = tier == null ? 1 : tier.getPointsOnShear();
        int points = base * getShearPointMultiplier(player) + getShearFlatBonus(player);
        if (isAbilityActive(activeJackpotShearsUntilByPlayer, player == null ? null : player.getUniqueId())) {
            points *= (2 + getQuestUpgradePowerLevel(player));
        }
        if (RANDOM.nextInt(100) < getDoublePointsChancePercent(player)) {
            points *= 2;
        }
        return Math.max(1, points);
    }

    public static int getWoolDropAmount(Player player) {
        return 1 + getShearShopLevel(player);
    }

    public static String buildTopPointsText(int maxEntries) {
        StringBuilder builder = new StringBuilder("Top Sheep Merge Points");
        pointsByPlayer.entrySet().stream()
                .sorted((left, right) -> Integer.compare(right.getValue(), left.getValue()))
                .limit(Math.max(1, maxEntries))
                .forEach(entry -> {
                    String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
                    if (name == null || name.isBlank()) {
                        name = entry.getKey().toString().substring(0, 8);
                    }
                    builder.append("\n").append(name).append(": ").append(entry.getValue());
                });
        return builder.toString();
    }

    public static boolean spawnOrMoveTopPointsDisplay(Player player) {
        if (player == null || player.getWorld() == null) {
            return false;
        }

        World world = player.getWorld();
        Location location = player.getLocation().clone().add(0, 2.2, 0);
        TextDisplay display = findTopPointsDisplay(world);
        if (display == null) {
            display = world.spawn(location, TextDisplay.class);
            display.getPersistentDataContainer().set(getTopPointsDisplayKey(), PersistentDataType.BYTE, (byte) 1);
            display.setBillboard(Display.Billboard.CENTER);
            display.setSeeThrough(true);
            display.setDefaultBackground(false);
            display.setShadowed(true);
            display.setLineWidth(260);
        } else {
            display.teleport(location);
        }

        display.setText(buildTopPointsText(10));
        return true;
    }

    private static TextDisplay findTopPointsDisplay(World world) {
        if (world == null) {
            return null;
        }
        for (TextDisplay display : world.getEntitiesByClass(TextDisplay.class)) {
            Byte marker = display.getPersistentDataContainer().get(getTopPointsDisplayKey(), PersistentDataType.BYTE);
            if (marker != null && marker == (byte) 1) {
                return display;
            }
        }
        return null;
    }

    public static void addPoints(Player player, int points) {
        if (player == null || points <= 0) {
            return;
        }
        UUID playerId = player.getUniqueId();
        pointsByPlayer.put(playerId, pointsByPlayer.getOrDefault(playerId, 0) + points);
        saveData();
        tickPrestigeReminder(player);
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
    }

    public static void clearMergeReminder(Player player) {
        if (player == null) {
            return;
        }
        UUID playerId = player.getUniqueId();
        lastMergeTimestampByPlayer.remove(playerId);
        lastMergeReminderTimestampByPlayer.remove(playerId);
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
        if (getPrestigeLevel(player) >= PRESTIGE_MAX_LEVEL) {
            clearPrestigeReminder(player);
            return;
        }

        int prestigeCost = getPrestigeCost(player);
        if (getPlayerPoints(player) < prestigeCost) {
            clearPrestigeReminder(player);
            return;
        }

        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();
        long lastReminder = lastPrestigeReminderTimestampByPlayer.getOrDefault(playerId, 0L);
        if (now - lastReminder < 20_000L) {
            return;
        }

        player.sendTitle(
                color("&ePrestige ready"),
                color("&7Use /sheepmerge prestige"),
                10,
                60,
                10);
        lastPrestigeReminderTimestampByPlayer.put(playerId, now);
    }

    public static void recordSheepMerge(Player player) {
        if (player == null) {
            return;
        }
        UUID playerId = player.getUniqueId();
        lastMergeTimestampByPlayer.put(playerId, System.currentTimeMillis());
        lastMergeReminderTimestampByPlayer.remove(playerId);
    }

    public static void announceTierUnlock(Player player, SheepTier tier) {
        if (player == null || tier == null) {
            return;
        }

        String message = getTierUnlockMessage(player, tier);
        if (plugin != null && plugin.getServer() != null) {
            plugin.getServer().broadcastMessage(message);
        } else {
            player.sendMessage(message);
        }

        playTierUnlockSound(player, tier);
    }

    public static boolean shouldAnnounceTierUnlock(Player player, SheepTier tier) {
        if (player == null || tier == null) {
            return false;
        }
        int highestAnnounced = highestAnnouncedTierByPlayer.getOrDefault(player.getUniqueId(),
                SheepTier.WHITE.getLevel());
        return tier.getLevel() > highestAnnounced;
    }

    public static void markTierUnlockAnnounced(Player player, SheepTier tier) {
        if (player == null || tier == null) {
            return;
        }
        UUID playerId = player.getUniqueId();
        int highestAnnounced = highestAnnouncedTierByPlayer.getOrDefault(playerId, SheepTier.WHITE.getLevel());
        if (tier.getLevel() <= highestAnnounced) {
            return;
        }
        highestAnnouncedTierByPlayer.put(playerId, tier.getLevel());
        saveData();
    }

    private static String getTierUnlockMessage(Player player, SheepTier tier) {
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
            case RAINBOW -> color(
                    "&8[&6SheepMerge&8] &e" + playerName + " &7unlocked &dRainbow Sheep&b! &7Legendary tier reached!");
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
        player.sendTitle(
                color("&eMerge sheep"),
                color("&7Sneak-right-click one sheep, then right-click the same tier"),
                10,
                60,
                10);
        lastMergeReminderTimestampByPlayer.put(playerId, now);
    }

    public static void enforceFarmLoadout(Player player) {
        if (player == null || !isSheepFarmWorld(player.getWorld())) {
            return;
        }

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

            if (itemStack == null) {
                continue;
            }

            Material type = itemStack.getType();
            if (type == Material.SHEARS || type == Material.NETHER_STAR) {
                storageContents[slot] = null;
                storageChanged = true;
            }
        }

        if (storageChanged) {
            inventory.setStorageContents(storageContents);
        }

        ItemStack offHand = inventory.getItemInOffHand();
        if (offHand == null || offHand.getType() != Material.SHEARS || offHand.getAmount() != 1) {
            inventory.setItemInOffHand(getSheepMergeShears());
        }
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

    public static boolean isSheepMergeUpgradeCommandItem(ItemStack itemStack) {
        return itemStack != null && itemStack.getType() == Material.NETHER_STAR;
    }

    public static boolean isForcedFarmLoadoutItem(ItemStack itemStack) {
        return itemStack != null
                && (itemStack.getType() == Material.SHEARS || isSheepMergeUpgradeCommandItem(itemStack));
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

    public static boolean trySpendPoints(Player player, int points) {
        if (player == null || points <= 0) {
            return false;
        }
        UUID uuid = player.getUniqueId();
        int current = pointsByPlayer.getOrDefault(uuid, 0);
        if (current < points) {
            return false;
        }
        pointsByPlayer.put(uuid, current - points);
        saveData();
        return true;
    }

    public static int getPlayerLimit(Player player) {
        if (player == null) {
            return BASE_SHEEP_LIMIT;
        }
        return BASE_SHEEP_LIMIT
                + extraLimitByPlayer.getOrDefault(player.getUniqueId(), 0);
    }

    public static int getOwnerLimit(World world) {
        UUID ownerId = getOwnerId(world);
        if (ownerId == null) {
            return BASE_SHEEP_LIMIT;
        }
        return BASE_SHEEP_LIMIT
                + extraLimitByPlayer.getOrDefault(ownerId, 0);
    }

    public static int getUpgradeCost(Player player) {
        return getDoubledUpgradeCost(LIMIT_UPGRADE_COST, getLimitUpgradeLevel(player));
    }

    public static int getLimitUpgradeStep() {
        return LIMIT_UPGRADE_STEP;
    }

    public static boolean upgradeLimit(Player player) {
        if (player == null) {
            return false;
        }
        int cost = getUpgradeCost(player);
        if (!trySpendPoints(player, cost)) {
            return false;
        }
        UUID playerId = player.getUniqueId();
        extraLimitByPlayer.put(playerId, extraLimitByPlayer.getOrDefault(playerId, 0) + LIMIT_UPGRADE_STEP);
        saveData();
        return true;
    }

    public static int getLimitUpgradeLevel(Player player) {
        if (player == null) {
            return 0;
        }
        return extraLimitByPlayer.getOrDefault(player.getUniqueId(), 0) / LIMIT_UPGRADE_STEP;
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
        return Math.max(MIN_EGG_INTERVAL_SECONDS,
                BASE_EGG_INTERVAL_SECONDS - eggSpeedLevelByPlayer.getOrDefault(player.getUniqueId(), 0));
    }

    public static int getEggSpeedMaxLevel(Player player) {
        // Egg speed reaches its real stat cap once MIN_EGG_INTERVAL_SECONDS is reached.
        return EGG_SPEED_MAX_LEVEL;
    }

    public static int getWoolRegenMaxLevel(Player player) {
        return WOOL_REGEN_MAX_LEVEL + getPrestigeHigherMaxLevel(player) * 2;
    }

    public static int getHigherTierChanceMaxLevel(Player player) {
        int boostedMax = HIGHER_TIER_CHANCE_MAX_LEVEL + getPrestigeHigherMaxLevel(player) * 2;
        return Math.min(HIGHER_TIER_CHANCE_HARD_MAX_LEVEL, boostedMax);
    }

    public static int getWoolRegenLevel(Player player) {
        if (player == null) {
            return 0;
        }
        return woolRegenLevelByPlayer.getOrDefault(player.getUniqueId(), 0);
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
        int base = higherTierChanceLevelByPlayer.getOrDefault(player.getUniqueId(), 0) * 5;
        if (isAbilityActive(activeLuckyBurstUntilByPlayer, player.getUniqueId())) {
            base += 25 + getQuestUpgradePowerLevel(player) * 5;
        }
        return Math.min(100, base);
    }

    public static int getHigherTierChancePercent(World world) {
        UUID ownerId = getOwnerId(world);
        if (ownerId == null) {
            return 0;
        }
        int base = higherTierChanceLevelByPlayer.getOrDefault(ownerId, 0) * 5;
        if (isAbilityActive(activeLuckyBurstUntilByPlayer, ownerId)) {
            int power = questUpgradePowerByPlayer.getOrDefault(ownerId, 0);
            base += 25 + power * 5;
        }
        return Math.min(100, base);
    }

    public static SheepTier rollSpawnTier(World world) {
        int cap = getUnlockedTierCap(world);
        int baseTierLevel = getBaseSpawnTierLevel(world);
        int chosen = Math.min(baseTierLevel, cap);
        int chance = getHigherTierChancePercent(world);
        while (chosen < cap && RANDOM.nextInt(100) < chance) {
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
        if (player == null || !isSheepFarmWorld(player.getWorld())) {
            return;
        }

        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long nextTimestamp = nextEggTimestampByPlayer.get(playerId);
        if (nextTimestamp == null) {
            nextEggTimestampByPlayer.put(playerId, now + getEggIntervalSeconds(player) * 1000L);
            updateEggHud(player);
            return;
        }

        long next = nextTimestamp;
        if (now < next) {
            updateEggHud(player);
            return;
        }

        if (getEggCount(player) >= getEggCap(player)) {
            nextEggTimestampByPlayer.put(playerId, now + 2000L);
            updateEggHud(player);
            return;
        }

        if (player.getInventory().firstEmpty() == -1) {
            nextEggTimestampByPlayer.put(playerId, now + 2000L);
            updateEggHud(player);
            return;
        }

        ItemStack egg = new ItemStack(Material.SHEEP_SPAWN_EGG, 1);
        player.getInventory().addItem(egg);
        showOverlay(player, action("+1 egg"));
        nextEggTimestampByPlayer.put(playerId, now + getEggIntervalSeconds(player) * 1000L);
        updateEggHud(player);
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

    private static int getEggCount(Player player) {
        if (player == null) {
            return 0;
        }
        int total = 0;
        for (ItemStack itemStack : player.getInventory().all(Material.SHEEP_SPAWN_EGG).values()) {
            if (itemStack != null) {
                total += itemStack.getAmount();
            }
        }
        return total;
    }

    public static int getPrestigeDoublePointsChanceLevel(Player player) {
        return player == null ? 0 : prestigeDoublePointsChanceByPlayer.getOrDefault(player.getUniqueId(), 0);
    }

    public static int getDoublePointsChancePercent(Player player) {
        return Math.min(100, getPrestigeDoublePointsChanceLevel(player) * 5);
    }

    public static int getPrestigeHigherMaxLevel(Player player) {
        if (player == null) {
            return 0;
        }
        int raw = prestigeHigherMaxLevelByPlayer.getOrDefault(player.getUniqueId(), 0);
        return Math.min(PRESTIGE_HIGHER_MAX_LEVEL_HARD_CAP, raw);
    }

    public static int getPrestigeStartEggsLevel(Player player) {
        return player == null ? 0 : prestigeStartEggsByPlayer.getOrDefault(player.getUniqueId(), 0);
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
        return getDoubledUpgradeCost(PRESTIGE_DOUBLE_POINTS_BASE_COST, getPrestigeDoublePointsChanceLevel(player));
    }

    public static int getPrestigeHigherMaxLevelCost(Player player) {
        return getDoubledUpgradeCost(PRESTIGE_HIGHER_MAX_LEVEL_BASE_COST, getPrestigeHigherMaxLevel(player));
    }

    public static int getPrestigeStartEggsCost(Player player) {
        return getDoubledUpgradeCost(PRESTIGE_START_EGGS_BASE_COST, getPrestigeStartEggsLevel(player));
    }

    public static int getPrestigeEggCapCost(Player player) {
        return getDoubledUpgradeCost(PRESTIGE_EGG_CAP_BASE_COST, getPrestigeEggCapLevel(player));
    }

    public static int getPrestigeBaseSpawnTierCost(Player player) {
        return getDoubledUpgradeCost(PRESTIGE_BASE_SPAWN_TIER_BASE_COST, getBaseSpawnTierLevel(player));
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

    private static boolean isAbilityActive(Map<UUID, Long> activeUntil, UUID playerId) {
        if (playerId == null) {
            return false;
        }
        return activeUntil.getOrDefault(playerId, 0L) > System.currentTimeMillis();
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

    private static int getTotalPrestigePointsForLevel(int prestigeLevel) {
        if (prestigeLevel <= 0) {
            return 0;
        }
        long total = (long) prestigeLevel * (prestigeLevel + 1L) / 2L;
        return total >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;
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
        if (clearRefundCooldown) {
            nextPrestigeRefundTimestampByPlayer.remove(playerId);
        }
    }

    private static int getPrestigeRefundAmount(Player player) {
        if (player == null) {
            return 0;
        }
        int total = 0;
        total += getSpentCostForLevel(PRESTIGE_DOUBLE_POINTS_BASE_COST, getPrestigeDoublePointsChanceLevel(player));
        total += getSpentCostForLevel(PRESTIGE_HIGHER_MAX_LEVEL_BASE_COST, getPrestigeHigherMaxLevel(player));
        total += getSpentCostForLevel(PRESTIGE_START_EGGS_BASE_COST, getPrestigeStartEggsLevel(player));
        total += getSpentCostForLevel(PRESTIGE_EGG_CAP_BASE_COST, getPrestigeEggCapLevel(player));
        total += getSpentCostForLevel(PRESTIGE_BASE_SPAWN_TIER_BASE_COST, getBaseSpawnTierLevel(player));
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
        prestigePointsByPlayer.put(playerId, getPrestigePoints(player) + refund);
        resetPrestigeUpgrades(playerId, false);
        nextPrestigeRefundTimestampByPlayer.put(playerId, now + PRESTIGE_REFUND_COOLDOWN_MS);
        saveData();
        return true;
    }

    private static String formatDuration(long durationMs) {
        long totalSeconds = Math.max(0L, durationMs / 1000L);
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return minutes + "m " + seconds + "s";
    }

    private static long getAbilityRemainingMs(Map<UUID, Long> activeUntil, UUID playerId) {
        if (activeUntil == null || playerId == null) {
            return 0L;
        }
        return Math.max(0L, activeUntil.getOrDefault(playerId, 0L) - System.currentTimeMillis());
    }

    private static String getAbilityMenuStatus(Map<UUID, Long> activeUntil, UUID playerId) {
        long remaining = getAbilityRemainingMs(activeUntil, playerId);
        return remaining > 0L ? "Time left: " + formatDuration(remaining) : "Time left: ready";
    }

    private static String getAbilityScoreLine(String label, Map<UUID, Long> activeUntil, UUID playerId) {
        long remaining = getAbilityRemainingMs(activeUntil, playerId);
        return label + ": " + (remaining > 0L ? formatDuration(remaining) : "ready");
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

    private static boolean upgradePrestigeDoublePoints(Player player) {
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
        if (getPrestigeHigherMaxLevel(player) >= PRESTIGE_HIGHER_MAX_LEVEL_HARD_CAP) {
            return false;
        }
        int cost = getPrestigeHigherMaxLevelCost(player);
        if (!trySpendPrestigePoints(player, cost)) {
            return false;
        }
        prestigeHigherMaxLevelByPlayer.put(player.getUniqueId(), getPrestigeHigherMaxLevel(player) + 1);
        saveData();
        return true;
    }

    private static boolean upgradePrestigeStartEggs(Player player) {
        int cost = getPrestigeStartEggsCost(player);
        if (!trySpendPrestigePoints(player, cost)) {
            return false;
        }
        prestigeStartEggsByPlayer.put(player.getUniqueId(), getPrestigeStartEggsLevel(player) + 1);
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

    public static void resetEggTimer(Player player) {
        if (player == null) {
            return;
        }
        nextEggTimestampByPlayer.put(player.getUniqueId(),
                System.currentTimeMillis() + getEggIntervalSeconds(player) * 1000L);
        updateEggHud(player);
    }

    public static void clearEggTimer(Player player) {
        if (player == null) {
            return;
        }
        nextEggTimestampByPlayer.remove(player.getUniqueId());
        restoreSavedExperience(player);
    }

    private static void updateEggHud(Player player) {
        if (player == null || !isSheepFarmWorld(player.getWorld())) {
            return;
        }

        saveExperienceStateIfNeeded(player);
        int eggCount = getEggCount(player);
        int eggCap = getEggCap(player);
        player.setLevel(eggCount);

        if (eggCount >= eggCap) {
            player.setExp(1.0f);
            return;
        }

        long now = System.currentTimeMillis();
        long intervalMs = Math.max(1000L, getEggIntervalSeconds(player) * 1000L);
        long next = nextEggTimestampByPlayer.getOrDefault(player.getUniqueId(), now + intervalMs);
        long remainingMs = Math.max(0L, next - now);
        float progress = 1.0f - Math.min(1.0f, remainingMs / (float) intervalMs);
        player.setExp(Math.max(0.0f, Math.min(1.0f, progress)));
    }

    private static void saveExperienceStateIfNeeded(Player player) {
        if (player == null) {
            return;
        }
        UUID playerId = player.getUniqueId();
        if (savedLevels.containsKey(playerId)) {
            return;
        }
        savedLevels.put(playerId, player.getLevel());
        savedExpProgress.put(playerId, player.getExp());
    }

    private static void restoreSavedExperience(Player player) {
        if (player == null) {
            return;
        }
        UUID playerId = player.getUniqueId();
        Integer savedLevel = savedLevels.remove(playerId);
        Float savedExp = savedExpProgress.remove(playerId);
        if (savedLevel == null || savedExp == null) {
            return;
        }
        player.setLevel(savedLevel);
        player.setExp(savedExp);
    }

    public static void openUpgradeMenu(Player player) {
        if (player == null) {
            return;
        }
        markTutorialUpgradeOpened(player);

        Inventory inventory = Bukkit.createInventory(null, 27, UPGRADE_MENU_TITLE);
        int limitLevel = getLimitUpgradeLevel(player);
        int limitCost = getUpgradeCost(player);
        inventory.setItem(LIMIT_UPGRADE_SLOT, MenuItemFactory.create(
                Material.OAK_FENCE,
                "Sheep Limit",
                List.of(
                        "Level: " + limitLevel,
                        "Current limit: " + getPlayerLimit(player),
                        "Increase by: +" + getLimitUpgradeStep(),
                        "Cost: " + limitCost + " points",
                        "Click to purchase")));

        int eggLevel = getEggSpeedLevel(player);
        int eggCost = getEggSpeedUpgradeCost(player);
        inventory.setItem(EGG_SPEED_UPGRADE_SLOT, MenuItemFactory.create(
                Material.CLOCK,
                "Faster Egg Spawn",
                List.of(
                        "Level: " + eggLevel + " / " + getEggSpeedMaxLevel(player),
                        "Egg interval: " + getEggIntervalSeconds(player) + "s",
                        eggLevel >= getEggSpeedMaxLevel(player) ? "MAXED" : "Cost: " + eggCost + " points",
                        "Click to purchase")));

        int woolLevel = woolRegenLevelByPlayer.getOrDefault(player.getUniqueId(), 0);
        int woolCost = getWoolRegenUpgradeCost(player);
        inventory.setItem(WOOL_REGEN_UPGRADE_SLOT, MenuItemFactory.createEnchanted(
                Material.WHITE_WOOL,
                "Faster Wool Regen",
                List.of(
                        "Level: " + woolLevel + " / " + getWoolRegenMaxLevel(player),
                        "Effect: -15% cooldown per level",
                        woolLevel >= getWoolRegenMaxLevel(player) ? "MAXED" : "Cost: " + woolCost + " points",
                        "Click to purchase")));

        int chanceLevel = higherTierChanceLevelByPlayer.getOrDefault(player.getUniqueId(), 0);
        int chanceCost = getHigherTierChanceUpgradeCost(player);
        inventory.setItem(HIGHER_TIER_CHANCE_UPGRADE_SLOT, MenuItemFactory.create(
                Material.GOLDEN_APPLE,
                "Higher Tier Spawn Chance",
                List.of(
                        "Level: " + chanceLevel + " / " + getHigherTierChanceMaxLevel(player),
                        "Chance: " + getHigherTierChancePercent(player) + "%",
                        chanceLevel >= getHigherTierChanceMaxLevel(player) ? "MAXED"
                                : "Cost: " + chanceCost + " points",
                        "Click to purchase")));

        inventory.setItem(PRESTIGE_MENU_OPEN_SLOT, MenuItemFactory.create(
                Material.NETHER_STAR,
                "Prestige Upgrades",
                List.of(
                        "Prestige level: " + getPrestigeLevel(player),
                        "Prestige points: " + getPrestigePoints(player),
                        "Click to open")));

        inventory.setItem(QUEST_MENU_OPEN_SLOT, MenuItemFactory.create(
                Material.BOOK,
                "Quests",
                List.of(
                        "Quest points: " + getQuestPoints(player),
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
                        "Flat bonus: +" + getShearFlatBonus(player),
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

    public static void handleUpgradeMenuClick(Player player, int slot) {
        if (player == null) {
            return;
        }
        switch (slot) {
            case LIMIT_UPGRADE_SLOT -> {
                if (upgradeLimit(player)) {
                    playUpgradeSound(player);
                    player.sendMessage(action("Limit up: " + getPlayerLimit(player)));
                } else {
                    player.sendMessage(warning("Not enough points."));
                }
            }
            case EGG_SPEED_UPGRADE_SLOT -> {
                if (upgradeEggSpeed(player)) {
                    playUpgradeSound(player);
                    player.sendMessage(action("Eggs: every " + getEggIntervalSeconds(player) + "s"));
                } else {
                    player.sendMessage(warning("Not enough points."));
                }
            }
            case WOOL_REGEN_UPGRADE_SLOT -> {
                int level = woolRegenLevelByPlayer.getOrDefault(player.getUniqueId(), 0);
                if (level >= getWoolRegenMaxLevel(player)) {
                    player.sendMessage(warning("Wool regen maxed."));
                    break;
                }
                if (upgradeWoolRegen(player)) {
                    playUpgradeSound(player);
                    player.sendMessage(action("Wool regen up"));
                } else {
                    player.sendMessage(warning("Not enough points."));
                }
            }
            case HIGHER_TIER_CHANCE_UPGRADE_SLOT -> {
                int level = higherTierChanceLevelByPlayer.getOrDefault(player.getUniqueId(), 0);
                if (level >= getHigherTierChanceMaxLevel(player)) {
                    player.sendMessage(warning("Spawn chance maxed."));
                    break;
                }
                if (upgradeHigherTierChance(player)) {
                    playUpgradeSound(player);
                    player.sendMessage(action("Spawn chance: " + getHigherTierChancePercent(player) + "%"));
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
        Inventory inventory = Bukkit.createInventory(null, 27, PRESTIGE_MENU_TITLE);
        inventory.setItem(PRESTIGE_UPGRADE_SLOT, MenuItemFactory.create(
                Material.NETHER_STAR,
                "Prestige Reset",
                List.of(
                        "Current prestige: " + getPrestigeLevel(player),
                        "Gain prestige points: +" + (getPrestigeLevel(player) + 1),
                        "Cost: " + getPrestigeCost(player) + " normal points",
                        "Resets normal-point upgrades",
                        "Click to prestige")));

        inventory.setItem(PRESTIGE_DOUBLE_POINTS_SLOT, MenuItemFactory.create(
                Material.EMERALD,
                "Double Points Chance",
                List.of(
                        "Level: " + getPrestigeDoublePointsChanceLevel(player),
                        "Chance: " + getDoublePointsChancePercent(player) + "%",
                        "Cost: " + getPrestigeDoublePointsCost(player) + " prestige points",
                        "Click to purchase")));

        inventory.setItem(PRESTIGE_HIGHER_MAX_LEVEL_SLOT, MenuItemFactory.create(
                Material.ENCHANTED_BOOK,
                "Higher Maximum Levels",
                List.of(
                        "Level: " + getPrestigeHigherMaxLevel(player) + " / " + PRESTIGE_HIGHER_MAX_LEVEL_HARD_CAP,
                        "Tier cap bonus: +" + (getPrestigeHigherMaxLevel(player) * 2),
                        getPrestigeHigherMaxLevel(player) >= PRESTIGE_HIGHER_MAX_LEVEL_HARD_CAP
                                ? "MAXED"
                                : "Cost: " + getPrestigeHigherMaxLevelCost(player) + " prestige points",
                        "Click to purchase")));

        inventory.setItem(PRESTIGE_START_EGGS_SLOT, MenuItemFactory.create(
                Material.SHEEP_SPAWN_EGG,
                "Start With Extra Eggs",
                List.of(
                        "Level: " + getPrestigeStartEggsLevel(player),
                        "Extra starting eggs: +" + getStartEggsBonus(player),
                        "Cost: " + getPrestigeStartEggsCost(player) + " prestige points",
                        "Click to purchase")));

        inventory.setItem(PRESTIGE_EGG_CAP_SLOT, MenuItemFactory.create(
                Material.EGG,
                "Egg Capacity",
                List.of(
                        "Level: " + getPrestigeEggCapLevel(player),
                        "Egg cap: " + getEggCap(player),
                        "Adds: +" + PRESTIGE_EGG_CAP_STEP + " eggs per level",
                        "Cost: " + getPrestigeEggCapCost(player) + " prestige points",
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
                                : "Cost: " + getPrestigeBaseSpawnTierCost(player) + " prestige points",
                        "Click to purchase")));

        long refundRemaining = getPrestigeRefundRemainingMs(player);
        int refundAmount = getPrestigeRefundAmount(player);
        inventory.setItem(PRESTIGE_REFUND_SLOT, MenuItemFactory.create(
                Material.BARRIER,
                "Refund Prestige Upgrades",
                List.of(
                        "Refund amount: " + refundAmount + " prestige points",
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
                if (prestige(player)) {
                    playPrestigeSound(player);
                    player.sendMessage(action("Prestige +1"));
                } else {
                    player.sendMessage(warning("Not enough points."));
                }
            }
            case PRESTIGE_DOUBLE_POINTS_SLOT -> {
                if (upgradePrestigeDoublePoints(player)) {
                    playUpgradeSound(player);
                    player.sendMessage(action("Double points: " + getDoublePointsChancePercent(player) + "%"));
                } else {
                    player.sendMessage(warning("Not enough prestige points."));
                }
            }
            case PRESTIGE_HIGHER_MAX_LEVEL_SLOT -> {
                if (getPrestigeHigherMaxLevel(player) >= PRESTIGE_HIGHER_MAX_LEVEL_HARD_CAP) {
                    player.sendMessage(warning("Higher max levels are capped."));
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
                if (upgradePrestigeStartEggs(player)) {
                    playUpgradeSound(player);
                    player.sendMessage(action("Start eggs up"));
                } else {
                    player.sendMessage(warning("Not enough prestige points."));
                }
            }
            case PRESTIGE_EGG_CAP_SLOT -> {
                if (upgradePrestigeEggCap(player)) {
                    playUpgradeSound(player);
                    player.sendMessage(action("Egg cap: " + getEggCap(player)));
                } else {
                    player.sendMessage(warning("Not enough prestige points."));
                }
            }
            case PRESTIGE_BASE_SPAWN_TIER_SLOT -> {
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
            case PRESTIGE_REFUND_SLOT -> {
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
                    player.sendMessage(action("Refunded " + refundAmount + " prestige points."));
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
                        "Quest points: " + getQuestPoints(player),
                        remaining > 0L ? "Next reset: " + formatDuration(remaining) : "Next reset: incoming",
                        (shearsComplete ? "DONE " : "TODO ")
                                + "Shear " + questShearsByPlayer.getOrDefault(playerId, 0) + "/" + QUEST_SHEARS_TARGET
                                + " (" + QUEST_SHEARS_REWARD + " pts)",
                        (spawnsComplete ? "DONE " : "TODO ")
                                + "Spawn " + questSpawnsByPlayer.getOrDefault(playerId, 0) + "/" + QUEST_SPAWNS_TARGET
                                + " (" + QUEST_SPAWNS_REWARD + " pts)",
                        (mergesComplete ? "DONE " : "TODO ")
                                + "Merge " + questMergesByPlayer.getOrDefault(playerId, 0) + "/" + QUEST_MERGES_TARGET
                                + " (" + QUEST_MERGES_REWARD + " pts)")));

        inventory.setItem(QUEST_ABILITY_LUCKY_BURST_SLOT, MenuItemFactory.create(
                Material.ENDER_EYE,
                "Lucky Burst",
                List.of(
                        "Cost: " + getQuestLuckyBurstCost(player) + " quest points",
                        "Effect: +" + (25 + getQuestUpgradePowerLevel(player) * 5) + "% spawn chance",
                        "Duration: " + formatDuration(getAbilityDurationMs(player, QUEST_LUCKY_BURST_BASE_DURATION_MS)),
                        getAbilityMenuStatus(activeLuckyBurstUntilByPlayer, playerId),
                        "Click to activate")));

        inventory.setItem(QUEST_ABILITY_WOOL_RUSH_SLOT, MenuItemFactory.create(
                Material.WHITE_WOOL,
                "Wool Rush",
                List.of(
                        "Cost: " + getQuestWoolRushCost(player) + " quest points",
                        "Effect: Major wool regen speed boost",
                        "Duration: " + formatDuration(getAbilityDurationMs(player, QUEST_WOOL_RUSH_BASE_DURATION_MS)),
                        getAbilityMenuStatus(activeWoolRushUntilByPlayer, playerId),
                        "Click to activate")));

        inventory.setItem(QUEST_ABILITY_JACKPOT_SHEARS_SLOT, MenuItemFactory.create(
                Material.GOLD_INGOT,
                "Jackpot Shears",
                List.of(
                        "Cost: " + getQuestJackpotCost(player) + " quest points",
                        "Effect: x" + (2 + getQuestUpgradePowerLevel(player)) + " shear points",
                        "Duration: "
                                + formatDuration(getAbilityDurationMs(player, QUEST_JACKPOT_SHEARS_BASE_DURATION_MS)),
                        getAbilityMenuStatus(activeJackpotShearsUntilByPlayer, playerId),
                        "Click to activate")));

        inventory.setItem(QUEST_ABILITY_AUTO_MERGE_SLOT, MenuItemFactory.create(
                Material.ANVIL,
                "Auto Merge",
                List.of(
                        "Cost: " + getQuestAutoMergeCost(player) + " quest points",
                        "Effect: Auto-merges sheep once per second",
                        "Duration: " + formatDuration(getAbilityDurationMs(player, QUEST_AUTO_MERGE_BASE_DURATION_MS)),
                        getAbilityMenuStatus(activeAutoMergeUntilByPlayer, playerId),
                        "Click to activate")));

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
                        "Quest points: " + getQuestPoints(player),
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
                if (activateQuestAbility(
                        player,
                        activeLuckyBurstUntilByPlayer,
                        getQuestLuckyBurstCost(player),
                        getAbilityDurationMs(player, QUEST_LUCKY_BURST_BASE_DURATION_MS),
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
                if (activateQuestAbility(
                        player,
                        activeWoolRushUntilByPlayer,
                        getQuestWoolRushCost(player),
                        getAbilityDurationMs(player, QUEST_WOOL_RUSH_BASE_DURATION_MS),
                        Sound.ENTITY_ENDER_DRAGON_FLAP,
                        org.bukkit.Particle.CLOUD)) {
                    markTutorialAbilityUsed(player);
                    player.getWorld().spawnParticle(org.bukkit.Particle.SPORE_BLOSSOM_AIR,
                            player.getLocation().add(0, 1.0, 0), 28, 0.5, 0.35, 0.5, 0.01);
                    playSound(player, Sound.BLOCK_MOSS_CARPET_PLACE, 1.0f, 0.8f);
                    player.sendMessage(action("Wool Rush active."));
                } else {
                    player.sendMessage(warning("Not enough quest points."));
                }
            }
            case QUEST_ABILITY_JACKPOT_SHEARS_SLOT -> {
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
                if (activateQuestAbility(
                        player,
                        activeAutoMergeUntilByPlayer,
                        getQuestAutoMergeCost(player),
                        getAbilityDurationMs(player, QUEST_AUTO_MERGE_BASE_DURATION_MS),
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
            case QUEST_OPEN_UPGRADES_SLOT -> {
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
                        "Bonus: +30s ability duration per level",
                        "Cost: " + getQuestUpgradeDurationCost(player) + " quest points",
                        "Click to upgrade")));
        inventory.setItem(QUEST_UPGRADE_POWER_SLOT, MenuItemFactory.create(
                Material.BLAZE_POWDER,
                "Amplified Buff Power",
                List.of(
                        "Level: " + getQuestUpgradePowerLevel(player),
                        "Bonus: stronger temporary abilities",
                        "Cost: " + getQuestUpgradePowerCost(player) + " quest points",
                        "Click to upgrade")));
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
        switch (slot) {
            case QUEST_UPGRADE_DURATION_SLOT -> {
                if (upgradeQuestDuration(player)) {
                    playUpgradeSound(player);
                    player.sendMessage(action("Quest duration upgrade purchased."));
                } else {
                    player.sendMessage(warning("Not enough quest points."));
                }
            }
            case QUEST_UPGRADE_POWER_SLOT -> {
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
        inventory.setItem(SHOP_SHEAR_SLOT, MenuItemFactory.create(
                Material.SHEARS,
                "Shear Upgrade",
                List.of(
                        "Level: " + getShearShopLevel(player),
                        "Cost: " + getShearUpgradeCost(player) + " points",
                        "Points: base x" + getShearPointMultiplier(player) + " +" + getShearFlatBonus(player),
                        "Wool per shear: 1 + level",
                        "Click to purchase")));
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
                if (upgradeShearShop(player)) {
                    player.sendMessage(action("Shear shop +1"));
                } else {
                    player.sendMessage(warning("Not enough points."));
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

    private static int getEggSpeedUpgradeCost(Player player) {
        int level = getEggSpeedLevel(player);
        return getDoubledUpgradeCost(EGG_SPEED_UPGRADE_BASE_COST, level);
    }

    private static int getWoolRegenUpgradeCost(Player player) {
        int level = getWoolRegenLevel(player);
        return getDoubledUpgradeCost(WOOL_REGEN_UPGRADE_BASE_COST, level);
    }

    private static int getHigherTierChanceUpgradeCost(Player player) {
        int level = getHigherTierChanceLevel(player);
        return getDoubledUpgradeCost(HIGHER_TIER_CHANCE_UPGRADE_BASE_COST, level);
    }

    private static boolean upgradeEggSpeed(Player player) {
        if (player == null) {
            return false;
        }
        int currentLevel = getEggSpeedLevel(player);
        if (currentLevel >= getEggSpeedMaxLevel(player)) {
            return false;
        }
        int cost = getEggSpeedUpgradeCost(player);
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
        int currentLevel = woolRegenLevelByPlayer.getOrDefault(player.getUniqueId(), 0);
        if (currentLevel >= getWoolRegenMaxLevel(player)) {
            return false;
        }
        int cost = getWoolRegenUpgradeCost(player);
        if (!trySpendPoints(player, cost)) {
            return false;
        }
        woolRegenLevelByPlayer.put(player.getUniqueId(), currentLevel + 1);
        saveData();
        return true;
    }

    private static boolean upgradeHigherTierChance(Player player) {
        if (player == null) {
            return false;
        }
        int currentLevel = getHigherTierChanceLevel(player);
        if (currentLevel >= getHigherTierChanceMaxLevel(player)) {
            return false;
        }
        int cost = getHigherTierChanceUpgradeCost(player);
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

    private static int getDoubledUpgradeCost(int baseCost, int level) {
        if (level <= 0) {
            return baseCost;
        }
        long multiplier = 1L << Math.min(30, level);
        long cost = baseCost * multiplier;
        return cost > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) cost;
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

    private static void renderPointsScoreboard(Player player, Scoreboard scoreboard, Objective objective) {
        if (player == null || scoreboard == null || objective == null) {
            return;
        }

        for (String entry : new HashSet<>(scoreboard.getEntries())) {
            scoreboard.resetScores(entry);
        }

        UUID playerId = player.getUniqueId();
        objective.getScore("Points: " + getPlayerPoints(player)).setScore(11);
        objective.getScore("Prestige Lv: " + getPrestigeLevel(player)).setScore(10);
        objective.getScore("Prestige Pts: " + getPrestigePoints(player)).setScore(9);
        objective.getScore(" ").setScore(8);
        objective.getScore("Abilities").setScore(7);
        objective.getScore(getAbilityScoreLine("Lucky", activeLuckyBurstUntilByPlayer, playerId)).setScore(6);
        objective.getScore(getAbilityScoreLine("Wool", activeWoolRushUntilByPlayer, playerId)).setScore(5);
        objective.getScore(getAbilityScoreLine("Jackpot", activeJackpotShearsUntilByPlayer, playerId)).setScore(4);
        objective.getScore(getAbilityScoreLine("Auto", activeAutoMergeUntilByPlayer, playerId)).setScore(3);
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
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (savedInventories.containsKey(player.getUniqueId())) {
                restorePlayerInventory(player);
            }
            if (savedScoreboards.containsKey(player.getUniqueId())) {
                restorePlayerScoreboard(player);
            }
            clearEggTimer(player);
            clearPickedUpSheep(player);
        }
        savedInventories.clear();
        savedScoreboards.clear();
        savedLevels.clear();
        savedExpProgress.clear();
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
            dataConfig.set("prestigeRefundCooldown", null);
            dataConfig.set("highestAnnouncedTier", null);
            dataConfig.set("prestigeExpandFarm", null);
            dataConfig.set("shearShop", null);
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
            dataConfig.set("tutorialShearShopOpened", null);
            dataConfig.set("farmVisitEnabled", null);
            dataConfig.set("questPoints", null);
            dataConfig.set("questReset", null);
            dataConfig.set("questUpgradeDuration", null);
            dataConfig.set("questUpgradePower", null);
            dataConfig.set("pendingInventory", null);
            for (Map.Entry<UUID, Integer> entry : pointsByPlayer.entrySet()) {
                dataConfig.set("points." + entry.getKey().toString(), entry.getValue());
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
            for (Map.Entry<UUID, Long> entry : nextPrestigeRefundTimestampByPlayer.entrySet()) {
                dataConfig.set("prestigeRefundCooldown." + entry.getKey().toString(), entry.getValue());
            }
            for (Map.Entry<UUID, Integer> entry : highestAnnouncedTierByPlayer.entrySet()) {
                dataConfig.set("highestAnnouncedTier." + entry.getKey().toString(), entry.getValue());
            }
            for (Map.Entry<UUID, Integer> entry : shearShopLevelByPlayer.entrySet()) {
                dataConfig.set("shearShop." + entry.getKey().toString(), entry.getValue());
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
                    pointsByPlayer.put(uuid, dataConfig.getInt("points." + key, 0));
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
                    prestigeDoublePointsChanceByPlayer.put(uuid, dataConfig.getInt("prestigeDoublePoints." + key, 0));
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
                    highestAnnouncedTierByPlayer.put(
                            uuid,
                            dataConfig.getInt("highestAnnouncedTier." + key, SheepTier.WHITE.getLevel()));
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
        player.addPassenger(sheep);
        carriedSheepByPlayer.put(player.getUniqueId(), sheep);
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
        player.removePassenger(sheep);
        sheep.setGravity(true);
        sheep.setAI(true);
        sheep.setInvulnerable(false);
        sheep.teleport(player.getLocation().add(player.getLocation().getDirection().multiply(1.0)).add(0, 1, 0));
        sheep.setVelocity(player.getLocation().getDirection().multiply(0.3));
        carriedSheepByPlayer.remove(player.getUniqueId());
        return true;
    }

    public static void clearPickedUpSheep(Player player) {
        if (player == null) {
            return;
        }
        Sheep sheep = carriedSheepByPlayer.remove(player.getUniqueId());
        if (sheep != null && sheep.isValid()) {
            if (player.getPassengers().contains(sheep)) {
                player.removePassenger(sheep);
            }
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
