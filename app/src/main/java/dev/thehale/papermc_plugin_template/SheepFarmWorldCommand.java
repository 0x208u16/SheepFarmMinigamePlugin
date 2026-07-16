package dev.thehale.papermc_plugin_template;

import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class SheepFarmWorldCommand implements CommandExecutor, TabCompleter {

    private static final List<String> ROOT_SUBCOMMANDS = List.of(
            "upgrade",
            "prestige",
            "shop",
            "tutorial",
            "visit",
            "kick",
            "status",
            "storm",
            "topdisplay",
            "resetdata",
            "givepoints",
            "setpoints",
            "givequestpoints",
            "setprestige",
            "mapsave",
            "mapload",
            "world");

    private static final List<String> WORLD_SUBCOMMANDS = List.of("save", "load");

    public static String getWorldName(java.util.UUID playerId) {
        return "sheepfarm_" + playerId.toString().replace("-", "");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("upgrade")) {
            SheepMergeManager.openUpgradeMenu(player);
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("shop")) {
            SheepMergeManager.openShopMenu(player);
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("prestige")) {
            SheepMergeManager.openPrestigeMenu(player);
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("tutorial")) {
            SheepMergeManager.startTutorial(player, true);
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("status")) {
            player.sendMessage("Points: " + SheepMergeManager.getPlayerPoints(player)
                    + ", Sheep limit: " + SheepMergeManager.getPlayerLimit(player)
                    + " (Lv." + SheepMergeManager.getLimitUpgradeLevel(player) + ")"
                    + ", Spawn eggs every " + SheepMergeManager.getEggIntervalSeconds(player) + " seconds"
                    + " (Lv." + SheepMergeManager.getEggSpeedLevel(player) + ")"
                    + ", Wool regen level: " + SheepMergeManager.getWoolRegenLevel(player)
                    + ", Higher-tier spawn chance: " + SheepMergeManager.getHigherTierChancePercent(player) + "%"
                    + " (Lv." + SheepMergeManager.getHigherTierChanceLevel(player) + ")"
                    + ", Egg cap: " + SheepMergeManager.getEggCap(player)
                    + " (Lv." + SheepMergeManager.getPrestigeEggCapLevel(player) + ")"
                    + ", Prestige: " + SheepMergeManager.getPrestigeLevel(player)
                    + ", Prestige points: " + SheepMergeManager.getPrestigePoints(player)
                    + ", Farm visit access: "
                    + (SheepMergeManager.isFarmVisitable(player.getUniqueId()) ? "open" : "closed"));
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("visit")) {
            if (args.length == 2 && args[1].equalsIgnoreCase("toggle")) {
                boolean nowOpen = SheepMergeManager.toggleFarmVisitable(player);
                player.sendMessage("Your farm is now " + (nowOpen ? "open" : "closed") + " to visitors.");
                return true;
            }

            if (args.length < 2) {
                player.sendMessage("Usage: /sheepmerge visit <player> or /sheepmerge visit toggle");
                return true;
            }

            Player owner = Bukkit.getPlayerExact(args[1]);
            if (owner == null) {
                player.sendMessage("That player is not online.");
                return true;
            }

            java.util.UUID ownerId = owner.getUniqueId();
            if (!player.isOp()
                    && !ownerId.equals(player.getUniqueId())
                    && !SheepMergeManager.isFarmVisitable(ownerId)) {
                player.sendMessage("That farm is closed to visitors.");
                return true;
            }

            String ownerWorldName = getWorldName(ownerId);
            World ownerWorld = ensureFarmWorld(ownerWorldName);
            if (ownerWorld == null) {
                player.sendMessage("Unable to open that farm world right now.");
                return true;
            }

            ownerWorld.setSpawnLocation(0, 101, 0);
            player.teleport(new Location(ownerWorld, 0.5, 101, 0.5));
            player.sendMessage("You were teleported to " + owner.getName() + "'s sheep farm.");
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("kick")) {
            if (!SheepMergeManager.isSheepFarmWorld(player.getWorld())
                    || !SheepMergeManager.isFarmOwner(player, player.getWorld())) {
                player.sendMessage("You can only use this in your own sheep farm world.");
                return true;
            }
            if (args.length < 2) {
                player.sendMessage("Usage: /sheepmerge kick <player>");
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null || !target.getWorld().equals(player.getWorld())) {
                player.sendMessage("That player is not in your farm right now.");
                return true;
            }
            if (target.equals(player)) {
                player.sendMessage("You cannot kick yourself.");
                return true;
            }
            if (target.isOp()) {
                player.sendMessage("You cannot kick operators from your farm.");
                return true;
            }

            World fallbackWorld = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
            if (fallbackWorld == null) {
                player.sendMessage("No safe world is available to move that player.");
                return true;
            }

            Location spawn = fallbackWorld.getSpawnLocation().clone().add(0.5, 0, 0.5);
            target.teleport(spawn);
            target.sendMessage("You were removed from " + player.getName() + "'s sheep farm.");
            player.sendMessage("You removed " + target.getName() + " from your farm.");
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("topdisplay")) {
            if (!player.isOp()) {
                player.sendMessage("Only server operators can use this command.");
                return true;
            }
            boolean createdOrMoved = SheepMergeManager.spawnOrMoveTopPointsDisplay(player);
            if (createdOrMoved) {
                player.sendMessage("Top points display created or moved to your position.");
            } else {
                player.sendMessage("Unable to create top points display right now.");
            }
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("storm")) {
            if (!player.isOp()) {
                player.sendMessage("Only server operators can use this command.");
                return true;
            }
            if (SheepMergeManager.triggerSheepStormEvent()) {
                player.sendMessage("Sheep storm triggered.");
            } else {
                player.sendMessage("A sheep storm is already active or could not be started.");
            }
            return true;
        }

        if ((args.length == 1 && args[0].equalsIgnoreCase("mapsave"))
                || (args.length == 2 && args[0].equalsIgnoreCase("world") && args[1].equalsIgnoreCase("save"))) {
            if (!player.isOp()) {
                player.sendMessage("Only server operators can use this command.");
                return true;
            }
            if (!SheepMergeManager.isSheepFarmWorld(player.getWorld())) {
                player.sendMessage("Use this command while standing in a sheep farm world.");
                return true;
            }
            if (!SheepMergeManager.saveSharedFarmLayoutFromWorld(player.getWorld())) {
                player.sendMessage("Unable to save farm layout right now.");
                return true;
            }
            int updated = SheepMergeManager.applySharedFarmLayoutToAllFarmWorlds();
            player.sendMessage("Saved farm layout and applied it to " + updated + " farm world(s).");
            return true;
        }

        if ((args.length == 1 && args[0].equalsIgnoreCase("mapload"))
                || (args.length == 2 && args[0].equalsIgnoreCase("world") && args[1].equalsIgnoreCase("load"))) {
            if (!player.isOp()) {
                player.sendMessage("Only server operators can use this command.");
                return true;
            }
            if (!SheepMergeManager.hasSavedFarmLayout()) {
                player.sendMessage("No saved farm layout found yet. Use /sheepmerge mapsave first.");
                return true;
            }
            int updated = SheepMergeManager.applySharedFarmLayoutToAllFarmWorlds();
            player.sendMessage("Loaded saved farm layout into " + updated + " farm world(s).");
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("resetdata")) {
            if (!player.isOp()) {
                player.sendMessage("Only server operators can use this command.");
                return true;
            }
            Player target = player;
            if (args.length >= 2) {
                Player byName = Bukkit.getPlayerExact(args[1]);
                if (byName != null) {
                    target = byName;
                }
            }
            SheepMergeManager.adminResetPlayer(target);
            player.sendMessage("Reset data for " + target.getName() + ".");
            SheepMergeManager.startTutorial(target, false);
            return true;
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("givepoints")) {
            if (!player.isOp()) {
                player.sendMessage("Only server operators can use this command.");
                return true;
            }
            int amount;
            try {
                amount = Integer.parseInt(args[1]);
            } catch (NumberFormatException exception) {
                player.sendMessage("Invalid amount. Usage: /sheepmerge givepoints <amount> [player]");
                return true;
            }
            Player target = player;
            if (args.length >= 3) {
                Player byName = Bukkit.getPlayerExact(args[2]);
                if (byName != null) {
                    target = byName;
                }
            }
            SheepMergeManager.adminGivePoints(target, amount);
            SheepMergeManager.updatePointsScoreboard(target);
            player.sendMessage("Updated points for " + target.getName() + ".");
            return true;
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("setpoints")) {
            if (!player.isOp()) {
                player.sendMessage("Only server operators can use this command.");
                return true;
            }
            int amount;
            try {
                amount = Integer.parseInt(args[1]);
            } catch (NumberFormatException exception) {
                player.sendMessage("Invalid amount. Usage: /sheepmerge setpoints <amount> [player]");
                return true;
            }
            Player target = player;
            if (args.length >= 3) {
                Player byName = Bukkit.getPlayerExact(args[2]);
                if (byName != null) {
                    target = byName;
                }
            }
            SheepMergeManager.adminSetPoints(target, amount);
            SheepMergeManager.updatePointsScoreboard(target);
            player.sendMessage("Set points for " + target.getName() + ".");
            return true;
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("givequestpoints")) {
            if (!player.isOp()) {
                player.sendMessage("Only server operators can use this command.");
                return true;
            }
            int amount;
            try {
                amount = Integer.parseInt(args[1]);
            } catch (NumberFormatException exception) {
                player.sendMessage("Invalid amount. Usage: /sheepmerge givequestpoints <amount> [player]");
                return true;
            }
            Player target = player;
            if (args.length >= 3) {
                Player byName = Bukkit.getPlayerExact(args[2]);
                if (byName != null) {
                    target = byName;
                }
            }
            SheepMergeManager.adminGiveQuestPoints(target, amount);
            player.sendMessage("Updated quest points for " + target.getName() + ".");
            return true;
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("setprestige")) {
            if (!player.isOp()) {
                player.sendMessage("Only server operators can use this command.");
                return true;
            }
            int level;
            try {
                level = Integer.parseInt(args[1]);
            } catch (NumberFormatException exception) {
                player.sendMessage("Invalid level. Usage: /sheepmerge setprestige <level> [player]");
                return true;
            }
            Player target = player;
            if (args.length >= 3) {
                Player byName = Bukkit.getPlayerExact(args[2]);
                if (byName != null) {
                    target = byName;
                }
            }
            if (!SheepMergeManager.adminSetPrestigeLevel(target, level)) {
                player.sendMessage("Invalid prestige level. Use a value between 0 and "
                        + SheepMergeManager.getPrestigeMaxLevel() + ".");
                return true;
            }
            SheepMergeManager.updatePointsScoreboard(target);
            player.sendMessage("Set prestige for " + target.getName() + " to " + level + ".");
            return true;
        }

        if (!SheepMergeManager.hasUnlockedFarm(player)) {
            SheepMergeManager.startTutorial(player, false);
            return true;
        }

        String worldName = getWorldName(player.getUniqueId());
        World world = ensureFarmWorld(worldName);

        if (world == null) {
            player.sendMessage("Unable to create your sheep farm world right now.");
            return true;
        }

        world.setSpawnLocation(0, 101, 0);
        Location teleportLocation = new Location(world, 0.5, 101, 0.5);
        player.teleport(teleportLocation);
        player.sendMessage("You were teleported to your sheep farm world.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filterSuggestions(ROOT_SUBCOMMANDS, args[0]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("world")) {
            return filterSuggestions(WORLD_SUBCOMMANDS, args[1]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("visit")) {
            List<String> visitOptions = new ArrayList<>();
            visitOptions.add("toggle");
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getName() != null) {
                    visitOptions.add(online.getName());
                }
            }
            return filterSuggestions(visitOptions, args[1]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("kick") && sender instanceof Player player) {
            List<String> kickTargets = new ArrayList<>();
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.equals(player) || online.isOp()) {
                    continue;
                }
                if (online.getWorld().equals(player.getWorld()) && online.getName() != null) {
                    kickTargets.add(online.getName());
                }
            }
            return filterSuggestions(kickTargets, args[1]);
        }

        if (args.length == 3 && sender.isOp() && (args[0].equalsIgnoreCase("givepoints")
                || args[0].equalsIgnoreCase("setpoints")
                || args[0].equalsIgnoreCase("givequestpoints")
                || args[0].equalsIgnoreCase("setprestige"))) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(player -> player.getName())
                    .filter(name -> name != null && name.toLowerCase().startsWith(args[2].toLowerCase()))
                    .toList();
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("resetdata")
                || args[0].equalsIgnoreCase("givepoints")
                || args[0].equalsIgnoreCase("setpoints")
                || args[0].equalsIgnoreCase("givequestpoints")
                || args[0].equalsIgnoreCase("setprestige"))) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(player -> player.getName())
                    .filter(name -> name != null && name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .toList();
        }

        return List.of();
    }

    private List<String> filterSuggestions(List<String> suggestions, String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return suggestions;
        }
        String lowerPrefix = prefix.toLowerCase();
        List<String> matches = new ArrayList<>();
        for (String suggestion : suggestions) {
            if (suggestion.toLowerCase().startsWith(lowerPrefix)) {
                matches.add(suggestion);
            }
        }
        return matches;
    }

    public static World ensureFarmWorld(String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            applyFarmWorldRules(world);
            return world;
        }
        return createFlatWorld(worldName);
    }

    public static void applyFarmRulesToLoadedWorlds() {
        for (World world : Bukkit.getWorlds()) {
            applyFarmWorldRules(world);
        }
    }

    public static World ensureTutorialWorld(java.util.UUID playerId) {
        if (playerId == null) {
            return null;
        }
        return ensureFarmWorld(SheepMergeManager.getTutorialWorldName(playerId));
    }

    public static boolean teleportToFarmWorld(Player player) {
        if (player == null) {
            return false;
        }
        World world = ensureFarmWorld(getWorldName(player.getUniqueId()));
        if (world == null) {
            return false;
        }
        world.setSpawnLocation(0, 101, 0);
        player.teleport(new Location(world, 0.5, 101, 0.5));
        return true;
    }

    public static boolean teleportToTutorialWorld(Player player) {
        if (player == null) {
            return false;
        }
        World world = ensureTutorialWorld(player.getUniqueId());
        if (world == null) {
            return false;
        }
        world.setSpawnLocation(0, 101, 0);
        player.teleport(new Location(world, 0.5, 101, 0.5));
        return true;
    }

    private static World createFlatWorld(String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            applyFarmWorldRules(world);
            return world;
        }
        WorldCreator creator = new WorldCreator(worldName);
        creator.type(WorldType.FLAT);
        creator.generateStructures(false);
        creator.generatorSettings("{" +
                "\"layers\": [{\"block\": \"minecraft:air\", \"height\": 1}]," +
                "\"biome\": \"minecraft:plains\"" +
                "}");
        world = Bukkit.createWorld(creator);
        if (world == null) {
            return null;
        }

        applyFarmWorldRules(world);
        SheepMergeManager.applyFarmLayout(world);
        return world;
    }

    private static void applyFarmWorldRules(World world) {
        if (world == null || !SheepMergeManager.isSheepFarmWorld(world)) {
            return;
        }
        world.setPVP(false);
        world.setDifficulty(Difficulty.PEACEFUL);
    }
}
