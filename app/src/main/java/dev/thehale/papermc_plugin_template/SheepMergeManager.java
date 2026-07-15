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
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
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
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

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
    private static final Map<UUID, Integer> shearShopLevelByPlayer = new HashMap<>();
    private static final Map<UUID, Boolean> tutorialCompletedByPlayer = new HashMap<>();
    private static final Map<UUID, Integer> tutorialShearsByPlayer = new HashMap<>();
    private static final Map<UUID, Integer> tutorialSpawnsByPlayer = new HashMap<>();
    private static final Map<UUID, Integer> tutorialMergesByPlayer = new HashMap<>();
    private static final Map<UUID, Long> nextEggTimestampByPlayer = new HashMap<>();
    private static final Map<UUID, Sheep> carriedSheepByPlayer = new HashMap<>();
    private static final Map<UUID, InventorySnapshot> savedInventories = new HashMap<>();
    private static final Map<UUID, Scoreboard> savedScoreboards = new HashMap<>();
    private static final Map<UUID, Integer> liveSheepCountByWorld = new HashMap<>();
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
    private static final int PRESTIGE_DOUBLE_POINTS_BASE_COST = 1;
    private static final int PRESTIGE_HIGHER_MAX_LEVEL_BASE_COST = 2;
    private static final int PRESTIGE_START_EGGS_BASE_COST = 1;
    private static final int PRESTIGE_MAX_LEVEL = 50;
    private static final int FARM_RADIUS = 5;
    private static final int FARM_BASE_Y = 100;
    private static final int FARM_MIN_Y = FARM_BASE_Y - 1;
    private static final int FARM_MAX_Y = FARM_BASE_Y + 4;
    private static final int SHEAR_SHOP_BASE_COST = 40;
    private static final int TUTORIAL_SHEAR_TARGET = 3;
    private static final int TUTORIAL_SPAWN_TARGET = 1;
    private static final int TUTORIAL_MERGE_TARGET = 1;
    public static final String UPGRADE_MENU_TITLE = "Sheep Merge Upgrades";
    public static final String PRESTIGE_MENU_TITLE = "Prestige Upgrades";
    public static final String SHOP_MENU_TITLE = "Shear Shop";
    public static final int LIMIT_UPGRADE_SLOT = 10;
    public static final int EGG_SPEED_UPGRADE_SLOT = 12;
    public static final int WOOL_REGEN_UPGRADE_SLOT = 14;
    public static final int HIGHER_TIER_CHANCE_UPGRADE_SLOT = 16;
    public static final int PRESTIGE_MENU_OPEN_SLOT = 22;
    public static final int SHOP_MENU_OPEN_SLOT = 24;
    public static final int PRESTIGE_UPGRADE_SLOT = 10;
    public static final int PRESTIGE_DOUBLE_POINTS_SLOT = 12;
    public static final int PRESTIGE_HIGHER_MAX_LEVEL_SLOT = 14;
    public static final int PRESTIGE_START_EGGS_SLOT = 16;
    public static final int PRESTIGE_BACK_TO_UPGRADES_SLOT = 26;
    public static final int SHOP_SHEAR_SLOT = 13;
    public static final int SHOP_BACK_TO_UPGRADES_SLOT = 26;

    private static SheepMergePlugin plugin;
    private static FileConfiguration dataConfig;
    private static File dataFile;
    private static FileConfiguration farmLayoutConfig;
    private static File farmLayoutFile;
    private static UUID pendingFarmLayoutSourceWorldId;
    private static BukkitTask pendingFarmLayoutSyncTask;

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
                    farmLayoutConfig.set("blocks." + keyFor(x, y, z), sourceWorld.getBlockAt(x, y, z).getType().name());
                }
            }
        }
        return saveFarmLayout();
    }

    public static boolean syncSharedFarmLayoutFromWorld(World sourceWorld) {
        if (!saveSharedFarmLayoutFromWorld(sourceWorld)) {
            return false;
        }
        applySharedFarmLayoutToAllFarmWorlds();
        return true;
    }

    public static boolean scheduleAutoFarmLayoutSync(World sourceWorld) {
        if (plugin == null || sourceWorld == null || !isSheepFarmWorld(sourceWorld)) {
            return false;
        }
        pendingFarmLayoutSourceWorldId = sourceWorld.getUID();
        if (pendingFarmLayoutSyncTask != null) {
            return true;
        }
        pendingFarmLayoutSyncTask = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            pendingFarmLayoutSyncTask = null;
            UUID sourceId = pendingFarmLayoutSourceWorldId;
            pendingFarmLayoutSourceWorldId = null;
            if (sourceId == null) {
                return;
            }
            World world = plugin.getServer().getWorld(sourceId);
            if (world == null) {
                return;
            }
            syncSharedFarmLayoutFromWorld(world);
        }, 20L);
        return true;
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
                    String name = farmLayoutConfig.getString("blocks." + keyFor(x, y, z));
                    world.getBlockAt(x, y, z).setType(parseMaterial(name));
                }
            }
        }
    }

    private static void applyDefaultFarmLayout(World world) {
        for (int x = -FARM_RADIUS; x <= FARM_RADIUS; x++) {
            for (int y = FARM_MIN_Y; y <= FARM_MAX_Y; y++) {
                for (int z = -FARM_RADIUS; z <= FARM_RADIUS; z++) {
                    Material material = Material.AIR;
                    if (y == FARM_MIN_Y) {
                        material = Material.DIRT;
                    } else if (y == FARM_BASE_Y) {
                        material = Material.GRASS_BLOCK;
                    } else if (y == FARM_BASE_Y + 1 && (Math.abs(x) == FARM_RADIUS || Math.abs(z) == FARM_RADIUS)) {
                        material = Material.OAK_FENCE;
                    }
                    world.getBlockAt(x, y, z).setType(material);
                }
            }
        }
    }

    private static Material parseMaterial(String materialName) {
        if (materialName == null || materialName.isBlank()) {
            return Material.AIR;
        }
        Material material = Material.matchMaterial(materialName);
        return material == null ? Material.AIR : material;
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
        return world != null && world.getName().startsWith("sheepfarm_");
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

    public static int getPrestigeLevel(Player player) {
        return player == null ? 0 : prestigeLevelByPlayer.getOrDefault(player.getUniqueId(), 0);
    }

    public static int getPrestigePoints(Player player) {
        return player == null ? 0 : prestigePointsByPlayer.getOrDefault(player.getUniqueId(), 0);
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

        // Reset regular progression purchased with normal points.
        pointsByPlayer.put(player.getUniqueId(), 0);
        extraLimitByPlayer.remove(player.getUniqueId());
        eggSpeedLevelByPlayer.remove(player.getUniqueId());
        woolRegenLevelByPlayer.remove(player.getUniqueId());
        higherTierChanceLevelByPlayer.remove(player.getUniqueId());
        nextEggTimestampByPlayer.remove(player.getUniqueId());

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

    public static void recordTutorialShear(Player player) {
        if (player == null) {
            return;
        }
        tutorialShearsByPlayer.put(player.getUniqueId(), getTutorialShearCount(player) + 1);
        checkTutorialCompletion(player);
    }

    public static void recordTutorialSpawn(Player player) {
        if (player == null) {
            return;
        }
        tutorialSpawnsByPlayer.put(player.getUniqueId(), getTutorialSpawnCount(player) + 1);
        checkTutorialCompletion(player);
    }

    public static void recordTutorialMerge(Player player) {
        if (player == null) {
            return;
        }
        tutorialMergesByPlayer.put(player.getUniqueId(), getTutorialMergeCount(player) + 1);
        checkTutorialCompletion(player);
    }

    public static String getTutorialProgressLine(Player player) {
        return "Tutorial tasks: Shear " + getTutorialShearCount(player) + "/" + TUTORIAL_SHEAR_TARGET
                + ", Spawn " + getTutorialSpawnCount(player) + "/" + TUTORIAL_SPAWN_TARGET
                + ", Merge " + getTutorialMergeCount(player) + "/" + TUTORIAL_MERGE_TARGET;
    }

    private static void checkTutorialCompletion(Player player) {
        if (player == null || isTutorialCompleted(player)) {
            return;
        }
        if (getTutorialShearCount(player) >= TUTORIAL_SHEAR_TARGET
                && getTutorialSpawnCount(player) >= TUTORIAL_SPAWN_TARGET
                && getTutorialMergeCount(player) >= TUTORIAL_MERGE_TARGET) {
            tutorialCompletedByPlayer.put(player.getUniqueId(), true);
            player.sendMessage("Tutorial complete! Run /sheepmerge again to enter your personal farm world.");
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
        shearShopLevelByPlayer.remove(id);
        tutorialCompletedByPlayer.remove(id);
        tutorialShearsByPlayer.remove(id);
        tutorialSpawnsByPlayer.remove(id);
        tutorialMergesByPlayer.remove(id);
        nextEggTimestampByPlayer.remove(id);
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
        return Math.max(1, (int) Math.ceil(baseSeconds * multiplier));
    }

    public static void processSheepEatTimer(Sheep sheep) {
        if (sheep == null || !sheep.isValid() || sheep.getWorld() == null
                || !isSheepFarmWorld(sheep.getWorld())) {
            return;
        }
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
            sheep.setAI(false);
        }
        updateSheepName(sheep);
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
    }

    public static void showOverlay(Player player, String message) {
        if (player == null || message == null || message.isBlank()) {
            return;
        }
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
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
        return EGG_SPEED_MAX_LEVEL + getPrestigeHigherMaxLevel(player) * 2;
    }

    public static int getWoolRegenMaxLevel(Player player) {
        return WOOL_REGEN_MAX_LEVEL + getPrestigeHigherMaxLevel(player) * 2;
    }

    public static int getHigherTierChanceMaxLevel(Player player) {
        return HIGHER_TIER_CHANCE_MAX_LEVEL + getPrestigeHigherMaxLevel(player) * 2;
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
        return Math.min(100, higherTierChanceLevelByPlayer.getOrDefault(player.getUniqueId(), 0) * 5);
    }

    public static int getHigherTierChancePercent(World world) {
        UUID ownerId = getOwnerId(world);
        if (ownerId == null) {
            return 0;
        }
        return Math.min(100, higherTierChanceLevelByPlayer.getOrDefault(ownerId, 0) * 5);
    }

    public static SheepTier rollSpawnTier(World world) {
        int cap = getUnlockedTierCap(world);
        int chance = getHigherTierChancePercent(world);
        int chosen = 0;
        while (chosen < cap && RANDOM.nextInt(100) < chance) {
            chosen++;
            chance = Math.max(5, chance / 2);
        }
        return SheepTier.byLevel(chosen);
    }

    public static void tickEggDistribution(Player player) {
        if (player == null || !isSheepFarmWorld(player.getWorld())) {
            return;
        }

        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();
        long next = nextEggTimestampByPlayer.getOrDefault(playerId, now + getEggIntervalSeconds(player) * 1000L);
        if (now < next) {
            return;
        }

        if (player.getInventory().firstEmpty() == -1) {
            nextEggTimestampByPlayer.put(playerId, now + 2000L);
            return;
        }

        ItemStack egg = new ItemStack(Material.SHEEP_SPAWN_EGG, 1);
        player.getInventory().addItem(egg);
        showOverlay(player, "+1 Sheep Spawn Egg");
        nextEggTimestampByPlayer.put(playerId, now + getEggIntervalSeconds(player) * 1000L);
    }

    public static int getStartEggsBonus(Player player) {
        return player == null ? 0 : prestigeStartEggsByPlayer.getOrDefault(player.getUniqueId(), 0) * 2;
    }

    public static int getPrestigeDoublePointsChanceLevel(Player player) {
        return player == null ? 0 : prestigeDoublePointsChanceByPlayer.getOrDefault(player.getUniqueId(), 0);
    }

    public static int getDoublePointsChancePercent(Player player) {
        return Math.min(100, getPrestigeDoublePointsChanceLevel(player) * 5);
    }

    public static int getPrestigeHigherMaxLevel(Player player) {
        return player == null ? 0 : prestigeHigherMaxLevelByPlayer.getOrDefault(player.getUniqueId(), 0);
    }

    public static int getPrestigeStartEggsLevel(Player player) {
        return player == null ? 0 : prestigeStartEggsByPlayer.getOrDefault(player.getUniqueId(), 0);
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

    public static void resetEggTimer(Player player) {
        if (player == null) {
            return;
        }
        nextEggTimestampByPlayer.put(player.getUniqueId(),
                System.currentTimeMillis() + getEggIntervalSeconds(player) * 1000L);
    }

    public static void clearEggTimer(Player player) {
        if (player == null) {
            return;
        }
        nextEggTimestampByPlayer.remove(player.getUniqueId());
    }

    public static void openUpgradeMenu(Player player) {
        if (player == null) {
            return;
        }

        Inventory inventory = Bukkit.createInventory(null, 27, UPGRADE_MENU_TITLE);
        int limitLevel = getLimitUpgradeLevel(player);
        int limitCost = getUpgradeCost(player);
        inventory.setItem(LIMIT_UPGRADE_SLOT, createMenuItem(
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
        inventory.setItem(EGG_SPEED_UPGRADE_SLOT, createMenuItem(
                Material.CLOCK,
                "Faster Egg Spawn",
                List.of(
                        "Level: " + eggLevel + " / " + getEggSpeedMaxLevel(player),
                        "Egg interval: " + getEggIntervalSeconds(player) + "s",
                        eggLevel >= getEggSpeedMaxLevel(player) ? "MAXED" : "Cost: " + eggCost + " points",
                        "Click to purchase")));

        int woolLevel = woolRegenLevelByPlayer.getOrDefault(player.getUniqueId(), 0);
        int woolCost = getWoolRegenUpgradeCost(player);
        inventory.setItem(WOOL_REGEN_UPGRADE_SLOT, createMenuItem(
                Material.SHEARS,
                "Faster Wool Regen",
                List.of(
                        "Level: " + woolLevel + " / " + getWoolRegenMaxLevel(player),
                        "Effect: -15% cooldown per level",
                        woolLevel >= getWoolRegenMaxLevel(player) ? "MAXED" : "Cost: " + woolCost + " points",
                        "Click to purchase")));

        int chanceLevel = higherTierChanceLevelByPlayer.getOrDefault(player.getUniqueId(), 0);
        int chanceCost = getHigherTierChanceUpgradeCost(player);
        inventory.setItem(HIGHER_TIER_CHANCE_UPGRADE_SLOT, createMenuItem(
                Material.GOLDEN_APPLE,
                "Higher Tier Spawn Chance",
                List.of(
                        "Level: " + chanceLevel + " / " + getHigherTierChanceMaxLevel(player),
                        "Chance: " + getHigherTierChancePercent(player) + "%",
                        chanceLevel >= getHigherTierChanceMaxLevel(player) ? "MAXED"
                                : "Cost: " + chanceCost + " points",
                        "Click to purchase")));

        inventory.setItem(PRESTIGE_MENU_OPEN_SLOT, createMenuItem(
                Material.NETHER_STAR,
                "Prestige Upgrades",
                List.of(
                        "Prestige level: " + getPrestigeLevel(player),
                        "Prestige points: " + getPrestigePoints(player),
                        "Click to open")));

        inventory.setItem(SHOP_MENU_OPEN_SLOT, createMenuItem(
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
                    player.sendMessage("Sheep limit upgraded to level " + getLimitUpgradeLevel(player)
                            + ". New limit: " + getPlayerLimit(player));
                } else {
                    player.sendMessage("Not enough points for Sheep Limit upgrade.");
                }
            }
            case EGG_SPEED_UPGRADE_SLOT -> {
                if (upgradeEggSpeed(player)) {
                    player.sendMessage("Egg spawn speed upgraded to level " + getEggSpeedLevel(player)
                            + ". New interval: " + getEggIntervalSeconds(player) + "s");
                } else {
                    player.sendMessage("Not enough points for Faster Egg Spawn upgrade.");
                }
            }
            case WOOL_REGEN_UPGRADE_SLOT -> {
                int level = woolRegenLevelByPlayer.getOrDefault(player.getUniqueId(), 0);
                if (level >= getWoolRegenMaxLevel(player)) {
                    player.sendMessage("Faster Wool Regen is already maxed.");
                    break;
                }
                if (upgradeWoolRegen(player)) {
                    player.sendMessage("Wool regen upgraded to level " + getWoolRegenLevel(player) + ".");
                } else {
                    player.sendMessage("Not enough points for Faster Wool Regen upgrade.");
                }
            }
            case HIGHER_TIER_CHANCE_UPGRADE_SLOT -> {
                int level = higherTierChanceLevelByPlayer.getOrDefault(player.getUniqueId(), 0);
                if (level >= getHigherTierChanceMaxLevel(player)) {
                    player.sendMessage("Higher Tier Spawn Chance is already maxed.");
                    break;
                }
                if (upgradeHigherTierChance(player)) {
                    player.sendMessage("Higher-tier spawn chance upgraded to level " + getHigherTierChanceLevel(player)
                            + " (" + getHigherTierChancePercent(player) + "%).");
                } else {
                    player.sendMessage("Not enough points for Higher Tier Spawn Chance upgrade.");
                }
            }
            case PRESTIGE_MENU_OPEN_SLOT -> {
                openPrestigeMenu(player);
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
        Inventory inventory = Bukkit.createInventory(null, 27, PRESTIGE_MENU_TITLE);
        inventory.setItem(PRESTIGE_UPGRADE_SLOT, createMenuItem(
                Material.NETHER_STAR,
                "Prestige Reset",
                List.of(
                        "Current prestige: " + getPrestigeLevel(player),
                        "Gain prestige points: +" + (getPrestigeLevel(player) + 1),
                        "Cost: " + getPrestigeCost(player) + " normal points",
                        "Resets normal-point upgrades",
                        "Click to prestige")));

        inventory.setItem(PRESTIGE_DOUBLE_POINTS_SLOT, createMenuItem(
                Material.EMERALD,
                "Double Points Chance",
                List.of(
                        "Level: " + getPrestigeDoublePointsChanceLevel(player),
                        "Chance: " + getDoublePointsChancePercent(player) + "%",
                        "Cost: " + getPrestigeDoublePointsCost(player) + " prestige points",
                        "Click to purchase")));

        inventory.setItem(PRESTIGE_HIGHER_MAX_LEVEL_SLOT, createMenuItem(
                Material.ENCHANTED_BOOK,
                "Higher Maximum Levels",
                List.of(
                        "Level: " + getPrestigeHigherMaxLevel(player),
                        "Tier cap bonus: +" + (getPrestigeHigherMaxLevel(player) * 2),
                        "Cost: " + getPrestigeHigherMaxLevelCost(player) + " prestige points",
                        "Click to purchase")));

        inventory.setItem(PRESTIGE_START_EGGS_SLOT, createMenuItem(
                Material.SHEEP_SPAWN_EGG,
                "Start With Extra Eggs",
                List.of(
                        "Level: " + getPrestigeStartEggsLevel(player),
                        "Extra starting eggs: +" + getStartEggsBonus(player),
                        "Cost: " + getPrestigeStartEggsCost(player) + " prestige points",
                        "Click to purchase")));

        inventory.setItem(PRESTIGE_BACK_TO_UPGRADES_SLOT, createMenuItem(
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
                    player.sendMessage("Prestige increased to " + getPrestigeLevel(player)
                            + ". Prestige points: " + getPrestigePoints(player));
                } else {
                    player.sendMessage("Not enough points to prestige.");
                }
            }
            case PRESTIGE_DOUBLE_POINTS_SLOT -> {
                if (upgradePrestigeDoublePoints(player)) {
                    player.sendMessage(
                            "Double points chance upgraded to " + getDoublePointsChancePercent(player) + "%.");
                } else {
                    player.sendMessage("Not enough prestige points for Double Points Chance.");
                }
            }
            case PRESTIGE_HIGHER_MAX_LEVEL_SLOT -> {
                if (upgradePrestigeHigherMaxLevel(player)) {
                    player.sendMessage("Higher maximum levels upgraded to " + getPrestigeHigherMaxLevel(player) + ".");
                } else {
                    player.sendMessage("Not enough prestige points for Higher Maximum Levels.");
                }
            }
            case PRESTIGE_START_EGGS_SLOT -> {
                if (upgradePrestigeStartEggs(player)) {
                    player.sendMessage("Start eggs upgraded to level " + getPrestigeStartEggsLevel(player) + ".");
                } else {
                    player.sendMessage("Not enough prestige points for Start With Extra Eggs.");
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

    public static void openShopMenu(Player player) {
        if (player == null) {
            return;
        }
        Inventory inventory = Bukkit.createInventory(null, 27, SHOP_MENU_TITLE);
        inventory.setItem(SHOP_SHEAR_SLOT, createMenuItem(
                Material.SHEARS,
                "Shear Upgrade",
                List.of(
                        "Level: " + getShearShopLevel(player),
                        "Cost: " + getShearUpgradeCost(player) + " points",
                        "Points: base x" + getShearPointMultiplier(player) + " +" + getShearFlatBonus(player),
                        "Wool per shear: 1 + level",
                        "Click to purchase")));
        inventory.setItem(SHOP_BACK_TO_UPGRADES_SLOT, createMenuItem(
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
                    player.sendMessage("Shear upgrade purchased. Level: " + getShearShopLevel(player));
                } else {
                    player.sendMessage("Not enough points for Shear Upgrade.");
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

    private static ItemStack createMenuItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
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
        ItemStack[] contents = cloneItemStackArray(player.getInventory().getContents());
        ItemStack[] armor = cloneItemStackArray(player.getInventory().getArmorContents());
        ItemStack offhand = player.getInventory().getItemInOffHand() == null ? null
                : player.getInventory().getItemInOffHand().clone();
        savedInventories.put(player.getUniqueId(), new InventorySnapshot(contents, armor, offhand));
    }

    public static void restorePlayerInventory(Player player) {
        if (player == null) {
            return;
        }
        InventorySnapshot snapshot = savedInventories.remove(player.getUniqueId());
        if (snapshot == null) {
            return;
        }
        player.getInventory().clear();
        player.getInventory().setContents(cloneItemStackArray(snapshot.contents));
        player.getInventory().setArmorContents(cloneItemStackArray(snapshot.armor));
        player.getInventory().setItemInOffHand(snapshot.offhand == null ? null : snapshot.offhand.clone());
    }

    public static boolean hasSavedInventory(Player player) {
        return player != null && savedInventories.containsKey(player.getUniqueId());
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
        objective.getScore("Prestige Lv").setScore(getPrestigeLevel(player));
        objective.getScore("Prestige Pts").setScore(getPrestigePoints(player));
        objective.getScore("Points").setScore(getPlayerPoints(player));
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
        objective.getScore("Prestige Lv").setScore(getPrestigeLevel(player));
        objective.getScore("Prestige Pts").setScore(getPrestigePoints(player));
        objective.getScore("Points").setScore(getPlayerPoints(player));
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
            dataConfig.set("prestigeExpandFarm", null);
            dataConfig.set("shearShop", null);
            dataConfig.set("tutorialCompleted", null);
            dataConfig.set("tutorialShears", null);
            dataConfig.set("tutorialSpawns", null);
            dataConfig.set("tutorialMerges", null);
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
            for (Map.Entry<UUID, Integer> entry : shearShopLevelByPlayer.entrySet()) {
                dataConfig.set("shearShop." + entry.getKey().toString(), entry.getValue());
            }
            for (Map.Entry<UUID, Boolean> entry : tutorialCompletedByPlayer.entrySet()) {
                dataConfig.set("tutorialCompleted." + entry.getKey().toString(), entry.getValue());
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
    }

    private static ItemStack[] cloneItemStackArray(ItemStack[] source) {
        if (source == null) {
            return null;
        }
        ItemStack[] clone = new ItemStack[source.length];
        for (int i = 0; i < source.length; i++) {
            clone[i] = source[i] == null ? null : source[i].clone();
        }
        return clone;
    }

    private static final class InventorySnapshot {
        private final ItemStack[] contents;
        private final ItemStack[] armor;
        private final ItemStack offhand;

        private InventorySnapshot(ItemStack[] contents, ItemStack[] armor, ItemStack offhand) {
            this.contents = contents;
            this.armor = armor;
            this.offhand = offhand;
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
