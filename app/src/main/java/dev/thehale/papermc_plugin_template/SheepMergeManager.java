package dev.thehale.papermc_plugin_template;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

public final class SheepMergeManager {

    private static final Map<UUID, Integer> pointsByPlayer = new HashMap<>();
    private static final Map<UUID, Integer> extraLimitByPlayer = new HashMap<>();
    private static final Map<UUID, Integer> eggSpeedLevelByPlayer = new HashMap<>();
    private static final Map<UUID, Integer> woolRegenLevelByPlayer = new HashMap<>();
    private static final Map<UUID, Integer> higherTierChanceLevelByPlayer = new HashMap<>();
    private static final Map<UUID, Long> nextEggTimestampByPlayer = new HashMap<>();
    private static final Map<UUID, Sheep> carriedSheepByPlayer = new HashMap<>();
    private static final Map<UUID, InventorySnapshot> savedInventories = new HashMap<>();
    private static final Map<UUID, Scoreboard> savedScoreboards = new HashMap<>();
    private static final Pattern OWNER_ID_PATTERN = Pattern.compile("^sheepfarm_([0-9a-fA-F]{32})$");
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
    public static final String UPGRADE_MENU_TITLE = "Sheep Merge Upgrades";
    public static final int LIMIT_UPGRADE_SLOT = 10;
    public static final int EGG_SPEED_UPGRADE_SLOT = 12;
    public static final int WOOL_REGEN_UPGRADE_SLOT = 14;
    public static final int HIGHER_TIER_CHANCE_UPGRADE_SLOT = 16;

    private static SheepMergePlugin plugin;
    private static FileConfiguration dataConfig;
    private static File dataFile;

    private SheepMergeManager() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void initialize(SheepMergePlugin plugin) {
        SheepMergeManager.plugin = plugin;
        dataFile = new File(plugin.getDataFolder(), "scores.yml");
        loadData();
    }

    public static NamespacedKey getTierKey() {
        return new NamespacedKey(plugin, "sheep-tier");
    }

    private static NamespacedKey getNextEatKey() {
        return new NamespacedKey(plugin, "sheep-next-eat");
    }

    public static boolean isSheepFarmWorld(World world) {
        return world != null && world.getName().startsWith("sheepfarm_");
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

    public static void addPoints(Player player, int points) {
        if (player == null || points <= 0) {
            return;
        }
        UUID playerId = player.getUniqueId();
        pointsByPlayer.put(playerId, pointsByPlayer.getOrDefault(playerId, 0) + points);
        saveData();
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
        return BASE_SHEEP_LIMIT + extraLimitByPlayer.getOrDefault(player.getUniqueId(), 0);
    }

    public static int getOwnerLimit(World world) {
        UUID ownerId = getOwnerId(world);
        if (ownerId == null) {
            return BASE_SHEEP_LIMIT;
        }
        return BASE_SHEEP_LIMIT + extraLimitByPlayer.getOrDefault(ownerId, 0);
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
        int chance = getHigherTierChancePercent(world);
        if (RANDOM.nextInt(100) < chance) {
            return SheepTier.ORANGE;
        }
        return SheepTier.WHITE;
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
        player.sendMessage("A sheep spawn egg has appeared in your inventory.");
        nextEggTimestampByPlayer.put(playerId, now + getEggIntervalSeconds(player) * 1000L);
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
                        "Level: " + eggLevel + " / " + EGG_SPEED_MAX_LEVEL,
                        "Egg interval: " + getEggIntervalSeconds(player) + "s",
                        eggLevel >= EGG_SPEED_MAX_LEVEL ? "MAXED" : "Cost: " + eggCost + " points",
                        "Click to purchase")));

        int woolLevel = woolRegenLevelByPlayer.getOrDefault(player.getUniqueId(), 0);
        int woolCost = getWoolRegenUpgradeCost(player);
        inventory.setItem(WOOL_REGEN_UPGRADE_SLOT, createMenuItem(
                Material.SHEARS,
                "Faster Wool Regen",
                List.of(
                        "Level: " + woolLevel + " / " + WOOL_REGEN_MAX_LEVEL,
                        "Effect: -15% cooldown per level",
                        woolLevel >= WOOL_REGEN_MAX_LEVEL ? "MAXED" : "Cost: " + woolCost + " points",
                        "Click to purchase")));

        int chanceLevel = higherTierChanceLevelByPlayer.getOrDefault(player.getUniqueId(), 0);
        int chanceCost = getHigherTierChanceUpgradeCost(player);
        inventory.setItem(HIGHER_TIER_CHANCE_UPGRADE_SLOT, createMenuItem(
                Material.GOLDEN_APPLE,
                "Higher Tier Spawn Chance",
                List.of(
                        "Level: " + chanceLevel + " / " + HIGHER_TIER_CHANCE_MAX_LEVEL,
                        "Chance: " + getHigherTierChancePercent(player) + "%",
                        chanceLevel >= HIGHER_TIER_CHANCE_MAX_LEVEL ? "MAXED" : "Cost: " + chanceCost + " points",
                        "Click to purchase")));

        player.openInventory(inventory);
    }

    public static boolean isUpgradeMenuTitle(String title) {
        return UPGRADE_MENU_TITLE.equals(title);
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
                if (level >= WOOL_REGEN_MAX_LEVEL) {
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
                if (level >= HIGHER_TIER_CHANCE_MAX_LEVEL) {
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
            default -> {
                return;
            }
        }
        updatePointsScoreboard(player);
        openUpgradeMenu(player);
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
        if (currentLevel >= EGG_SPEED_MAX_LEVEL) {
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
        if (currentLevel >= WOOL_REGEN_MAX_LEVEL) {
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
        if (currentLevel >= HIGHER_TIER_CHANCE_MAX_LEVEL) {
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
        return world.getEntitiesByClass(Sheep.class).size();
    }

    public static boolean isWorldAtLimit(World world) {
        if (world == null) {
            return false;
        }
        return getSheepCount(world) >= getOwnerLimit(world);
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
            return null;
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
